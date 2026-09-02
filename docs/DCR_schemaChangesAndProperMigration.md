# Design Change Request (DCR): Schema Changes & Proper Migration Path

**Document ID:** DCR-2026-09-01-B
**Target Component:** [DatabaseHelper.java](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java) (SQLite schema/versioning), [CsvExporter.java](../app/src/main/java/com/merware/dayssincepro/CsvExporter.java), [CsvImporter.java](../app/src/main/java/com/merware/dayssincepro/CsvImporter.java) (CSV I/O), `EditEventActivity` (UI for new fields)
**Status:** Partially implemented — Phase 3 complete (schema v4 + details UI + migration); Phase 4 CSV work pending
**Author:** DaysSincePro Architecture

---

## 1. Motivation

Two schema-related gaps have been identified:

1. **`end_date` exists in the database but is not represented in CSV.** The `event` table already has an `end_date DATE` column (added in schema version 3), and it is fully wired through the UI ([EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java)) and list/detail views ([MyEventAdapter.java](../app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java#L153-L215), [PastFutureListFragment.java](../app/src/main/java/com/merware/dayssincepro/PastFutureListFragment.java#L150)). However, [CsvExporter.java](../app/src/main/java/com/merware/dayssincepro/CsvExporter.java#L88-L89) only emits `event,date,recur` (single-category) / `category,event,date,recur` (multi-category) — `end_date` is silently dropped on export, and [CsvImporter.java](../app/src/main/java/com/merware/dayssincepro/CsvImporter.java) has no column mapping for it on import. A full CSV export/import round-trip today **loses end-date data**.
2. **A new `details` free-text field is planned**, in addition to the existing `event` (title) column — a genuine new schema column, not just a CSV gap.

Making these changes safely requires confronting an existing structural risk in the migration path (§3) before any new schema version is added.

---

## 2. Current Schema State (as of DB version 3)

```sql
CREATE TABLE category (
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  category TEXT,
  type INTEGER
);

CREATE TABLE event (
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  catId INTEGER,
  event TEXT,
  date DATE,
  recur INTEGER,
  end_date DATE          -- added in v3
);

CREATE TABLE history (
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  eventId INTEGER,
  catID INTEGER,
  date DATE,
  onTime INTEGER,
  note TEXT,
  UNIQUE(eventId, date)
);
```

Version history (from [DatabaseHelper.java](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java#L33-L36)):

- v1: original schema (`category`, `event`)
- v2: added `history` table
- v3: added `event.end_date`

Current CSV schema ([CsvExporter.java](../app/src/main/java/com/merware/dayssincepro/CsvExporter.java#L88-L89)):

- Single-category: `"event","date","recur"`
- Multi-category / full export: `"category","event","date","recur"`

`recur` is an integer sentinel encoding recurrence, not just a boolean: `0` = one-time/non-recurring, `7` = weekly, `14` = biweekly, `30` ≈ monthly, `90` = quarterly, `180` = semi-annual, `365` = annual (see `EditEventActivity` end-date-prefill `switch (iRecur)` logic, [EditEventActivity.java#L202](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java#L202)). This existing "0 means N/A" convention is directly relevant to the placeholder question in §5.

**Does the `""` (empty-string) recommendation for `end_date`/`details` imply any change to the `recur = 0` convention?** No — unrelated, and `recur` stays exactly as-is. `recur` is an `INTEGER` column where `0` has no other legitimate meaning (unlike a `DATE` column, where a magic sentinel date risks colliding with a real date once the pickable range is extended — see §5). There is no ambiguity risk for `recur`, so there's no reason to change its existing sentinel-int convention just because dates are moving to empty-string/`NULL` semantics for "not set."

---

## 3. Critical Pre-Existing Risk: Destructive `onUpgrade` Fallback

**Status: IMPLEMENTED (Phase 0, 2026-09-01).**

**This was addressed before adding schema version 4**, independent of what the new column(s) turn out to be. See [DCR_090126PhasedImpl.md §2](DCR_090126PhasedImpl.md#2-phase-0--standalone-bug-fixes--risk-reduction-no-schema-change-highest-priority) for the completed Phase 0 record.

**Implemented as:** `DatabaseHelper.onUpgrade()` now delegates to a pure, unit-tested `getMigrationStatements(oldVersion, newVersion)` function that chains per-version-step SQL (currently 1→2, 2→3) rather than matching `(oldVersion, newVersion)` pairs combinatorially. An unrecognized transition now throws `IllegalStateException` instead of silently dropping all tables. Test coverage: `DatabaseMigrationTest` (pure-function assertions plus real-SQLite-backed migration execution via `org.xerial:sqlite-jdbc`, verifying v1→v3 and v2→v3 preserve all existing rows and reach the correct final schema).

[DatabaseHelper.onUpgrade()](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java#L70-L91) currently handles only three explicit transitions:

```java
if (oldVersion == 1 && newVersion == 2) { /* add history table */ }
if (oldVersion == 1 && newVersion == 3) { /* add history table + end_date */ }
else if (oldVersion == 2 && newVersion == 3) { /* add end_date */ }
else {
    // DROPS category, event, and history tables entirely, then recreates empty.
    db.execSQL("DROP TABLE IF EXISTS category");
    db.execSQL("DROP TABLE IF EXISTS event");
    db.execSQL("DROP TABLE IF EXISTS history");
    onCreate(db);
}
```

**Any `oldVersion`/`newVersion` combination not explicitly matched falls into the `else` branch, which silently deletes all user data** (all categories, events, and history) and recreates empty tables. If a version 4 is introduced and the upgrade matrix is not extended with an explicit `oldVersion == 3 && newVersion == 4` (and ideally `oldVersion == X && newVersion == 4` for every prior version), **any user still on v1 or v2 who upgrades directly to the version-4-carrying app build will have their entire database silently wiped** with no warning, confirmation, or backup prompt.

**Required remediation, prior to or alongside adding v4:**

1. Replace the `if/else-if/else` chain with a **linear, stepwise migration** structure: apply each version-to-version `ALTER`/`CREATE` step in sequence from `oldVersion` up to `newVersion` (e.g. a `switch`/loop applying v1→v2, then v2→v3, then v3→v4 as needed), rather than matching specific `(oldVersion, newVersion)` pairs combinatorially. This scales correctly regardless of how far behind a user's installed version is, without needing a new explicit branch per possible starting version.
2. Remove (or make explicitly opt-in / logged / confirmed) the destructive drop-and-recreate fallback. At minimum, it should never be the silent default for an unrecognized-but-plausible version transition.
3. Add a regression test that simulates upgrading from each known prior version (1, 2, 3) to the new version and asserts no data loss and correct final schema — this is realistic to write given the project's existing pattern of JVM-testable, real-SQLite-backed tests (`org.xerial:sqlite-jdbc` test dependency already present in [app/build.gradle](../app/build.gradle)).

---

## 4. Planned Schema Additions

1. **`event.details` (TEXT, nullable)** — new free-text description field, distinct from the existing `event` (title) column. Requires:
   - DB version bump to 4, with an explicit `ALTER TABLE event ADD COLUMN details TEXT` migration step (per the hardened migration approach in §3).
   - New form field in `EditEventActivity`.
   - CSV column addition (see §5).
2. **CSV support for `end_date`** — no DB migration required (column already exists since v3); this is purely a CSV format/import-export code change.

---

## 5. CSV Format Evolution & the Missing-Value Placeholder Question

Adding `end_date` and `details` columns to CSV means some rows will legitimately have **no value** for one or both (most events have no end date; `details` is optional). This directly intersects with the fragility already flagged for date parsing (see [DCR_evolveToMaterialDatePicker.md §7.1](DCR_evolveToMaterialDatePicker.md#71-storageparsing-format-is-separate-from-the-picker--and-its-fragile)).

**Two representation strategies considered:**

| Strategy                                                                                                               | Description                                                                                                                                                                                                   | Assessment                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Empty CSV field (`""`)**                                                                                             | Absent `end_date`/`details` written as an empty quoted string, same as how `CsvExporter.formatIsoDate()` already returns `""` for a null/blank input date. Importer treats `""` as "no value" → `NULL` in DB. | **Recommended.** Consistent with existing exporter behavior ([CsvExporter.java](../app/src/main/java/com/merware/dayssincepro/CsvExporter.java#L124-L138) already returns `""` for blank dates), unambiguous, and matches standard CSV/NULL conventions. No collision risk with real data, ever.                                                                                                                                                                                                                                                                                                                                     |
| **Sentinel placeholder date (e.g. `1/1/0` or similar "magic" date)**, analogous to `recur = 0` meaning "not recurring" | Represent "no end date" as a fixed magic date value rather than an empty field.                                                                                                                               | **Not recommended**, for a reason specific to this app's current direction: [DCR_evolveToMaterialDatePicker.md](DCR_evolveToMaterialDatePicker.md) is actively proposing to extend the _minimum selectable real date_ back to 1/1/1 (recorded-history floor). Any magic-date sentinel near year 0/1 risks **colliding with a legitimate, real, user-entered date** once that floor is extended — an ambiguity the `recur = 0` pattern doesn't have, because `recur` has no "real" data value of `0` that means something else. Dates don't have a safe unused corner of their value space once the range is opened up that far back. |

**Recommendation:** Use empty-string/`NULL` semantics for missing `end_date` and `details` in CSV, **not** a sentinel date — precisely because of the concurrent work to open up the low end of the valid date range. This should be treated as a hard constraint shared between both DCRs, not an independent decision.

**Import-side handling implications:**

- `CsvImporter` must treat an empty `end_date` field as "no end date" (`NULL`/absent), matching current `EditEventActivity` semantics where an absent end date already means "not set" (see `showEndDateFields(false)` path).
- Backward compatibility: existing 3-column (`event,date,recur`) and 4-column (`category,event,date,recur`) CSV files (without `end_date`/`details`) must continue to import correctly — column detection/mapping in `CsvImporter` needs to treat the new columns as optional, not required, keyed by header name (the importer already does header-name-based column mapping per its class documentation, which should extend cleanly).
- Export format version/documentation should be updated so external tools/users editing CSVs by hand know the new column semantics (empty means "not set", not "invalid").

---

## 6. `details` Field Length & Character Support

**Practical length cap: 256 characters, recommended.**

- No storage-layer constraint requires a cap — SQLite `TEXT` is unbounded, and RFC-4180 CSV (already used by `CsvExporter`/`CsvImporter`) has no field-length limit of its own (Excel's cell limit is ~32,767 characters, far beyond anything relevant here).
- The cap is purely a **UX/screen-real-estate decision**, enforced at the UI layer (`android:maxLength` on the new `details` `EditText`, plus the same limit validated before DB insert so CSV-imported rows can't bypass it).
- 256 characters comfortably fits multi-sentence descriptive text. Worked example from the request — "Ørsted discovers electromagnetism. Shows that electric currents produce magnetic fields, unifying electricity and magnetism experimentally." — is ~140 characters, well under a 256 cap, with headroom for similarly-scoped entries (e.g. the ~155-character Pliny the Elder example in [DCR_evolveToMaterialDatePicker.md §7.4](DCR_evolveToMaterialDatePicker.md#74-minimum-date-floor-01010001)). Recommend **256** as the cap; revisit to ~500 only if real usage shows 256 is frequently truncating legitimate entries.
- UI implication: the `details` field should use a multi-line `EditText` (`android:inputType="textMultiLine|textCapSentences"`) rather than the single-line style used for `event` today ([edit_event.xml#L19-L25](../app/src/main/res/layout/edit_event.xml#L19-L25)), given the longer expected content.

**Side question — does the existing `event` (title) field support characters like "Ørsted" or "Schrödinger"? Confirmed: yes, already, no change needed.**

- [edit_event.xml](../app/src/main/res/layout/edit_event.xml#L19-L25)'s `event` `EditText` has no `inputType` restriction and no character filter — plain Unicode text entry.
- `event` is a SQLite `TEXT` column (unicode-safe by default), Java `String`/`SimpleDateFormat` handling is UTF-16 internally, and CSV export/import already uses `StandardCharsets.UTF_8` explicitly ([CsvExporter.java](../app/src/main/java/com/merware/dayssincepro/CsvExporter.java), [CsvImporter.java](../app/src/main/java/com/merware/dayssincepro/CsvImporter.java)).
- Every layer already round-trips standard Unicode (Ø, ö, etc. are ordinary Basic-Multilingual-Plane characters — no surrogate-pair or normalization handling required). The same will hold for the new `details` column with no extra work. Worth a regression test regardless (see §8).

---

## 7. Resolved Decisions

1. **Migration matrix rewrite (§3) sequencing — RESOLVED: standalone, prioritized ahead of the `details`/CSV work.** It's a data-loss risk that exists today regardless of any new column, so it should not wait on or be bundled with the v4 schema change.
2. **`details` field length/validation — RESOLVED:** see §6 (256-character cap, multi-line input, no DB/CSV-layer constraint needed).
3. **CSV column ordering — RESOLVED: append as `event,date,recur,end_date,details`.** Minimizes disruption to existing hand-edited/external files, matches likely usage frequency (older/simpler files stay valid prefixes of the new format), and puts `details` last so it wraps naturally as the final column when opened in Excel/Sheets.
4. **Retroactive CSV re-export — RESOLVED: "import old, re-export new" is sufficient; no "stale file" messaging needed.** Given the small number of existing users on this path, this was a deliberate, conscious scope decision rather than an oversight — the implementation should include a short code comment noting this was considered and intentionally deferred, so a future reader doesn't mistake the lack of staleness-detection for an omission.
5. **Duplicate-detection key impact — RESOLVED: do _not_ fold `end_date`/`details` into the uniqueness key.** The request was: if an incoming CSV row matches an existing `(catId, event, date, recur)` key but carries `end_date`/`details` that the existing DB row lacks, the _richer_ incoming data should "win." This is **not the same as** folding those fields into the key — folding them in would mean two rows differing only by `details` are treated as two distinct events (both retained, no dedup), which is not what's wanted here. Instead, the key stays `(catId, event, date, recur)` exactly as today, and import behavior changes from "skip on key match" to an **enrichment-on-duplicate** rule: on a key match, if the existing DB row's `end_date`/`details` is `NULL`/empty and the incoming row has a non-empty value, `UPDATE` the existing row with the incoming value(s) instead of skipping; if the existing row already has a value, leave it untouched (existing data is never overwritten by import). This is a behavior change from the current importer, which unconditionally skips on key match ([CsvImporter.java#L592-L596](../app/src/main/java/com/merware/dayssincepro/CsvImporter.java#L592-L596)).
6. **Placeholder representation — RESOLVED: use `""` (empty string / `NULL`), not a sentinel date.** Confirmed and cross-referenced with [DCR_evolveToMaterialDatePicker.md §7 / §8](DCR_evolveToMaterialDatePicker.md), which has been updated to reflect the same resolution. See §5 above for full rationale.

---

## 8. Additional Tests to Capture

- **Migration regression suite** (§3, item 3): simulate upgrade from each known prior version (1, 2, 3) to the new version 4 and assert no data loss plus correct final schema, using the existing `org.xerial:sqlite-jdbc` JVM test dependency.
- **`recur = 14` (biweekly) coverage:** confirm existing recurrence-based logic (end-date default prefill, notification interval math) correctly handles `14` alongside the other sentinel values — currently only implicitly exercised; add an explicit case.
- **CSV round-trip test for `end_date` and `details`:** export → re-import a row with both populated, both empty, and only one populated; assert exact round-trip fidelity.
- **Backward-compatibility import test:** import a legacy 3-column (`event,date,recur`) and legacy 4-column (`category,event,date,recur`) CSV file (i.e. without `end_date`/`details` headers) and confirm they still import correctly under the new importer.
- **Enrichment-on-duplicate test (§7, item 5):** import a row matching an existing key where the existing DB row has `NULL` `end_date`/`details` and the incoming row has values — assert the existing row is updated in place, not skipped or duplicated; and a second test where the existing row already has values — assert it is left untouched.
- **Unicode round-trip test:** an `event`/`details` value containing "Ørsted" and "Schrödinger" (or similar) through DB insert/read, and through CSV export/import, asserting byte-exact round-trip.
- **`details` length-cap UI test:** confirm `EditText` `maxLength` enforcement; confirm an over-length value arriving via CSV import is both truncated to 256 characters **and** recorded as a warning in `CsvImportResult` (resolved behavior, see §9 item 1).

---

## 9. Resolved Decisions (continued)

1. **Details over-length CSV import handling — RESOLVED: truncate to 256 characters AND record a warning in `CsvImportResult`.** Not a silent truncation, and not a row rejection — the row still imports (with truncated `details`), but the import summary surfaces that truncation occurred so the user is aware. Scheduled for Phase 4 in [DCR_090126PhasedImpl.md](DCR_090126PhasedImpl.md#6-phase-4--csv-format-v2-end_date--details).
2. **Phased rollout sequencing across DCRs — RESOLVED: see [DCR_090126PhasedImpl.md](DCR_090126PhasedImpl.md).** That document sequences this DCR alongside [DCR_evolveToMaterialDatePicker.md](DCR_evolveToMaterialDatePicker.md) and [DCR_OldEvents-HandlingNotifications.md](DCR_OldEvents-HandlingNotifications.md) into 5 phases.
