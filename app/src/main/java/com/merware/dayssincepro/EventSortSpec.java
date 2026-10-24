package com.merware.dayssincepro;

import android.content.SharedPreferences;

/**
 * User-selected event sort family and direction.
 *
 * The sort family determines which value is used; the direction is reversed by
 * activating the currently selected tab.
 */
final class EventSortSpec {

    static final String PREF_FAMILY = "event_sort_order";
    static final String PREF_DIRECTION = "event_sort_direction";
    static final String PREF_MIGRATION_VERSION = "event_sort_migration_version";
    static final int CURRENT_MIGRATION_VERSION = 1;
    static final String DEFAULT_FAMILY_VALUE = "tab_value";

    enum Family {
        ENTRY_ORDER,
        ALPHABETICAL,
        TAB_VALUE
    }

    enum Direction {
        ASCENDING,
        DESCENDING;

        Direction reversed() {
            return this == ASCENDING ? DESCENDING : ASCENDING;
        }

        static Direction fromPreferenceValue(String value) {
            return "1".equals(value) || "DESCENDING".equals(value)
                    ? DESCENDING : ASCENDING;
        }
    }

    static final EventSortSpec DEFAULT =
            new EventSortSpec(Family.TAB_VALUE, Direction.ASCENDING);

    static void migratePreference(SharedPreferences preferences) {
        if (preferences.getInt(PREF_MIGRATION_VERSION, 0) >= CURRENT_MIGRATION_VERSION) {
            return;
        }

        String currentValue = preferences.getString(PREF_FAMILY, null);
        SharedPreferences.Editor editor = preferences.edit();
        String migratedValue = migratedFamilyValue(currentValue);
        if (!migratedValue.equals(currentValue)) {
            editor.putString(PREF_FAMILY, migratedValue);
            editor.putString(PREF_DIRECTION, "0");
        }
        editor.putInt(PREF_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION).apply();
    }

    static String migratedFamilyValue(String value) {
        if ("entry_order".equals(value)
                || "alphabetical".equals(value)
                || DEFAULT_FAMILY_VALUE.equals(value)) {
            return value;
        }
        return DEFAULT_FAMILY_VALUE;
    }

    final Family family;
    final Direction direction;

    EventSortSpec(Family family, Direction direction) {
        if (family == null) {
            throw new IllegalArgumentException("Sort family must not be null");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Sort direction must not be null");
        }
        this.family = family;
        this.direction = direction;
    }

    EventSortSpec reversed() {
        return new EventSortSpec(family, direction.reversed());
    }

    EventSortSpec withFamily(Family newFamily) {
        return new EventSortSpec(newFamily, Direction.ASCENDING);
    }

    EventSortSpec withDirection(Direction newDirection) {
        return new EventSortSpec(family, newDirection);
    }

    static EventSortSpec fromPreferenceValue(String value) {
        if ("entry_order".equals(value)) {
            return new EventSortSpec(Family.ENTRY_ORDER, Direction.ASCENDING);
        }
        if ("alphabetical".equals(value)) {
            return new EventSortSpec(Family.ALPHABETICAL, Direction.ASCENDING);
        }
        if ("tab_value".equals(value)) {
            return DEFAULT;
        }

        int option;
        try {
            option = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return DEFAULT;
        }

        switch (option) {
            case 0:
                return new EventSortSpec(Family.ENTRY_ORDER, Direction.ASCENDING);
            case 1:
                return new EventSortSpec(Family.ALPHABETICAL, Direction.ASCENDING);
            case 2:
                return new EventSortSpec(Family.ALPHABETICAL, Direction.DESCENDING);
            case 3:
                return new EventSortSpec(Family.TAB_VALUE, Direction.DESCENDING);
            case 4:
                return new EventSortSpec(Family.TAB_VALUE, Direction.ASCENDING);
            default:
                return DEFAULT;
        }
    }

    static EventSortSpec fromLegacyPreference(String value) {
        return fromPreferenceValue(value);
    }
}
