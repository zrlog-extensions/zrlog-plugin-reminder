package com.zrlog.plugin.reminder.model;

public class ReminderTaskInput {

    private String id;
    private String title;
    private String note;
    private String dueAt;
    private String repeatType;
    private String priority;
    private String status;
    private Boolean emailNotify;
    private Boolean done;
    private Boolean recordOnly;

    public ReminderTask toTask(String defaultStatus) {
        ReminderTask task = new ReminderTask();
        task.setId(text(id));
        task.setTitle(text(title));
        task.setNote(text(note));
        task.setDueAt(text(dueAt));
        task.setRepeatType(text(repeatType));
        task.setPriority(text(priority));
        task.setStatus(text(status).isEmpty() ? defaultStatus : text(status));
        task.setEmailNotify(emailNotify != null && emailNotify);
        return task;
    }

    public boolean recordOnly() {
        return recordOnly != null && recordOnly;
    }

    public boolean done() {
        return done != null && done;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDueAt() {
        return dueAt;
    }

    public void setDueAt(String dueAt) {
        this.dueAt = dueAt;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getEmailNotify() {
        return emailNotify;
    }

    public void setEmailNotify(Boolean emailNotify) {
        this.emailNotify = emailNotify;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Boolean getRecordOnly() {
        return recordOnly;
    }

    public void setRecordOnly(Boolean recordOnly) {
        this.recordOnly = recordOnly;
    }
}
