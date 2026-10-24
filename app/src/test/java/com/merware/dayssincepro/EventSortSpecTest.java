package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;

public class EventSortSpecTest {

    @Test
    public void default_isByTabValueAscending() {
        assertEquals(EventSortSpec.Family.TAB_VALUE, EventSortSpec.DEFAULT.family);
        assertEquals(EventSortSpec.Direction.ASCENDING, EventSortSpec.DEFAULT.direction);
    }

    @Test
    public void reversed_preservesFamily_andFlipsDirection() {
        EventSortSpec original = new EventSortSpec(
                EventSortSpec.Family.ALPHABETICAL,
                EventSortSpec.Direction.ASCENDING);

        EventSortSpec reversed = original.reversed();

        assertEquals(EventSortSpec.Family.ALPHABETICAL, reversed.family);
        assertEquals(EventSortSpec.Direction.DESCENDING, reversed.direction);
        assertNotSame(original, reversed);
    }

    @Test
    public void switchingFamily_resetsDirectionToAscending() {
        EventSortSpec original = new EventSortSpec(
                EventSortSpec.Family.ENTRY_ORDER,
                EventSortSpec.Direction.DESCENDING);

        EventSortSpec changed = original.withFamily(EventSortSpec.Family.TAB_VALUE);

        assertEquals(EventSortSpec.Family.TAB_VALUE, changed.family);
        assertEquals(EventSortSpec.Direction.ASCENDING, changed.direction);
    }

    @Test
    public void nullFamily_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventSortSpec(null, EventSortSpec.Direction.ASCENDING));
    }

    @Test
    public void nullDirection_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventSortSpec(EventSortSpec.Family.TAB_VALUE, null));
    }

    @Test
    public void legacyPreferenceMapping_preservesExistingDirectionIntent() {
        assertSpec("0", EventSortSpec.Family.ENTRY_ORDER,
                EventSortSpec.Direction.ASCENDING);
        assertSpec("1", EventSortSpec.Family.ALPHABETICAL,
                EventSortSpec.Direction.ASCENDING);
        assertSpec("2", EventSortSpec.Family.ALPHABETICAL,
                EventSortSpec.Direction.DESCENDING);
        assertSpec("3", EventSortSpec.Family.TAB_VALUE,
                EventSortSpec.Direction.DESCENDING);
        assertSpec("4", EventSortSpec.Family.TAB_VALUE,
                EventSortSpec.Direction.ASCENDING);
    }

    @Test
    public void invalidLegacyPreference_usesNewDefault() {
        assertEquals(EventSortSpec.DEFAULT.family,
                EventSortSpec.fromLegacyPreference("not-a-number").family);
        assertEquals(EventSortSpec.DEFAULT.direction,
                EventSortSpec.fromLegacyPreference("99").direction);
    }

    private void assertSpec(String value,
                            EventSortSpec.Family family,
                            EventSortSpec.Direction direction) {
        EventSortSpec spec = EventSortSpec.fromLegacyPreference(value);
        assertEquals(family, spec.family);
        assertEquals(direction, spec.direction);
    }
}
