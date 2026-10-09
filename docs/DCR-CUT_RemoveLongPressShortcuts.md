# DCR: Remove Long-Press Occurrence and Rescheduling Shortcuts

Document ID: DCR-2026-10-09-CUT
Status: Implemented
Author: DaysSincePro Architecture

## 1. Decision

Remove these five event long-press context-menu actions:

- Happened Yesterday
- Happened Today
- To Happen Tomorrow
- Happened Earlier (weekday choices)
- Skip

Keep **History** and **Remove** in the event context menu. Users can still add, edit, and remove History entries through the History screen. This change does not alter existing event or History data, change the database schema, or remove recurrence calculations used by event lists and notifications.

The removed implementation was concentrated in `app/src/main/java/com/merware/dayssincepro/PastFutureListFragment.java`; it was not an independent feature module. The manual History workflow remains in `HistoryActivity.java` and `EditHistory.java`.

## 2. Verified Behavior Before Removal

### History-entry shortcuts

Happened Yesterday, Happened Today, and Happened Earlier inserted a History row directly. The chosen date was relative to the current local date. Happened Earlier selected the most recent prior occurrence of a chosen weekday, from one to seven days earlier.

These shortcuts did not open the History editor, accept a note, or ask the user to set the on-time status. The code initially marked an entry on time, then could mark it off time by comparing the age of the primary event date to the recurrence value. That comparison used the current date rather than the shortcut's selected date and did not use the previous History entry as its reference. The shortcut also silently ignored a duplicate event/date insert; History permits only one row per event per date.

### Primary-date actions

To Happen Tomorrow changed the event's primary `date` to tomorrow. It did not add a History row or set the separate planned date.

Skip calculated `event.date + recur` as a fixed number of days, displayed that calculated date, then wrote it back as the event's primary date. It did not add a History row or calculate from the effective recurrence anchor/current next occurrence. This could diverge from recurrence logic that treats selected interval values as calendar months or years, and could behave unexpectedly for old events or events whose recurrence schedule had been reset by off-time history.

### Preserved History semantics

The manual History workflow is unchanged. A new entry defaults to On Time checked; saving now reads the checkbox's displayed state. Returning from History refreshes the event tabs so Since Last and Until Next reflect saved changes.

For recurring events, current list calculations use the latest History entry for Since Last, while the latest off-time entry is used as the recurrence anchor; absent an off-time entry, the primary event date remains the recurrence anchor. An on-time History entry does not reset that schedule. See `EventTimeline.java` and `EventTimelineTest.java`.

## 3. Scope and Compatibility

- The event context menu retains History and Remove.
- History entry, editing, deletion, and recurrence calculations remain available through their existing paths.
- Existing event dates, History rows, recurrence values, and reminders are not migrated or rewritten.
- Shortcut-only menu identifiers, handlers, date/history helper methods, and one-time-only action checks are removed from `PastFutureListFragment.java`.
- Shortcut-only translations are removed from the main and sideload locale resources. Shared date-display strings and the `untilToSince` event-edit message are retained.
- The previous description in `DCR_DynamicHistoryDepartureFromPrimaryEventCreation.md` has been clarified: opening History does not itself insert a History row.

## 4. Reintroduction Guidance

Do not restore the old handlers unchanged. If user feedback justifies bringing back fast actions, first define the semantics for each action:

1. Keep the original event date as the Days Since anchor unless the user explicitly edits/reschedules that anchor.
2. Define how an occurrence's on-time status is determined, using its selected date and the correct preceding occurrence/schedule rather than today's age from the primary date.
3. Specify how on-time and off-time History entries affect Since Last and Until Next, including recurrence edits and calendar-based intervals.
4. Decide what repeated entry for the same event/date does and give clear feedback when it is not added.
5. Define Skip as a schedule operation or a logged skipped occurrence; do not conflate it with moving the primary event anchor.
6. Define Tomorrow as a planned future occurrence or an explicit anchor edit.
7. Refresh the event tabs after any History change.

If reintroduced, keep menu wiring in the fragment but put date/schedule decisions and persistence behind a small, testable action/service boundary. Add tests for selected-date classification, duplicate handling, recurrence anchors (including off-time history), skip behavior, and tab refresh before exposing the actions again.

## 5. Recovering the Removed Implementation

The removed source is intentionally not kept as uncompiled Java. Git history is the source of record. Locate the removal commit with:

```text
git log --follow -- app/src/main/java/com/merware/dayssincepro/PastFutureListFragment.java
```

Then inspect the revision immediately before removal with:

```text
git show <pre-removal-revision>:app/src/main/java/com/merware/dayssincepro/PastFutureListFragment.java
```

Use that code only as historical reference; implement against the semantics and tests in section 4.
