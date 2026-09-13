package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PastFutureListFragmentReminderResetTest {

    @Test
    public void dateChange_resetsLastNotifiedDate() {
        assertTrue(PastFutureListFragment.shouldResetLastNotifiedDate(
                "2026-09-01", 365, "2026-10-01", 365));
    }

    @Test
    public void recurrenceChange_resetsLastNotifiedDate() {
        assertTrue(PastFutureListFragment.shouldResetLastNotifiedDate(
                "2026-09-01", 365, "2026-09-01", 30));
    }

    @Test
    public void unchangedDateAndRecurrence_doesNotResetLastNotifiedDate() {
        assertFalse(PastFutureListFragment.shouldResetLastNotifiedDate(
                "2026-09-01", 365, "2026-09-01", 365));
    }
}
