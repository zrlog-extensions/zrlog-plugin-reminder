package com.zrlog.plugin.reminder.model;

import com.zrlog.plugin.message.Plugin;

public class ReminderPageData {

    private boolean dark;
    private String adminColorPrimary;
    private Plugin plugin;
    private Object tasks;
    private ReminderNotificationChannels notificationChannels;

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public String getAdminColorPrimary() {
        return adminColorPrimary;
    }

    public void setAdminColorPrimary(String adminColorPrimary) {
        this.adminColorPrimary = adminColorPrimary;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public Object getTasks() {
        return tasks;
    }

    public void setTasks(Object tasks) {
        this.tasks = tasks;
    }

    public ReminderNotificationChannels getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(ReminderNotificationChannels notificationChannels) {
        this.notificationChannels = notificationChannels;
    }
}
