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
        channels.setDefaultChannels(Arrays.asList("email", "webhook"));
        channels.setImportantChannels(null);

        ReminderTask task = new ReminderTask();
        task.setPriority("normal");

        assertEquals(Arrays.asList("email", "webhook"),
                ReminderNotificationChannels.normalize(channels).channelsFor(task));
    }

    @Test
    public void shouldUseImportantChannelsForHighPriorityTask() {
        ReminderNotificationChannels channels = new ReminderNotificationChannels();
        channels.setDefaultChannels(Arrays.asList("email"));
        channels.setImportantChannels(Arrays.asList("email", "sms"));

        ReminderTask task = new ReminderTask();
        task.setPriority("high");

        assertEquals(Arrays.asList("email", "sms"),
                ReminderNotificationChannels.normalize(channels).channelsFor(task));
    }
}
