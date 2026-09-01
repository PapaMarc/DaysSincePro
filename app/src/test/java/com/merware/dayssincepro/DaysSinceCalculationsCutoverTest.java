package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Regression coverage for the Julian/Gregorian cutover fix: DaysSinceCalculations'
 * yyyy-MM-dd parser is explicitly configured for proleptic Gregorian (no cutover), so
 * day-count math for dates before 1582-10-15 doesn't silently mix Julian and Gregorian
 * leap-year rules with modern (post-cutover) dates.
 *
 * Empirically verified (throwaway JDK experiment, not checked in) that java.util.GregorianCalendar's
 * default Oct 15, 1582 cutover only affects field&lt;-&gt;millis conversion at the point fields are
 * set/read - applying setGregorianChange() to a Calendar that only ever calls setTime()/
 * getTimeInMillis() (as DaysSinceCalculations.daysBetween() does) has no effect at all. The fix
 * must live at the parse call site (the SimpleDateFormat used to turn "yyyy-MM-dd" strings into
 * Date objects), which is what this test targets.
 */
public class DaysSinceCalculationsCutoverTest {

    @Test
    public void daysBetween_usesProlepticGregorianLeapYearRule_beforeCutover() {
        // 1500 is a leap year under the Julian calendar (divisible by 4) but NOT under
        // proleptic Gregorian rules (divisible by 100, not by 400): Feb 1500 has 28 days
        // under proleptic Gregorian (1 day from Feb 28 to Mar 1), vs 29 days (2 day gap)
        // if the default JDK Julian-before-1582 cutover were still in effect.
        DaysSinceCalculations dsc = new DaysSinceCalculations("1500-02-28", "1500-03-01");
        assertEquals(1, dsc.getDaysSinceEvent());
    }

    @Test
    public void daysBetween_stillCorrectForModernPostCutoverDates() {
        DaysSinceCalculations dsc = new DaysSinceCalculations("2024-01-01", "2024-02-01");
        assertEquals(31, dsc.getDaysSinceEvent());
    }

    @Test
    public void daysBetween_correctAcrossLeapYearAfterCutover() {
        // 2024 is a leap year under both Julian and Gregorian rules (not a century year),
        // so this is unaffected by the cutover either way - a sanity check that ordinary
        // leap-year math still works after the change.
        DaysSinceCalculations dsc = new DaysSinceCalculations("2024-02-28", "2024-03-01");
        assertEquals(2, dsc.getDaysSinceEvent());
    }
}
