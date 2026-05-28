package com.zrlog.plugin.reminder.util;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.reminder.model.ReminderEmailResponse;
import com.zrlog.plugin.reminder.model.ReminderTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ReminderEmailUtils {

    private static final int SUCCESS_STATUS = 200;
    private static final Duration EMAIL_SERVICE_TIMEOUT = Duration.ofSeconds(60);

    public static void sendReminder(IOSession session, ReminderTask task) {
        Map<String, String> map = new HashMap<>();
        map.put("title", "[ZrLog Reminder] " + task.getTitle());
        map.put("content", buildContent(task));
        int msgId = session.requestService("emailService", map);
        MsgPacket response = session.getResponseMsgPacketByMsgId(msgId, EMAIL_SERVICE_TIMEOUT);
        if (response == null) {
            throw new IllegalStateException("emailService response timeout");
        }
        if (response.getStatus() != MsgPacketStatus.RESPONSE_SUCCESS) {
            throw new IllegalStateException("emailService response error " + response.getStatus());
        }
        ReminderEmailResponse emailResponse = response.convertToClass(ReminderEmailResponse.class);
        if (emailResponse.getStatus() != SUCCESS_STATUS) {
            throw new IllegalStateException("emailService send failed, status " + emailResponse.getStatus());
        }
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
