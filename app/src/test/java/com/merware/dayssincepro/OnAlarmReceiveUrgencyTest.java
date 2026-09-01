package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Regression coverage for the perpetual-overdue notification bug: OnAlarmReceive's
 * urgency is computed from days elapsed since an event's most recent recurrence, not its
 * original stored date, so a years-old recurring event isn't red/overdue forever.
 */
public class OnAlarmReceiveUrgencyTest {

    private static final double PERCENT = 0.75;

    @Test
    public void nonRecurringEvent_dueToday_isGreen() {
        assertEquals(OnAlarmReceive.Urgency.GREEN,
                OnAlarmReceive.computeUrgency(0, 0, PERCENT));
    }

    @Test
    public void nonRecurringEvent_notToday_isNone() {
        assertEquals(OnAlarmReceive.Urgency.NONE,
                OnAlarmReceive.computeUrgency(5, 0, PERCENT));
    }

    @Test
    public void recurringEvent_onAnniversary_isGreen() {
        assertEquals(OnAlarmReceive.Urgency.GREEN,
                OnAlarmReceive.computeUrgency(0, 365, PERCENT));
    }

    @Test
    public void recurringEvent_wellWithinCycle_isNone() {
        // 30 days into a 365-day cycle - nowhere near due, well below the 75% mark.
        assertEquals(OnAlarmReceive.Urgency.NONE,
                OnAlarmReceive.computeUrgency(30, 365, PERCENT));
    }

    @Test
    public void recurringEvent_pastPercentThresholdAndWithinAWeekOfDue_isYellow() {
        // 75% of 365 = 273.75; 280 days in is ~6.25 days past that threshold, still <= 7.
        assertEquals(OnAlarmReceive.Urgency.YELLOW,
                OnAlarmReceive.computeUrgency(280, 365, PERCENT));
    }

    @Test
    public void recurringEvent_pastPercentThresholdButMoreThanAWeekOut_isNone() {
        // ~16.25 days past the 75% threshold - past the "within a week" window.
        assertEquals(OnAlarmReceive.Urgency.NONE,
                OnAlarmReceive.computeUrgency(290, 365, PERCENT));
    }

    @Test
    public void recurringEvent_pastFullInterval_isRed() {
        assertEquals(OnAlarmReceive.Urgency.RED,
                OnAlarmReceive.computeUrgency(370, 365, PERCENT));
    }

    @Test
    public void yearsOldAnnualEvent_evaluatedRelativeToLastOccurrence_isNoLongerPerpetuallyRed() {
        // The actual regression case: prior to this fix, urgency was computed from raw
        // days-since-ORIGINAL-date (e.g. 10 years = ~3650 days), which always exceeded
        // nEstDays and was permanently RED. Computed relative to the most recent
        // occurrence (as OnAlarmReceive now does via RecurrenceCycle.computeOccurrences),
        // an event 40 days into its current annual cycle is correctly NONE, not RED.
        long daysSinceLastOccurrence = 40;
        assertEquals(OnAlarmReceive.Urgency.NONE,
                OnAlarmReceive.computeUrgency(daysSinceLastOccurrence, 365, PERCENT));
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
