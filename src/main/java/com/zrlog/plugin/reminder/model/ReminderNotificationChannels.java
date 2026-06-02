package com.zrlog.plugin.reminder.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReminderNotificationChannels {

    public static final String DEFAULT_CHANNELS_KEY = "notificationDefaultChannels";
    public static final String IMPORTANT_CHANNELS_KEY = "notificationImportantChannels";
    public static final String FAILED_CHANNELS_KEY = "notificationFailedChannels";
    private static final List<String> FALLBACK_CHANNELS = Arrays.asList("email");

    private List<String> defaultChannels = new ArrayList<String>(FALLBACK_CHANNELS);
    private List<String> importantChannels = new ArrayList<String>(FALLBACK_CHANNELS);
    private List<String> failedChannels = new ArrayList<String>(FALLBACK_CHANNELS);

    public static ReminderNotificationChannels defaults() {
        return normalize(new ReminderNotificationChannels());
    }

    public static ReminderNotificationChannels normalize(ReminderNotificationChannels channels) {
        ReminderNotificationChannels normalized = channels == null ? new ReminderNotificationChannels() : channels;
        normalized.setDefaultChannels(normalizeChannels(normalized.getDefaultChannels(), FALLBACK_CHANNELS));
        normalized.setImportantChannels(normalizeChannels(normalized.getImportantChannels(), normalized.getDefaultChannels()));
        normalized.setFailedChannels(normalizeChannels(normalized.getFailedChannels(), normalized.getDefaultChannels()));
        return normalized;
    }

    public List<String> channelsFor(ReminderTask task) {
        ReminderNotificationChannels normalized = normalize(this);
        if (task != null && "high".equals(task.getPriority())) {
            return copy(normalized.getImportantChannels());
        }
        return copy(normalized.getDefaultChannels());
    }

    public static List<String> decodeChannels(String text, List<String> fallback) {
        if (text == null || text.trim().isEmpty()) {
            return normalizeChannels(null, fallback);
        }
        return normalizeChannels(Arrays.asList(text.split(",")), fallback);
    }

    public static String encodeChannels(List<String> channels) {
        return String.join(",", normalizeChannels(channels, FALLBACK_CHANNELS));
    }

    private static List<String> normalizeChannels(List<String> channels, List<String> fallback) {
        List<String> values = new ArrayList<String>();
        if (channels != null) {
            for (String channel : channels) {
                if (channel == null) {
                    continue;
                }
                String text = channel.trim();
                if (!text.isEmpty() && !values.contains(text)) {
                    values.add(text);
                }
            }
        }
        if (values.isEmpty()) {
            values.addAll(fallback == null || fallback.isEmpty() ? FALLBACK_CHANNELS : fallback);
        }
        return values;
    }

    private static List<String> copy(List<String> values) {
        return new ArrayList<String>(values == null || values.isEmpty() ? FALLBACK_CHANNELS : values);
    }

    public List<String> getDefaultChannels() {
        return defaultChannels;
    }

    public void setDefaultChannels(List<String> defaultChannels) {
        this.defaultChannels = defaultChannels;
    }

    public List<String> getImportantChannels() {
        return importantChannels;
    }

    public void setImportantChannels(List<String> importantChannels) {
        this.importantChannels = importantChannels;
    }

    public List<String> getFailedChannels() {
        return failedChannels;
    }

    public void setFailedChannels(List<String> failedChannels) {
        this.failedChannels = failedChannels;
    }
}
