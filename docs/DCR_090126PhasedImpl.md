# Design Change Request (DCR): Phased Implementation Plan — Date Range, Schema, and Notification Overhaul

**Document ID:** DCR-2026-09-01-D
**Status:** Draft — sequencing proposal, pending final confirmation
**Author:** DaysSincePro Architecture
**Cross-referenced DCRs (this document sequences all three into one release):**

1. [DCR_evolveToMaterialDatePicker.md](DCR_evolveToMaterialDatePicker.md) — extend minimum selectable date to 0001-01-01 via `MaterialDatePicker`, fix the Julian→Gregorian cutover.
2. [DCR_schemaChangesAndProperMigration.md](DCR_schemaChangesAndProperMigration.md) — harden the destructive migration fallback, add `event.details`, add CSV support for `end_date`/`details`.
3. [DCR_OldEvents-HandlingNotifications.md](DCR_OldEvents-HandlingNotifications.md) — fix the perpetual-overdue notification bug, add `event.last_notified_date` dismiss tracking.

---

## 1. Sequencing Principles

Phases are ordered by a few consistent rules extracted from the three source documents:

1. **Bug fixes with no schema impact ship first**, independent of everything else — they're pure risk-reduction with no dependencies.
2. **Investigation/spike tasks come before the implementation they gate** — e.g. the date-floor viability check must resolve before the picker UI work locks in a floor value.
3. **All new schema columns land in a single version bump (v4)**, sharing one migration step, rather than staggering multiple schema versions across phases.
4. **CSV/UI-facing feature work follows its underlying schema/data-layer work**, never the reverse.
5. Each phase should be independently shippable/testable, per the existing project testing conventions (JVM-testable static helpers, real-SQLite-backed migration tests).

---

## 2. Phase 0 — Standalone Bug Fixes & Risk Reduction (no schema change, highest priority)

No user-visible feature work; pure correctness/safety fixes. Can ship ahead of everything else in this plan.

| Item                                                                                                                                                          | Source                                                                                                                                        | Why first                                                                                                                                 |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Fix `OnAlarmReceive`'s perpetual-overdue calculation by extracting/reusing `MyEventAdapter.addRecurrenceInterval()`'s cycle-aware `lastDate`/`nextDate` logic | [DCR-C §2–§3](DCR_OldEvents-HandlingNotifications.md#3-proposed-fix-no-schema-change-required--recommended-for-front-of-queue-prioritization) | Actively-reported, user-impacting bug (Phone Link notification throttling) happening today; no schema dependency.                         |
| Replace `DatabaseHelper.onUpgrade()`'s destructive drop-and-recreate fallback with a linear stepwise migration                                                | [DCR-B §3](DCR_schemaChangesAndProperMigration.md#3-critical-pre-existing-risk-destructive-onupgrade-fallback)                                | Silent full data-loss risk for any future schema bump (including this plan's own v4 in Phase 3) — must land before v4 is introduced.      |
| Julian→Gregorian cutover fix in `DaysSinceCalculations.daysBetween()` (`setGregorianChange(new Date(Long.MIN_VALUE))`)                                        | [DCR-A §5](DCR_evolveToMaterialDatePicker.md#5-julian--gregorian-calendar-cutover-fix)                                                        | Trivial, low-risk, fully standalone; only becomes user-visible once dates before 1582 are enterable (Phase 2), but safe/cheap to fix now. |

**Tests:** regression test per [DCR-C §7](DCR_OldEvents-HandlingNotifications.md#7-suggested-tests-to-capture) (perpetual-overdue + cycle-boundary + `OnAlarmReceive`/`MyEventAdapter` parity), migration-matrix test per [DCR-B §8](DCR_schemaChangesAndProperMigration.md#8-additional-tests-to-capture) (upgrade from v1/v2/v3 with no data loss), Julian/Gregorian cutover test per [DCR-A §5](DCR_evolveToMaterialDatePicker.md#5-julian--gregorian-calendar-cutover-fix).

---

## 3. Phase 1 — Investigation Spikes (gate Phase 2's scope)

Two open investigation items from DCR-A must resolve before the picker UI implementation locks in its final parameters. No user-facing changes in this phase.

| Item                                                                                                 | Source                                                                                                                                                                   | Resolves                                                                                                                                                                                                   |
| ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Audit all `SimpleDate` format strings for correct behavior with years < 1000                         | [DCR-A §7.2](DCR_evolveToMaterialDatePicker.md#72-simpledates-multiple-format-paths-must-agree--open-investigation-not-yet-resolved)                                     | Confirms (or fixes) date parsing/formatting correctness ahead of allowing such dates to be entered.                                                                                                        |
| Empirically verify `MaterialDatePicker`'s year-grid behavior at a ~2000-year span down to 0001-01-01 | [DCR-A §7.3](DCR_evolveToMaterialDatePicker.md#73-materialdatepickers-practical-bounds-need-empirical-verification--floor-is-the-adjustable-lever-not-the-picker-choice) | Confirms whether the 0001-01-01 floor (§7.4) holds as-is, or must be raised to a value where the year-grid performs acceptably — this is a **hard gate** on Phase 2's `CalendarConstraints` configuration. |

**Output of this phase:** a confirmed final minimum-date floor value (target: 0001-01-01, per [DCR-A §7.4](DCR_evolveToMaterialDatePicker.md#74-minimum-date-floor-01010001), subject to adjustment if the year-grid investigation says otherwise) and a confirmed-correct `SimpleDate` formatting behavior for that range.

---

## 4. Phase 2 — MaterialDatePicker Rollout

Depends on: Phase 1's confirmed floor value; independent of Phases 3–4.

1. Bump `com.google.android.material:material` from 1.12.0 to 1.14.0 ([DCR-A §4](DCR_evolveToMaterialDatePicker.md#4-material-components-library-version)).
2. Replace all 5 `DatePickerDialog` call sites with `MaterialDatePicker`, sharing one common minimum-date constant ([DCR-A §2, §7.4](DCR_evolveToMaterialDatePicker.md#2-current-state--date-picker-call-sites)):
   - `DaysDiffActivity` (date A, date B)
   - `EditEventActivity` (start date, end date — preserving the recurrence-based end-date default-prefill logic, including the previously-missing `recur = 14` biweekly case per [DCR-A §7.5](DCR_evolveToMaterialDatePicker.md#75-preserve-editeventactivitys-end-date-default-prefill-logic))
   - `EditHistory` (history entry date)
3. Route picker output through the existing `SimpleDate`/formatter classes rather than trusting picker-returned strings directly ([DCR-A §7.6](DCR_evolveToMaterialDatePicker.md#76-localedate-format-display--confirmed-no-open-questions)).

**Tests:** per [DCR-A §7.7](DCR_evolveToMaterialDatePicker.md#77-testing-scope-and-sequencing--confirmed-no-open-questions) — `SimpleDate` round-trip for years 1–999, recurrence-interval logic including `recur = 14`. (Julian/Gregorian cutover test already landed in Phase 0.)

---

## 5. Phase 3 — Schema Version 4: `event.details` + `event.last_notified_date`

Depends on: Phase 0's hardened migration engine. Independent of Phases 1–2 (can proceed in parallel with them if desired, though sequencing after Phase 0 is a hard requirement).

1. Add explicit `oldVersion == 3 && newVersion == 4` migration step (and any needed lower-version chains) using the Phase-0-hardened stepwise migration approach:
   ```sql
   ALTER TABLE event ADD COLUMN details TEXT;
   ALTER TABLE event ADD COLUMN last_notified_date DATE;
   ```
   Both columns are additive/nullable and land in the same version bump ([DCR-B §4](DCR_schemaChangesAndProperMigration.md#4-planned-schema-additions), [DCR-C §4.1](DCR_OldEvents-HandlingNotifications.md#41-what-the-schema-change-looks-like)).
2. `EditEventActivity` UI: add a multi-line `details` field, 256-character cap ([DCR-B §6](DCR_schemaChangesAndProperMigration.md#6-details-field-length--character-support)).
3. `OnAlarmReceive`: read/write `last_notified_date`, suppress re-notification within the current occurrence cycle (`[lastDate, nextDate)`), building directly on Phase 0's cycle-aware fix ([DCR-C §4.1, §6 item 1](DCR_OldEvents-HandlingNotifications.md#41-what-the-schema-change-looks-like)). Manual "Review timely events now" intentionally ignores dismiss state for now ([DCR-C §6 item 2](DCR_OldEvents-HandlingNotifications.md#6-resolved-decisions)).
4. No backfill logic on migration — a single one-time catch-up notification burst on first upgrade is accepted as-is ([DCR-C §6 item 3](DCR_OldEvents-HandlingNotifications.md#6-resolved-decisions)).

**Tests:** migration test for the new v4 step (extending Phase 0's migration suite), `details` length-cap UI test, Unicode round-trip test for `event`/`details`, `last_notified_date` suppression test (same-cycle vs. next-cycle), one-time-event same-day double-fire test ([DCR-B §8](DCR_schemaChangesAndProperMigration.md#8-additional-tests-to-capture), [DCR-C §7](DCR_OldEvents-HandlingNotifications.md#7-suggested-tests-to-capture)).

---

## 6. Phase 4 — CSV Format v2: `end_date` + `details`

Depends on: Phase 3 (columns must exist before CSV can read/write them).

1. `CsvExporter`: append `end_date` and `details` columns — `event,date,recur,end_date,details` ([DCR-B §5, §7 item 3](DCR_schemaChangesAndProperMigration.md#5-csv-format-evolution--the-missing-value-placeholder-question)). Missing values written as `""`, never a sentinel date ([DCR-B §5, §7 item 6](DCR_schemaChangesAndProperMigration.md#5-csv-format-evolution--the-missing-value-placeholder-question); cross-confirmed in [DCR-A §8 item 3](DCR_evolveToMaterialDatePicker.md#8-resolved-decisions--remaining-open-items)).
2. `CsvImporter`: treat `end_date`/`details` as optional, header-name-mapped columns; empty field → `NULL`. Maintain backward compatibility with legacy 3-/4-column files.
3. Change duplicate-match behavior from unconditional skip to **enrichment-on-duplicate**: on a `(catId, event, date, recur)` key match, if the existing row's `end_date`/`details` is empty and the incoming row has a value, update the existing row; otherwise leave it untouched ([DCR-B §7 item 5](DCR_schemaChangesAndProperMigration.md#7-resolved-decisions)).
4. `last_notified_date` is intentionally **excluded** from CSV entirely — omission causes at most one extra notification on re-import, an accepted trade-off to avoid CSV complexity for operational state ([DCR-C §4.2](DCR_OldEvents-HandlingNotifications.md#42-does-this-need-to-round-trip-through-csv)).
5. Add code comment noting "import old, re-export new" was a deliberate, conscious scope decision (no staleness detection) ([DCR-B §7 item 4](DCR_schemaChangesAndProperMigration.md#7-resolved-decisions)).
6. `details` values exceeding the 256-character cap arriving via CSV import: **truncate to 256 characters AND record a warning** in `CsvImportResult` (resolved; see [DCR-B §9 item 1](DCR_schemaChangesAndProperMigration.md#9-open-questions-remaining)).

**Tests:** CSV round-trip for `end_date`/`details` (populated/empty combinations), legacy-format backward-compatibility import, enrichment-on-duplicate (both directions), Unicode round-trip through CSV ([DCR-B §8](DCR_schemaChangesAndProperMigration.md#8-additional-tests-to-capture)).

---

## 7. Summary Table

| Phase                                           | Schema change? | Depends on                                | Ships independently?        |
| ----------------------------------------------- | -------------- | ----------------------------------------- | --------------------------- |
| 0 — Bug fixes & migration hardening             | No             | —                                         | Yes, first                  |
| 1 — Investigation spikes                        | No             | Phase 0 not required, but logically first | Yes (no user-facing change) |
| 2 — MaterialDatePicker rollout                  | No             | Phase 1 (floor value)                     | Yes                         |
| 3 — Schema v4 (`details`, `last_notified_date`) | Yes (v4)       | Phase 0 (migration engine)                | Yes                         |
| 4 — CSV format v2                               | No (CSV only)  | Phase 3 (columns must exist)              | Yes                         |

Phases 1–2 and Phase 3 have no dependency on each other and can proceed in parallel if desired; Phase 4 strictly follows Phase 3; Phase 0 strictly precedes Phase 3 (and is recommended to precede everything, given its risk-reduction nature).

---

## 8. Open Items Carried Forward (non-blocking)

- [DCR-A §7.2](DCR_evolveToMaterialDatePicker.md#72-simpledates-multiple-format-paths-must-agree--open-investigation-not-yet-resolved) / [§7.3](DCR_evolveToMaterialDatePicker.md#73-materialdatepickers-practical-bounds-need-empirical-verification--floor-is-the-adjustable-lever-not-the-picker-choice) — confirmed to occur as part of Phase 1's investigation work, per §3 above.
