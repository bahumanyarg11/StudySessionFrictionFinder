package com.friction.service;

import com.friction.model.StudySession;
import com.friction.model.StudyTask;
import com.friction.model.TaskStatus;
import com.friction.util.DateUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Builds an explainable plan for a future date from saved history:
 *  - unfinished tasks (PARTIAL / PENDING / SKIPPED) from the last 7 days are carried forward
 *  - if a subject+type usually runs over its estimate, the suggested time is increased
 * The reason for each suggestion is stored in the task's note.
 */
public class RecommendationEngine {
    private static final int LOOKBACK_DAYS = 7;
    private static final int HISTORY_DAYS = 30;

    private final SessionHistoryService historyService;
    private final AnalyticsService analytics;

    public RecommendationEngine(SessionHistoryService historyService, AnalyticsService analytics) {
        this.historyService = historyService;
        this.analytics = analytics;
    }

    public List<StudyTask> recommend(LocalDate targetDate) throws SQLException {
        LocalDate dayBefore = targetDate.minusDays(1);

        // 1. Average overrun ratio per "Subject Type" from the last 30 days
        List<StudyTask> history = new ArrayList<>();
        for (StudySession s : historyService.getSessions(targetDate.minusDays(HISTORY_DAYS), dayBefore)) {
            history.addAll(s.getTasks());
        }
        Map<String, Double> ratios = new HashMap<>();
        for (Map.Entry<String, List<StudyTask>> e : analytics.groupBySubjectAndType(history).entrySet()) {
            ratios.put(e.getKey(), analytics.averageRatio(e.getValue()));
        }

        // 2. Latest unfinished version of each task (later completion removes it).
        // Name alone is not enough: two different subjects can use the same task name.
        Map<String, StudyTask> unfinished = new LinkedHashMap<>();
        Map<String, LocalDate> lastSeen = new HashMap<>();
        for (StudySession s : historyService.getSessions(targetDate.minusDays(LOOKBACK_DAYS), dayBefore)) {
            for (StudyTask t : s.getTasks()) {
                String key = taskKey(t);
                if (t.isUnfinished()) {
                    unfinished.put(key, t);
                    lastSeen.put(key, s.getDate());
                } else {
                    unfinished.remove(key);
                }
            }
        }

        // 3. Turn each unfinished task into a suggestion with a reason
        List<StudyTask> plan = new ArrayList<>();
        for (Map.Entry<String, StudyTask> e : unfinished.entrySet()) {
            StudyTask old = e.getValue();
            String from = DateUtil.format(lastSeen.get(e.getKey()));
            int minutes = old.getPlannedMinutes();
            String reason;

            if (old.getStatus() == TaskStatus.PARTIAL) {
                minutes = Math.max(old.getPlannedMinutes() - old.getActualMinutes(), 15);
                reason = "Incomplete task from " + from + " (" + minutes + " min remaining)";
            } else if (old.getStatus() == TaskStatus.SKIPPED) {
                reason = "Previously planned but skipped on " + from;
            } else {
                reason = "Not started on " + from;
            }

            double ratio = ratios.getOrDefault(AnalyticsService.groupKey(old), 1.0);
            if (ratio > 1.1 && old.getStatus() != TaskStatus.PARTIAL) {
                minutes = roundTo5(minutes * ratio);
                reason += "; " + AnalyticsService.groupKey(old) + " usually exceeds estimate";
            }

            StudyTask suggestion = new StudyTask(old.getName(), old.getSubject(), old.getType(),
                    minutes, old.getPriority());
            suggestion.setNote(reason);
            plan.add(suggestion);
        }

        // HIGH priority first, then larger tasks first
        plan.sort(Comparator.comparing(StudyTask::getPriority)
                .thenComparing(StudyTask::getPlannedMinutes, Comparator.reverseOrder()));
        return plan;
    }

    private int roundTo5(double minutes) {
        return (int) (Math.round(minutes / 5.0) * 5);
    }

    /** Stable identity of a task across days, independent of letter case. */
    private static String taskKey(StudyTask task) {
        return (task.getName() + "|" + task.getSubject() + "|" + task.getType()).toLowerCase(Locale.ROOT);
    }
}
