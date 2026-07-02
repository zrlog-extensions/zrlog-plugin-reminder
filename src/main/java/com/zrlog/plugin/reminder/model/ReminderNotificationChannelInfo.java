package com.zrlog.plugin.reminder.model;

import com.zrlog.plugin.message.NotificationChannelProvider;

import java.util.List;

public class ReminderNotificationChannelInfo {

    private ReminderNotificationChannels settings;
    private List<NotificationChannelProvider> providers;

    public ReminderNotificationChannels getSettings() {
        return settings;
    }

    public void setSettings(ReminderNotificationChannels settings) {
        this.settings = settings;
    }

    public List<NotificationChannelProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<NotificationChannelProvider> providers) {
        this.providers = providers;
    }
}
