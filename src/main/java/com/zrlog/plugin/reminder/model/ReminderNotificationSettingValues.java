package com.zrlog.plugin.reminder.model;

public class ReminderNotificationSettingValues {

    private String notificationDefaultChannels;
    private String notificationImportantChannels;
    private String notificationFailedChannels;

    public String getNotificationDefaultChannels() {
        return notificationDefaultChannels;
    }

    public void setNotificationDefaultChannels(String notificationDefaultChannels) {
        this.notificationDefaultChannels = notificationDefaultChannels;
    }

    public String getNotificationImportantChannels() {
        return notificationImportantChannels;
    }

    public void setNotificationImportantChannels(String notificationImportantChannels) {
        this.notificationImportantChannels = notificationImportantChannels;
    }

    public String getNotificationFailedChannels() {
        return notificationFailedChannels;
    }

    public void setNotificationFailedChannels(String notificationFailedChannels) {
        this.notificationFailedChannels = notificationFailedChannels;
    }
}
