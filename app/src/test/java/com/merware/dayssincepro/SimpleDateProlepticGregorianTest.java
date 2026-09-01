package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Phase 1 audit (DCR_evolveToMaterialDatePicker.md §7.2): verifies every SimpleDateFormat
 * path in SimpleDate.java behaves correctly for years below 1000, and that SimpleDate now
 * agrees with DaysSinceCalculations (fixed in Phase 0) on pre-1582 dates - closing a gap
 * found during this audit where SimpleDate's own formatters were still using the JDK's
 * default Julian-before-1582 cutover after DaysSinceCalculations' had already been fixed.
 */
public class SimpleDateProlepticGregorianTest {

    @Test
    public void singleArgConstructor_roundTripsYearBelow1000() {
        SimpleDate sd = new SimpleDate("0045-03-01");
        assertEquals("0045-03-01", sd.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void twoArgUsConstructor_roundTripsYearBelow1000() {
        SimpleDate sd = new SimpleDate("0045-03-01", SimpleDate.DateStyle.US);
        assertEquals("0045-03-01", sd.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void twoArgUkConstructor_roundTripsYearBelow1000() {
        SimpleDate sd = new SimpleDate("03-01-0045", SimpleDate.DateStyle.UK);
        assertEquals("0045-03-01", sd.getDate(SimpleDate.DateStyle.YMD));
    }

    @Test
    public void intConstructor_acceptsUnpaddedYearMonthDay() {
        // SimpleDate(int, int, int) builds an unpadded "y-m-d" string internally
        // (e.g. "45-3-1") before parsing it - verifies lenient parsing still resolves
        // the year correctly rather than truncating/misreading it.
        SimpleDate sd = new SimpleDate(45, 3, 1);
        assertEquals(45, sd.getYear());
        assertEquals(3, sd.getMonth());
        assertEquals(1, sd.getDay());
    }

    @Test
    public void getYearMonthDay_correctForYearBelow1000() {
        SimpleDate sd = new SimpleDate("0045-03-01");
        assertEquals(45, sd.getYear());
        assertEquals(3, sd.getMonth());
        assertEquals(1, sd.getDay());
    }

    @Test
    public void agreesWithDaysSinceCalculations_onPreCutoverDayCount() {
        // Regression for the cross-class inconsistency found during this audit: before
        // this fix, SimpleDate's formatter still used the JDK's default Julian-before-1582
        // cutover even after DaysSinceCalculations' had been fixed to proleptic Gregorian
        // (Phase 0), meaning the same nominal dates produced different day-counts
        // depending on which class parsed them.
        DaysSinceCalculations viaStrings = new DaysSinceCalculations("1500-02-28", "1500-03-01");

        SimpleDate d1 = new SimpleDate("1500-02-28");
        SimpleDate d2 = new SimpleDate("1500-03-01");
        long daysViaSimpleDate = DaysSinceCalculations.daysBetween(d1.getDate(), d2.getDate());

        assertEquals(1, viaStrings.getDaysSinceEvent());
        assertEquals(viaStrings.getDaysSinceEvent(), daysViaSimpleDate);
    }
}
