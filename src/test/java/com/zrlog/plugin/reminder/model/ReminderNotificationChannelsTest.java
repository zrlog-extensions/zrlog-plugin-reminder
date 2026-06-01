package com.zrlog.plugin.reminder.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ReminderNotificationChannelsTest {

    @Test
    public void shouldDefaultToEmailChannel() {
        ReminderTask task = new ReminderTask();
        task.setPriority("normal");

        assertEquals(Arrays.asList("email"), ReminderNotificationChannels.defaults().channelsFor(task));
    }

    @Test
    public void shouldUseConfiguredDefaultChannels() {
        ReminderNotificationChannels channels = new ReminderNotificationChannels();
        ReminderNotificationChannels.ReminderNotificationChannelData data =
                new ReminderNotificationChannels.ReminderNotificationChannelData();
        data.setDefaultChannels(Arrays.asList("email", "webhook"));
        data.setImportantChannels(null);
        channels.setData(data);

        ReminderTask task = new ReminderTask();
        task.setPriority("normal");

        assertEquals(Arrays.asList("email", "webhook"),
                ReminderNotificationChannels.normalize(channels).channelsFor(task));
    }

    @Test
    public void shouldUseImportantChannelsForHighPriorityTask() {
        ReminderNotificationChannels channels = new ReminderNotificationChannels();
        ReminderNotificationChannels.ReminderNotificationChannelData data =
                new ReminderNotificationChannels.ReminderNotificationChannelData();
        data.setDefaultChannels(Arrays.asList("email"));
        data.setImportantChannels(Arrays.asList("email", "sms"));
        channels.setData(data);

        ReminderTask task = new ReminderTask();
        task.setPriority("high");

        assertEquals(Arrays.asList("email", "sms"),
                ReminderNotificationChannels.normalize(channels).channelsFor(task));
    }
}
