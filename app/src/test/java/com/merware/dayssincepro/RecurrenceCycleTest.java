package com.merware.dayssincepro;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

/**
 * Verifies RecurrenceCycle's cycle-aware occurrence math - the shared logic behind both
 * the list-view "Since Last"/"Until Next" display (MyEventAdapter) and, as of this change,
 * the notification urgency calculation (OnAlarmReceive), so the two can no longer diverge.
 */
public class RecurrenceCycleTest {

    private Calendar calendarFor(String isoDate) {
        SimpleDate sd = new SimpleDate(isoDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(sd.getDate());
        return cal;
    }

    @Test
    public void nonRecurringEvent_returnsOriginalDateForBothOccurrences() {
        SimpleDate eventDate = new SimpleDate("2020-01-15");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 0, calendarFor("2026-09-01"));

        assertEquals("2020-01-15", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2020-01-15", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void futureEvent_returnsOriginalDateForBothOccurrences() {
        SimpleDate eventDate = new SimpleDate("2030-01-15");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 365, calendarFor("2026-09-01"));

        assertEquals("2030-01-15", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2030-01-15", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void yearsOldAnnualEvent_lastOccurrenceIsThisYearsAnniversary_notOriginalDate() {
        // Regression case for the perpetual-overdue bug: an event created 10 years ago,
        // recurring annually, evaluated on a date that isn't near its anniversary.
        SimpleDate eventDate = new SimpleDate("2016-03-10");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 365, calendarFor("2026-09-01"));

        // Most recent anniversary on/before 2026-09-01 is 2026-03-10, not 2016-03-10.
        assertEquals("2026-03-10", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2027-03-10", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void biweeklyEvent_advancesByFourteenDaysPerCycle() {
        SimpleDate eventDate = new SimpleDate("2026-08-01");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 14, calendarFor("2026-09-01"));

        // 2026-08-01 + 14 = 08-15; +14 = 08-29 (last on/before 09-01); +14 = 09-12 (next).
        assertEquals("2026-08-29", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-09-12", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void weeklyEvent_advancesBySevenDaysPerCycle() {
        SimpleDate eventDate = new SimpleDate("2026-08-25");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 7, calendarFor("2026-09-03"));

        assertEquals("2026-09-01", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-09-08", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void occurrenceLandingExactlyOnNow_countsAsLastOccurrence_notNextOccurrence() {
        // Boundary semantics that directly enable the "due today" GREEN notification case:
        // an occurrence that falls exactly on "now" must be treated as the last (most
        // recent, on/before today) occurrence, not the upcoming one.
        SimpleDate eventDate = new SimpleDate("2026-08-25");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 7, calendarFor("2026-09-01"));

        assertEquals("2026-09-01", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-09-08", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void monthlyEvent_advancesByCalendarMonth_notFixedThirtyDays() {
        // Calendar-field-based advance: Jan 15 + 1 month lands on Feb 15, not a fixed
        // 30-day offset (which would land on Feb 14).
        SimpleDate eventDate = new SimpleDate("2026-01-15");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 30, calendarFor("2026-09-01"));

        assertEquals("2026-08-15", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-09-15", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void monthlyEvent_startingOnDay31_driftsToDay28AfterFebruary() {
        // Documents an existing, unchanged characteristic of calendar-field-based month
        // advancement (verified empirically, not introduced by this change): starting on
        // Jan 31, Calendar.add(MONTH, 1) normalizes Feb to the 28th (2026 is not a leap
        // year), and every subsequent monthly addition continues from that normalized day
        // -  it does not "spring back" to 31 in later months. Sequence from 2026-01-31:
        // 02-28, 03-28, 04-28, ... This is a pre-existing property of the shared
        // addRecurrenceInterval() logic moved here unchanged from MyEventAdapter.
        SimpleDate eventDate = new SimpleDate("2026-01-31");
        RecurrenceCycle.Occurrences occ =
                RecurrenceCycle.computeOccurrences(eventDate, 30, calendarFor("2026-09-01"));

        assertEquals("2026-08-28", occ.lastOccurrence.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-09-28", occ.nextOccurrence.getDate(SimpleDate.DateStyle.YMD));
    }
}
