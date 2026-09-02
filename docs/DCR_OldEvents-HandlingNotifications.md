# Design Change Request (DCR): Handling of Old/Stale Event Notifications

**Document ID:** DCR-2026-09-01-C
**Target Component:** [OnAlarmReceive.java](../app/src/main/java/com/merware/dayssincepro/OnAlarmReceive.java) (daily + manual "review" notification trigger), [AlarmHelper.java](../app/src/main/java/com/merware/dayssincepro/AlarmHelper.java) (alarm scheduling), [MyEventAdapter.java](../app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java) (list-view recurrence calculation, used here as the _correct_ reference implementation), possibly [DatabaseHelper.java](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java) (schema, if a dismiss/seen mechanism is added)
**Status:** Implemented through Phase 4 (2026-09-01)
**Author:** DaysSincePro Architecture

---

## 1. Motivation / Symptom

Reported behavior: notifications resurface for events that are years old — both from the automatic daily notification and from manually tapping "Review timely events now". Old recurring events keep appearing as if newly due, seemingly forever, not just around their actual anniversary. On Windows with Phone Link, this has escalated to the OS itself throttling the app ("We've paused additional banners for 3 minutes to reduce interruptions") due to the sheer volume of stale popups generated in a single review pass.

Two distinct concerns were raised, which this document treats separately because they have different causes and different fixes:

1. **Why does a years-old recurring event get flagged as currently due at all, indefinitely?** (This turns out to be a genuine bug — see §2.)
2. **Even for a legitimately-currently-due event, why does it re-notify every single day with no way to say "I've seen this, stop reminding me until the next real occurrence"?** (This is a missing feature/dismiss-state gap — see §4.)

---

## 2. Root Cause Analysis: Perpetual "Overdue" State for Recurring Events

Both the daily 8am alarm and the manual "Review timely events now" action ([strings.xml#L88](../app/src/main/res/values/strings.xml#L88)) invoke the **same code path**: [OnAlarmReceive.onReceive()](../app/src/main/java/com/merware/dayssincepro/OnAlarmReceive.java#L164-L172), `eventID == 0` branch, which queries **every event whose stored `date` is `<= today`** ([OnAlarmReceive.java#L166-L169](../app/src/main/java/com/merware/dayssincepro/OnAlarmReceive.java#L166-L169)) — i.e., every event ever entered, since a recurring event's original `date` is never updated after creation.

For each such event, the recurring-event branch computes notification urgency using:

```java
dsc1 = new DaysSinceCalculations(usDate);   // usDate = the event's ORIGINAL stored date, never advanced
...
if (dsc1.getDaysSinceEvent() > nEstDays) {
    // red - overdue
}
```

`dsc1.getDaysSinceEvent()` is the **raw total number of days since the event's original stored date** — not "days since the most recent occurrence." For an annually-recurring event (`recur = 365`) created 10 years ago, this value is roughly 3650+ and growing by 1 every day, forever. Since `nEstDays` (365) is a fixed interval, `dsc1.getDaysSinceEvent() > nEstDays` becomes `true` once and then **stays true every single day for the rest of the event's existence** — there is no modulo/cycle-reset logic to recognize that the event actually recurred multiple times since its original date and is likely not "10 years overdue," it's just "due again this year."

**This is a genuine, pre-existing bug**, not a missing feature. Confirmed by contrast: the list-view code in [MyEventAdapter.java](../app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java#L94-L122) already computes this _correctly_ for recurring events, via `addRecurrenceInterval()` ([MyEventAdapter.java#L29-L47](../app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java#L29-L47)): it advances a `Calendar` forward one recurrence interval at a time (using calendar-field arithmetic, so leap years don't cause drift) from the original date until it passes today, yielding both `lastDate` (most recent occurrence on/before today) and `nextDate` (next occurrence after today). The notification logic in `OnAlarmReceive` never adopted this same approach. These were effectively divergent, and should now be reconciled consistently.

**Conclusion:** the "years-old notifications" symptom is primarily explained by this divergence, not by any missing "seen/dismissed" tracking. Fixing `OnAlarmReceive` to compute status relative to the _current recurrence cycle_ — reusing or mirroring `MyEventAdapter.addRecurrenceInterval()` — should resolve the bulk of the reported symptom **without any schema change**.

**Why the divergence exists (timeline, not neglect):** the correct `lastDate`/`nextDate` cycle-aware calculation in `MyEventAdapter` is itself fairly recent and was assembled in stages:

- `d874b367` (2026-04-29) — added `DaysUntilTest`, introducing the next-occurrence calculation for recurring events.
- `26016235` (2026-08-27) — added the "Since Last" tab, computing days-since-most-recent-recurrence (the `lastDate` half of the calculation) and renamed "Days Until" to "Until Next".
- `f891895e` (2026-08-30) — reworked the notification feature itself (renamed to "Review timely events now", shifted default timing to 8:00 AM, added Android 13+ permission handling, added Toast feedback) — but did **not** revisit `OnAlarmReceive`'s underlying urgency calculation.

So at the point the notification feature was last reworked (`f891895e`), the cycle-aware `lastDate`/`nextDate` logic it should have been built on had only just been assembled in the list-view code (`26016235`, three days earlier) and was never carried over. The two pieces of logic were built by separate, closely-spaced efforts that never got reconciled — not a case of one being old/neglected and the other new; both are recent, they just haven't been merged into a single shared implementation yet.

---

## 3. Proposed Fix (No Schema Change Required) — **recommended for front-of-queue prioritization**

**Status: IMPLEMENTED (Phase 0, 2026-09-01).**

This was a pure bug fix with no schema impact, prioritized at the front of the overall implementation plan alongside [DCR_schemaChangesAndProperMigration.md §3](DCR_schemaChangesAndProperMigration.md#3-critical-pre-existing-risk-destructive-onupgrade-fallback) (the destructive `onUpgrade` fallback) — see [DCR_090126PhasedImpl.md §2](DCR_090126PhasedImpl.md#2-phase-0--standalone-bug-fixes--risk-reduction-no-schema-change-highest-priority) for the completed Phase 0 record.

**Implemented as:** a new shared `RecurrenceCycle` class (`computeOccurrences()`) used by both `MyEventAdapter` and `OnAlarmReceive`, plus a package-visible, unit-tested `OnAlarmReceive.computeUrgency(daysSinceLastOccurrence, nEstDays, percent)` pure decision function and `OnAlarmReceive.currentCycleCalculations(usDate, nEstDays)` helper. Test coverage: `RecurrenceCycleTest`, `OnAlarmReceiveUrgencyTest`.

1. Extract `MyEventAdapter.addRecurrenceInterval()` (or an equivalent "compute last/next occurrence for a recurring event" helper) into a shared, statically-testable location (e.g. a small utility class, following the existing project convention of extracting testable static helpers — see repo memory notes on `MainActivity.refreshTabs()`/`dispatchSearch()`).
2. Update `OnAlarmReceive`'s recurring-event branch to compute its red/yellow/green urgency thresholds relative to the _current cycle's_ occurrence (i.e., days since `lastDate`, or days until `nextDate`), matching what the list view already displays — rather than relative to the event's original stored `date`.
3. This is a pure correctness fix, contained to `OnAlarmReceive`'s status-computation logic; the query that selects candidate events (`date <= today`) and the overall red/yellow/green percent-based thresholding preference (`remind_percent`) are unaffected and can stay as-is.

This should be scoped, prioritized, and tested independently of §4 below — it is a bug fix, not a feature.

---

## 4. Secondary Gap: No "Already Notified / Dismissed" State

Even after §3's fix, a _legitimately_ currently-due/overdue event will still notify **every day** for as long as it remains within its overdue window (which, depending on `remind_percent` and the recurrence interval, could be from a few days up to the full interval length). There is currently no mechanism to say "I've seen this reminder for the current cycle, don't show it again until the next real occurrence" — every alarm firing (daily, or manual review) re-evaluates and re-notifies from scratch.

Investigated whether the existing `history` table could already serve this purpose: **no** — `history` ([DatabaseHelper.java#L59-L61](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java#L59-L61), `eventId, catID, date, onTime, note`, `UNIQUE(eventId, date)`) is a distinct, user-facing "on-time tracking" feature (manually logged via [HistoryActivity.java](../app/src/main/java/com/merware/dayssincepro/HistoryActivity.java)/[EditHistory.java](../app/src/main/java/com/merware/dayssincepro/EditHistory.java) to track punctuality percentage), unrelated to notification delivery state. It should **not** be repurposed or overloaded for this — it has its own semantics and its own user workflow.

**This part likely does need a small schema change**, since there is nowhere today to record "the user has already been notified about this event for its current occurrence/cycle."

### Options considered

| Option                                                   | Description                                                                                                                                                                                                                       | Notes                                                                                                                                                                                                                                                                                           |
| -------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **A. `event.last_notified_date` (DATE, nullable)**       | Store the date the notification was last shown for this event. On each alarm firing, suppress re-notifying if `last_notified_date` falls within the current occurrence cycle (i.e., same cycle as `lastDate`/`nextDate` from §3). | Minimal schema footprint (one column). Naturally resets each cycle without extra bookkeeping — once the next real occurrence's window begins, the old `last_notified_date` no longer falls within the new cycle, so it notifies again automatically. Fully automatic — no user action required. |
| **B. Separate `notification_log` table**                 | Fully separate table, similar shape to `history` but distinct purpose.                                                                                                                                                            | **Rejected — no separate table wanted.** Higher schema/complexity cost for what's fundamentally a single "last shown" fact per event, with no identified need for a full delivery history.                                                                                                      |
| **C. Manual "mute" flag only, no automatic suppression** | User explicitly mutes a specific event's notifications going forward.                                                                                                                                                             | **Rejected — requires manual user intervention, which is explicitly not wanted here.** The goal is for the app to "just handle it" automatically, not add another setting the user has to remember to toggle.                                                                                   |

**Decision: Option A.** Single nullable column, fully automatic, no separate table, no manual step.

### 4.1 What the schema change looks like

```sql
ALTER TABLE event ADD COLUMN last_notified_date DATE;
```

- **Written automatically** by `OnAlarmReceive` whenever it actually delivers a notification for an event (no user action involved).
- **Read automatically** by the same urgency-check added in §3: before notifying, compare `last_notified_date` against the current cycle window (`[lastDate, nextDate)`); if it already falls within that window, suppress.
- **Bundled into the same schema version bump as `event.details`** from [DCR_schemaChangesAndProperMigration.md §4](DCR_schemaChangesAndProperMigration.md#4-planned-schema-additions) — both are additive, nullable `event` columns, so they can land in the same `ALTER TABLE`/version-4 migration step, and both are subject to the same migration-matrix hardening required in that document's §3. Final sequencing of _when_ this ships is deferred to the cross-DCR phased implementation plan.

### 4.2 Does this need to round-trip through CSV?

**Recommendation: no — leave it out of CSV export/import entirely, and this is a reasonable trade-off.**

- `last_notified_date` is **operational/derived state** ("has the app already shown this?"), not user-authored content like `event`, `date`, `recur`, `end_date`, or `details` — it doesn't represent something the user typed in or would expect to see/edit in a spreadsheet.
- If it's simply omitted from CSV, the only consequence of a re-import is that a re-imported event's dismiss state resets to "not yet notified," which at worst produces **one extra notification** for that event on the next alarm firing — a minor, self-correcting cosmetic effect, not a data-loss or correctness issue.
- This is a good trade against the alternative (adding CSV column handling, backward-compatibility parsing, and round-trip tests for a field that's rare to exercise via CSV in the first place, e.g. full-database backup/restore is more likely to preserve it via the `.db` path anyway, which is a raw SQLite file copy and would preserve it automatically without any CSV-specific work).
- Net effect: `last_notified_date` is preserved correctly through `.db` backup/restore (full SQLite file copy), and is acceptably (intentionally) reset through the CSV import/export path.

---

## 5. What Should NOT Change

- **Same-day re-notification is expected and fine.** If today genuinely is the day of (or within the configured `remind_percent` window of) a recurring event, showing that notification is correct behavior — including for long-standing annual events. The issue is specifically about _years-old_ events being treated as perpetually current, not about legitimately-due reminders.
- **One-time events (`recur = 0`) are not part of this bug.** `OnAlarmReceive`'s one-time-event branch only notifies when `dsc1.getDaysSinceEvent() == 0` (i.e., exactly today) — already self-limiting, since it's a single fixed date with no cycle to diverge from. §4's dismiss-state gap (repeated same-day notification across multiple alarm firings) could still theoretically apply here too, if the daily alarm and a manual "review" both fire on the same day — worth covering in the same fix for consistency, even though it's a much smaller version of the same underlying gap.

---

## 6. Resolved Decisions

1. **Cycle-boundary definition for "already notified" (Option A, §4) — RESOLVED: occurrence-cycle granularity.** Suppress when `last_notified_date` falls within the current recurrence window — i.e. "already notified since `lastDate`, before `nextDate`" — not merely "already notified today." This correctly avoids re-notifying every day throughout a multi-day overdue window.
2. **Manual "Review timely events now" interaction with dismiss state — RESOLVED (for now): ignore dismiss state entirely.** Manual review shows everything currently due/overdue regardless of `last_notified_date`. This is intentionally a simple, easily-reversible switch (a single conditional on whether the manual path checks `last_notified_date`) — flip it later if the manual action turns out to be too noisy in practice. Worth keeping in mind as a possible future Settings toggle, but not needed now.
3. **Retroactive behavior on first upgrade — RESOLVED: a one-time catch-up notification burst is acceptable.** No backfill/special-casing on migration; existing events simply have `last_notified_date = NULL` after the schema change, notify once more on the first post-upgrade firing, then settle into correct suppressed behavior going forward. No extra migration-time logic needed beyond the column addition itself.
4. **Relationship to the other DCRs / schema versioning — RESOLVED: same schema version bump as `event.details`.** See §4.1. Final ordering/sequencing across all three DCRs is deferred to the joint phased implementation plan, once all feature work across the documents is locked.
5. **Scope/sequencing relative to §3's bug fix — RESOLVED: §3 ships first, prioritized at the front of the implementation plan.** This is the actual bug (perpetual overdue state) and is independent of any schema work — it should be one of the earliest items across all three DCRs' combined implementation plan, alongside [DCR_schemaChangesAndProperMigration.md §3](DCR_schemaChangesAndProperMigration.md#3-critical-pre-existing-risk-destructive-onupgrade-fallback) and [DCR_evolveToMaterialDatePicker.md §7.3](DCR_evolveToMaterialDatePicker.md#73-materialdatepickers-practical-bounds-need-empirical-verification--floor-is-the-adjustable-lever-not-the-picker-choice).

---

## 7. Suggested Tests to Capture

- **Regression test for §2/§3:** an annual (`recur = 365`) event with a stored `date` many years in the past, evaluated "today" at a date that is _not_ near its yearly anniversary — assert it does **not** classify as due/overdue (red), contrasting with the current buggy behavior. Mirror as a static/extractable-helper test the same way `MainActivity.refreshTabs()`/`dispatchSearch()` were made testable per existing project convention.
- **Cycle-boundary correctness test:** same event, evaluated at a date that _is_ within the legitimate due/overdue window relative to its most recent occurrence — assert it **does** classify as due, to confirm the fix doesn't over-correct into never notifying.
- **Parity test between `OnAlarmReceive` and `MyEventAdapter`:** given the same event/date, assert both arrive at the same `lastDate`/`nextDate`, closing the gap identified in §2 and guarding against future divergence.
- **`last_notified_date` suppression test (§4.1):** simulate two consecutive daily alarm firings within the same occurrence cycle — assert only the first produces a notification; simulate a firing after the cycle boundary (next occurrence window) — assert it notifies again.
- **One-time event same-day double-fire test (§5):** simulate the daily alarm and a manual "review" both firing on the same day for a one-time event due today — assert the intended behavior per §6 item 2 (manual review ignores dismiss state, so it is expected/acceptable to notify again on manual trigger even if the daily alarm already fired).
- **Migration/backfill test (§6, open question 3):** once the schema decision is made, add a migration test consistent with the hardened migration-matrix approach mandated in [DCR_schemaChangesAndProperMigration.md §3](DCR_schemaChangesAndProperMigration.md#3-critical-pre-existing-risk-destructive-onupgrade-fallback).
