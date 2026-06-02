package com.zrlog.plugin.reminder.util;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.NotificationRequest;
import com.zrlog.plugin.render.SimpleTemplateRender;
import com.zrlog.plugin.reminder.model.ReminderNotificationChannels;
import com.zrlog.plugin.reminder.model.ReminderTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ReminderNotificationUtils {

    private static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(60);
    private static final SimpleTemplateRender TEMPLATE_RENDER = new SimpleTemplateRender();

    private ReminderNotificationUtils() {
    }

    public static void publishReminder(IOSession session, ReminderTask task, ReminderNotificationChannels channels) {
        NotificationRequest request = createRequest(task, channels, session.getPlugin().getId(), session.getPlugin().getShortName());
        int msgId = session.publishNotification(request, null);
        MsgPacket response = session.getResponseMsgPacketByMsgId(msgId, NOTIFICATION_TIMEOUT);
        if (response == null) {
            throw new IllegalStateException("notification publish response timeout");
        }
        if (response.getStatus() != MsgPacketStatus.RESPONSE_SUCCESS) {
            throw new IllegalStateException("notification publish failed " + response.getStatus());
        }
    }

    static NotificationRequest createRequest(ReminderTask task,
                                             ReminderNotificationChannels channels,
                                             String sourcePluginId,
                                             String sourcePluginName) {
        NotificationRequest request = new NotificationRequest();
        request.setSourcePluginId(sourcePluginId);
        request.setSourcePluginName(sourcePluginName);
        request.setSourceCapabilityKey("reminder.scanDueTasks");
        request.setEventType("reminder.due");
        request.setNotificationType("reminder");
        request.setChannels(ReminderNotificationChannels.normalize(channels).channelsFor(task));
        request.setTitle("[待办提醒] " + task.getTitle());
        request.setContent(TEMPLATE_RENDER.render("/notification/reminder-due", null, templateData(task)));
        request.setLevel(level(task.getPriority()));
        request.setPayload(payload(task));
        return request;
    }

    private static Map<String, Object> templateData(ReminderTask task) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", escape(task.getTitle()));
        map.put("dueAt", escape(emptyText(task.getDueAt())));
        map.put("priorityText", escape(priorityText(task.getPriority())));
        map.put("note", escape(emptyText(task.getNote())));
        return map;
    }

    private static Map<String, Object> payload(ReminderTask task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("title", task.getTitle());
        map.put("note", task.getNote());
        map.put("dueAt", task.getDueAt());
        map.put("priority", task.getPriority());
        return map;
    }

    private static String level(String priority) {
        if ("high".equals(priority)) {
            return "warning";
        }
        return "info";
    }

    private static String priorityText(String priority) {
        if ("high".equals(priority)) {
            return "高";
        }
        if ("low".equals(priority)) {
            return "低";
        }
        return "普通";
    }

    private static String emptyText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
