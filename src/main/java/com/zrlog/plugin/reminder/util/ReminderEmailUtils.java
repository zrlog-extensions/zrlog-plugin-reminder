package com.zrlog.plugin.reminder.util;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.reminder.model.ReminderTask;

import java.util.HashMap;
import java.util.Map;

public class ReminderEmailUtils {

    public static void sendReminder(IOSession session, ReminderTask task) {
        Map<String, String> map = new HashMap<>();
        map.put("title", "[ZrLog Reminder] " + task.getTitle());
        map.put("content", buildContent(task));
        session.requestService("emailService", map);
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
