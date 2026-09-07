package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HistoryDateRulesTest {

    @Test
    public void nullOrEmpty_isNotFuture() {
        assertFalse(HistoryDateRules.isFutureHappenedDate(null));
        assertFalse(HistoryDateRules.isFutureHappenedDate(""));
        assertFalse(HistoryDateRules.isFutureHappenedDate("   "));
    }

    @Test
    public void today_isNotFuture() {
        String today = HistoryDateRules.todayIsoDate();
        assertFalse(HistoryDateRules.isFutureHappenedDate(today));
    }

    @Test
    public void tomorrow_isFuture_andYesterdayIsNotFuture() {
        SimpleDate sd = new SimpleDate(HistoryDateRules.todayIsoDate());

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(sd.getDate());
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        String tomorrow = new SimpleDate(cal.getTime()).getDate(SimpleDate.DateStyle.YMD);

        cal.add(java.util.Calendar.DAY_OF_YEAR, -2);
        String yesterday = new SimpleDate(cal.getTime()).getDate(SimpleDate.DateStyle.YMD);

        assertTrue(HistoryDateRules.isFutureHappenedDate(tomorrow));
        assertFalse(HistoryDateRules.isFutureHappenedDate(yesterday));
    }
}
