package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Regression coverage for fixed-day reminder urgency and cycle-aware recurrence math in
 * OnAlarmReceive.
 */
public class OnAlarmReceiveUrgencyTest {

    @Test
    public void nonRecurringEvent_dueToday_isGreen() {
        assertEquals(OnAlarmReceive.Urgency.GREEN,
        OnAlarmReceive.computeUrgency(0, 0, 0, 3));
    }

    @Test
    public void nonRecurringEvent_nearDueWithinLeadWindow_isYellow() {
    assertEquals(OnAlarmReceive.Urgency.YELLOW,
        OnAlarmReceive.computeUrgency(0, -2, 2, 3));
    }

    @Test
    public void nonRecurringEvent_futureOutsideLeadWindow_isNone() {
    assertEquals(OnAlarmReceive.Urgency.NONE,
        OnAlarmReceive.computeUrgency(0, -10, 10, 3));
    }

    @Test
    public void nonRecurringEvent_afterEventDay_isNone() {
    assertEquals(OnAlarmReceive.Urgency.NONE,
        OnAlarmReceive.computeUrgency(0, 5, 0, 3));
    }

    @Test
    public void recurringEvent_onAnniversary_isGreen() {
        assertEquals(OnAlarmReceive.Urgency.GREEN,
        OnAlarmReceive.computeUrgency(365, 0, 365, 21));
    }

    @Test
    public void recurringEvent_withinLeadWindow_isYellow() {
    // 6 days before the next annual occurrence with 21-day lead window.
    assertEquals(OnAlarmReceive.Urgency.YELLOW,
        OnAlarmReceive.computeUrgency(365, 359, 6, 21));
    }

    @Test
    public void recurringEvent_outsideLeadWindow_isNone() {
        assertEquals(OnAlarmReceive.Urgency.NONE,
        OnAlarmReceive.computeUrgency(365, 30, 335, 21));
    }

    @Test
    public void yearsOldAnnualEvent_evaluatedRelativeToLastOccurrence_isNoLongerPerpetuallyRed() {
    // The actual regression case: urgency is computed relative to current cycle math,
    // not raw days-since-original-date.
        long daysSinceLastOccurrence = 40;
        assertEquals(OnAlarmReceive.Urgency.NONE,
        OnAlarmReceive.computeUrgency(365, daysSinceLastOccurrence, 325, 21));
    }

    @Test
    public void currentCycleCalculations_usesLastOccurrence_notOriginalStoredDate() {
        // End-to-end style check of the helper OnAlarmReceive now uses in both branches:
        // an annual event stored 10 years ago must report a small "days since" figure
        // (relative to this year's anniversary), never a raw ~3650-day figure.
        java.util.Calendar tenYearsAgo = java.util.Calendar.getInstance();
        tenYearsAgo.add(java.util.Calendar.YEAR, -10);
        String storedDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(tenYearsAgo.getTime());

        DaysSinceCalculations dsc = OnAlarmReceive.currentCycleCalculations(storedDate, 365);

        org.junit.Assert.assertTrue(
                "days-since-last-occurrence for a recurring event should stay within one interval, not accumulate across years",
                dsc.getDaysSinceEvent() < 365);
    }
}
