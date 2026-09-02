# Uncategorized Events: Visibility, Semantics, and Round-Trip Safety

## Summary

Uncategorized events are currently represented by sentinel category id 0 in the event table, but the UI mostly presents only real category rows from the category table. That mismatch causes uncategorized events to be easy to hide and hard to rediscover once real categories are created.

Target outcome: shift Uncategorized from a hidden fallback state to a first-class, explicit, user-selectable category concept across filter, picker, export, and import flows.

There is also a CSV semantic drift risk: full CSV export currently writes the text value Uncategorized, and CSV import treats non-empty category text as a real category name, which can convert sentinel-uncategorized events into a newly created real category named Uncategorized.

No immediate hard data deletion is forced by this behavior alone, but the current design increases risk of accidental user confusion, invisible data, and destructive follow-on actions.

## Decision Status

1. Selected approach: Option A is accepted as the implementation direction for this DCR.
2. Not selected: Option B and Option C remain documented for context but are not planned for execution.
3. Compatibility posture: preserve backward compatibility and validate thoroughly before release, while recognizing current user/data footprint is small enough to allow pragmatic staged rollout and aggressive test coverage.

## Current Behavior (Evidence)

1. Filter selection only lists real category table rows, not sentinel uncategorized.

- Category chooser queries only category table rows in [app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java](../app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java#L241).
- It persists selected category ids into preference CategoryIds, and if nothing is selected it writes [0] (sentinel uncategorized) in [app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java](../app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java#L283).
- If nothing is selected while categories exist, it shows confirmation dialog no_category_chosen + uncategorized_only in [app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java](../app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java#L264) and text in [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml#L126).

2. Event list filtering applies CategoryIds directly as catId IN (...).

- Filtering SQL applies catID in selected ids in [app/src/main/java/com/merware/dayssincepro/PastFutureListFragment.java](../app/src/main/java/com/merware/dayssincepro/PastFutureListFragment.java#L172).
- Therefore, once one or more real categories are selected, uncategorized (catId=0) is excluded unless 0 is explicitly selected.
- There is no regular selectable uncategorized row in the category chooser.

3. Add/Edit Event category dropdown also lists only real category table rows.

- Spinner data source uses db.query("category", ...) in [app/src/main/java/com/merware/dayssincepro/EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java#L716).
- Uncategorized assignment is handled via checkbox off => catId=0 in [app/src/main/java/com/merware/dayssincepro/EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java#L312).
- This is functional, but uncategorized is not a visible peer option in the category list itself.

4. Other category dropdown surface also excludes uncategorized.

- Event chooser category spinner is built from select \_id, category from category in [app/src/main/java/com/merware/dayssincepro/EventChooserActivity.java](../app/src/main/java/com/merware/dayssincepro/EventChooserActivity.java#L202).
- If categories exist, event selection is constrained to selected real category in [app/src/main/java/com/merware/dayssincepro/EventChooserActivity.java](../app/src/main/java/com/merware/dayssincepro/EventChooserActivity.java#L235).

5. CSV full export labels missing category as text Uncategorized.

- COALESCE(c.category, 'Uncategorized') in [app/src/main/java/com/merware/dayssincepro/CsvExporter.java](../app/src/main/java/com/merware/dayssincepro/CsvExporter.java#L209).

6. CSV import treats any non-empty category text as real category name.

- Category resolution/creation path in [app/src/main/java/com/merware/dayssincepro/CsvImporter.java](../app/src/main/java/com/merware/dayssincepro/CsvImporter.java#L463).
- Row import picks resolved category when category column is non-empty in [app/src/main/java/com/merware/dayssincepro/CsvImporter.java](../app/src/main/java/com/merware/dayssincepro/CsvImporter.java#L700).
- Net effect: exported Uncategorized text may be re-imported as a real category row, not sentinel 0.

7. DB backup/restore path is raw SQLite snapshot/copy and preserves catId values exactly.

- DB export snapshot and stream in [app/src/main/java/com/merware/dayssincepro/MainActivity.java](../app/src/main/java/com/merware/dayssincepro/MainActivity.java#L511).
- DB restore file replacement in [app/src/main/java/com/merware/dayssincepro/MainActivity.java](../app/src/main/java/com/merware/dayssincepro/MainActivity.java#L573).

8. Category long-press CSV export filename is based on selected category display text.

- Long-press export builds filename as sanitizeFilename(selectedCategory) + .csv in [app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java](../app/src/main/java/com/merware/dayssincepro/CategoriesActivity.java#L567).
- Today, because the category list only contains real category table rows, there is no long-press export path for uncategorized events.
- If uncategorized later becomes a synthetic selectable row, this flow would naturally propose Uncategorized.csv unless explicitly overridden.

## Why This Feels Like Lost Data

Users experience a visibility trap:

1. They create categories and select one or more of them.
2. Uncategorized events disappear from filtered tabs because catId 0 is not represented in normal category selections.
3. Recovery path is unintuitive: deselect all and accept warning dialog to force [0] (uncategorized only).
4. In CSV round trips, uncategorized can silently drift into a normal category label, changing future filter behavior.

This is mostly discoverability and semantic consistency failure, with secondary data interpretation drift in CSV.

## Round-Trip Matrix (Current)

1. DB export -> DB import: safe.

- catId is preserved bit-for-bit.
- Uncategorized sentinel semantics remain intact.

2. CSV export-all -> CSV import-all: semantic drift risk.

- Export writes category text Uncategorized for uncategorized rows.
- Import treats that as a named category and may create category row Uncategorized.
- Events may move from catId 0 to real catId N.

3. CSV per-category export/import: uncategorized unsupported as first-class category.

- Per-category export is initiated from real category context menu only.
- There is no uncategorized row to export as a peer category today.
- So there is currently no way to generate Uncategorized.csv from category long-press.

4. Cross-mode (DB -> CSV -> DB restore later): mixed behavior.

- DB path preserves sentinel state.
- CSV path may normalize into named category state.
- User can end up with different semantics depending on backup mode.

## Remedy Options

### Option A: Treat Uncategorized as a first-class synthetic category in UI and CSV semantics (recommended)

1. Keep storage model unchanged: catId 0 continues to mean uncategorized.
2. Introduce synthetic category row in every category selector surface:

- CategoriesActivity multi-select list.
- EditEventActivity category spinner/list control.
- EventChooserActivity category spinner.

3. Show synthetic row only when uncategorized event count > 0 (as requested), or always show it disabled/enabled by policy. Recommended default: show when count > 0.
4. Remove no-category-chosen warning when user explicitly chooses synthetic uncategorized state.
5. Preserve explicit mixed filtering support: users can select both real categories and uncategorized simultaneously.

Pros:

- Matches user mental model.
- Removes hidden-state hunting.
- No schema migration needed.

Cons:

- Requires adapter-level augmentation logic in multiple screens.

### Option B: Materialize a real category row named Uncategorized in database

1. Create/maintain a real category record for uncategorized.
2. Migrate catId 0 events to that row.

Pros:

- Simple list handling (no synthetic rows).

Cons:

- Breaks current sentinel assumptions.
- Needs migration and careful conflict handling.
- Makes uncategorized removable unless heavily guarded.

Not recommended for this codebase as first step.

### Option C: Keep current model, only tweak warning copy

Pros:

- Minimal code.

Cons:

- Does not solve discoverability trap.
- Does not solve CSV semantic drift.

Not recommended.

## Recommended Holistic Design

### 1) Canonical meaning

1. Internal canonical uncategorized value remains catId 0.
2. UI display label is Uncategorized.
3. Uncategorized is treated as reserved semantic token, not a normal user-editable category identity.

### 2) Centralize uncategorized policy in helper layer

Add one shared utility (for example CategorySelectionPolicy) with methods like:

1. hasUncategorizedEvents(db): boolean.
2. buildCategoryOptions(db, sortOrder, includeSyntheticUncategorizedIfPresent): list/model.
3. isUncategorizedToken(text): boolean for import parsing, including blank/null and normalized uncategorized literals.
4. normalizeImportedCategory(catText, defaultCategoryId): returns target catId with reserved-token handling.

This avoids repeated ad hoc checks in each Activity.

### 3) UI behavior changes

1. CategoriesActivity:

- Include synthetic uncategorized row when count > 0.
- Allow selecting it alongside real categories.
- Remove warning path tied to deselect-all -> uncategorized-only, because uncategorized becomes explicit and selectable.
- Continue writing CategoryIds with 0 when uncategorized is selected.
- Add long-press behavior for the synthetic uncategorized row so users can export uncategorized-only events directly.

4. CategoriesActivity long-press export naming and semantics:

- Expected export filename for synthetic uncategorized row: Uncategorized.csv.
- Export content shape remains the single-category format (no category column), but all rows map to catId 0 semantics.
- Importing that file via category-scoped import should route to catId 0 (uncategorized), not create a real category row named Uncategorized.
- Reserve the uncategorized display token in UI policy to avoid ambiguity between synthetic uncategorized and a user-created literal category named Uncategorized.

2. EditEventActivity:

- Include uncategorized option in category dropdown model when count > 0.
- Keep checkbox behavior for backward compatibility, or simplify by replacing checkbox with direct dropdown choice if desired in a later UX pass.

3. EventChooserActivity:

- Include uncategorized option to allow selecting uncategorized events directly when both categorized and uncategorized data exist.

### 4) CSV semantics changes

1. Export:

- Prefer empty category field for uncategorized rows, or reserved marker explicitly documented as sentinel.
- Strong recommendation: export empty category field for uncategorized to avoid creating fake categories in third-party editing.

2. Import:

- Treat blank/null/whitespace category as uncategorized (catId 0) when category column exists.
- Treat normalized text token Uncategorized as uncategorized sentinel (backward compatibility with existing exports).
- Never auto-create a real category row for reserved uncategorized token.

3. Backward compatibility:

- Existing historical CSVs containing Uncategorized should import to catId 0 after this change.

4. Per-category long-press compatibility:

- If users manually rename exported uncategorized files, routing should still be based on row/category token semantics, not filename alone.
- Filename inference should not be allowed to convert uncategorized exports into a real category named Uncategorized.

### 5) DB backup/restore

No semantic change needed. Current DB path is already correct for preserving uncategorized state.

## Suggested Test Additions

Add JVM tests around extracted pure helpers plus SQL-level behavior checks.

1. Category filter model tests

- Given categories A/B plus uncategorized events present, options include synthetic uncategorized.
- Given no uncategorized events, synthetic row omitted (if using conditional policy).

2. CategoryIds persistence tests

- Selecting A + uncategorized persists ids containing both A.id and 0.
- Applying those ids to filter SQL returns union of categorized and uncategorized rows.

3. CSV export/import uncategorized tests

- Full CSV export writes uncategorized representation per new policy.
- Import of blank category routes to catId 0.
- Import of literal Uncategorized routes to catId 0 (compat mode).
- Import does not create category row named Uncategorized from reserved token.
- Synthetic uncategorized long-press export proposes Uncategorized.csv and exports only catId 0 events.
- Re-import of Uncategorized.csv through category-import and app-level import both preserve catId 0 semantics.

4. Round-trip matrix regression tests

- Seed DB with mixed categorized/uncategorized events.
- DB->DB round trip preserves exact catId assignments.
- CSV->CSV round trip preserves uncategorized semantics as catId 0 (not reified category).

5. UI policy helper unit tests

- Reserved token matcher handles case/space variants.
- Default category fallback still applies correctly when category column absent.

## Migration and Rollout Notes

1. Prefer introducing helper and tests first, then wire into UI surfaces one by one.
2. Keep old behavior behind temporary compatibility branches only if needed for safe rollout.
3. Update user-facing copy in category screen to explain explicit uncategorized selection if needed.

## Decision Recommendation

Choose Option A.

It solves the immediate usability issue you described, keeps current schema stable, and closes CSV semantic drift that can otherwise alter uncategorized meaning during round trips.

Status: accepted.

## Implementation Strategy

Phased delivery is recommended over one-pass delivery.

Rationale:

1. The change cuts across UI selection models, persisted filter ids, CSV export/import semantics, and regression tests.
2. Phasing reduces integration risk and makes behavioral diffs easy to validate between steps.
3. With low current production criticality, we can prioritize correctness and test depth before release without rushing a large all-at-once merge.

## Phase Status Updates

1. Phase 1: complete.

- Completed deliverables: uncategorized policy seam via CategorySelectionPolicy, CSV importer decision routing through centralized policy, and focused helper tests.
- Verification completed: targeted JVM unit tests and full release bundle build.
- Build/version note for Phase 1 completion: versionName advanced from 3.9.57.49 to 3.10.58.49.

2. Phase 2: complete.

- Completed deliverables: synthetic uncategorized option (shown only when uncategorized events exist) across CategoriesActivity, EditEventActivity, and EventChooserActivity; normal-flow no-category warning removal; reserved-name blocking for create/edit paths.
- Verification completed: targeted Phase 2 unit tests and full release bundle build.
- Build/version note for Phase 2 completion: versionName advanced from 3.10.58.49 to 3.10.59.49.

3. Phase 3: complete.

- Completed deliverables: CSV uncategorized semantic hardening (empty category export representation for uncategorized sentinel rows, reserved-token mapping to catId 0 on import, and no reserved-name category creation via filename inference).
- Verification completed: targeted CSV/policy unit tests and full release bundle build.
- Build/version note for Phase 3 completion: versionName advanced from 3.10.59.49 to 3.10.60.49.

4. Phase 4: complete.

- Completed deliverables: end-to-end validation sweep and release-readiness pass for uncategorized semantics.
- Validation matrix completed and passing:
  - DB path validation: BackupRestoreValidationTest passed.
  - CSV semantics validation: CsvExportImportTest passed.
  - Policy/selection validation: CategorySelectionPolicyTest and CategorySelectionFilterSqlTest passed.
  - Full regression sweep: :app:testDebugUnitTest passed.
  - Full build gate: bundleRelease passed.
- Build/version note for Phase 4 completion: versionName NOT advanced from 3.10.60.49 as no code was changed and this was strickly intensive validation (and doc only update).

5. Phase 5: complete.

- Chronology note: Phase 5 was identified after Phases 1-4 were already conceived and implemented.
- Why it was added late: post-implementation review showed an intent gap where surfacing Uncategorized as a frequent first/default Add Event choice could implicitly reinforce continued non-use of intentionally defined categories.
- Objective: improve Add Event category adoption by making category creation the visible first action when Uncategorized would otherwise be the first/default selection.
- Scope focus: Add Event screen only.
- First-time UX note: include a lightweight nudge in Add Event to promote intentional category creation without adding friction.
- Nudge persistence rule: keep showing the nudge until at least one real (non-sentinel) category exists.
- Cancel behavior rule: if user cancels category creation, nudge remains and is shown again on later Add Event visits until a real category exists.
- Existing-user gating rule: users who already have one or more real categories do not see this nudge after update.
- Completed deliverables:
  - Add Event category picker now includes synthetic top-row action <Add New Category>.
  - Option 2 enforcement: when Add Event is launched from uncategorized context (catId 0), default category selection is <Add New Category>.
  - Option 2 enforcement: save is blocked while <Add New Category> remains selected, requiring explicit user category action.
  - Selecting the synthetic action opens Categories and auto-opens Add Category input.
  - Add Event no longer hides category controls in zero/zero first-time state; the guided action remains visible.
  - Add Event save path guards against persisting synthetic action ids.
  - Returning from category creation rebinds options and preselects created category when present.
  - First-time nudge copy is shown only while real category count is zero.
- Verification completed:
  - Targeted test: :app:testDebugUnitTest --tests com.merware.dayssincepro.CategorySelectionPolicyTest passed.
  - Full regression sweep: :app:testDebugUnitTest passed.
  - Full build gate: bundleRelease passed.
- Build/version note for Phase 5 completion: versionName advanced from 3.10.60.49 to 3.10.62.49.

### Proposed Phases

1. Phase 1: Policy and test seams

- Introduce centralized uncategorized policy helper(s) for token mapping and option building.
- Add unit tests for token normalization, category option synthesis, and id mapping rules.

2. Phase 2: Filter and picker UX first-classing

- Add synthetic Uncategorized row/option (conditional on uncategorized presence) to CategoriesActivity, EditEventActivity, and EventChooserActivity.
- Remove or repurpose no-category-chosen warning path now that uncategorized is explicit.
- Ensure mixed selections (real categories plus uncategorized) persist and filter correctly.

3. Phase 3: CSV semantic hardening

- Export uncategorized using the chosen canonical CSV representation.
- Import blank/reserved Uncategorized tokens back to catId 0.
- Prevent creation of a real category row from reserved uncategorized token values.
- Cover long-press uncategorized export and re-import semantics explicitly.

4. Phase 4: End-to-end matrix validation and release readiness

- Execute DB and CSV round-trip matrix tests with mixed categorized/uncategorized fixtures.
- Run full unit/build verification and regression pass.
- Confirm docs, migration notes, and release notes are aligned before shipping.

5. Phase 5: Add Event guided category creation UX

- Add a synthetic top-of-list action row in Add Event category picker: <Add New Category>.
- If selected, route user to category management and open Add Category input immediately.
- Preserve existing uncategorized semantics, but reduce accidental defaulting into uncategorized when real categories should be used.
- Keep behavior scoped to Add Event so other category surfaces remain semantically stable.
- This phase is an intent-correction phase: preserve first-class Uncategorized support while restoring positive behavioral nudging toward intentional category use.
- Add first-time lightweight guidance near Add Event category controls to surface category value at the moment of action.
- Nudge copy (primary only): Create a category to keep events organized. Choose Add New Category.
- First-time zero/zero state requirement: when there are zero real categories and zero uncategorized events, Add Event must still show category controls with <Add New Category> so first-time users are actively guided into category creation.

## Pre-Start Considerations

1. Canonical CSV representation decision for uncategorized should be locked before coding (recommended: empty category field plus import compatibility for literal Uncategorized).
2. Synthetic Uncategorized labeling must be reserved in policy logic to avoid accidental collision with user-created literal names.
3. Existing saved preference values in CategoryIds and Categories should be tested for compatibility when introducing synthetic options.
4. Long-press export naming and behavior for synthetic Uncategorized should be finalized (target: Uncategorized.csv, with sentinel semantics preserved on import).
5. Add regression tests in each phase, not only at the end, to catch semantic drift early.

## Resolved Pre-Start Decisions

The following are explicitly confirmed for implementation:

1. Synthetic Uncategorized option is shown only when uncategorized events currently exist.
2. Option A remains the accepted implementation path.
3. Uncategorized display/token handling is reserved to avoid collision with ordinary category-name semantics.
4. Synthetic Uncategorized long-press export uses filename Uncategorized.csv, and import preserves sentinel catId 0 semantics.
5. Canonical CSV policy is accepted as: export uncategorized with an empty category field, and import both empty and normalized Uncategorized token values as catId 0 for backward compatibility.
6. Phase 5 includes a one-time lightweight Add Event nudge encouraging intentional category creation.
7. Nudge is not manually dismissed; it auto-stops only after at least one real category exists.
8. In the initial zero/zero state (no real categories and no uncategorized events), Add Event still surfaces category controls and <Add New Category> as the primary path.

These decisions supersede open ambiguity in the considerations list and should be treated as execution constraints in all phases.

## Acceptance Criteria By Phase

The following criteria are frozen for phase pass/fail.

1. Phase 1 pass criteria (policy and test seams)

- Central uncategorized policy helper exists and is used as the authoritative mapping surface for reserved token checks and option-building rules.
- Targeted helper unit tests pass.
- No user-visible behavior changes are required in this phase.

2. Phase 2 pass criteria (filter and picker UX first-classing)

- CategoriesActivity, EditEventActivity, and EventChooserActivity expose synthetic Uncategorized only when uncategorized events exist.
- Mixed category selection (real categories plus uncategorized) persists and filters correctly.
- Legacy no-category-chosen warning flow is no longer used in normal UX once explicit uncategorized selection exists.
- Targeted UI-policy and selection-state unit tests pass.

3. Phase 3 pass criteria (CSV semantic hardening)

- Export path uses canonical uncategorized CSV representation (empty category field in multi-category exports).
- Import path maps blank category and reserved Uncategorized tokens to catId 0.
- Import path does not auto-create a real category row from reserved Uncategorized tokens.
- Long-press synthetic uncategorized export/import semantics are preserved (Uncategorized.csv naming, sentinel mapping on import).
- Targeted CSV unit tests pass.

4. Phase 4 pass criteria (release readiness)

- Round-trip matrix validations pass for DB and CSV paths with mixed categorized/uncategorized fixtures.
- At least one full build succeeds for the phase gate.
- DCR and release notes are aligned with shipped behavior.

5. Phase 5 pass criteria (guided category creation)

- Add Event category picker shows <Add New Category> as a synthetic first row when category assignment controls are visible.
- In zero/zero first-time state, category controls are visible and <Add New Category> is presented as the actionable default path.
- Selecting <Add New Category> opens Categories flow and auto-opens Add Category input ready for typing.
- Returning from category creation reloads Add Event category options and selects the newly created category by default.
- Canceling the category-add flow does not save with a synthetic action id and does not corrupt category selection state.
- Existing reserved-name guardrails for Uncategorized remain enforced.
- First-time nudge is shown in Add Event with the approved primary copy only.
- Nudge is shown on Add Event visits until a real category exists, including after canceled category-creation attempts.
- Nudge is never shown to users who already have one or more real categories.
- Targeted Phase 5 tests and one full build gate pass.

## Proposed Phase 5 Design Notes

1. Current state validation

- Add Event currently prepends synthetic Uncategorized when uncategorized events exist.
- Add Event currently has no direct in-flow action row to create a category from the category picker.
- Categories Add Category dialog already requests input focus and soft keyboard visibility when opened.

2. Proposed behavior

- Introduce <Add New Category> as a non-persisted synthetic action row in Add Event category picker.
- If there are zero real categories and zero uncategorized events, do not hide category controls; show <Add New Category> immediately as the first-time default action.
- Option 2 guardrail: when Add Event is opened from an Uncategorized filter context (incoming catId 0), default spinner selection to <Add New Category> rather than Uncategorized.
- Option 2 guardrail: if save is attempted while <Add New Category> remains selected, block save and require an explicit category selection action (choose real category, explicitly choose Uncategorized, or create a category).
- Selecting that row launches Categories and auto-opens the Add Category dialog.
- On return to Add Event:
  - if a category was created, rebind spinner and auto-select that new category;
  - if canceled/no new category, restore prior valid selection.
- Do not expose <Add New Category> as a real category id and never allow it to flow into event persistence.

3. Rationale

- Preserves first-class Uncategorized support while nudging users toward intentional category creation.
- Reduces accidental perpetuation of uncategorized-only usage caused by top/default ordering.
- Keeps coercion positive and contextual, instead of removing uncategorized capability.
- Explicitly corrects the post-Phase-2 observation that visibility improvements for Uncategorized can, without further UX guidance, produce the opposite of the category-adoption intent.

6. First-time-user opportunity

- Phase 5 already addresses the core issue head-on at the highest-leverage moment: Add Event category selection.
- Confirmed for Phase 5: implement a one-time first-run nudge in Add Event as brief helper guidance near the category control.
- Approved copy: Create a category to keep events organized. Choose Add New Category.
- Nudge appears only when no real categories exist; once a real category exists, it no longer appears.

4. Risks and mitigations

- Risk: synthetic action row could be mistaken for a real category id.
  - Mitigation: explicit sentinel/action id isolation and save-path guards.
- Risk: return flow complexity from Categories into Add Event may regress selection state.
  - Mitigation: explicit activity result contract and targeted tests for cancel/create branches.
- Risk: overreach into non-Add-event surfaces.
  - Mitigation: scope lock to Add Event only in Phase 5.

5. Recommended tests for Phase 5

- Policy/helper tests:
  - synthetic action-row identity is never treated as a persistable category id.
  - Add Event option-order rules put <Add New Category> before Uncategorized when shown.
- Add Event selection-state tests:
  - when entering Add Event from an Uncategorized filter context, default spinner selection is <Add New Category>.
  - save attempt while <Add New Category> is still selected is blocked and does not persist event.
  - explicit user selection of Uncategorized still persists catId 0.
  - selecting <Add New Category> triggers navigation intent instead of save-path category resolution.
  - cancel return path restores prior valid category selection.
  - create return path auto-selects newly created category.
- Categories auto-open tests:
  - Add Category dialog auto-opens when requested via intent extra.
  - input field is focused and ready for typing.
- Regression tests:
  - existing Uncategorized visibility condition (only when uncategorized events exist) remains intact.
  - reserved-name blocking for Uncategorized remains intact.

## Execution Guardrails

1. Reserved-name behavior

- Creation and rename to literal Uncategorized are blocked in UI create/edit paths.
- Import still normalizes reserved Uncategorized tokens for backward compatibility.

2. Legacy warning behavior

- Existing no-category-chosen warning remains only until explicit uncategorized selection lands.
- Once Phase 2 behavior is active, the warning is removed from normal flow.

3. Test gate per phase

- Each phase requires targeted unit tests for changed logic plus at least one full build before phase completion.

4. Release gate

- Changes remain phased.
- Final release requires a single end-to-end round-trip validation sweep across DB export/import, CSV export/import, and long-press Uncategorized.csv scenarios.
