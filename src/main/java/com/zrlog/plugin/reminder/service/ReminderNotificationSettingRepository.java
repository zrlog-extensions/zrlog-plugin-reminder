package com.zrlog.plugin.reminder.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.SessionKvRepository;
import com.zrlog.plugin.reminder.model.ReminderNotificationChannels;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderNotificationSettingRepository {

    private static final Logger LOGGER = LoggerUtil.getLogger(ReminderNotificationSettingRepository.class);
    private static final ReminderNotificationSettingRepository INSTANCE = new ReminderNotificationSettingRepository();
    private final Gson gson = new Gson();

    public static ReminderNotificationSettingRepository getInstance() {
        return INSTANCE;
    }

    public ReminderNotificationChannels get(IOSession session) {
        try {
            String json = SessionKvRepository.of(session).get(ReminderNotificationChannels.STORE_KEY).orElse("");
            if (!ReminderRepository.notBlank(json)) {
                return ReminderNotificationChannels.defaults();
            }
            return ReminderNotificationChannels.normalize(gson.fromJson(json, ReminderNotificationChannels.class));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "read reminder notification channels from website config error", e);
            return ReminderNotificationChannels.defaults();
        }
    }

    public void save(IOSession session, ReminderNotificationChannels channels) {
        SessionKvRepository.of(session).put(ReminderNotificationChannels.STORE_KEY,
                gson.toJson(ReminderNotificationChannels.normalize(channels)));
    }
}
