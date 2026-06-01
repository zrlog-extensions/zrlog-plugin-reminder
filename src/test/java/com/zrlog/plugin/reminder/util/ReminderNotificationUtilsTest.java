package com.zrlog.plugin.reminder.util;

import com.zrlog.plugin.message.NotificationRequest;
import com.zrlog.plugin.reminder.model.ReminderNotificationChannels;
import com.zrlog.plugin.reminder.model.ReminderTask;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ReminderNotificationUtilsTest {

    @Test
    public void shouldCreateNotificationRequestWithConfiguredChannels() {
        ReminderNotificationChannels channels = new ReminderNotificationChannels();
        ReminderNotificationChannels.ReminderNotificationChannelData data =
                new ReminderNotificationChannels.ReminderNotificationChannelData();
        data.setDefaultChannels(Arrays.asList("email", "webhook"));
        channels.setData(data);

        ReminderTask task = new ReminderTask();
        task.setId("task-1");
        task.setTitle("Release");
        task.setPriority("normal");

        NotificationRequest request = ReminderNotificationUtils.createRequest(task, channels, "reminder", "reminder");

        assertEquals(Arrays.asList("email", "webhook"), request.getChannels());
        assertEquals("reminder.scanDueTasks", request.getSourceCapabilityKey());
        assertEquals("reminder.due", request.getEventType());
        assertEquals("reminder", request.getNotificationType());
    }
}
