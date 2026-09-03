# Design Change Request (DCR): Harden Search Reliability Across Emulator and Physical Device

**Document ID:** DCR-2026-09-03-B  
**Target Component:** Main search behavior (`SearchView` query dispatch and fragment list filtering)  
**Status:** Proposed  
**Author:** DaysSincePro Architecture

---

## 1. Motivation

Search behavior is inconsistent across environments:

1. Emulator: live filtering works while typing; `x` clears query; back arrow exits search mode and restores list state.
2. Physical device: query text appears, but list does not update while typing or on submit; `x` still clears query; back arrow exits search mode.

Goal: guarantee equivalent search behavior on emulator and physical device before Main-screen presentation modernization.

---

## 2. Scope and Boundaries

### 2.1 In Scope

1. Live-typing query behavior parity.
2. Submit path behavior parity via keyboard submit fallback.
3. Query clear behavior parity (`x` control).
4. Search exit behavior parity (left back arrow in search field).
5. Lifecycle-safe dispatch so search reaches active tab fragments consistently.
6. Hide the visible in-field submit affordance (`>`).

### 2.2 Out of Scope

1. Main toolbar/tab visual modernization.
2. Typography/color redesign.
3. Broader app-chrome changes (owned by [DCR_ModernizeUI.md](DCR_ModernizeUI.md) Mini-B).
4. Explicit force-show keyboard-on-expand behavior.

---

## 3. Root-Cause Hypothesis Set

1. Fragment reference lifecycle mismatch: UI fragments may be restored by `FragmentManager` while Main's fragment fields are not re-bound, so dispatch can silently no-op on some devices.
2. Query dispatch asymmetry between live typing and submit path.
3. Legacy submit affordance (`>`) enabled even when live typing is primary behavior, causing ambiguous UX.
4. SQL filtering robustness and case handling should be hardened to avoid environment-sensitive behavior.

---

## 4. Desired Behavior Contract

1. Search is live by default; list updates as user types without requiring submit.
2. `x` clears query and restores pre-search filtered category view.
3. Left search back arrow exits search mode and restores normal list header/title state.
4. Keyboard submit should behave as equivalent fallback trigger for the same filter path.
5. Visible in-field submit affordance (`>`) is hidden.
6. Submit callback remains wired as a fallback trigger for environments where keyboard submit is used.

---

## 5. Acceptance Criteria

### 5.1 Functional Matrix

For each theme mode and environment, verify:

1. Typing `ji` filters matching rows immediately.
2. Typing mixed case (for example `Ji`) yields equivalent results.
3. Pressing keyboard submit does not break or diverge from live filtering behavior.
4. Tapping `x` clears query and restores prior non-search list state.
5. Tapping left search back arrow exits search mode and restores prior non-search list state.
6. Query behavior is consistent across all three tabs (`Days Since`, `Since Last`, `Until Next`) for both live typing and keyboard submit.
7. `Until Next` search inclusion logic must be parity-correct with tab intent, including recurring future events that belong in that tab.

### 5.2 Environment Matrix (Mandatory)

1. Emulator, Light
2. Emulator, Dark
3. Physical device, Light
4. Physical device, Dark

No sign-off unless all pass.

---

## 6. Sequencing and Dependencies

1. This DCR is Phase 0.5 in [DCR_ModernizeUI.md](DCR_ModernizeUI.md).
2. Must be completed before Mini-B begins.
3. Mini-B must reuse the stabilized search behavior contract and not re-implement search logic.
4. Any Mini-B regression in search after Phase 0.5 sign-off is treated as Mini-B integration regression.

---

## 7. Implementation Principles

1. Behavior-first changes only; avoid presentation refactor in this phase.
2. Keep a single internal search dispatch pathway used by live typing and submit fallback.
3. Prefer deterministic fragment lookup/dispatch that is resilient to lifecycle restore differences.
4. Hide the visible submit affordance while retaining submit callback wiring.
5. Do not add explicit keyboard force-show logic in this phase.

---

## 8. Risks and Mitigations

### 8.1 Risks

1. Subtle regressions in tab-specific list filtering.
2. State restoration edge cases when returning from add/edit/category flows.
3. Inconsistent submit fallback behavior if keyboard action diverges from live typing path.

### 8.2 Mitigations

1. Validate all three tabs under active category filter and uncategorized contexts.
2. Test return-paths from add/edit/category after an active search session.
3. Keep one shared dispatch path for live typing and keyboard submit.
4. Add explicit regression checks for `Until Next` search behavior to avoid tab-specific logic drift.

---

## 9. Definition of Done

This DCR is complete when:

1. Search behavior is consistent across emulator and physical device in light/dark.
2. Live typing is reliable and keyboard submit path is equivalent fallback while visible `>` remains hidden.
3. Clear and back behaviors restore expected pre-search state.
4. Mini-B can consume this behavior unchanged from a logic perspective.
5. Three-tab query parity is confirmed, including `Until Next` recurring-future search correctness.
