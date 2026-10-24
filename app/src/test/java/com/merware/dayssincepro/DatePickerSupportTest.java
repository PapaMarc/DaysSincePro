package com.merware.dayssincepro;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies DatePickerSupport's pure UTC calendar-field/millis conversion logic - the
 * shared configuration used by all 5 MaterialDatePicker call sites (DaysDiffActivity x2,
 * EditEventActivity x2, EditHistory) per DCR_evolveToMaterialDatePicker.md §2/§7.4.
 */
public class DatePickerSupportTest {

    @Test
    public void minDateFloor_isJanuaryFirstYearOne() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(DatePickerSupport.MIN_DATE_UTC_MILLIS);

        assertEquals(1, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void utcMillis_roundTripsThroughToUtcCalendar() {
        long millis = DatePickerSupport.utcMillis(2026, Calendar.SEPTEMBER, 1);
        Calendar cal = DatePickerSupport.toUtcCalendar(millis);

        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(Calendar.SEPTEMBER, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void utcMillis_handlesYearBelow1000() {
        long millis = DatePickerSupport.utcMillis(45, Calendar.MARCH, 1);
        Calendar cal = DatePickerSupport.toUtcCalendar(millis);

        assertEquals(45, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void utcMillis_isTimeZoneIndependent() {
        // Regression guard: since MaterialDatePicker operates in UTC internally, the
        // conversion must not drift a day depending on the JVM's default time zone.
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati")); // UTC+14
            long millisFarEast = DatePickerSupport.utcMillis(2026, Calendar.JANUARY, 1);

            TimeZone.setDefault(TimeZone.getTimeZone("Etc/GMT+12")); // UTC-12
            long millisFarWest = DatePickerSupport.utcMillis(2026, Calendar.JANUARY, 1);

            assertEquals(millisFarEast, millisFarWest);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void isoDateString_zeroPadsYearBelow1000() {
        // Regression test for the "event silently missing from list" bug: the previous
        // hand-rolled `year + "-" + month + "-" + day` concatenation never zero-padded the
        // year, producing "45-01-15" instead of "0045-01-15". Since list/filter queries
        // (e.g. PastFutureListFragment's "date <= 'today'") compare this TEXT column
        // lexicographically, an un-padded year sorts/compares incorrectly against a
        // 4-digit "today" string, silently excluding the row from date-range-filtered views.
        assertEquals("0045-01-15", DatePickerSupport.isoDateString(45, Calendar.JANUARY, 15));
    }

    @Test
    public void isoDateString_zeroPadsYearBelow100AndBelow10() {
        assertEquals("0007-03-04", DatePickerSupport.isoDateString(7, Calendar.MARCH, 4));
        assertEquals("0001-01-01", DatePickerSupport.isoDateString(1, Calendar.JANUARY, 1));
    }

    @Test
    public void isoDateString_zeroPadsMonthAndDay() {
        assertEquals("2026-01-05", DatePickerSupport.isoDateString(2026, Calendar.JANUARY, 5));
    }

    @Test
    public void isoDateString_unaffectedForOrdinaryModernDates() {
        assertEquals("2026-09-01", DatePickerSupport.isoDateString(2026, Calendar.SEPTEMBER, 1));
    }

    @Test
    public void isoDateString_sortsLexicographicallyConsistentWithChronologicalOrder() {
        // The actual property that matters for "date <= 'today'"-style SQL filters: a
        // chronologically earlier date must also be lexicographically smaller as a string.
        String ancient = DatePickerSupport.isoDateString(45, Calendar.JANUARY, 15);
        String modern = DatePickerSupport.isoDateString(2026, Calendar.SEPTEMBER, 1);

        assertTrue(ancient.compareTo(modern) < 0);
    }
}
