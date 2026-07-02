package com.zrlog.plugin.reminder.model;

public class ReminderActionResponse {

    private String message;
    private Object surface;

    public ReminderActionResponse() {
    }

    public ReminderActionResponse(String message, Object surface) {
        this.message = message;
        this.surface = surface;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getSurface() {
        return surface;
    }

    public void setSurface(Object surface) {
        this.surface = surface;
    }
}
