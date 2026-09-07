# Design Change Request (DCR): Dynamic History Departure From Primary Event Creation

Document ID: DCR-2026-09-07-E
Status: Draft - Option 1 selected for near-term implementation planning; Option 2 deferred
Author: DaysSincePro Architecture

## 1. Problem Statement

The current long-press action model mixes two concepts:

1. Event anchor date semantics (the primary event date used by tabs and sorting)
2. Occurrence history semantics (a timeline of happened instances)

Because long-press actions currently mutate the primary event date and also write history in some paths, one-time events can appear to "reverse" expected meaning between Days Since, Since Last, and Until Next. In addition, history deletion can appear to fail because reopening History can auto-recreate the deleted row.

## 2. Repro Scenario From Ad-hoc testing/exploration of inherited codebase

Scenario A: one-time event with Happened Today

1. Create one-time event: "Test2 individ event" at 2026-08-27
2. Long press event in event list after initial persistence and choose Happened Today on 2026-09-07
3. Observe tab behavior:

- Days Since now reflects 2026-09-07 anchor behavior
- Since Last still appears tied to original semantics in user mental model
- Overall feels reversed for a one-time event

Scenario B: add To Happen Tomorrow after prior steps

1. Same event now has historical dates visible: 2026-08-27, 2026-09-07, 2026-09-08
2. User expectation: all three concepts should be representable in order:

- Days Since (original creation/anchor)
- Since Last (most recent happened)
- Until Next (future planned)

3. Current behavior does not consistently render this mapping

Scenario C: remove history row appears not to persist

1. Open History
2. Long press 2026-09-07 row and remove, confirm yes
3. Return to event list, reopen History
4. 2026-09-07 row appears again
5. User expectation: row should remain deleted

## 3. Current Experience vs Expected Experience

Current experience (implemented today)

1. Happened Yesterday/Today/Tomorrow writes a history row using the previous event date and then updates event.date to the new selected date.
2. History launch from event long-press auto-writes a history row before opening History, if the unique key permits.
3. Days Since and Since Last tab eligibility are based on event.date <= today; Until Next eligibility is based on event.date > today or recurring with future eligibility rules.
4. History rows do not drive tab eligibility or core anchor semantics.
5. CSV exports event rows only (category, event, date, recur, end_date, details) and excludes history.

Expected experience (Marc's intent as the codebase owner/maintainer)

1. One-time event should keep clear anchor semantics:

- Days Since should generally stay tied to original anchor date unless explicitly edited as anchor.

2. Since Last should represent most recent happened occurrence when a history concept is present.
3. Until Next should appear only when a future planned occurrence is present, including one-off planned future dates where appropriate.
4. Removing a history row should actually persist without silent re-creation side effects.
5. User should understand the persistence model and roundtrip behavior:

- DB backup/restore should preserve full occurrence/history state.
- CSV export/import should either preserve history or clearly disclose that it does not.

## 4. Root Causes Identified During Investigation

1. Action coupling:

- Happened actions currently do two things at once: log history and mutate event.date.
- This conflates occurrence logging with anchor editing.

2. History auto-insert on open:

- Opening History from the event menu triggers a history insert path before rendering the screen.
- Deleting a row can look ineffective because reopen can re-add the same date.

3. Tab semantics are event-row centric, not occurrence-centric:

- Tab filters primarily read event table date/recur/end_date, not history table.
- As a result, multiple history rows do not naturally map to simultaneous Days Since / Since Last / Until Next views for one-time events.

4. Data transport asymmetry:

- Full DB path preserves event + history.
- CSV path preserves event rows but drops history.

## 5. Candidate Solutions

Maintainer direction update

1. Option 3 is de-prioritized and not under current consideration.
2. Option 1 is the near-term implementation direction.
3. Option 2 remains open for future reevaluation if usage or product value justifies deeper investment.
4. Option A is selected for CSV full-dataset risk disclosure under Option 1.

### Option 1: Decouple actions with minimal schema change (lowest risk)

Summary

Keep current event table as primary anchor record. Redefine long-press actions so Happened actions write occurrence history only by default, and do not mutate event.date unless user chooses explicit anchor edit.

Scope note

1. Option 1 explicitly includes the implementation requirements in section 6.
2. Section 6 is not separate work; it is the first-pass Option 1 delivery checklist.

Behavior

1. Happened Yesterday/Today/Earlier:

- Insert occurrence in history only.
- Keep event.date unchanged.

2. To Happen Tomorrow:

- For one-time events, set planned next occurrence field (new nullable column, e.g. planned_date) or create a future occurrence record type.
- For recurring events, continue recurrence-driven next behavior.
- UX requirement: in the long-press menu for one-time events, To Happen Tomorrow is disabled and visually greyed out.

3. Skip:

- For recurring events, skip should advance cycle semantics without rewriting historical anchor meaning.
- UX requirement: for one-time events, Skip is disabled and visually greyed out.

4. History:

- Never auto-insert on open.
- History Add/Edit enforces happened-date validity: future dates are not allowed.

Schema impact

1. Optional small addition:

- event.planned_date DATE nullable (for one-time future planning)

2. No mandatory history table rewrite.

Pros

1. Closest to user expectation with small migration footprint.
2. Clear mental model: anchor edit is explicit, occurrence log is separate.
3. Fixes the remove-then-reappear bug by removing auto-insert on History open.

Cons

1. Requires careful UI copy to separate "Mark happened" from "Change anchor date".
2. Some existing users may rely on current date-mutation behavior.

CSV/DB implications

1. DB backup/restore preserves all fields.
2. CSV remains history-lossy by format, but Option 1 adds a full-dataset CSV export warning gate (Option A) when any history data exists.
3. Warning applies only to full dataset export (DaysSince.csv style), not per-category/sheet exports.
4. Warning always includes both counts: total History entries and distinct events with History.
5. Warning message instructs users that DB export is required for full-fidelity roundtrip/replace workflows.
6. Option A action behavior is explicit:

- Continue CSV Export: proceed with full CSV export.
- Export .db Instead: route directly to DB export flow.
- Cancel: abort export and return without writing CSV.

Recommendation level

High.

### Option 2: Occurrence-first model (most correct, highest effort)

Summary

Treat event as metadata and store all temporal instances in an occurrence table (historical and planned). Compute all three tabs from occurrence projections.

Behavior

1. Event anchor remains immutable creation/reference date.
2. Days Since uses configured anchor strategy (original anchor by default).
3. Since Last uses latest occurred instance.
4. Until Next uses nearest planned/future instance.
5. Long-press actions create/update occurrence rows only.

Schema impact

1. New table, e.g. occurrence:

- id
- event_id
- date
- type (happened or planned or skipped)
- source_action
- note

2. Migration to backfill initial occurrence from event.date.

Pros

1. Cleanest semantics and strongest future extensibility.
2. Natural fit for one-time plus planned-next and recurring workflows.
3. Strong auditability and undo potential.

Cons

1. Largest migration and query refactor.
2. Higher regression risk and testing burden.

CSV/DB implications

1. DB preserves full model.
2. CSV would need a second file or expanded schema for occurrences to avoid loss.

Recommendation level

Medium to high if planning a broader revamp cycle.

### Option 3: Hybrid compatibility mode with explicit user setting

Summary

Support two long-press behavior modes:

1. Legacy mode (current mutate + history behavior)
2. Occurrence mode (log only, no anchor mutation)

Behavior

1. New setting: Long-press happened actions update anchor date (on/off).
2. Default for new installs: off (occurrence mode).
3. Existing installs migrate with legacy default to reduce surprise.
4. Remove History auto-insert regardless of mode.

Schema impact

1. No mandatory schema change.
2. Optional planned_date support still recommended for one-time future planning.

Pros

1. Lowest disruption for existing users.
2. Allows staged transition and user feedback collection.

Cons

1. Ongoing complexity in code paths and QA matrix.
2. Product semantics can remain fragmented.

CSV/DB implications

1. Same as Option 1 unless occurrence export is expanded.

Recommendation level

Medium.

Current disposition

1. Not being pursued.

## 6. Option 1 First-Pass Implementation Requirements

1. Remove history auto-insert when entering History from event context menu.
2. Ensure history delete is authoritative and remains deleted unless user explicitly re-adds.
3. Event delete must cascade history cleanup by eventId:

- When deleting an event, also delete all history rows where history.eventId matches the event \_id.
- Execute history-delete + event-delete in a single transaction.

4. Align long-press action labels with actual behavior.
5. For one-time events, disable and grey out To Happen Tomorrow in the long-press menu.
6. On full dataset CSV export only, if any History exists, show pre-export warning (Option A) before allowing CSV continuation.
7. For one-time events, disable and grey out Skip in the long-press menu.
8. Block future happened entries at all applicable history-entry points:

- Picker-level guard in History Add/Edit date selection.
- Save-time validation in History Add/Edit flow.
- Defensive write-time validation before History insert/update.

8a. Planned-date auto-clear UX transparency:

- If planned_date is auto-cleared because it is no longer a valid future date, show a single non-blocking informational message.
- Message style: Toast/Snackbar only (no modal dialog, no user confirmation prompt).
- Suggested copy: Planned future date cleared because it is no longer in the future.

9. Schema v5 pull-forward cleanup policy:

- When upgrading existing installs to schema version 5, run a one-time orphan cleanup for legacy history rows:
  - delete from history where eventId not in (select \_id from event)
- This cleanup is migration-time only for upgraded installs.
- Fresh installs on version 5+ do not run a special orphan cleanup path.

10. Add regression tests covering:

- one-time 2026-08-27 -> Happened Today on 2026-09-07
- add To Happen Tomorrow (recurring event path)
- delete history row then reopen
- delete event and verify related history rows are removed
- tab projections after each step
- full dataset CSV export warning appears when History exists and is bypassed when no History exists
- one-time long-press menu shows To Happen Tomorrow disabled and Skip disabled
- history Add/Edit rejects future happened dates
- schema v5 upgrade removes legacy orphan history rows while preserving valid history rows
- planned_date auto-clear emits one informational message and does not block user flow

## 6.1 Projection Rules Clarification (explicit semantics target)

The following rules are the intended semantic contract for projections:

1. One-time event (recur = 0):

- If no explicit future planned date exists, Until Next is blank.
- A future-dated history row does not project into Until Next.
- Long-press menu behavior is explicit: To Happen Tomorrow is disabled and greyed out for one-time events.
- Long-press menu behavior is explicit: Skip is disabled and greyed out for one-time events.

2. Recurring event (recur > 0):

- Baseline Since Last and Until Next derive from recurrence schedule.
- Manual history rows can override recurrence-derived projection only when they are nearer/more relevant to today and logically valid for that projection.
- Result may be:
  - Since Last only,
  - Until Next only,
  - or both, when both past and future relevant occurrences exist.

3. Global projection contract:

- Since Last = latest valid occurred date on or before today.
- Until Next = earliest valid planned/recurring date strictly after today.
- Future "happened" rows are invalid as occurred-state inputs.
- Future "happened" date entry is blocked, not merely ignored.

## 6.2 Option 1 Runway to Potential Future Option 2

Transition feasibility

1. Yes, Option 1 can transition to Option 2 in a straightforward and low-risk phased path.
2. This runway is intentionally future-facing and is not part of current Option 1 delivery scope.

Suggested migration path

1. Phase A (Option 1 implementation):

- Decouple happened-actions from anchor mutation.
- Remove history auto-insert on History open.
- Add minimal planned-future representation for one-time events (for example event.planned_date).

2. Phase B (bridge release):

- Introduce occurrence table in parallel.
- Backfill occurrence rows from event anchor + history + planned future state.
- Start dual-write (legacy fields and occurrence table).

3. Phase C (Option 2 activation):

- Switch projections (Days Since, Since Last, Until Next) to occurrence-first reads.
- Keep dual-write briefly for verification.

4. Phase D (stabilization):

- Remove obsolete read dependencies from legacy projection logic.
- Keep one-time migration/backfill safeguards and tests.

Rationale

1. This preserves near-term delivery value from Option 1 while keeping a clean upgrade path to Option 2 without rework waste.

## 7. Persistence and Roundtrip Clarification

Current state

1. DB backup/restore path is full-fidelity for event and history.
2. CSV export/import is event-centric and does not preserve history.

Implication

1. CSV roundtrip can lose occurrence history semantics today.
2. Under Option 1, this risk is actively mitigated for full dataset CSV export via an explicit warning gate (Option A) that redirects replace-intent users to DB export.
3. If occurrence semantics become first-class, CSV strategy must be versioned:

- either multi-file export (events + occurrences)
- or expanded CSV model with row type markers

Notification scope note (Option 1)

1. Option 1 does not change reminder generation semantics.
2. Existing reminders continue to derive from event-level recurrence fields (date, recur, last_notified_date), not from history rows.
3. Known limitation during Option 1: Until Next display may reflect a manual planned/future override before reminder timing reflects that same override.
4. This notification-alignment work is deferred and may be addressed in a follow-up phase.

DB roundtrip orphan note

1. DB export/import is byte-for-byte state preservation and does not perform relational cleanup.
2. Therefore, any existing orphan history rows present at export time remain present after DB restore.
3. Option 1 prevents new event-delete orphans going forward via transactional cascade delete behavior.
4. Option 1 also includes a one-time schema v5 upgrade cleanup for legacy orphans.
5. Fresh installs on version 5+ do not require a cleanup pass.

## 8. Direction and Deferred Path

Current direction:

1. Implement Option 1.
2. Keep Option 2 documented as a future path, not current scope.

Option 1.1 (planned intermediate step before Option 2 consideration):

1. Align reminder timing source selection with the same Until Next projection source used by Option 1 display semantics.
2. Keep this as a targeted follow-up scope without adopting the full Option 2 occurrence-first model.
3. Reevaluate Option 2 only after Option 1 and Option 1.1 outcomes are observed in usage and support feedback.

Option1.1 is probably a medium add-on, not a major rewrite.

Roughly:

1. Effort: about 1 to 2.5 days if done immediately after Option1.
2. Risk: medium-low, mostly logic consistency and edge-case testing.
3. Why not “trivial”: notifications are currently driven by event-level recurrence fields, so we’d be changing the reminder source-of-truth to match display projection rules, then validating no regressions in existing reminder timing.

Deferred reevaluation trigger candidates:

1. Measurable growth in manual-history feature usage.
2. Repeated user demand for richer occurrence-first workflows.
3. Data model pressure that cannot be cleanly handled in Option 1.

## 9. Option 1 Effort Estimate

Scope assumptions for estimate

1. No compatibility-mode setting (Option 3 excluded).
2. No full occurrence-table migration in this phase.
3. Include production code updates, unit tests, and light DCR/docs updates.

Estimated effort

1. Engineering: 4.5 to 7.5 working days.
2. Testing and validation hardening: 1 to 2 additional days.
3. Total: 5.5 to 9.5 working days.

Work breakdown

1. Long-press action decoupling and history-open behavior cleanup: 1.5 to 2.5 days.
2. Projection-rule updates across Days Since / Since Last / Until Next for one-time vs recurring semantics: 1.0 to 2.0 days.
3. Minimal planned-future representation for one-time events (including schema migration if adding event.planned_date): 1.0 to 1.5 days.
4. Regression test additions for reported scenarios and edge cases: 1.0 to 1.5 days.
5. Full dataset CSV warning gate (Option A) including counts and action choices: 0.5 to 1.0 day.
6. Triage/fix cycle after integration pass: 0.5 to 1.0 day.

Risk-adjusted notes

1. If planned-future support is deferred (no schema change), estimate trends toward low end.
2. If planned-future is included plus richer UI affordances, estimate trends toward high end.
3. If Option 2 bridge hooks (dual-write scaffolding) are added early, add roughly 1 to 2 days.

Option A warning copy target (full dataset CSV export; only when history entries > 0)

1. Title: History data not included in CSV export
2. Body: CSV exports include events and categories, but not History entries. Your data currently has <historyEntries> History entries across <eventsWithHistory> Events. If you plan to roundtrip and replace all data later, use Export Database (.db) instead.
3. Actions: Continue CSV Export, Export .db Instead, Cancel

Count definitions

1. <historyEntries> = total rows in history.
2. <eventsWithHistory> = count of distinct events that have at least one history row.
3. Counts are computed across the full database scope (not category-filtered), matching full dataset export scope.

## 10. Acceptance Criteria (Option 1 first pass)

1. History delete persistence:

- Deleting a history row and reopening History does not recreate the deleted row unless user explicitly re-adds it.

2. History-open side effects:

- Opening History does not auto-insert a history row.

3. One-time projection behavior:

- One-time event shows blank Until Next unless explicit planned future date exists.

4. One-time long-press menu constraints:

- To Happen Tomorrow is disabled and greyed out.
- Skip is disabled and greyed out.

5. Future happened-date validation:

- History Add/Edit cannot persist a happened date after today.

6. CSV warning scope and trigger:

- Full dataset CSV export: warning shown only when historyEntries > 0.
- Full dataset CSV export: no warning when historyEntries == 0.
- Per-category CSV export: no warning.

7. CSV warning content and actions:

- Warning includes both counts: historyEntries and eventsWithHistory.
- Export .db Instead action routes directly to DB export flow.
- Continue CSV Export and Cancel behave as labeled.

8. Event delete cascade behavior:

- Deleting an event removes all related history rows for that eventId in the same transaction.

9. Legacy orphan behavior documentation:

- DB export/import does not claim to auto-clean existing orphans.

10. Schema v5 upgrade cleanup behavior:

- Upgraded installs run one-time orphan cleanup during migration to v5.
- Fresh installs on v5+ do not require a special orphan cleanup pass.

11. Planned-date auto-clear UX behavior:

- Auto-clear shows one non-blocking informational message and does not require user confirmation.

12. Notification-scope boundary (Option 1):

- Option 1 does not alter reminder generation logic.
- Any Until Next vs reminder-date alignment beyond current event-level recurrence logic is explicitly deferred.

Implementation planning checklist (Option 1):

1. UI copy updates
2. schema migration plan (if any)
3. query/projection changes by tab
4. DB and CSV roundtrip policy
5. regression test matrix
