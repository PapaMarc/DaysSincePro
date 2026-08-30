package com.merware.dayssincepro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Regression test for a crash where MainActivity.onQueryTextSubmit called listDataAjax()
 * on a tab fragment the ViewPager had never instantiated yet (still null), throwing a
 * NullPointerException on the first search of a session. See MainActivity.dispatchSearch().
 */
public class MainActivityDispatchSearchTest {

    // Records invocations instead of touching any real Android/db code, so this
    // can run as a plain JVM unit test without Robolectric.
    private static class RecordingFragment extends PastFutureListFragment {
        int listDataAjaxCallCount = 0;
        String lastQuery = null;

        @Override
        public void listDataAjax(String str) {
            listDataAjaxCallCount++;
            lastQuery = str;
        }
    }

    @Test
    public void dispatchSearch_callsListDataAjaxOnEveryNonNullFragment() {
        RecordingFragment daysSince = new RecordingFragment();
        RecordingFragment sinceLast = new RecordingFragment();
        RecordingFragment daysUntil = new RecordingFragment();

        MainActivity.dispatchSearch(daysSince, sinceLast, daysUntil, "party");

        assertEquals(1, daysSince.listDataAjaxCallCount);
        assertEquals(1, sinceLast.listDataAjaxCallCount);
        assertEquals(1, daysUntil.listDataAjaxCallCount);
        assertEquals("party", daysSince.lastQuery);
        assertEquals("party", sinceLast.lastQuery);
        assertEquals("party", daysUntil.lastQuery);
    }

    @Test
    public void dispatchSearch_skipsFragmentsNotYetInstantiatedByThePager() {
        RecordingFragment daysSince = new RecordingFragment();
        RecordingFragment sinceLast = new RecordingFragment();

        // simulates the "Until Next" tab never having been scrolled to / instantiated,
        // e.g. searching right after app launch before visiting every tab
        MainActivity.dispatchSearch(daysSince, sinceLast, null, "party");

        assertEquals(1, daysSince.listDataAjaxCallCount);
        assertEquals(1, sinceLast.listDataAjaxCallCount);
    }

    @Test
    public void dispatchSearch_allNull_doesNotThrow() {
        // simulates submitting a search before any tab has ever been instantiated
        MainActivity.dispatchSearch(null, null, null, "party");
        assertTrue(true); // reaching here means no NullPointerException was thrown
    }
}
