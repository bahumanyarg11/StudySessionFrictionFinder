package com.friction.ui;

import com.friction.model.StudySession;
import com.friction.model.StudyTask;
import com.friction.model.TaskStatus;
import com.friction.report.ReportGenerator;
import com.friction.service.RecommendationEngine;
import com.friction.service.SessionHistoryService;
import com.friction.service.StudyPlanService;
import com.friction.util.DateUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Shows the menu and calls the services. No SQL or calculations here. */
public class MenuController {
    private final StudyPlanService planService;
    private final SessionHistoryService historyService;
    private final ReportGenerator dailyReport;
    private final ReportGenerator weeklyReport;
    private final RecommendationEngine recommendationEngine;
    private final InputHelper input = new InputHelper();

    public MenuController(StudyPlanService planService, SessionHistoryService historyService,
                          ReportGenerator dailyReport, ReportGenerator weeklyReport,
                          RecommendationEngine recommendationEngine) {
        this.planService = planService;
        this.historyService = historyService;
        this.dailyReport = dailyReport;
        this.weeklyReport = weeklyReport;
        this.recommendationEngine = recommendationEngine;
    }

    public void start() {
        while (true) {
            printMenu();
            int choice = input.readInt("Enter your choice: ", 0, 6);
            if (choice == 0) {
                System.out.println("Goodbye!");
                return;
            }
            try {
                switch (choice) {
                    case 1 -> createPlan();
                    case 2 -> showHistory();
                    case 3 -> updateSession();
                    case 4 -> System.out.println(dailyReport.generate(input.readDate("Date (DD-MM-YYYY)", LocalDate.now())));
                    case 5 -> System.out.println(weeklyReport.generate(input.readDate("Any date in the week (DD-MM-YYYY)", LocalDate.now())));
                    case 6 -> recommendedPlan();
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("      STUDY SESSION FRICTION FINDER");
        System.out.println("========================================");
        System.out.println("  1. Create Study Plan");
        System.out.println("  2. Session History");
        System.out.println("  3. Update Session");
        System.out.println("  4. Daily Report");
        System.out.println("  5. Weekly Report");
        System.out.println("  6. Recommended Plan");
        System.out.println("  0. Exit");
        System.out.println("========================================");
    }

    // ---------- 1. Create Study Plan ----------
    private void createPlan() throws SQLException {
        System.out.println("========== CREATE STUDY PLAN ==========");
        LocalDate date = input.readDate("Date (DD-MM-YYYY)", LocalDate.now());
        if (planService.planExists(date)) {
            System.out.println("A plan already exists for " + DateUtil.format(date) + ". Use Update Session instead.");
            return;
        }
        StudySession session = new StudySession(date);
        do {
            System.out.println();
            String name = input.readText("Task name: ", StudyTask.MAX_NAME_LENGTH);
            String subject = input.readText("Subject: ", StudyTask.MAX_SUBJECT_LENGTH);
            String type = input.readText("Task type: ", StudyTask.MAX_TYPE_LENGTH);
            int minutes = input.readInt("Estimated duration (minutes): ", 1, 1440);
            session.addTask(new StudyTask(name, subject, type, minutes, input.readPriority("Priority (High/Medium/Low): ")));
        } while (input.readYesNo("\nAdd another task? (Y/N): "));

        planService.createPlan(session);
        System.out.println("\nPlan created successfully.");
        System.out.println("Tasks added: " + session.getTasks().size());
        System.out.println("Total planned time: " + session.getTotalPlannedMinutes() + " minutes");
    }

    // ---------- 2. Session History ----------
    private void showHistory() throws SQLException {
        System.out.println("========== SESSION HISTORY ==========");
        LocalDate date = input.readDate("Enter date (DD-MM-YYYY)", LocalDate.now());
        StudySession session = historyService.getSession(date);
        if (session == null) {
            System.out.println("No study plan found for " + DateUtil.format(date) + ".");
            return;
        }
        System.out.printf("%n%-28s %8s %8s   %s%n", "Task", "Planned", "Actual", "Status");
        System.out.println("-".repeat(60));
        int[] counts = new int[TaskStatus.values().length];
        for (StudyTask t : session.getTasks()) {
            System.out.printf("%-28s %4d min %4d min   %s%n", t.getName(), t.getPlannedMinutes(),
                    t.getActualMinutes(), t.getStatus());
            counts[t.getStatus().ordinal()]++;
        }
        System.out.printf("%nCompleted: %d | Partial: %d | Pending: %d | Skipped: %d%n",
                counts[TaskStatus.COMPLETED.ordinal()], counts[TaskStatus.PARTIAL.ordinal()],
                counts[TaskStatus.PENDING.ordinal()], counts[TaskStatus.SKIPPED.ordinal()]);
    }

    // ---------- 3. Update Session ----------
    private void updateSession() throws SQLException {
        System.out.println("========== UPDATE SESSION ==========");
        LocalDate date = input.readDate("Date (DD-MM-YYYY)", LocalDate.now());
        StudySession session = historyService.getSession(date);
        if (session == null) {
            System.out.println("No study plan found for " + DateUtil.format(date) + ".");
            return;
        }
        List<StudyTask> tasks = session.getTasks();
        System.out.println("Tasks:");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.printf("%d. %-28s [%s]%n", i + 1, tasks.get(i).getName(), tasks.get(i).getStatus());
        }
        StudyTask task = tasks.get(input.readInt("\nSelect task: ", 1, tasks.size()) - 1);

        System.out.println("New status:");
        TaskStatus[] options = {TaskStatus.COMPLETED, TaskStatus.PARTIAL, TaskStatus.SKIPPED, TaskStatus.PENDING};
        for (int i = 0; i < options.length; i++) System.out.println((i + 1) + ". " + options[i]);
        TaskStatus status = options[input.readInt("Choice: ", 1, options.length) - 1];

        int actual = 0;
        if (status != TaskStatus.SKIPPED) {
            System.out.println("(Enter the TOTAL time spent so far; currently " + task.getActualMinutes() + " min)");
            actual = input.readInt("Actual time spent (minutes): ", 0, 1440);
        }
        String note = input.readOptional("Optional note: ", StudyTask.MAX_NOTE_LENGTH);

        planService.updateTask(task, status, actual, note);
        System.out.println("\nTask updated successfully.");
    }

    // ---------- 6. Recommended Plan ----------
    private void recommendedPlan() throws SQLException {
        LocalDate target = input.readDate("Target date (DD-MM-YYYY)", LocalDate.now().plusDays(1));
        if (planService.planExists(target)) {
            System.out.println("A plan already exists for " + DateUtil.format(target) + "; it will not be overwritten.");
            return;
        }
        List<StudyTask> plan = recommendationEngine.recommend(target);
        if (plan.isEmpty()) {
            System.out.println("No unfinished tasks in the last 7 days - nothing to recommend.");
            return;
        }

        while (true) {
            printRecommendation(target, plan);
            int choice = input.readInt("Enter your choice: ", 1, 3);
            if (choice == 3 || plan.isEmpty()) {
                System.out.println("Recommendation discarded.");
                return;
            }
            if (choice == 1) {
                StudySession session = new StudySession(target);
                for (StudyTask t : plan) session.addTask(t);
                planService.createPlan(session);
                System.out.println("Recommended plan saved for " + DateUtil.format(target) + ".");
                return;
            }
            editRecommendation(plan);
        }
    }

    private void printRecommendation(LocalDate target, List<StudyTask> plan) {
        System.out.println("\n======= RECOMMENDED PLAN =======");
        System.out.println("Target date: " + DateUtil.format(target) + "\n");
        int total = 0;
        for (int i = 0; i < plan.size(); i++) {
            StudyTask t = plan.get(i);
            System.out.println((i + 1) + ". " + t.getName() + " (" + t.getSubject() + ", " + t.getPriority() + ")");
            System.out.println("   Reason: " + t.getNote());
            System.out.println("   Suggested time: " + t.getPlannedMinutes() + " min\n");
            total += t.getPlannedMinutes();
        }
        System.out.println("Total suggested time: " + total + " min\n");
        System.out.println("1. Accept and save");
        System.out.println("2. Edit tasks");
        System.out.println("3. Return to menu");
        System.out.println("================================");
    }

    private void editRecommendation(List<StudyTask> plan) {
        StudyTask task = plan.get(input.readInt("Task number to edit: ", 1, plan.size()) - 1);
        System.out.println("1. Change suggested time\n2. Remove task");
        if (input.readInt("Choice: ", 1, 2) == 1) {
            task.setPlannedMinutes(input.readInt("New time (minutes): ", 1, 1440));
        } else {
            plan.remove(task);
            System.out.println("Task removed.");
        }
    }
}
