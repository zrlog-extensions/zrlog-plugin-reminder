package com.zrlog.plugin.reminder.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReminderNotificationChannels {

    public static final String STORE_KEY = "plugin.reminder.notification.channels";
    public static final String SCHEMA = STORE_KEY;
    private static final List<String> FALLBACK_CHANNELS = Arrays.asList("email");

    private String schema = SCHEMA;
    private int version = 1;
    private ReminderNotificationChannelData data = new ReminderNotificationChannelData();

    public static ReminderNotificationChannels defaults() {
        return normalize(new ReminderNotificationChannels());
    }

    public static ReminderNotificationChannels normalize(ReminderNotificationChannels channels) {
        ReminderNotificationChannels normalized = channels == null ? new ReminderNotificationChannels() : channels;
        normalized.setSchema(SCHEMA);
        if (normalized.getVersion() <= 0) {
            normalized.setVersion(1);
        }
        ReminderNotificationChannelData data = normalized.getData();
        if (data == null) {
            data = new ReminderNotificationChannelData();
            normalized.setData(data);
        }
        data.setDefaultChannels(normalizeChannels(data.getDefaultChannels(), FALLBACK_CHANNELS));
        data.setImportantChannels(normalizeChannels(data.getImportantChannels(), data.getDefaultChannels()));
        data.setFailedChannels(normalizeChannels(data.getFailedChannels(), data.getDefaultChannels()));
        return normalized;
    }

    public List<String> channelsFor(ReminderTask task) {
        ReminderNotificationChannels normalized = normalize(this);
        if (task != null && "high".equals(task.getPriority())) {
            return copy(normalized.getData().getImportantChannels());
        }
        return copy(normalized.getData().getDefaultChannels());
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

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public ReminderNotificationChannelData getData() {
        return data;
    }

    public void setData(ReminderNotificationChannelData data) {
        this.data = data;
    }

    public static class ReminderNotificationChannelData {
        private List<String> defaultChannels = new ArrayList<String>(FALLBACK_CHANNELS);
        private List<String> importantChannels = new ArrayList<String>(FALLBACK_CHANNELS);
        private List<String> failedChannels = new ArrayList<String>(FALLBACK_CHANNELS);

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
}
