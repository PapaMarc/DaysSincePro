# Design Change Request (DCR): Evolve to MaterialDatePicker (Extended Historical Date Range)

**Document ID:** DCR-2026-09-01-A
**Target Component:** `DaysDiffActivity`, `EditEventActivity`, `EditHistory` (all date-selection UI), `app/build.gradle` (Material Components version), `DaysSinceCalculations` (date math)
**Status:** Draft / Investigation Complete, Pending Approval
**Author:** DaysSincePro Architecture

---

## 1. Motivation

The app currently cannot select a date earlier than **01/01/1900**, and even where that floor is lifted, the stock calendar-style date picker requires swiping back one month at a time — functionally unusable for any date more than a few years in the past (e.g. reaching the 1700s, let alone year 1, would take an impractical number of swipes).

Investigation findings:

- The 1900 floor is **not** app-imposed. No `setMinDate`/`setMaxDate` call exists anywhere in the codebase (confirmed via search across [DaysDiffActivity.java](../app/src/main/java/com/merware/dayssincepro/DaysDiffActivity.java), [EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java), [EditHistory.java](../app/src/main/java/com/merware/dayssincepro/EditHistory.java)). It is `android.widget.DatePicker`'s own built-in default minimum when no bound is explicitly configured.
- Nothing in Android or in `java.util.Calendar`/`GregorianCalendar` structurally prevents supporting dates back to **1/1/1** (recorded-history floor) or earlier — the practical limit is the `long` millisecond range (effectively unbounded for this app's purposes).
- The real blocker is **UX feasibility**: the stock calendar-grid picker has no fast way to jump to a distant year.

**Goal:** Support selecting/storing dates back to 1/1/1 (or a similarly distant floor), with an acceptable UX that does not require scrolling through hundreds or thousands of years.

---

## 2. Current State — Date Picker Call Sites

All date selection currently goes through the legacy `android.app.DatePickerDialog` (calendar-mode, no min/max bound set). Five distinct dialog instances across three activities:

| #   | File                                                                                                     | Purpose                                                                |
| --- | -------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| 1   | [DaysDiffActivity.java](../app/src/main/java/com/merware/dayssincepro/DaysDiffActivity.java#L101-L106)   | Date A picker (days-between comparison)                                |
| 2   | [DaysDiffActivity.java](../app/src/main/java/com/merware/dayssincepro/DaysDiffActivity.java#L104-L106)   | Date B picker (days-between comparison)                                |
| 3   | [EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java#L620-L624) | Event start date                                                       |
| 4   | [EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java#L626-L668) | Event end date (has recurrence-based default-prefill logic — see §7.5) |
| 5   | [EditHistory.java](../app/src/main/java/com/merware/dayssincepro/EditHistory.java#L126-L128)             | History entry date                                                     |

**Requirement:** any picker change must be applied **consistently across all five** call sites, including an identical minimum-date floor everywhere. A mix of old/new pickers, or different floors per screen, would be a worse regression than the current uniform (if limited) behavior.

---

## 3. Picker Options Considered

| Option                                                                      | Description                                                                                                                                                                                                                                                                                    | Verdict                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **A. Stock `DatePicker` spinner mode** (`android:datePickerMode="spinner"`) | Three `NumberPicker` columns (month/day/year); year is directly typeable.                                                                                                                                                                                                                      | Rejected as primary path — AOSP has not actively maintained spinner mode since Android 7.0 (API 24); some OEM skins force calendar mode regardless of the attribute, so behavior is inconsistent across devices.                                                                                                                                                                                                                                            |
| **B. `MaterialDatePicker`** (androidx/Material Components)                  | Modern picker with a built-in scrollable **year grid** (tap header to jump straight to a year) and a **text-input toggle** (type `MM/DD/YYYY` directly, no scrolling). Bounds set via `CalendarConstraints` (arbitrary `long` millis, including a custom `DateValidator` for extended ranges). | **Selected.** Directly solves the "don't scroll 2000 years" problem via two built-in mechanisms (year grid + text entry), and the app already depends on the Material Components library (see §4), so adoption cost is UI-layer only.                                                                                                                                                                                                                       |
| **C. Custom 3-`NumberPicker` dialog**                                       | Hand-rolled dialog with explicit year/month/day pickers, full control over bounds.                                                                                                                                                                                                             | **Rejected outright, not kept as a fallback.** Highest implementation/maintenance cost, reinvents what Option B already provides, and introduces a second date-picker code path to maintain long-term. If Option B's year-grid proves impractical at the target range during implementation (§7.3), the mitigation is to **adjust the minimum-date floor itself** (a config/constant change), not to add a hand-rolled picker as a parallel implementation. |

**Decision: Option B — `MaterialDatePicker`, with no custom-picker fallback path.** If empirical testing shows the year-grid doesn't hold up at the target floor, the response is to move the floor, not to introduce Option C.

---

## 4. Material Components Library Version

Current dependency ([app/build.gradle](../app/build.gradle)):

```
implementation 'com.google.android.material:material:1.12.0'
```

- **Footprint:** Negligible. The Material library is already a linked dependency; `MaterialDatePicker` is a class within it, not a new library. With `minifyEnabled true` + `shrinkResources true` already enabled in the release build, R8 keeps only referenced classes — expect a small increase in kept-class count, not a meaningful size change.
- **Currency:** 1.12.0 is **two stable releases behind**. Latest stable is **1.14.0** (May 2026); **1.13.0** (Sep 2025) sits between. Confirmed compatible with this project:
  - 1.13.0+ requires `minSdkVersion` ≥ 21 → project's `minSdk 30` clears this.
  - 1.13.0+ requires `compileSdkVersion` ≥ 35 → project's `compileSdk 36` clears this.
- **Recommendation:** Bump `1.12.0` → `1.14.0` as part of this change (low risk, and exercises the exact `MaterialDatePicker`/`CalendarConstraints` API surface being newly adopted, so it should be validated together rather than as a separate unrelated bump).

---

## 5. Julian → Gregorian Calendar Cutover Fix

**Status: IMPLEMENTED (Phase 0, 2026-09-01).**

Extending the supported date range back past **October 15, 1582** surfaces a latent correctness issue independent of the picker UI itself.

**Issue:** `java.util.GregorianCalendar` has an implicit cutover date (`getGregorianChange()`, default Oct 15, 1582) before which it interprets dates using the **Julian** calendar, not proleptic Gregorian. Any day-count math spanning that boundary would therefore be computed using two different calendar systems.

**Correction to this document's original investigation:** the fix was initially assumed to belong inside [DaysSinceCalculations.daysBetween()](../app/src/main/java/com/merware/dayssincepro/DaysSinceCalculations.java), by calling `setGregorianChange()` on its two `GregorianCalendar` instances. Empirical verification (a throwaway JDK experiment) during implementation showed this was **incorrect and would have been a no-op**: `daysBetween()`'s calendars only ever call `setTime()`/`getTimeInMillis()` (raw millisecond arithmetic), and the cutover only affects `Calendar`'s field↔millis _conversion_ — it has zero effect on a calendar whose fields are never read. The actual Julian/Gregorian ambiguity is introduced earlier, at the point an ISO date **string** is parsed into a `Date` (i.e. `SimpleDateFormat.parse()`), since that internally converts calendar fields to millis using its own default-cutover `Calendar`.

**Actual fix implemented:** [DaysSinceCalculations.java](../app/src/main/java/com/merware/dayssincepro/DaysSinceCalculations.java)'s `yyyy-MM-dd` parsing `formatter` field is now constructed via a small factory that configures its underlying calendar for proleptic Gregorian before any parsing happens:

```java
SimpleDateFormat formatter = newProlepticGregorianFormatter("yyyy-MM-dd");

private static SimpleDateFormat newProlepticGregorianFormatter(String pattern) {
    SimpleDateFormat fmt = new SimpleDateFormat(pattern);
    ((GregorianCalendar) fmt.getCalendar()).setGregorianChange(new Date(Long.MIN_VALUE));
    return fmt;
}
```

Verified empirically that this location — not `daysBetween()` — is where the fix takes effect, and that it doesn't alter behavior for ordinary post-1582 dates.

- **Scope note:** this fixes the dominant, direct string-parsing path (`new DaysSinceCalculations(String)`, used throughout the app for day-count math from raw DB date strings). It intentionally does **not** touch [SimpleDate.java](../app/src/main/java/com/merware/dayssincepro/SimpleDate.java)'s own separate `formatter`/field-extraction `SimpleDateFormat`s (used for display/UI formatting), since a full audit of that class's multiple format paths remains explicitly open investigation work in [§7.2](#72-simpledates-multiple-format-paths-must-agree--open-investigation-not-yet-resolved) (Phase 1). Full correctness for `SimpleDate`-originated pre-1582 dates (e.g. display formatting) is tracked there, not claimed as resolved by this fix.
- **Effort:** Small, contained to one field's construction in one class.
- **Risk:** Low. `setGregorianChange` is a long-stable, non-deprecated JDK API with identical behavior across Android API levels. The only real behavior change is for stored events dated before 1582 (if any exist), whose computed day-count would shift by the historical Julian/Gregorian drift — a correctness _improvement_.
- **Tests added:** [DaysSinceCalculationsCutoverTest.java](../app/src/test/java/com/merware/dayssincepro/DaysSinceCalculationsCutoverTest.java) — asserts correct proleptic-Gregorian day-count using a clean, well-known Julian/Gregorian divergence (year 1500: a Julian leap year, not a proleptic-Gregorian one), plus sanity checks that ordinary modern-date math is unaffected.

---

## 6. Scope Summary

This DCR covers:

1. Bump `com.google.android.material:material` to 1.14.0.
2. Replace all 5 `DatePickerDialog` call sites with `MaterialDatePicker`, sharing one common minimum-date constant.
3. Apply the Julian→Gregorian cutover fix in `DaysSinceCalculations.daysBetween()`.
4. Add regression tests for both the cutover fix and (per §7) date-format round-tripping for early years.

**Out of scope** (tracked separately in [DCR_schemaChangesAndProperMigration.md](DCR_schemaChangesAndProperMigration.md)): the `event.details` column addition, CSV schema changes for `end_date`/`details`, and database migration-path hardening. That work is independent but intersects with this DCR at one point: the "no end date" placeholder scheme in CSV/DB — resolved jointly as `""`/`NULL`, not a sentinel date, specifically because of this DCR's extended floor (see §8, item 3).

---

## 7. Implementation Considerations

### 7.1 Storage/parsing format is separate from the picker — and it's fragile

[SimpleDate.java](../app/src/main/java/com/merware/dayssincepro/SimpleDate.java) and [DaysSinceCalculations.java](../app/src/main/java/com/merware/dayssincepro/DaysSinceCalculations.java#L65) use `SimpleDateFormat("yyyy-MM-dd")` for DB storage/parsing; CSV import/export ([CsvExporter.java](../app/src/main/java/com/merware/dayssincepro/CsvExporter.java#L142), [CsvImporter.java](../app/src/main/java/com/merware/dayssincepro/CsvImporter.java#L355)) use the same ISO pattern plus fallback patterns. The `"yyyy"` pattern token handles small years (e.g. `"0001-01-01"`) correctly in principle, but this has not been verified with a test and must be, independent of the picker UI change.

### 7.2 `SimpleDate`'s multiple format paths must agree — **open investigation, not yet resolved**

`SimpleDate` contains several distinct `SimpleDateFormat` patterns for different date styles (US/EU/ISO, plus long-form display strings). All must consistently zero-pad/represent years below 1000 the same way. This has **not** been audited yet — it remains open investigation work: each format string in [SimpleDate.java](../app/src/main/java/com/merware/dayssincepro/SimpleDate.java) needs to be individually verified (e.g. via a quick unit test per pattern) to confirm it round-trips a year-45 or year-1 date correctly before the picker floor is relied upon. Treat as a required pre-requisite check, not an assumption.

### 7.3 `MaterialDatePicker`'s practical bounds need empirical verification — floor is the adjustable lever, not the picker choice

`CalendarConstraints` accepts arbitrary `long` millis bounds via the API, but its internal year-grid/month-index implementation is designed around "reasonable" ranges (e.g. birthdates, a few decades to ~a century). A ~2000-year span (year 1 → present) is far outside typical usage and should be manually tested for any internal overflow, lag, or degraded scroll performance in the year-grid view.

**Decision on how to respond if this investigation turns up problems:** do not maintain a second hand-rolled picker code path (Option C) as a fallback — that trades a UX limitation for permanent code/maintenance overhead. Instead, if the year-grid proves impractical below some point (e.g. below year 1000), the response is to **raise the minimum-date floor itself** to whatever value empirically works well, and accept that adjusted floor as the real constraint, rather than solving it with more code. This keeps exactly one picker implementation in the app, permanently.

### 7.4 Minimum date floor: 01/01/0001

Target floor, pending the §7.3 investigation: **January 1, year 1 (0001-01-01)**. This should be adequate to capture entries reasonably characterized as "recorded history," for example:

| Field         | Example value                                                                                                              |
| ------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Event date    | 45 AD                                                                                                                      |
| Event title   | Pliny the Elder begins compiling "Naturalis Historia"                                                                      |
| Event details | One of the earliest encyclopedic attempts to systematically record knowledge of nature, minerals, astronomy, and medicine. |

All 5 call sites (§2) should reference a single shared constant for this minimum selectable date (and any shared maximum), rather than each hardcoding its own `CalendarConstraints`, to prevent drift if the floor is later adjusted per §7.3.

### 7.5 Preserve `EditEventActivity`'s end-date default-prefill logic

The end-date dialog ([EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java#L626-L663)) computes a suggested default end date based on the selected recurrence interval (weekly/**biweekly**/monthly/quarterly/semi-annual/annual — note the recurrence switch also has a `14` = biweekly case not previously called out here, see [EditEventActivity.java#L202](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java#L202)) when no end date has been set yet. This logic is independent of the picker widget and must be ported as-is to `MaterialDatePicker.Builder.datePicker().setSelection(...)`.

### 7.6 Locale/date-format display — confirmed, no open questions

`MaterialDatePicker`'s text-input toggle formats/parses using the device locale's date pattern — a UX improvement over the current manual string concatenation in [DaysDiffActivity.java](../app/src/main/java/com/merware/dayssincepro/DaysDiffActivity.java#L122) (`mYear + "-" + month + "-" + mDay`). Output from the picker should be routed through the existing `SimpleDate`/formatter classes rather than trusted directly, to keep a single source of truth for date formatting.

### 7.7 Testing scope and sequencing — confirmed, no open questions

Because [DaysSinceCalculations.java](../app/src/main/java/com/merware/dayssincepro/DaysSinceCalculations.java) and `SimpleDate` are plain-JVM-testable per existing project convention (see repo memory notes), add unit tests for:

- Day-diff math across the 1582 Julian/Gregorian cutover (§5).
- `SimpleDate` parse/format round-trip for years 1–999 (§7.2).
- Recurrence-interval logic (§7.5) including `recur = 14` (biweekly), not just the previously-covered values.

...**before** touching the UI layer — cheaper to catch date-math/formatting correctness bugs here than after wiring up the new picker UI.

---

## 8. Resolved Decisions & Remaining Open Items

1. **Minimum-date floor value — RESOLVED, see §7.4: 01/01/0001.**
2. **`MaterialDatePicker` year-grid performance at ~2000-year span — RESOLVED: no hybrid/dual-picker approach.** If the §7.3 investigation finds the year-grid impractical below some point, the fix is to raise the floor to whatever value works well empirically — not to add a second (Option C) picker implementation as a parallel code path.
3. **Interaction with [DCR_schemaChangesAndProperMigration.md](DCR_schemaChangesAndProperMigration.md) — RESOLVED.** Both documents now agree: missing `end_date` in CSV/DB is represented as `""` (empty string) / `NULL`, never a sentinel date, precisely to avoid collision with a legitimate real date once this document's floor (§7.4) extends selection down to year 1. See [DCR_schemaChangesAndProperMigration.md §5 and §7 item 6](DCR_schemaChangesAndProperMigration.md#5-csv-format-evolution--the-missing-value-placeholder-question) for the full resolution.
4. **Rollout scope for the Gregorian-cutover fix, and phasing generally — pending, deferred until scope is locked.** This DCR and the schema DCR are intended to ship as part of a common release update. Once total scope across both documents is finalized, they should be jointly updated with an agreed phased implementation/check-in sequence (e.g. which fixes land in which order/PRs). Not yet defined — intentionally deferred rather than guessed at this stage.

**Remaining open item still requiring investigation (not yet resolved):**

- §7.2 — audit of all `SimpleDate` format strings for correct behavior with years below 1000, still open.
- §7.3 — empirical verification of `MaterialDatePicker`'s year-grid behavior at the year-1 floor, still open (drives whether §7.4's floor holds as-is or needs adjustment).
