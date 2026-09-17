# DCR Add Event Usability

## Purpose

Capture observed Add Event usability friction from direct user observation, define likely root causes, and propose implementation-ready UX improvements that reduce hidden controls, ambiguous category creation behavior, and excessive cognitive load in the Add Event flow.

## Observation Summary

Scenario observed:

1. User was asked to create a birthday event.
2. User needed to make the event annual and place it in a newly created Kin category.
3. Event title entry was easy.
4. Category picker and bottom action buttons were not visible because soft keyboard covered lower content.
5. Selecting Add New Category in category picker was not self-explanatory and appeared to require another click on synthetic Add New Category to proceed to the Add New Category dialog.
6. After category creation, user returned to Add Event with keyboard still shown and critical controls (Cancel/Ok) still obscured.

Primary outcome:

- Task completion required hidden knowledge (manual keyboard dismiss, scrolling, repeated tapping), not clear system guidance.

## Problem Statement

The Add Event screen currently allows core form controls and primary actions to be obscured by the soft keyboard during common input paths, while category creation is routed through a command-like spinner entry that is discoverability-poor and state-fragile for first-time users.

## Scope

In scope:

- Add Event layout and interaction flow.
- Keyboard, focus, and return-flow behavior.
- Category creation affordance from Add Event.
- Reminder section presentation strategy in Add Event.

Out of scope:

- Database schema changes.
- Reminder architecture or delivery engine changes.
- Category model or filter semantics (uncategorized sentinel, reserved-name policy, persistence rules).

## Finalized Pre-Implementation Decisions

1. Layout/presentation improvements apply to both Add and Edit modes for consistency.
2. Category-coercion policy behavior remains Add-mode specific as currently designed.
3. Category selection uses value rows only; category creation is an explicit one-tap action control labeled Add Category.
4. Add Category control copy and related guidance/validation copy must be fully localized across supported languages and validated in pseudo-locales.
5. Initial focus remains Event Title, but Category and primary actions must stay reachable while keyboard is open.
6. Successful category creation returns with created category selected, keyboard dismissed, and top section visible (Title, Category, Date, Recurrence) with primary actions reachable.
7. Canceled category creation restores the prior valid category selection and leaves no transient action state selected.
8. Existing category-coercion intent remains in place; only the control mechanism changes.
9. Coercion behavior parity contract:
   - Keep first-time guidance behavior (localized nudge shown when there are zero real categories).
   - Keep save-time guard behavior when category requirement is not satisfied.
   - Do not add repeated harassing prompts in non-first-time paths.
   - Update wording/control references from spinner synthetic row language to explicit Add Category action language.
10. IME behavior contract uses resize plus persistent bottom actions; the flow must not rely on manual keyboard dismissal.
11. Category model semantics remain unchanged.
12. Reminder architecture and scheduling logic remain unchanged.
13. Phase 1 keeps End day behavior/logic unchanged.
14. Phase 1 verification gate uses explicit Gradle commands and must pass:

- .\\gradlew :app:testDebugUnitTest
- .\\gradlew :app:assembleDebug
- .\\gradlew :app:bundleRelease

## Root Cause Hypotheses

1. Primary actions (OK/Cancel) are inside scrollable content and can move beneath the keyboard.
2. Category picker placement is below high-vertical-content sections, so it is frequently off-screen while typing.
3. Spinner row Add New Category acts as both value and command, which is a known affordance anti-pattern.
4. Return from category creation does not consistently reset input focus or keyboard visibility for continuation.
5. Reminder block has high visual and vertical weight relative to earlier critical decisions.

## UX Goals

1. Keep primary actions continuously reachable.
2. Keep category assignment visible and early in the flow during title entry.
3. Make category creation a direct, explicit action, not a hidden spinner behavior.
4. Ensure successful category creation is obvious and immediately actionable.
5. Reduce form intimidation and scanning effort for first-time users.

## Proposed Improvements

## A) Persistent Bottom Action Bar (Highest Priority)

1. Move OK and Cancel outside ScrollView into a sticky bottom action row.
2. Apply keyboard-aware insets so the action row remains visible above IME.
3. Keep only informational/form fields in scrolling region.

Why:

- Removes hidden-action failure mode and reduces completion friction for all users.

## B) Replace Spinner Command Row With Explicit Category Action

1. Category dropdown should contain only category values.
2. Add an explicit adjacent action button: Add Category.
3. For zero-category state, show a first-run CTA panel: Create your first category.
4. Add Category and supporting guidance/validation copy must come from string resources and be localized in all supported locales, including pseudo-locales used in QA.

Why:

- Removes command/value ambiguity and eliminates double-select confusion.

## C) Keyboard and Focus Reset On Category Return

1. After successful category creation, auto-select created category.
2. Clear focus from title/details fields unless user explicitly returns to them.
3. Dismiss keyboard on return to Add Event.
4. Show brief confirmation text/snackbar with created category name.

Why:

- Makes progress visible and preserves user orientation.

## D) Reorder Fields For Task Flow

Recommended top sequence:

1. Event Title
2. Category + Add Category action
3. Date
4. Recurrence
5. Reminders summary
6. Details

Why:

- Reinforces category understanding at the moment users define the event identity.
- Supports a simple mental model: What (title/category), When (date/recurrence/reminder summary), Optional (details).

## D.1) Information Architecture Principle

Main form organization should follow progressive intent:

1. What: Event Title, Category + Add Category action.
2. When: Date, Recurrence, Reminders summary, End day summary.
3. Optional: Details and deeper controls.

Why:

- This sequence matches natural decision-making and minimizes scroll-to-find behavior.

## E) Collapse Reminder and End Day Editing Surface

1. Replace full reminder box with compact summary row in main form.
2. Open reminder editor via bottom sheet/dialog when summary is tapped.
3. Keep End day as a compact summary row in main form (for example: No end date, or Ends on <date>).
4. Open End day picker/details only when user taps/enables End day.
5. Keep global disabled messaging in reminder editor and optionally one-line muted summary in form.

Why:

- Reduces initial cognitive load and keeps high-value fields visible first.

## Recommended Delivery Plan

Phase 1: Add Event Usability First Pass

1. Implement persistent bottom action bar.
2. Replace synthetic spinner command row with explicit one-tap Add Category action control.
3. Reorder top-of-form fields to: Title, Category + Add Category action, Date, Recurrence.
4. Implement keyboard/focus reset on return from category creation.
5. Preserve existing coercion level with updated control/copy references.
6. Keep reminders and End day logic/architecture unchanged.
7. Verify no regressions in add/edit save behavior.

Phase 2: Progressive Disclosure (Optional Follow-Up)

1. Replace reminder box with compact summary row plus dedicated editor surface.
2. Optionally present End day with summary-first entry and reveal details on demand.
3. Validate whether this improves completion without adding navigation friction.

Phase 3: IA and Copy Polish (Optional Follow-Up)

1. Refine spacing/typography and helper copy based on Phase 1/2 learnings.
2. Tighten localization wording for guidance and validation prompts.

## Acceptance Criteria

1. OK and Cancel remain visible and tappable while keyboard is open.
2. User can create a new category from Add Event in one clear action path.
3. No double-selection or hidden-selection requirement exists for category creation.
4. Add Category action label and related copy are localized across supported locales and validated in pseudo-locales.
5. After creating category, Add Event returns with:
   - new category selected,
   - keyboard dismissed,
   - top section visible in order (Title, Category, Date, Recurrence),
   - primary actions visible.
6. In first-run zero-category state, user is explicitly guided to create a category.
7. Main form follows What/When/Optional grouping with category before date.
8. Reminders boxed group remains below the initial top four fields in Phase 1.
9. Reminder and End day logic remain unchanged in Phase 1.

## Test Recommendations

Unit and integration tests:

1. Add Event action row visibility contract under IME open state.
2. Category creation return flow sets created category and leaves no synthetic command selection.
3. Save path remains blocked for invalid category action states.
4. Keyboard/focus state after category-create result is deterministic.
5. Top-of-form field order remains Title, Category, Date, Recurrence.
6. First-time localized guidance still appears under existing zero-real-category conditions.
7. Non-first-time flows do not introduce repeated harassment prompts.
8. Add Category action launches category creation on a single tap.
9. Localization coverage includes pseudo-locale validation for new/updated strings.

UI tests (instrumented/manual QA checklist):

1. First-run create-event flow completes without manual keyboard dismissal knowledge.
2. Category and primary actions are reachable without exploratory scrolling while keyboard is active.
3. Title and category assignment can be completed before date/recurrence in a single downward reading pass.
4. Reminder box remains present below top-four section for Phase 1.

## Risks and Mitigations

1. Risk: Existing users may rely on current field order.
   Mitigation: Stage rollout, keep behavior familiar where possible, include release note callout.

2. Risk: Reminder editor extraction increases navigation depth.
   Mitigation: Keep summary row explicit and one-tap open; preserve last-used values.

3. Risk: Insets behavior may vary across OEM keyboards.
   Mitigation: Validate on at least Gboard and one OEM keyboard profile.

## Alternatives Considered

1. Keep current layout and add helper text only.
   Rejected because discoverability issues are structural, not copy-only.

2. Keep Add New Category as spinner row but improve labeling.
   Rejected because command-in-list-value ambiguity remains.

3. Move reminders to separate full activity.
   Deferred in favor of bottom sheet/dialog to reduce overhead.

## Non-Goals

1. Reworking category/filter model semantics globally.
2. Modifying reminder architecture or reminder scheduling logic.
3. Redesigning all Edit Event visual styles outside this usability scope.

## Status

Proposed, not yet implemented.

No code changes are included in this DCR.
