package com.zrlog.plugin.reminder.model;

public class ReminderApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ReminderApiResponse() {
    }

    private ReminderApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ReminderApiResponse<T> success(T data) {
        return new ReminderApiResponse<T>(true, null, data);
    }

    public static ReminderApiResponse<Void> error(String message) {
        return new ReminderApiResponse<Void>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
