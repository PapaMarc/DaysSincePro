package com.merware.dayssincepro;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OnAlarmReceiveSuppressionTest {

    @Test
    public void recurringEvent_lastNotifiedWithinCurrentCycle_isSuppressed() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.SEPTEMBER, 1, 10, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        RecurrenceCycle.Occurrences occurrences =
                OnAlarmReceive.currentCycleOccurrences("2026-08-01", 14, now);

        assertTrue(OnAlarmReceive.alreadyNotifiedInCurrentCycle(
                "2026-08-29",
                occurrences.lastOccurrence,
                occurrences.nextOccurrence,
                14,
                "2026-09-01"));
    }

    @Test
    public void recurringEvent_lastNotifiedBeforeCurrentCycle_isNotSuppressed() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.SEPTEMBER, 1, 10, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        RecurrenceCycle.Occurrences occurrences =
                OnAlarmReceive.currentCycleOccurrences("2026-08-01", 14, now);

        assertFalse(OnAlarmReceive.alreadyNotifiedInCurrentCycle(
                "2026-08-15",
                occurrences.lastOccurrence,
                occurrences.nextOccurrence,
                14,
                "2026-09-01"));
    }

    @Test
    public void oneTimeEvent_notifiedToday_isSuppressed() {
        assertTrue(OnAlarmReceive.alreadyNotifiedInCurrentCycle(
                "2026-09-01",
                new SimpleDate("2026-09-01"),
                new SimpleDate("2026-09-01"),
                0,
                "2026-09-01"));
    }

    @Test
    public void oneTimeEvent_notifiedOnDifferentDay_isNotSuppressed() {
        assertFalse(OnAlarmReceive.alreadyNotifiedInCurrentCycle(
                "2026-08-31",
                new SimpleDate("2026-09-01"),
                new SimpleDate("2026-09-01"),
                0,
                "2026-09-01"));
    }
}
