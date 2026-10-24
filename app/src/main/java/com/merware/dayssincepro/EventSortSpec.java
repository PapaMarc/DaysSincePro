package com.merware.dayssincepro;

/**
 * User-selected event sort family and direction.
 *
 * The sort family determines which value is used; the direction is reversed by
 * activating the currently selected tab.
 */
final class EventSortSpec {

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
    }

    static final EventSortSpec DEFAULT =
            new EventSortSpec(Family.TAB_VALUE, Direction.ASCENDING);

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

    static EventSortSpec fromLegacyPreference(String value) {
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
}
