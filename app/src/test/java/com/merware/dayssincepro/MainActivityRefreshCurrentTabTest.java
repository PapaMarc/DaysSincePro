package com.merware.dayssincepro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Regression tests for returning from settings where only the currently-visible tab
 * should be refreshed immediately (so updated font size is visible without tab switching).
 */
public class MainActivityRefreshCurrentTabTest {

    private static class RecordingFragment extends PastFutureListFragment {
        int listDataCallCount = 0;

        @Override
        public void listData() {
            listDataCallCount++;
        }
    }

    @Test
    public void refreshCurrentTab_tab0_refreshesOnlyDaysSince() {
        RecordingFragment daysSince = new RecordingFragment();
        RecordingFragment sinceLast = new RecordingFragment();
        RecordingFragment daysUntil = new RecordingFragment();

        MainActivity.refreshCurrentTab(daysSince, sinceLast, daysUntil, 0);

        assertEquals(1, daysSince.listDataCallCount);
        assertEquals(0, sinceLast.listDataCallCount);
        assertEquals(0, daysUntil.listDataCallCount);
    }

    @Test
    public void refreshCurrentTab_tab1_refreshesOnlySinceLast() {
        RecordingFragment daysSince = new RecordingFragment();
        RecordingFragment sinceLast = new RecordingFragment();
        RecordingFragment daysUntil = new RecordingFragment();

        MainActivity.refreshCurrentTab(daysSince, sinceLast, daysUntil, 1);

        assertEquals(0, daysSince.listDataCallCount);
        assertEquals(1, sinceLast.listDataCallCount);
        assertEquals(0, daysUntil.listDataCallCount);
    }

    @Test
    public void refreshCurrentTab_tab2_refreshesOnlyDaysUntil() {
        RecordingFragment daysSince = new RecordingFragment();
        RecordingFragment sinceLast = new RecordingFragment();
        RecordingFragment daysUntil = new RecordingFragment();

        MainActivity.refreshCurrentTab(daysSince, sinceLast, daysUntil, 2);

        assertEquals(0, daysSince.listDataCallCount);
        assertEquals(0, sinceLast.listDataCallCount);
        assertEquals(1, daysUntil.listDataCallCount);
    }

    @Test
    public void refreshCurrentTab_missingVisibleFragment_doesNotThrow() {
        RecordingFragment sinceLast = new RecordingFragment();

        MainActivity.refreshCurrentTab(null, sinceLast, null, 0);

        assertTrue(true);
    }
}
