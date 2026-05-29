package com.zrlog.plugin.reminder.service;

import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.reminder.model.ReminderTask;
import com.zrlog.plugin.reminder.util.ReminderNotificationUtils;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReminderScheduler {

    private static final Logger LOGGER = LoggerUtil.getLogger(ReminderScheduler.class);

    public static int remindDueTasks(IOSession session) {
        return scanDueTasks(session).getSuccessCount();
    }

    public static ReminderScanResult scanDueTasks(IOSession session) {
        ReminderScanResult result = new ReminderScanResult();
        int count = 0;
        List<ReminderTask> tasks = ReminderRepository.getInstance().dueTasks(session, System.currentTimeMillis());
        result.setDueCount(tasks.size());
        for (ReminderTask task : tasks) {
            try {
                ReminderNotificationUtils.publishReminder(session, task);
                ReminderRepository.getInstance().markReminded(session, task.getId());
                count++;
            } catch (Exception e) {
                result.failed();
                LOGGER.log(Level.SEVERE, "publish reminder notification error", e);
            }
        }
        result.setSuccessCount(count);
        return result;
    }

    public static class ReminderScanResult {
        private int dueCount;
        private int successCount;
        private int failedCount;

        public int getDueCount() {
            return dueCount;
        }

        public void setDueCount(int dueCount) {
            this.dueCount = dueCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public void failed() {
            failedCount++;
        }
    }
}
