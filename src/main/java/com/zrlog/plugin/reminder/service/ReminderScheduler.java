package com.zrlog.plugin.reminder.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.reminder.model.ReminderTask;
import com.zrlog.plugin.reminder.util.ReminderEmailUtils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderScheduler {

    private static final Logger LOGGER = LoggerUtil.getLogger(ReminderScheduler.class);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static ScheduledExecutorService executorService;

    public static void start(final IOSession session) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                remindDueTasks(session);
            }
        }, 15, 60, TimeUnit.SECONDS);
    }

    public static void stop() {
        if (!STARTED.compareAndSet(true, false)) {
            return;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    public static int remindDueTasks(IOSession session) {
        int count = 0;
        List<ReminderTask> tasks = ReminderRepository.getInstance().dueTasks(session, System.currentTimeMillis());
        for (ReminderTask task : tasks) {
            try {
                ReminderEmailUtils.sendReminder(session, task);
                ReminderRepository.getInstance().markReminded(session, task.getId());
                count++;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "send reminder email error", e);
            }
        }
        return count;
    }
}
