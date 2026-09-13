package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EditEventActivityReminderInputTest {

    @Test
    public void parseNotifyLeadDays_null_returnsNull() {
        assertNull(EditEventActivity.parseNotifyLeadDaysOrNull(null));
    }

    @Test
    public void parseNotifyLeadDays_blank_returnsNull() {
        assertNull(EditEventActivity.parseNotifyLeadDaysOrNull("   "));
    }

    @Test
    public void parseNotifyLeadDays_nonNumeric_returnsNull() {
        assertNull(EditEventActivity.parseNotifyLeadDaysOrNull("abc"));
    }

    @Test
    public void parseNotifyLeadDays_outOfRange_returnsNull() {
        assertNull(EditEventActivity.parseNotifyLeadDaysOrNull("-1"));
        assertNull(EditEventActivity.parseNotifyLeadDaysOrNull("31"));
    }

    @Test
    public void parseNotifyLeadDays_inRange_returnsValue() {
        assertEquals(Integer.valueOf(0), EditEventActivity.parseNotifyLeadDaysOrNull("0"));
        assertEquals(Integer.valueOf(21), EditEventActivity.parseNotifyLeadDaysOrNull("21"));
        assertEquals(Integer.valueOf(30), EditEventActivity.parseNotifyLeadDaysOrNull("30"));
    }
}
