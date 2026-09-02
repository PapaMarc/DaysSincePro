package com.merware.dayssincepro;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventTimelineTest {

    private Calendar calendarFor(String isoDate) {
        SimpleDate sd = new SimpleDate(isoDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(sd.getDate());
        return cal;
    }

    @Test
    public void endedRecurringEvent_keepsDaysSinceAtInception_andSinceLastAtEndDate() {
        EventTimeline.Snapshot snapshot = EventTimeline.compute(
                new SimpleDate("2019-08-03"),
                "2021-08-15",
                90,
                calendarFor("2026-09-01"));

        assertEquals("2019-08-03", snapshot.daysSinceReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2021-08-15", snapshot.sinceLastReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertFalse(snapshot.hasFutureOccurrence);
    }

    @Test
    public void activeRecurringEvent_usesCurrentCycleDatesForSinceLastAndUntilNext() {
        EventTimeline.Snapshot snapshot = EventTimeline.compute(
                new SimpleDate("2019-08-03"),
                null,
                90,
                calendarFor("2026-09-01"));

        assertEquals("2026-08-03", snapshot.sinceLastReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-11-03", snapshot.untilNextReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertTrue(snapshot.hasFutureOccurrence);
    }

    @Test
    public void recurringEventWithEndBeforeNextOccurrence_hasNoFutureOccurrence() {
        EventTimeline.Snapshot snapshot = EventTimeline.compute(
                new SimpleDate("2026-08-03"),
                "2026-10-15",
                90,
                calendarFor("2026-09-01"));

        assertEquals("2026-11-03", snapshot.untilNextReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertFalse(snapshot.hasFutureOccurrence);
    }

    @Test
    public void oneTimeFutureEvent_hasFutureOccurrenceAtStartDate() {
        EventTimeline.Snapshot snapshot = EventTimeline.compute(
                new SimpleDate("2026-11-10"),
                null,
                0,
                calendarFor("2026-09-01"));

        assertTrue(snapshot.hasFutureOccurrence);
        assertEquals("2026-11-10", snapshot.untilNextReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-11-10", snapshot.sinceLastReferenceDate.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void oneTimePastEventWithEndDate_usesEndForSinceLast() {
        EventTimeline.Snapshot snapshot = EventTimeline.compute(
                new SimpleDate("2020-01-01"),
                "2020-01-15",
                0,
                calendarFor("2026-09-01"));

        assertEquals("2020-01-01", snapshot.daysSinceReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2020-01-15", snapshot.sinceLastReferenceDate.getDate(SimpleDate.DateStyle.YMD));
        assertFalse(snapshot.hasFutureOccurrence);
    }
}
