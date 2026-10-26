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

    @Test
    public void recurringToday_usesAdjacentOccurrenceForBoundaryDirections() {
        EventPresentation presentation = EventPresentation.compute(
                null,
                "1967-10-26",
                null,
                365,
                null,
                null,
                null,
                calendarFor("2026-10-26"));

        assertTrue(presentation.recurringToday);
        assertEquals("2025-10-26", presentation.displayDate(
                TabKind.SinceLast, EventSortSpec.Direction.DESCENDING)
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals(365, presentation.boundaryExplanationCalculation(
                null,
                TabKind.SinceLast,
                new EventSortSpec(EventSortSpec.Family.TAB_VALUE,
                        EventSortSpec.Direction.DESCENDING))
                .getDaysSinceEvent());
        assertEquals(0, presentation.boundaryExplanationCalculation(
                null,
                TabKind.SinceLast,
                new EventSortSpec(EventSortSpec.Family.TAB_VALUE,
                        EventSortSpec.Direction.ASCENDING))
                .getDaysSinceEvent());
        assertEquals(0, presentation.boundaryExplanationCalculation(
                null,
                TabKind.DaysUntil,
                new EventSortSpec(EventSortSpec.Family.TAB_VALUE,
                        EventSortSpec.Direction.ASCENDING))
                .getDaysSinceEvent());
        assertEquals(-365, presentation.boundaryExplanationCalculation(
                null,
                TabKind.DaysUntil,
                new EventSortSpec(EventSortSpec.Family.TAB_VALUE,
                        EventSortSpec.Direction.DESCENDING))
                .getDaysSinceEvent());
        assertEquals("2026-10-26", presentation.displayDate(
                TabKind.DaysUntil, EventSortSpec.Direction.ASCENDING)
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals("2027-10-26", presentation.displayDate(
                TabKind.DaysUntil, EventSortSpec.Direction.DESCENDING)
                .getDate(SimpleDate.DateStyle.YMD));
        assertEquals(0, EventRowSorter.dayZeroPriority(presentation, TabKind.SinceLast));
        assertEquals(0, EventRowSorter.dayZeroPriority(presentation, TabKind.DaysUntil));
    }

    @Test
    public void oneTimeToday_doesNotReceiveRecurringBoundaryTreatment() {
        EventPresentation presentation = EventPresentation.compute(
                null,
                "2026-10-26",
                null,
                0,
                null,
                null,
                null,
                calendarFor("2026-10-26"));

        assertTrue(!presentation.recurringToday);
        assertTrue(!presentation.hasRecurringTodayPriority(TabKind.SinceLast));
        assertEquals("2026-10-26", presentation.displayDate(
                TabKind.DaysUntil, EventSortSpec.Direction.ASCENDING)
                .getDate(SimpleDate.DateStyle.YMD));
    }
}
