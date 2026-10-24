package com.merware.dayssincepro;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

/**
 * Calculates tab-aware sort keys in Java and exposes the ordered rows through
 * the cursor contract used by the existing event adapter.
 */
final class EventRowSorter {

    private EventRowSorter() {
    }

    static Cursor reorder(Context context,
                          Cursor source,
                          TabKind kind,
                          EventSortSpec sortSpec) {
        String[] columns = source.getColumnNames();
        List<Row> rows = new ArrayList<>(source.getCount());
        Calendar now = Calendar.getInstance();

        int originalPosition = source.getPosition();
        try {
            source.moveToPosition(-1);
            while (source.moveToNext()) {
                Object[] values = copyCurrentRow(source);
                EventPresentation presentation = EventPresentation.fromCursor(
                        context, source, now);
                rows.add(new Row(
                        source.getLong(source.getColumnIndexOrThrow("_id")),
                        source.getString(source.getColumnIndexOrThrow("event")),
                        presentation,
                        values));
            }
        } finally {
            source.moveToPosition(originalPosition);
        }

        rows.sort(comparator(kind, sortSpec));

        MatrixCursor ordered = new MatrixCursor(columns, rows.size());
        for (Row row : rows) {
            ordered.addRow(row.values);
        }
        return ordered;
    }

    private static Comparator<Row> comparator(TabKind kind, EventSortSpec sortSpec) {
        Comparator<Row> primary;
        switch (sortSpec.family) {
            case ENTRY_ORDER:
                primary = Comparator.comparingLong(row -> row.id);
                break;
            case ALPHABETICAL:
                primary = Comparator.comparing(
                        row -> row.eventName == null ? "" : row.eventName,
                        String.CASE_INSENSITIVE_ORDER);
                break;
            case TAB_VALUE:
                primary = Comparator.comparingLong(row -> tabValue(row.presentation, kind));
                break;
            default:
                throw new IllegalStateException("Unsupported sort family "
                        + sortSpec.family);
        }

        if (sortSpec.direction == EventSortSpec.Direction.DESCENDING) {
            primary = primary.reversed();
        }

        Comparator<Row> tieBreaker = Comparator
                .comparing((Row row) -> row.eventName == null ? "" : row.eventName,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(row -> row.id);
        return primary.thenComparing(tieBreaker);
    }

    private static long tabValue(EventPresentation presentation, TabKind kind) {
        switch (kind) {
            case DaysSince:
                return presentation.daysSinceValue();
            case SinceLast:
                return presentation.sinceLastValue();
            case DaysUntil:
                return presentation.untilNextValue();
            default:
                throw new IllegalStateException("Unsupported tab kind " + kind);
        }
    }

    private static Object[] copyCurrentRow(Cursor source) {
        Object[] values = new Object[source.getColumnCount()];
        for (int index = 0; index < values.length; index++) {
            switch (source.getType(index)) {
                case Cursor.FIELD_TYPE_NULL:
                    values[index] = null;
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values[index] = source.getLong(index);
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    values[index] = source.getDouble(index);
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    values[index] = source.getBlob(index);
                    break;
                case Cursor.FIELD_TYPE_STRING:
                default:
                    values[index] = source.getString(index);
                    break;
            }
        }
        return values;
    }

    private static final class Row {
        final long id;
        final String eventName;
        final EventPresentation presentation;
        final Object[] values;

        Row(long id, String eventName, EventPresentation presentation, Object[] values) {
            this.id = id;
            this.eventName = eventName;
            this.presentation = presentation;
            this.values = values;
        }
    }
}
