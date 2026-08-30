package com.merware.dayssincepro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Regression test for a crash where MainActivity.onActivityResult (add-event flow) called
 * listData() on a tab fragment that the ViewPager had never instantiated yet (still null),
 * throwing a NullPointerException. See MainActivity.refreshTabs().
 */
public class MainActivityRefreshTabsTest {

    // Records invocations instead of touching any real Android/db code, so this
    // can run as a plain JVM unit test without Robolectric.
    private static class RecordingFragment extends PastFutureListFragment {
        int listDataCallCount = 0;

        @Override
        public void listData() {
            listDataCallCount++;
        }
    }

    @Test
    public void refreshTabs_callsListDataOnEveryNonNullFragment() {
        RecordingFragment daysSince = new RecordingFragment();
        RecordingFragment sinceLast = new RecordingFragment();
        RecordingFragment daysUntil = new RecordingFragment();

        MainActivity.refreshTabs(daysSince, sinceLast, daysUntil);

        assertEquals(1, daysSince.listDataCallCount);
        assertEquals(1, sinceLast.listDataCallCount);
        assertEquals(1, daysUntil.listDataCallCount);
    }

    @Test
    public void refreshTabs_skipsFragmentsNotYetInstantiatedByThePager() {
        RecordingFragment daysSince = new RecordingFragment();
        RecordingFragment sinceLast = new RecordingFragment();

        // simulates the "Until Next" tab never having been scrolled to / instantiated
        MainActivity.refreshTabs(daysSince, sinceLast, null);

        assertEquals(1, daysSince.listDataCallCount);
        assertEquals(1, sinceLast.listDataCallCount);
    }

    @Test
    public void refreshTabs_allNull_doesNotThrow() {
        // simulates returning from Add-event before any tab has ever been instantiated
        MainActivity.refreshTabs(null, null, null);
        assertTrue(true); // reaching here means no NullPointerException was thrown
    }
}
