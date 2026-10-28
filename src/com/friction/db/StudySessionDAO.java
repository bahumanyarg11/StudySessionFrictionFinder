package com.friction.db;

import com.friction.model.Priority;
import com.friction.model.StudySession;
import com.friction.model.StudyTask;
import com.friction.model.TaskStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JDBC implementation of StudySessionRepository. All SQL lives here. */
public class StudySessionDAO implements StudySessionRepository {

    private static final String SELECT_BETWEEN =
            "SELECT s.session_id, s.session_date, " +
            "t.task_id, t.task_name, t.subject, t.task_type, t.planned_minutes, " +
            "t.actual_minutes, t.priority, t.status, t.note " +
            "FROM study_sessions s " +
            "LEFT JOIN study_tasks t ON t.session_id = s.session_id " +
            "WHERE s.session_date BETWEEN ? AND ? ORDER BY s.session_date, t.task_id";

    @Override
    public int save(StudySession session) throws SQLException {
        String insertSession = "INSERT INTO study_sessions (session_date) VALUES (?)";
        String insertTask = "INSERT INTO study_tasks (session_id, task_name, subject, task_type, " +
                "planned_minutes, actual_minutes, priority, status, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false); // transaction: either the whole plan is saved or nothing
            try (PreparedStatement ps = con.prepareStatement(insertSession, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDate(1, Date.valueOf(session.getDate()));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    session.setId(keys.getInt(1));
                }
                try (PreparedStatement taskPs = con.prepareStatement(insertTask, Statement.RETURN_GENERATED_KEYS)) {
                    for (StudyTask t : session.getTasks()) {
                        taskPs.setInt(1, session.getId());
                        taskPs.setString(2, t.getName());
                        taskPs.setString(3, t.getSubject());
                        taskPs.setString(4, t.getType());
                        taskPs.setInt(5, t.getPlannedMinutes());
                        taskPs.setInt(6, t.getActualMinutes());
                        taskPs.setString(7, t.getPriority().name());
                        taskPs.setString(8, t.getStatus().name());
                        taskPs.setString(9, t.getNote());
                        taskPs.executeUpdate();
                        try (ResultSet keys = taskPs.getGeneratedKeys()) {
                            if (keys.next()) {
                                t.setId(keys.getInt(1));
                            }
                        }
                        t.setSessionId(session.getId());
                    }
                }
                con.commit();
                return session.getId();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    @Override
    public StudySession findByDate(LocalDate date) throws SQLException {
        List<StudySession> sessions = findBetween(date, date);
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    @Override
    public List<StudySession> findBetween(LocalDate from, LocalDate to) throws SQLException {
        // LinkedHashMap keeps sessions in date order while grouping their tasks
        Map<Integer, StudySession> sessions = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BETWEEN)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int sessionId = rs.getInt("session_id");
                    StudySession session = sessions.get(sessionId);
                    if (session == null) {
                        session = new StudySession(rs.getDate("session_date").toLocalDate());
                        session.setId(sessionId);
                        sessions.put(sessionId, session);
                    }
                    if (rs.getObject("task_id") != null) {
                        session.addTask(mapTask(rs));
                    }
                }
            }
        }
        return new ArrayList<>(sessions.values());
    }

    @Override
    public void updateTask(StudyTask task) throws SQLException {
        String sql = "UPDATE study_tasks SET status = ?, actual_minutes = ?, note = ? WHERE task_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, task.getStatus().name());
            ps.setInt(2, task.getActualMinutes());
            ps.setString(3, task.getNote());
            ps.setInt(4, task.getId());
            ps.executeUpdate();
        }
    }

    private StudyTask mapTask(ResultSet rs) throws SQLException {
        StudyTask t = new StudyTask(
                rs.getString("task_name"),
                rs.getString("subject"),
                rs.getString("task_type"),
                rs.getInt("planned_minutes"),
                Priority.valueOf(rs.getString("priority")));
        t.setId(rs.getInt("task_id"));
        t.setSessionId(rs.getInt("session_id"));
        t.setActualMinutes(rs.getInt("actual_minutes"));
        t.setStatus(TaskStatus.valueOf(rs.getString("status")));
        t.setNote(rs.getString("note"));
        return t;
    }
}
