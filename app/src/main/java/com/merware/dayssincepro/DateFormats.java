package com.merware.dayssincepro;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Factory for SimpleDateFormat instances configured for proleptic Gregorian dates (no
 * Julian/Gregorian cutover), so day-count math and date display for pre-1582 dates don't
 * silently mix calendar systems.
 *
 * The fix must live here (at parse/format time), not in downstream Calendar arithmetic
 * that only ever calls setTime()/getTimeInMillis() - GregorianCalendar's cutover only
 * affects field&lt;-&gt;millis conversion, which happens when a formatter parses a date
 * string or formats a Date back into field values (see DaysSinceCalculationsCutoverTest).
 */
final class DateFormats {

    private DateFormats() {
    }

    static SimpleDateFormat prolepticGregorian(String pattern) {
        SimpleDateFormat fmt = new SimpleDateFormat(pattern);
        ((GregorianCalendar) fmt.getCalendar()).setGregorianChange(new Date(Long.MIN_VALUE));
        return fmt;
    }
}
