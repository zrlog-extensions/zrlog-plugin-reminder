package com.zrlog.plugin.reminder.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReminderRepositoryTest {

    @Test
    public void shouldNormalizeRepeatType() {
        assertEquals("none", ReminderRepository.normalizeRepeatType(null));
        assertEquals("none", ReminderRepository.normalizeRepeatType("cron"));
        assertEquals("yearly", ReminderRepository.normalizeRepeatType("yearly"));
        assertTrue(ReminderRepository.isRecurring("monthly"));
        assertFalse(ReminderRepository.isRecurring("none"));
    }

    @Test
    public void shouldAdvanceYearlyReminderToNextOccurrence() {
        long now = ReminderRepository.parseTime("2026-06-02 10:00:00", -1);

        String nextDueAt = ReminderRepository.nextDueAt("2026-05-20 09:30:00", "yearly", now);

        assertEquals("2027-05-20 09:30:00", nextDueAt);
    }

    @Test
    public void shouldAdvanceMonthlyReminderPastCurrentTime() {
        long now = ReminderRepository.parseTime("2026-06-02 10:00:00", -1);

        String nextDueAt = ReminderRepository.nextDueAt("2026-04-01 08:00:00", "monthly", now);

        assertEquals("2026-07-01 08:00:00", nextDueAt);
    }
}
