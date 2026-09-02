# Fix Post Option2

## Purpose

Document the post-Option2 behavior issues observed in Add Event, clarify root causes, define solution options, and recommend implementation tracks with test coverage before any further code changes.

## Scope

In scope:

- Add Event category assignment UX and control behavior.
- Add Event -> Categories -> return flow behavior.
- Global category filter side effects caused by category-creation flow reuse.
- Test coverage gaps related to interaction behavior and regression protection.

Out of scope:

- CSV import/export semantics.
- Database schema changes.
- Notification behavior.

## Finalized Pre-Start Decisions

1. Issue 1 implementation path: go directly to Track2.

- Implement a dedicated CreateCategoryActivity (or dialog-host activity) for Add Event category creation.
- Do not reuse CategoriesActivity for Add Event category creation.

2. Issue 2 implementation path: implement solution 1 plus 2.

- Scope callback suppression only to explicit programmatic spinner selection paths.
- Explicitly reset suppression state after adapter rebind and return-flow handling.

3. Issue 3 implementation path: implement solution 1 and 3.

- Functional change: remove add-flow side effects on global filter preferences.
- Documentation change: clarify single-catId model and multi-filter union behavior.
- Temporary telemetry/logging is optional and only for development diagnostics.

4. Issue 4 implementation path: remove Category checkbox.

5. Issue 5 implementation path: move category controls into one contiguous section and place Details below category controls.

6. Uncategorized visibility rule remains unchanged.

- Uncategorized option is shown only when one or more uncategorized events exist.

7. Lifecycle expectation for category-create input.

- Rotation: preserve in-progress typed category text (continue where user left off).
- Crash/force-close/process death: draft recovery is not required for this fix.

8. Duplicate category rule.

- Block duplicate category creation using trimmed, case-insensitive matching.
- Preserve ability to correct casing by editing the existing category name (case-only rename allowed on same row).

9. Side-effect contract.

- Add-flow category creation must never write global filter preferences CategoryIds/Categories.
- CategoriesActivity in normal filter-management mode must continue to write filter preferences as before.

10. Delivery/versioning plan.

- Phase A target version: 3.10.64.49.
- Phase B target version: 3.10.65.49.

## Confirmed Issues

1. Add Event category creation path currently reuses the Categories filter-management screen.

- Impact: category creation from Add Event unintentionally mutates global filter state (CategoryIds).
- Symptom seen by user: returning from category creation can leave multiple categories selected for list filtering, which is valid for filtering but unexpected in add flow.

2. Add New Category selection can appear to do nothing in some return states.

- Impact: user selects Add New Category in Add Event dropdown and no navigation occurs.
- Likely cause: selection callback suppression lifecycle can swallow the next user selection event.

3. Perceived data corruption after returning to list view.

- Impact: user sees event in an uncategorized-filtered context after flow interactions and suspects dual category assignment.
- Actual model: each event has a single catId. The list can show a union of selected filter categories.
- Problem is mainly state coupling and UX ambiguity, not multi-category event storage.

4. Category checkbox meaning is unclear and conflicts with Option2 intent.

- Impact: checkbox-driven gating creates confusion about whether category assignment is optional, explicit, or required.
- UX conflict: Option2 asks for stronger intentional category choice, while checkbox implies bypass pathways.

5. Add Event field ordering causes comprehension break.

- Impact: Details section currently interrupts category controls, making category selection flow feel fragmented.

## Root Cause Summary

1. Flow coupling: Add Event category creation depends on a screen whose primary responsibility is global filter management.
2. Callback fragility: spinner callback suppression state is too broad and can survive transitions where it should be cleared.
3. UX legacy: checkbox and control ordering predate newer category guidance behavior and now conflict with current product intent.
4. Test gap: interaction-heavy behavior was not covered by targeted tests; most coverage is policy/helper level.

## Solution Options And Recommendations

## Issue 1: Flow Coupling (Add Event using Categories filter screen)

Potential solutions:

1. Add a context mode in CategoriesActivity for add-flow use.

- In this mode, activity auto-opens add dialog, returns created category id, and never writes CategoryIds/Categories preferences.

2. Create a dedicated lightweight CreateCategoryActivity (or dialog host) used only by Add Event.

- Returns created category id directly; no filter UI is shown.

3. Keep current flow and patch around side effects.

- Attempt to snapshot and restore preferences around navigation.

Recommendation:

- Track2 is approved and required for this implementation.
- CreateCategoryActivity (or dialog-host activity) is the selected approach.
- Avoid preference snapshot/restore patching approaches.

Recommended track for this project now:

- Track2 direct implementation (no intermediate Track1 stage).

## Issue 2: Add New Category no-op selection after return

Potential solutions:

1. Tighten callback suppression to a scoped guard around only specific programmatic setSelection operations.
2. Add explicit state reset after adapter rebind and after onActivityResult.
3. Replace selection-driven launch with a dedicated Add Category button.

Recommendation:

- Implement 1 plus 2.
- Keep selection-driven launch behavior.
- Do not add a separate Add Category button in this fix.

## Issue 3: Perceived data corruption

Potential solutions:

1. Remove filter preference side effects from add-flow category creation.
2. Add temporary telemetry/toast/debug log to show selected filter ids when returning to list.
3. Update doc/help text clarifying single-catId model and multi-filter union behavior.

Recommendation:

- Implement 1 as the functional fix.
- Implement 3 in docs as immediate clarification.
- Use 2 only if needed during implementation diagnostics.

## Issue 4: Category checkbox ambiguity

Potential solutions:

1. Remove checkbox and always show category picker with explicit options (including Uncategorized).
2. Keep checkbox but rename to Assign category and improve helper text.
3. Keep behavior unchanged and rely on documentation.

Recommendation:

- Implement 1 in this effort (remove Category checkbox).

## Issue 5: Add Event layout flow confusion

Potential solutions:

1. Move category block into a contiguous section: Category label/controls together, then Details section below.
2. Keep current order and add visual separators/headings.

Recommendation:

- Choose 1; this is cleaner and aligns with user mental model.

## Suggested Tests

## Core interaction tests (highest priority)

1. Add flow from uncategorized context defaults to Add New Category.
2. Selecting Add New Category when already selected still launches category-create flow.
3. Returning with created category id rebinds options and selects created category.
4. Returning canceled/no category creation restores prior valid selection.
5. Save is blocked while Add New Category action row remains selected.
6. Explicit user selection of Uncategorized still persists catId = 0.
7. Selecting Add New Category from an already-selected default still launches create flow after return scenarios.
8. Spinner suppression reset does not swallow the first user selection after adapter rebind/onActivityResult.

## Side-effect prevention tests

9. Add-flow category creation mode does not write CategoryIds/Categories preferences.
10. Categories filter mode still writes CategoryIds/Categories as before (no regression to filter feature).

## Duplicate/casing tests

11. Creating a category whose trimmed lowercase matches an existing category is blocked.
12. Editing an existing category allows case-only rename on the same row.

## Data integrity tests

13. New event insert persists exactly one catId value.
14. Edit flow maintains single catId semantics after category changes.

## UX contract tests

15. Category controls remain visible in zero/zero first-time state.
16. Nudge visibility appears only while real category count is zero.
17. Details section renders below category controls in revised layout.
18. Category checkbox is not present in Add/Edit Event UI.
19. Rotation preserves in-progress category-create input text.

## Test tooling guidance

1. Add JVM-level policy tests for helper decisions.
2. Add Robolectric tests for Add Event and Categories interaction flows (activity result, spinner selections, shared preferences effects).
3. Keep at least one full unit suite and release build gate per phase.

## Implementation Strategy

Recommended approach: phased, not single pass.

Rationale:

1. There are multiple interacting concerns: state coupling, spinner event lifecycle, and UX structure.
2. A one-pass change increases rollback/debug complexity if regressions appear.
3. Phase gates allow quick confirmation that each risk area is closed before the next.

## Proposed Phases

Phase A: Flow separation and reliability hardening (target 3.10.64.49)

- Implement dedicated add-flow create-category surface (Track2), with focused result contract.
- Ensure add-flow category creation does not mutate global filter preferences.
- Scope/reset spinner suppression to prevent first-selection no-op.
- Keep Option2 explicit-choice guardrail behavior.
- Add and pass interaction and side-effect tests (core tests 1-10).
- Gate: targeted tests + full unit suite + bundle release.

Phase B: UX normalization and category semantics polish (target 3.10.65.49)

- Remove Category checkbox from Add/Edit Event.
- Reorder UI so category section is contiguous and Details follows below it.
- Add duplicate-blocking in create flow (trimmed case-insensitive) while allowing case-only rename via edit flow.
- Add/update UX and duplicate/casing tests (tests 11-19).
- Gate: targeted tests + full unit suite + bundle release.

## Acceptance Criteria

1. Add-flow category creation no longer changes global category filter preferences.
2. Selecting Add New Category always launches creation flow, including same-item reselection and post-return cases.
3. Save-path behavior enforces explicit user category choice under Option2.
4. Event records remain single-catId with no multi-assignment behavior.
5. Add-flow category creation does not write CategoryIds/Categories preferences.
6. Duplicate creation is blocked by trimmed case-insensitive match, while case-only rename of existing category remains possible.
7. Category checkbox is removed and category controls remain clear/contiguous with Details below.
8. Rotation preserves in-progress category-create input text.

## Risks And Mitigations

1. Risk: Regression in existing category filter management.

- Mitigation: dual-mode tests for add-flow mode vs filter mode.

2. Risk: Spinner callback timing differences across devices.

- Mitigation: scoped suppression plus Robolectric interaction tests.

3. Risk: UX change churn from checkbox removal.

- Mitigation: stage checkbox simplification in P2 after functional stabilization.

## Release Note Candidate

Improved Add Event category workflow to prevent unintended filter-state changes, ensure reliable Add New Category launch behavior, and reinforce explicit category selection before save.
