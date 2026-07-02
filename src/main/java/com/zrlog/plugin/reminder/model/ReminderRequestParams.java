package com.zrlog.plugin.reminder.model;

import com.google.gson.Gson;

import java.util.function.Function;

public class ReminderRequestParams {

    private String actionRef;
    private ReminderTaskInput values;
    private ReminderTaskInput task;
    private String id;
    private String title;
    private String note;
    private String dueAt;
    private String repeatType;
    private String priority;
    private String status;
    private Boolean emailNotify;
    private Boolean done;
    private Object defaultChannels;
    private Object importantChannels;
    private Object failedChannels;

    public static ReminderRequestParams fromParams(Function<String, Object> paramValue, Gson gson) {
        ReminderRequestParams request = new ReminderRequestParams();
        request.setActionRef(stringValue(paramValue.apply("actionRef")));
        request.setValues(taskInput(paramValue.apply("values"), gson));
        request.setDefaultChannels(paramValue.apply("defaultChannels"));
        request.setImportantChannels(paramValue.apply("importantChannels"));
        request.setFailedChannels(paramValue.apply("failedChannels"));
        request.setId(stringValue(paramValue.apply("id")));
        request.setTitle(stringValue(paramValue.apply("title")));
        request.setNote(stringValue(paramValue.apply("note")));
        request.setDueAt(stringValue(paramValue.apply("dueAt")));
        request.setRepeatType(stringValue(paramValue.apply("repeatType")));
        request.setPriority(stringValue(paramValue.apply("priority")));
        request.setStatus(stringValue(paramValue.apply("status")));
        request.setEmailNotify(booleanValue(paramValue.apply("emailNotify")));
        request.setDone(booleanValue(paramValue.apply("done")));
        return request;
    }

    public ReminderTaskInput effectiveTask() {
        if (values != null) {
            return values;
        }
        if (task != null) {
            return task;
        }
        ReminderTaskInput input = new ReminderTaskInput();
        input.setId(id);
        input.setTitle(title);
        input.setNote(note);
        input.setDueAt(dueAt);
        input.setRepeatType(repeatType);
        input.setPriority(priority);
        input.setStatus(status);
        input.setEmailNotify(emailNotify);
        input.setDone(done);
        return input;
    }

    private static ReminderTaskInput taskInput(Object raw, Gson gson) {
        Object value = firstValue(raw);
        if (value instanceof ReminderTaskInput) {
            return (ReminderTaskInput) value;
        }
        String text = stringValue(value);
        if (text.startsWith("{")) {
            ReminderTaskInput input = gson.fromJson(text, ReminderTaskInput.class);
            return input == null ? null : input;
        }
        if (value != null && !(value instanceof String)) {
            ReminderTaskInput input = gson.fromJson(gson.toJson(value), ReminderTaskInput.class);
            return input == null ? null : input;
        }
        return null;
    }

    private static Object firstValue(Object value) {
        if (value instanceof String[]) {
            String[] values = (String[]) value;
            return values.length == 0 ? "" : values[0];
        }
        if (value instanceof Object[]) {
            Object[] values = (Object[]) value;
            return values.length == 0 ? "" : values[0];
        }
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                return item;
            }
            return "";
        }
        return value;
    }

    private static String stringValue(Object value) {
        Object first = firstValue(value);
        return first == null ? "" : String.valueOf(first);
    }

    private static Boolean booleanValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = stringValue(value);
        return "true".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text) || "1".equals(text);
    }

    public String getActionRef() {
        return actionRef;
    }

    public void setActionRef(String actionRef) {
        this.actionRef = actionRef;
    }

    public ReminderTaskInput getValues() {
        return values;
    }

    public void setValues(ReminderTaskInput values) {
        this.values = values;
    }

    public ReminderTaskInput getTask() {
        return task;
    }

    public void setTask(ReminderTaskInput task) {
        this.task = task;
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

    public Object getDefaultChannels() {
        return defaultChannels;
    }

    public void setDefaultChannels(Object defaultChannels) {
        this.defaultChannels = defaultChannels;
    }

    public Object getImportantChannels() {
        return importantChannels;
    }

    public void setImportantChannels(Object importantChannels) {
        this.importantChannels = importantChannels;
    }

    public Object getFailedChannels() {
        return failedChannels;
    }

    public void setFailedChannels(Object failedChannels) {
        this.failedChannels = failedChannels;
    }
}
