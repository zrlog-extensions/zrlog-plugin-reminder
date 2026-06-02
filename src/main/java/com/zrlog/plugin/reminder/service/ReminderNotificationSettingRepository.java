package com.zrlog.plugin.reminder.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SessionKvRepository;
import com.zrlog.plugin.reminder.model.ReminderNotificationChannels;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderNotificationSettingRepository {

    private static final Logger LOGGER = LoggerUtil.getLogger(ReminderNotificationSettingRepository.class);
    private static final ReminderNotificationSettingRepository INSTANCE = new ReminderNotificationSettingRepository();

    public static ReminderNotificationSettingRepository getInstance() {
        return INSTANCE;
    }

    public ReminderNotificationChannels get(IOSession session) {
        try {
            Map<String, Object> values = SessionKvRepository.of(session).read(
                    ReminderNotificationChannels.DEFAULT_CHANNELS_KEY,
                    ReminderNotificationChannels.IMPORTANT_CHANNELS_KEY,
                    ReminderNotificationChannels.FAILED_CHANNELS_KEY);
            ReminderNotificationChannels channels = new ReminderNotificationChannels();
            channels.setDefaultChannels(ReminderNotificationChannels.decodeChannels(
                    stringValue(values.get(ReminderNotificationChannels.DEFAULT_CHANNELS_KEY)), null));
            channels.setImportantChannels(ReminderNotificationChannels.decodeChannels(
                    stringValue(values.get(ReminderNotificationChannels.IMPORTANT_CHANNELS_KEY)),
                    channels.getDefaultChannels()));
            channels.setFailedChannels(ReminderNotificationChannels.decodeChannels(
                    stringValue(values.get(ReminderNotificationChannels.FAILED_CHANNELS_KEY)),
                    channels.getDefaultChannels()));
            return ReminderNotificationChannels.normalize(channels);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "read reminder notification channels from website config error", e);
            return ReminderNotificationChannels.defaults();
        }
    }

    public void save(IOSession session, ReminderNotificationChannels channels) {
        ReminderNotificationChannels normalized = ReminderNotificationChannels.normalize(channels);
        Map<String, String> values = new HashMap<>();
        values.put(ReminderNotificationChannels.DEFAULT_CHANNELS_KEY,
                ReminderNotificationChannels.encodeChannels(normalized.getDefaultChannels()));
        values.put(ReminderNotificationChannels.IMPORTANT_CHANNELS_KEY,
                ReminderNotificationChannels.encodeChannels(normalized.getImportantChannels()));
        values.put(ReminderNotificationChannels.FAILED_CHANNELS_KEY,
                ReminderNotificationChannels.encodeChannels(normalized.getFailedChannels()));
        SessionKvRepository.of(session).write(values);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
