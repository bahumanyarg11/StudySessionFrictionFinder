package com.friction.model;

/** One to-do item inside a daily study plan. */
public class StudyTask {
    // Limits match the column sizes in database/schema.sql
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_SUBJECT_LENGTH = 50;
    public static final int MAX_TYPE_LENGTH = 50;
    public static final int MAX_NOTE_LENGTH = 255;

    private int id;
    private int sessionId;
    private String name;
    private String subject;
    private String type;
    private int plannedMinutes;
    private int actualMinutes;
    private Priority priority;
    private TaskStatus status;
    private String note;

    /** Creates a new PENDING task (not saved yet). */
    public StudyTask(String name, String subject, String type, int plannedMinutes, Priority priority) {
        setName(name);
        setSubject(subject);
        setType(type);
        setPlannedMinutes(plannedMinutes);
        this.priority = priority;
        this.status = TaskStatus.PENDING;
        this.actualMinutes = 0;
    }

    /** Positive = took longer than planned, negative = finished early. */
    public int getOverrun() {
        return actualMinutes - plannedMinutes;
    }

    public boolean isUnfinished() {
        return status != TaskStatus.COMPLETED;
    }

    // ---------- getters / setters with validation ----------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Task name cannot be empty.");
        String value = name.trim();
        if (value.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Task name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        this.name = value;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) {
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Subject cannot be empty.");
        String value = subject.trim();
        if (value.length() > MAX_SUBJECT_LENGTH) {
            throw new IllegalArgumentException("Subject must be at most " + MAX_SUBJECT_LENGTH + " characters.");
        }
        this.subject = value;
    }

    public String getType() { return type; }
    public void setType(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Task type cannot be empty.");
        String value = type.trim();
        if (value.length() > MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException("Task type must be at most " + MAX_TYPE_LENGTH + " characters.");
        }
        this.type = value;
    }

    public int getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(int plannedMinutes) {
        if (plannedMinutes <= 0) throw new IllegalArgumentException("Planned duration must be positive.");
        this.plannedMinutes = plannedMinutes;
    }

    public int getActualMinutes() { return actualMinutes; }
    /** Total time spent so far (replaces the old value, so repeated updates never double-count). */
    public void setActualMinutes(int actualMinutes) {
        if (actualMinutes < 0) throw new IllegalArgumentException("Actual time cannot be negative.");
        this.actualMinutes = actualMinutes;
    }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) {
        if (note == null || note.isBlank()) {
            this.note = null;
            return;
        }
        String value = note.trim();
        if (value.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Note must be at most " + MAX_NOTE_LENGTH + " characters.");
        }
        this.note = value;
    }
}
