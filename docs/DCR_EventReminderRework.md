# Design Change Request (DCR): Event Reminder Threshold Rework

**Document ID:** DCR-2026-09-13-EventReminderRework
**Status:** Draft
**Author:** DaysSincePro Architecture
**Target release:** TBD

---

## 1. Problem Statement

The current reminder model uses a global percent-of-interval threshold for recurring events. This causes unintuitive behavior for longer recurrences.

Observed example:

1. Annual recurring event.
2. Last occurrence was 278 days ago.
3. Global threshold is 75 percent of interval.
4. 278 / 365 is about 76 percent.
5. Event is classified as near due earlier than many users expect for annual reminders.

This can be reasonable for short recurrences (such as weekly), but less intuitive for increasingly long recurrences, and borders on absurd in the annual event case. Further, there's probably more or less lead time which deserves custom per event differentiation where reminder intent differs by event type (for example taxes vs birthdays could use varying lead time for notification to be useful in practice).

---

## 2. Existing Model Concerns

1. A single global threshold drives all recurrence categories.
2. Percent-based trigger timing is not intuitive for longer intervals.
3. Annual events with different intent are treated the same.
4. Notification urgency and list color behavior are threshold-driven and should remain consistent with each other.
5. Settings currently expose only percent-based reminder control, which becomes obsolete under a fixed-day model.

Current implementation touchpoints:

1. Notification urgency logic: [OnAlarmReceive.java](../app/src/main/java/com/merware/dayssincepro/OnAlarmReceive.java)
2. List color/urgency logic: [MyEventAdapter.java](../app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java)
3. Recurrence cycle semantics: [RecurrenceCycle.java](../app/src/main/java/com/merware/dayssincepro/RecurrenceCycle.java)
4. Settings wiring: [options.xml](../app/src/main/res/xml/options.xml), [PrefActivity.java](../app/src/main/java/com/merware/dayssincepro/PrefActivity.java), [arrays.xml](../app/src/main/res/values/arrays.xml)

---

## 3. Proposed Solution

Replace percent-based thresholding with fixed-day lead thresholds.

Core model:

1. Define static default lead days per recurrence type.
2. In Add/Edit Event, display the default for the selected recurrence.
3. Allow optional per-event override between 0 and 30 days.
4. Persist override per event when modified.
5. Add a per-event notification enable/disable control in Add/Edit Event.
6. Keep notification and color urgency logic tied to the same effective lead-days value.
7. Remove percent threshold preference from Settings.
8. Do not add a new global days preference in Settings.
9. Do not backfill or retrofit old events; existing events default naturally.

---

## 4. Functional Requirements

1. Recurrence defaults are static and centrally defined.
2. Per-event override is optional and bounded to 0 through 30 inclusive.
3. Effective lead-days resolution:
4. If `event.notify_lead_days` is non-null, use it as the event's custom lead days.
5. If `event.notify_lead_days` is null, event inherits recurrence default lead days.
6. `event.notify_lead_days` is never populated with copied recurrence defaults; null is the canonical inherited/default state.
7. Per-event notification toggle controls whether notifications are emitted for that event.
8. Notification precedence is evaluated in this order:
9. If global notifications are off, do not notify.
10. Else if per-event notifications are disabled, do not notify for that event.
11. Else evaluate due/near-due from effective lead days.
12. Both notification near-due and color near-due decisions use the same effective lead-days resolver.
13. Color urgency is independent of `notify_enabled`; disabling notifications for an event suppresses banners/alerts only, not list color urgency.
14. Due/overdue behavior remains cycle-aware and recurrence-based.
15. One Time Event behavior uses the fixed-day model: near-due in the configured pre-event window, due on event day, overdue after event day.
16. Add/Edit Event UI indicates lead-days source (Default or Custom) and notification enabled state.
17. Event details context displays effective lead days, lead-days source, and notification enabled state.
18. Settings no longer expose percent thresholding.
19. Clamping rule: for Number of Days recurrence only, effective lead days are clamped to the recurrence interval (`effective_lead_days = min(configured_lead_days, recurrence_interval_days)`).
20. Named recurrence types (One Time, Weekly, BiWeekly, Monthly, Quarterly, Semi-annually, Annual) are not clamped by interval length.

---

## 4A. Default Lead-Days Table

Static defaults by recurrence type:

| Recurrence type | Default lead days |
| --------------- | ----------------- |
| One Time Event  | 3                 |
| Weekly          | 3                 |
| BiWeekly        | 3                 |
| Monthly         | 7                 |
| Quarterly       | 7                 |
| Semi-annually   | 21                |
| Annual          | 21                |

Special rule for Number of Days recurrence:

1. If recurrence interval is greater than 14 days, default lead days is 7.
2. If recurrence interval is 14 down to 3 days, default lead days is 3.
3. If recurrence interval is 2 days, default lead days is 2.
4. If recurrence interval is 1 day, default lead days is 1.
5. If recurrence interval is 0 days, default lead days is 0.

Implementation note:

1. These values are defaults only and must not be written into `event.notify_lead_days` for inherited/default behavior; inherited/default remains represented by null.
2. Number of Days custom lead values are subject to runtime clamping by interval length (see §4 item 19).

---

## 5. Non-Goals

1. No CSV roundtrip for per-event lead-days override.
2. No migration backfill to assign explicit override values to historical rows.
3. No multi-reminder ladder behavior in this phase.
4. No split threshold model between notifications and colors.
5. No notification-channel UX tuning in this DCR (sound/vibration/importance/channel taxonomy changes are out of scope).

---

## 6. Data Model and Migration

Per-event persistence requires schema change.

Proposed new columns:

1. `event.notify_lead_days INTEGER NULL`
2. `NULL` means use recurrence default.
3. Non-null values must validate to 0 through 30.
4. `event.notify_enabled INTEGER NOT NULL DEFAULT 1`
5. `1` means notifications enabled for this event; `0` means disabled.
6. `notify_enabled` is separate from `notify_lead_days` to avoid sentinel coupling and to preserve custom lead-days values while disabled.

Lead-days null semantics (required):

1. Inherited/default behavior is always represented by `notify_lead_days = NULL`.
2. Explicit custom behavior is represented by `notify_lead_days` in range 0 through 30.
3. The system must not write recurrence default values into `notify_lead_days` just to materialize defaults.
4. Clearing custom lead days from the UI must write `NULL`.
5. Disabling notifications must not clear or overwrite `notify_lead_days`; re-enabling restores prior custom value (or inherited default if null).

Notification precedence semantics:

1. Global notifications off -> suppress all event notifications.
2. Global notifications on and `notify_enabled = 0` -> suppress that event's notifications.
3. Global notifications on and `notify_enabled = 1` -> evaluate near-due/due from effective lead days.

Recurrence-change behavior in Add/Edit:

1. If event is using inherited/default lead days (`notify_lead_days = NULL`) and recurrence changes, effective lead days should recalculate from the new recurrence default automatically.
2. If event has custom lead days (`notify_lead_days` non-null) and recurrence changes, preserve the persisted custom value unless the user explicitly confirms a new value.
3. When user opens lead-days edit after a recurrence change, the dialog/input may prefill with the new recurrence default to make re-baselining easy, but this prefill must remain non-committal until user confirms.
4. Canceling lead-days edit leaves previously persisted custom lead days unchanged.
5. Lead-days field focus/open must not silently persist changes.
6. Recurrence change must not silently clear custom lead-days overrides.

Migration approach:

1. Increment schema version in [DatabaseHelper.java](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java).
2. Add additive migration steps for both new columns.
3. No backfill required.
4. Existing events continue using recurrence defaults via null lead-days state and default enabled notifications.
5. Migration tests must assert `notify_enabled = 1` for all pre-existing rows after upgrade.

Concrete migration SQL (reference):

```sql
ALTER TABLE event ADD COLUMN notify_lead_days INTEGER;
ALTER TABLE event ADD COLUMN notify_enabled INTEGER NOT NULL DEFAULT 1;
```

Compatibility implications:

1. Older installed databases migrate forward on open.
2. Restored database files that include the new schema preserve values naturally.

---

## 7. Import/Export Behavior

CSV:

1. Do not add `notify_lead_days` to CSV format.
2. Do not add `notify_enabled` to CSV format.
3. CSV import/export intentionally ignores both fields.

CSV full-export warning behavior:

1. Preserve existing warning behavior when full CSV export is requested and history rows exist (`history` count > 0).
2. Extend the same warning path to also trigger when at least one event has a custom notification lead-days override (`event.notify_lead_days` is non-null).
3. Extend the same warning path to also trigger when at least one event has event-level notifications disabled (`event.notify_enabled = 0`).
4. Warning should trigger when any condition is true, including combinations of conditions.
5. Warning copy should clearly explain that CSV export omits history entries, per-event custom notification lead-days overrides, and per-event notification enabled/disabled state, and recommend `.db` export to preserve full fidelity.
6. Warning copy should include fixed, count-based lines for all categories (including zero values) to preserve a stable, localization-friendly message shape.
7. History entries count (`history_count`).
8. Events with history count (`events_with_history_count`).
9. Custom lead-days override count (`custom_notify_days_count`, where custom means `notify_lead_days` is non-null).
10. Event-level notification-disabled count (`notify_disabled_count`, where disabled means `notify_enabled = 0`).
11. Existing user flow options remain unchanged (continue CSV export, choose `.db` export instead, or cancel).
12. Locked count-line labels for message body:
13. History entries: %1$d
14. Events with history: %2$d
15. Events with custom notify days: %3$d
16. Events with notifications disabled: %4$d
17. Performance note: compute warning counts once per full CSV export request and reuse the same query/cursor results to compose the warning.

Database file backup/restore:

1. Per-event overrides are preserved automatically in full database file export/restore.
2. Existing backup path already copies full SQLite state in [MainActivity.java](../app/src/main/java/com/merware/dayssincepro/MainActivity.java).

---

## 8. UX and Settings Changes

Settings:

1. Remove `remind_percent` preference from [options.xml](../app/src/main/res/xml/options.xml).
2. Remove related percent arrays from [arrays.xml](../app/src/main/res/values/arrays.xml).
3. Remove related preference summary and binding logic from [PrefActivity.java](../app/src/main/java/com/merware/dayssincepro/PrefActivity.java).
4. Remove/adjust obsolete reminder strings in [strings.xml](../app/src/main/res/values/strings.xml) and localized values-\* files.
5. Update localized warning strings for CSV full-export risk messaging in all supported `values-*` `strings.xml` variants so warning behavior and copy remain language-complete.
6. Localization policy: if any supported locale is missing required new reminder-warning strings, the build must fail.

Add/Edit Event:

1. Show recurrence default lead days for current recurrence selection.
2. Provide optional custom lead-days input (0 to 30).
3. Provide per-event notification enabled toggle.
4. Support default inheritance behavior without requiring a dedicated new reset button.
5. Persist on save and rehydrate on edit.

Event details visibility:

1. Show effective lead days.
2. Show source state (Default from recurrence, or Custom override).
3. Show whether notifications are enabled or disabled for that event.

Locked UI copy labels (English source):

1. Lead days source: Default (from recurrence)
2. Lead days source: Custom
3. Notifications: Enabled
4. Notifications: Disabled

---

## 9. Logic Architecture

Introduce one shared resolver to avoid divergence between notification and color paths.

Recommended shared components:

1. Recurrence default table provider.
2. Effective lead-days resolver (event override or recurrence default).
3. Near-due evaluator using effective lead days.
4. Notification eligibility gate that applies global + per-event enable/disable precedence before threshold evaluation.

Expected code touchpoints:

1. [OnAlarmReceive.java](../app/src/main/java/com/merware/dayssincepro/OnAlarmReceive.java)
2. [MyEventAdapter.java](../app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java)
3. [EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java)
4. [PastFutureListFragment.java](../app/src/main/java/com/merware/dayssincepro/PastFutureListFragment.java)
5. [DatabaseHelper.java](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java)
6. [options.xml](../app/src/main/res/xml/options.xml)
7. [arrays.xml](../app/src/main/res/values/arrays.xml)
8. [strings.xml](../app/src/main/res/values/strings.xml)
9. values-\* localization files for updated copy.

---

## 10. Testing Strategy

Unit tests:

1. Effective lead-days resolver prefers override when present.
2. Effective lead-days resolver falls back to recurrence default when null.
3. Override bounds validation handles 0 and 30 correctly and rejects invalid values.
4. Notification near-due classification based on fixed-day model.
5. Color near-due classification parity with notification classification.
6. Migration test for schema update and existing-row compatibility.
7. Notification precedence tests: global off suppresses all, event-disabled suppresses event, enabled events continue to evaluate thresholds.
8. Toggle durability test: disabling and re-enabling does not erase prior custom `notify_lead_days`.

Integration behavior tests:

1. Add event using default lead days.
2. Edit event to custom lead days.
3. Edit event clear override back to default.
4. Pre-existing events after migration use recurrence defaults.
5. Database file export/restore preserves per-event override and per-event notification enabled state.
6. CSV intentionally does not preserve per-event override or per-event notification enabled state.
7. Full CSV export warning appears when history count is greater than 0 and no overrides exist.
8. Full CSV export warning appears when custom override count is greater than 0 and no history exists.
9. Full CSV export warning appears when event-disabled count is greater than 0 and no history or overrides exist.
10. Full CSV export warning appears when combinations of history/custom-override/event-disabled are present.
11. Full CSV export warning does not appear when all three counts are 0.
12. Warning copy always renders all four count lines (including zero values) and localizes correctly across all supported language resources.
13. Recurrence-change test: inherited/default events recalculate to new recurrence defaults.
14. Recurrence-change test: custom lead-days events preserve custom value after recurrence change unless a new value is explicitly confirmed.
15. Recurrence-change test: opening/focusing lead-days edit after recurrence change does not persist any value until explicit confirmation.
16. Recurrence-change test: canceling lead-days edit after recurrence change preserves prior custom value.
17. One-time-event test: near-due/due/overdue transitions follow fixed-day window semantics.
18. Localization gate test: build fails when required reminder-warning strings are missing from any supported locale.

---

## 11. Risk Assessment

Primary risks:

1. Behavioral drift if notification and color paths do not share one resolver.
2. UX confusion if Default vs Custom source is not clearly visible.
3. Localization churn due to settings text removal and new add/edit labels.
4. Migration defect risk if version-step chain is incomplete.

Mitigations:

1. One shared effective-threshold resolver used by all touchpoints.
2. Explicit source-state labels in event editor and details surfaces.
3. Migration and parity tests as release gates.
4. Keep CSV scope unchanged to reduce blast radius.

---

## 12. Complexity Assessment

Compared with static recurrence-default-only fixed-day model, this proposal increases complexity by one level due to per-event persistence and UI state management.

Assessment:

1. Overall complexity: Medium-High.
2. Main complexity increase: per-event schema, editor UX, and state roundtrip.
3. Complexity reduction in one area: settings simplification (global percent removal).
4. Data risk remains low with additive nullable schema and no backfill.

---

## 13. Rollout Plan

Phase 1: Data and shared logic

1. Add `event.notify_lead_days` (nullable) and `event.notify_enabled` (default enabled) columns and migration.
2. Add shared defaults + effective lead-days resolver.
3. Switch notification and color logic to fixed-day evaluator.

Phase 2: UX and settings

1. Add Add/Edit Event controls for default display and custom override.
2. Add event details display for effective lead-days and source state.
3. Remove obsolete percent preference and resources from Settings.

Phase 3: Localization and hardening

1. Update localized strings and arrays.
2. Execute regression suite covering migration, parity, and persistence behavior.

---

## 14. Acceptance Criteria

1. Percent reminder threshold setting no longer exists.
2. Near-due behavior is fixed-day based for both notifications and colors.
3. Per-event override (0 to 30) can be set, persisted, and cleared.
4. Events with null override use recurrence defaults.
5. Existing event data migrates safely with no backfill requirement.
6. Database file export/restore preserves per-event overrides.
7. Database file export/restore preserves per-event notification enabled/disabled state.
8. CSV does not include or preserve per-event override or per-event notification enabled/disabled state by design.
9. Notification and color outcomes remain consistent for the same event/date context.
10. Notification precedence is enforced (global setting, then per-event enable/disable, then threshold evaluation).
11. Full CSV export warning is shown when history exists and/or custom per-event notification lead-days overrides exist and/or event-level notifications are disabled.
12. Updated warning copy is present and localized in all supported values-\* `strings.xml` files.
13. Warning body includes all four count lines (History entries, Events with history, Events with custom notify days, Events with notifications disabled), including zero values.
14. Color urgency remains visible even when an event's notifications are disabled.
15. One Time Event uses fixed-day near-due/due/overdue behavior.
16. After migration, all existing event rows have `notify_enabled = 1`.
17. No runtime reads remain for `remind_percent` or `remind_percent_values` after rollout.
18. Number of Days recurrence applies interval clamp to effective lead days; named recurrence types do not clamp.
19. UI copy uses the locked source-state and notification-state labels.
20. Build fails when required reminder-warning localization keys are missing from any supported locale.

---
