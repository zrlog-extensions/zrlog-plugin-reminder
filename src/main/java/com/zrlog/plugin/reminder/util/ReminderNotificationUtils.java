package com.zrlog.plugin.reminder.util;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.message.NotificationRequest;
import com.zrlog.plugin.reminder.model.ReminderNotificationChannels;
import com.zrlog.plugin.reminder.model.ReminderTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ReminderNotificationUtils {

    private static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(60);

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
        request.setTitle("[ZrLog Reminder] " + task.getTitle());
        request.setContent(buildContent(task));
        request.setLevel(level(task.getPriority()));
        request.setPayload(payload(task));
        return request;
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

    private static String buildContent(ReminderTask task) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;line-height:1.7\">");
        builder.append("<h2 style=\"margin:0 0 12px\">").append(escape(task.getTitle())).append("</h2>");
        if (task.getDueAt() != null && !task.getDueAt().isEmpty()) {
            builder.append("<p><strong>截止时间：</strong>").append(escape(task.getDueAt())).append("</p>");
        }
        builder.append("<p><strong>优先级：</strong>").append(escape(priorityText(task.getPriority()))).append("</p>");
        if (task.getNote() != null && !task.getNote().isEmpty()) {
            builder.append("<p style=\"white-space:pre-wrap\">").append(escape(task.getNote())).append("</p>");
        }
        builder.append("</div>");
        return builder.toString();
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
