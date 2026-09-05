# Design Change Request (DCR): UI Modernization and Navigation Consistency

**Document ID:** DCR-2026-09-03-A  
**Target Component:** App chrome, secondary screens, and dialogs with legacy Activity/ListActivity/PreferenceActivity implementations  
**Status:** Proposed  
**Author:** DaysSincePro Architecture

---

## 1. Motivation

The app currently mixes:

- legacy framework components (`Activity`, `ListActivity`, `PreferenceActivity`)
- AppCompat components (`AppCompatActivity`)
- mixed theme parents (Holo + AppCompat)

This causes visible cross-device inconsistencies in top bars, icon/title rendering, and navigation affordances (especially Up/Back visibility).

The immediate trigger is Settings: Up/Back appears in emulator but not consistently on physical device, despite identical app code path.

Primary goal: make high-use screens render consistently across emulator and physical devices, in both Light and Dark modes, with predictable top-bar behavior.

---

## 2. Goals and Non-Goals

### 2.1 Goals

1. Standardize top-bar interaction for targeted screens:

- far-left back arrow
- centered screen title
- consistent title text and spacing

2. Standardize on AppCompat/Material-compatible screen host patterns for targeted phases.

3. Preserve existing feature behavior (data edits, category filtering, event creation, preferences persistence).

4. Define explicit Light/Dark pass criteria and require both modes to pass before phase completion.
5. Ensure each phase builds on one shared modernization framework and reusable UI primitives.
6. Remove legacy UI host/theme paths progressively as each replacement is stabilized.

### 2.2 Non-Goals

1. Full visual redesign of list rows, typography scale, or iconography across the whole app.
2. Database/schema behavior changes.
3. Search visual redesign outside Main modernization (Main search presentation remains in Mini-B scope).
4. Maintaining multiple long-term parallel modernization patterns for equivalent screens.

### 2.3 Architectural Guardrails (Mandatory)

1. Phases are cumulative, not independent experiments.
2. Every new modernized screen must use the same shared top-bar/navigation framework established in earlier phases unless a written exception is approved in this DCR.
3. Do not fan out alternate implementations for equivalent concerns (for example, multiple toolbar/title/arrow patterns for secondary screens).
4. Legacy host/theme paths must be removed or explicitly scheduled for removal immediately after replacement is verified, to avoid permanent dual-stack maintenance.
5. Any deliberate exception must include: reason, scope, owner, and planned convergence milestone.

---

## 3. Current State Summary

### 3.1 Screen Type Mix

- `MainActivity`: `AppCompatActivity`
- `EditEventActivity`: `AppCompatActivity`
- `PrefActivity`: `PreferenceActivity`
- `CategoriesActivity`: `ListActivity`
- `HistoryActivity`: `ListActivity`
- `EventChooserActivity`: `Activity`
- `CreateCategoryActivity`: `Activity`
- `ConfigWidgetActivity`: `Activity`

### 3.2 Key Inconsistency Drivers

1. App-level manifest theme still points at Holo light base.
2. Multiple screens set themes at runtime from separate legacy style families.
3. Legacy activity classes have platform-dependent action bar behavior.
4. Some titles come from activity labels, others from preference category headers.

---

## 4. UX Specification for Modernized Screens

### 4.1 Top Bar Rules

For each screen included in a modernization phase:

1. Back arrow is visible at far-left and dismisses current screen context.
2. Title is centered in top bar.
3. Title uses screen-level title text (for example, `Settings`), not section headers (for example, `User Settings`).
4. Back system gesture/button and top-left arrow perform equivalent navigation.

Exception for Main screen:

1. Main screen is exempt from strict centered-title requirements because right-side action density (`+`, category folder, overflow) is intentionally preserved.
2. Main title/alignment should prioritize visual balance and action clarity over forced centering.

### 4.2 Settings-Specific Rule

- Screen title is `Settings`.
- Remove `User Settings` as a separate content header to avoid duplicate labeling.
- Settings content should begin directly with setting rows under the single top-bar title.

---

## 5. Phase Plan

### 5.1 Phase 0 (Settings-Only Pre-Phase)

completed 9/3/26

Scope:

1. Settings screen only

Primary outcomes for Phase 0:

1. Resolve the cross-device Settings top-bar inconsistency that triggered this DCR.
2. Validate the reusable top-bar modernization pattern in one low-blast-radius screen.
3. Verify Light/Dark parity on emulator and physical device before broader rollout.
4. Standardize Settings labeling to a single visible screen title (`Settings`) with no duplicate `User Settings` header.

### 5.2 Phase 0.5 (Search Reliability Gate)

completed 9/3/26

Scope:

1. Functional hardening of Main search behavior only (no visual modernization).
2. Emulator and physical-device parity for live filtering.
3. Hide the visible in-field submit affordance (`>`) while retaining keyboard submit callback as equivalent fallback trigger.
4. Keep keyboard behavior unchanged (no explicit force-show keyboard-on-expand logic in this phase).

Dependency and ownership model:

1. Detailed scope and acceptance criteria are tracked in [DCR_HardenSearch.md](DCR_HardenSearch.md).
2. This phase must complete before Mini-B to prevent coupling search defect triage with Main presentation refactor.
3. Search hardening must be implementation-reusable by Mini-B (no throwaway temporary path).

### 5.3 Mini-Phase A (High-Use Pilot)

completed 9/3/26

Scope:

1. Categories filter flow launched from Main (folder entry)
2. Categories flow launched from synthetic Add New Category entry in Add Event path
3. Add Event screen
4. About dialog

Why both category entry paths are included together:

- they are two user-visible entry contexts into closely related category UX
- users perceive them as one conceptual capability
- shipping one without the other creates inconsistent behavior and visual language

Primary outcomes for Mini-A:

1. Verify modernization pattern across multiple UI types:

- list-based selection/filter screen
- form/edit screen
- modal dialog

2. Prove consistency on both emulator and physical device.
3. Build on the validated Settings pattern from Phase 0 while still keeping blast radius lower than touching Main.
4. Integrate theme-mode hardening as a required Mini-A acceptance gate (not deferred follow-up).

Mini-A integrated theme hardening requirement:

1. Use one single source of truth for theme transition handling on Settings return.
2. Ensure no stale-theme return path for Main and About when leaving Settings via back/up.
3. Avoid duplicate restart/recreate triggers across Settings-return lifecycle paths.

### 5.4 Mini-Phase B (Non-Main Everything Else)

Scope:

1. Remaining legacy and lower-frequency non-Main screens not covered in Mini-A
2. Additional dialog standardization where beneficial
3. Fix known rendering and chrome consistency issues in those non-Main surfaces

Mini-B cumulative architecture requirements:

1. Reuse shared top-bar/navigation/theme primitives from prior phases by default.
2. Extend shared primitives only when a real gap is identified.
3. Avoid screen-local one-offs unless a deliberate exception is documented with reason, scope, owner, and planned convergence milestone.

Why this ordering:

- resolves current non-Main rendering issues earlier while deferring Main blast radius
- preserves cumulative architecture momentum from Phases 0/0.5/A

Mini-B explicit enum and launch mapping (all remaining non-Main surfaces):

1. `DaysDiffActivity`

- from Main launch path: Main overflow menu -> Days Between
- source path: `MainActivity.onOptionsItemSelected(menu_daysdiff)`
- Mini-B role: non-Main visual/architecture standardization to shared top-bar/theme approach

2. `EventChooserActivity`

- from Main launch path: Main overflow menu -> Days Between -> Event button A or Event button B
- source path: `DaysDiffActivity.eventACallback/eventBCallback`
- Mini-B role: legacy host modernization and convergence with shared primitives

3. `HistoryActivity`

- from Main launch path: Main event long-press context menu -> History
- source path: `PastFutureListFragment.onContextItemSelected(MENU_HISTORY)`
- Mini-B role: migrate legacy `ListActivity` path and align top-bar/navigation/theme behavior

4. `EditHistory`

- from Main launch path: Main event long-press context menu -> History -> add/edit occurrence flow
- source path: `HistoryActivity.addHappenedItem/editHappenedItem`
- Mini-B role: align with shared top-bar/navigation/theme primitives (remove remaining off-pattern chrome)

5. `ConfigWidgetActivity`

- from Main launch path: no direct in-app Main path; launch originates from Android home-screen widget add/config flow
- source path: widget provider configuration intent path (`DaysSinceAppWidgetProvider`)
- Mini-B role: include widget configuration UX in non-Main standardization now (not deferred)

Mini-B boundary note:

1. Main-screen app chrome/search/tabs modernization remains out of scope for Mini-B and is handled only in Mini-C(a).
2. Mini-B may update shared primitives when needed, but must not perform Main-specific visual rewrites.

Recommended Mini-B execution slicing:

1. Mini-B1: screen host/chrome modernization for the five enumerated surfaces.
2. Mini-B2: non-Main dialog/popup/theme convergence across those same surfaces.
3. Keep each slice behavior-preserving and independently rollback-safe.

### 5.5 Mini-Phase C (Main + Post-Main Convergence)

Scope:

1. Mini-C(a): Main screen modernization and top app chrome alignment
2. Mini-C(a): adopt the already-stabilized search behavior contract from Phase 0.5 without re-implementing search logic
3. Mini-C(b): post-Main cleanup and convergence, including legacy style/theme cleanup that should occur only after Main is stable

Why separated internally:

- Main remains highest blast radius (tabs/search/overflow/navigation fan-out)
- post-Main cleanup is safer after Main behavior and chrome are validated

---

## 6. Technical Approach

### 6.1 Baseline Direction

Use AppCompat-compatible host architecture for modernized screens and a shared toolbar pattern.

Settings migration target:

- `AppCompatActivity` host + `PreferenceFragmentCompat`
- top bar via Material/AppCompat toolbar with explicit navigation icon and centered title

List-based legacy screens migration target:

- move from `ListActivity` to `AppCompatActivity`
- host explicit `ListView`/Recycler pattern in layout under shared top bar container

### 6.2 Reuse Strategy

Create one shared top-bar setup pattern used by all modernized screens:

1. configure title text
2. configure centered title behavior
3. configure nav icon and close behavior
4. apply light/dark tint behavior

Framework reuse requirements:

1. New modernized screens must adopt the shared implementation directly, not copy/paste local variants.
2. Shared implementation changes must be made in one place and consumed by all migrated screens.
3. Screen-specific overrides are permitted only for behavior that is truly unique to that screen, not for general navigation/title styling.

Expected savings when done this way:

- less per-screen custom action-bar code
- less device/OEM variance
- lower maintenance for future screens

### 6.3 Dialog Strategy

Dialogs keep current behavior semantics, but should use a consistent theme wrapper where required to match light/dark and typography conventions.

### 6.4 Theme Lifecycle Contract (Mini-A)

1. Theme preference value (`theme`) in shared preferences is the single source of truth.
2. Settings applies the selected theme immediately within Settings so users get instant feedback.
3. Main reconciles theme changes in one place on Settings return and only restarts when the theme value actually changed.
4. About inherits current Main host theme at launch; no separate theme state is maintained for About.
5. Categories and Add Event read the same shared preference at activity creation and therefore naturally align once Main/Settings transition handling is correct.
6. Notification preference side effects should be reconciled in the same Main Settings-return contract to avoid divergent result-code-only paths.

Mini-A short RCA summary:

1. Root cause: Main previously depended on `onActivityResult` with `RESULT_OK` to reconcile Settings changes.
2. Back/up dismissal from Settings can return without `RESULT_OK`, skipping Main reconciliation.
3. Result: Settings reflected new theme, but Main (and About launched from it) could remain stale until full relaunch.
4. Fix direction: move reconciliation to a single Settings-return lifecycle contract and gate restarts on actual theme change.

---

## 7. Acceptance Criteria

### 7.1 Functional

1. Phase 0: Settings opens and closes correctly from Main.
2. Phase 0.5: search behavior passes [DCR_HardenSearch.md](DCR_HardenSearch.md) acceptance criteria on emulator and physical device.
3. Data behavior remains unchanged:

- preferences still persist immediately
- category selection and category creation paths remain intact
- event add/edit flow remains intact

4. Mini-A screens open and close correctly from their existing entry points.
5. Mini-A theme hardening passes with no stale-theme screen after leaving Settings via back or up.
6. Main and About parity after Settings theme toggle is required for Mini-A completion.

### 7.2 Visual and Navigation

For each in-scope screen of the active phase:

1. far-left back arrow present and tappable
2. centered title present and correct (except Main-screen exemption in §4.1)
3. no unintended app icon/title substitutions
4. no overlap/clipping at status bar or content top inset

### 7.3 Light/Dark Requirements (Mandatory)

Each phase acceptance item must pass in:

1. Light mode on emulator
2. Dark mode on emulator
3. Light mode on physical device
4. Dark mode on physical device

No phase is complete until all four combinations pass for all in-scope screens.

---

## 8. Test Matrix

### 8.1 Navigation Matrix

1. Enter each in-scope screen for the active phase from every supported entry point.
2. Exit via top-left back arrow.
3. Exit via system back gesture/button.
4. Confirm return destination is correct.

### 8.2 Search Gate Matrix (Phase 0.5)

1. Execute search live-typing checks defined in [DCR_HardenSearch.md](DCR_HardenSearch.md).
2. Validate `clear` and back-arrow behaviors restore expected pre-search view state.
3. Validate parity across emulator/device and light/dark before opening Mini-B.

### 8.3 State Matrix

1. Theme toggled Light/Dark before launch.
2. During Phase 0, theme changed in Settings and return flow verified.
3. Rotation/config-change smoke checks for affected screens where relevant.

### 8.4 Mini-A Theme Parity Matrix (Settings Return + About from Main)

1. Light -> Dark: open Settings, toggle theme, exit via back/up, verify Main is Dark, launch About from Main, verify About is Dark.
2. Dark -> Light: open Settings, toggle theme, exit via back/up, verify Main is Light, launch About from Main, verify About is Light.
3. Repeat both flows on emulator.
4. Repeat both flows on physical device.
5. Mini-A is blocked from completion if any stale-theme screen remains after Settings exit.

### 8.5 Device Matrix

1. Current emulator image(s)
2. At least one physical Android device used by maintainer

### 8.6 Mini-B Surface Acceptance Checklist (Non-Main Everything Else)

Each Mini-B surface must pass §7.2 and §7.3 in addition to behavior-preservation checks.

1. `DaysDiffActivity`

- launch path from Main: overflow menu -> Days Between
- verify centered title/back behavior aligns to shared pattern
- verify both Date and Event picker entry points still work and return expected values

2. `EventChooserActivity`

- launch path from Main: overflow menu -> Days Between -> Event button A/B
- verify selection controls render correctly in Light/Dark and return chosen event/date payload
- verify back/up and system back return to Days Between without stale state

3. `HistoryActivity`

- launch path from Main: long-press event -> History
- verify list render, long-press/context actions, and navigation parity (back arrow/system back)
- verify event/history CRUD behavior remains unchanged

4. `EditHistory`

- launch path from Main: long-press event -> History -> add/edit occurrence
- verify title semantics remain contextual (`Add`/`Edit`) while using shared top-bar behavior
- verify date selection, on-time checkbox, notes persistence, and return-result behavior

5. `ConfigWidgetActivity`

- launch path from Main: none direct; Android launcher widget add/config flow
- verify widget config controls render correctly in Light/Dark
- verify saved widget options persist and widget updates broadcast correctly

Mini-B sign-off gate:

1. Mini-B is incomplete until all five surfaces pass emulator + physical device checks in both Light and Dark modes.

---

## 9. Risks and Mitigations

### 9.1 Risks

1. Regressions from replacing legacy base classes (`ListActivity`, `PreferenceActivity`).
2. Theme regressions between existing dark/light variants.
3. Navigation regressions around return flow from Settings and category flows.

### 9.2 Mitigations

1. Phase separation (0 then 0.5 then A then B then C).
2. Preserve behavior-first constraints in each migration PR.
3. Keep PRs scoped to one phase and run full compile/test/build checks.
4. Validate on emulator and physical device for both themes before phase sign-off.
5. Add a convergence checkpoint at the end of each phase: verify no newly introduced duplicate modernization paths remain.
6. Keep search hardening isolated from Main visual refactor to reduce confounded regressions.
7. During Mini-B, enforce cumulative reuse and prevent non-Main one-off modernization forks that would increase Mini-C convergence risk.

---

## 10. Rollout and Rollback

### 10.1 Rollout

1. Implement Phase 0 (Settings-only) in a focused PR.
2. Stabilize and verify acceptance matrix for Phase 0.
3. Implement Phase 0.5 (Search reliability) per [DCR_HardenSearch.md](DCR_HardenSearch.md).
4. Stabilize and verify Phase 0.5 acceptance matrix and gate.
5. Implement Mini-A in a focused PR.
6. Stabilize and verify acceptance matrix for Mini-A.
7. Implement Mini-B (non-Main everything else) in dedicated PR(s) with strict cumulative-reuse checks.
8. Implement Mini-C(a) Main modernization in dedicated PR.
9. Implement Mini-C(b) post-Main cleanup/convergence after Mini-C(a) is stable.

### 10.2 Rollback

If critical regressions appear:

1. Revert only the affected phase PR.
2. Keep prior phase intact.
3. Re-scope and retry with smaller per-screen increments.

Mini-B specific note:

1. If a non-Main modernization change in Mini-B requires a shared primitive extension, rollback or isolate only the extension and affected screens; do not discard already-validated shared primitives consumed by prior phases.

Phase-specific note:

1. If Mini-B introduces search regressions after Phase 0.5 sign-off, treat as Mini-B integration defects unless Phase 0.5 acceptance criteria are found incomplete.

Mini-C specific note:

1. If Mini-C(a) Main modernization introduces regressions unrelated to shared primitives, isolate rollback to Main changes first, then re-evaluate whether any Mini-C(b) cleanup should be deferred.

---

## 11. Resolved Decisions and Mini-B Execution Confirmations

### 11.1 Resolved Design Decisions (Captured)

1. Centered-title behavior is the default for modernized screens, with Main explicitly exempted for action-density balance per §4.1.
2. About dialog remains intentionally somewhat custom while following host-theme parity expectations established by prior phases.
3. Add Event retains contextual title variants (`Add Event` / `Edit Event`) while using shared top-bar mechanics.
4. Category subflows intentionally remain separate hosts and are standardized visually/behaviorally as one conceptual capability.

### 11.2 Mini-B Execution Confirmations (Current)

1. PR slicing is confirmed as Mini-B1 then Mini-B2 (not a single combined phase PR).
2. `ConfigWidgetActivity` is explicitly included in Mini-B scope (not deferred).
3. `EditHistory` is explicitly included in Mini-B scope.
4. `DaysDiffActivity` is explicitly included in Mini-B scope.
5. Mini-B non-Main enum + Main-origin launch mapping in §5.4 is the authoritative scope boundary for execution.
6. Verification ownership is split as follows:

- implementation verification by agent: code review + JVM unit tests + compile/build checks
- runtime UX verification by maintainer: emulator + physical device checks per §8 matrix

---

## 12. Definition of Done for This DCR

This DCR is considered complete when:

1. Phases 0, 0.5, A, B, and C are implemented and verified.
2. Light/Dark acceptance matrix passes on emulator and physical device.
3. Each phase demonstrably reuses the shared modernization framework rather than introducing duplicate equivalents.
4. Legacy theming/shims made obsolete by the migration are removed or documented for deferred cleanup with explicit milestone/date.
5. Follow-up docs capture any deliberate exceptions.
6. Mini-B demonstrates cumulative reuse of shared primitives with no unresolved one-off forks left without an explicit convergence milestone.
