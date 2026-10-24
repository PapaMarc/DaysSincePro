package com.merware.dayssincepro;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventPresentationTest {

    private Calendar calendarFor(String isoDate) {
        SimpleDate date = new SimpleDate(isoDate);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date.getDate());
        return calendar;
    }

    @Test
    public void recurringPresentation_reusesTimelineReferences() {
        EventPresentation presentation = EventPresentation.compute(
                null,
                "2019-08-03",
                null,
                90,
                null,
                "2026-08-03",
                null,
                calendarFor("2026-09-01"));

        assertEquals("2019-08-03", presentation.timeline.daysSinceReferenceDate
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-08-03", presentation.sinceLastDate
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-11-03", presentation.nextDate
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals(presentation.timeline.nextOccurrence,
                presentation.untilNextDisplayDate());
    }

    @Test
    public void futurePlannedOneTimePresentation_usesPlannedDateForUntilNext() {
        EventPresentation presentation = EventPresentation.compute(
                null,
                "2020-01-01",
                null,
                0,
                "2099-01-15",
                null,
                null,
                calendarFor("2026-09-01"));

        assertTrue(presentation.hasPlannedFutureDate);
        assertEquals("2099-01-15", presentation.untilNextDisplayDate()
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals(presentation.plannedCalculation.getDaysSinceEvent(),
                presentation.untilNextCalculation.getDaysSinceEvent());
    }

    @Test
    public void offTimeHistoryPresentation_usesHistoryAsRecurrenceAnchor() {
        EventPresentation presentation = EventPresentation.compute(
                null,
                "2026-05-25",
                null,
                120,
                null,
                "2026-10-08",
                "2026-10-08",
                calendarFor("2026-10-08"));

        assertEquals("2027-02-05", presentation.nextDate
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2026-10-08", presentation.sinceLastDate
                .getDate(SimpleDate.DateStyle.YMD));
    }
}
