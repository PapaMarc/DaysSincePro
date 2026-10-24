# Housekeeping

## Lint + Google Play Console App Warnings (Loc release 52 / 3.13.243.52)

## Overview

This document tracks cleanup work that is not feature work but affects quality gates, release risk, and Play Console health.

Current lint status from `:app:lint`:

- 16 errors
- 425 warnings

Google Play Console warning text (from provided screenshot):

1. Edge-to-edge may not display for all users

- From Android 15, apps targeting SDK 35 will display edge-to-edge by default. Apps targeting SDK 35 should handle insets to make sure that their app displays correctly on Android 15 and later. Investigate this issue and allow time to test edge-to-edge and make the required updates. Alternatively, call enableEdgeToEdge() for Kotlin or EdgeToEdge.enable() for Java for backward compatibility.

2. Your app uses deprecated APIs or parameters for edge-to-edge

- One or more of the APIs you use or parameters that you set for edge-to-edge and window display have been deprecated in Android 15. To fix this, migrate away from these APIs or parameters.

## Lint Issue Types and Counts

Blocking errors by issue type:

| Issue Type            | Count |
| --------------------- | ----: |
| MissingTranslation    |     5 |
| NewApi                |     4 |
| RestrictedApi         |     3 |
| GestureBackNavigation |     2 |
| PropertyEscape        |     1 |
| AppCompatCustomView   |     1 |

Top warning-heavy issue types:

| Issue Type        | Count |
| ----------------- | ----: |
| Typos             |   204 |
| UnusedResources   |    49 |
| RtlHardcoded      |    42 |
| MissingQuantity   |    20 |
| ObsoleteSdkInt    |    17 |
| RelativeOverlap   |    11 |
| Autofill          |     7 |
| StringFormatCount |     7 |
| ApplySharedPref   |     6 |
| SetTextI18n       |     6 |

## Relationship: Lint vs Play Console Warnings

There is partial overlap, but they are not the same signal.

- Direct overlap: `NewApi` lint findings on `android:windowOptOutEdgeToEdgeEnforcement` map to Play edge-to-edge/deprecation concerns.
- Adjacent overlap: `GestureBackNavigation` lint findings are not edge-to-edge, but are part of Android 15/16 behavior modernization and should be staged together.
- Separate work: localization, typos, resource cleanup, and formatting warnings are useful quality work but do not directly clear the Play edge-to-edge warnings.

## Staged Execution Plan (Low Regression Strategy)

Guiding principle: remove release blockers first, then high-value warning clusters, with narrow PRs and verification gates between each stage.

### Stage 0: Baseline and Guardrails (No behavior change)

Scope:

- Capture this inventory and keep it updated.
- Keep lint output artifacts for before/after comparison.

Verification:

- `./gradlew :app:lint --console=plain`
- `./gradlew test`
- `./gradlew assembleSideload`

Exit criteria:

- Reproducible baseline counts by issue type.

### Stage 1: Clear Lint Errors with Minimal Surface Area

Scope order:

1. `PropertyEscape` (config-only; no runtime behavior)
2. `MissingTranslation` for newly introduced keys
3. `RestrictedApi` replacement for `MenuBuilder` usage
4. `AppCompatCustomView` migration for custom spinner base class
5. `GestureBackNavigation` migration to `OnBackPressedDispatcher`
6. `NewApi` style attribute cleanup/scope correction

Risk profile:

- Low to medium, mostly contained changes.
- Main behavior risk is back navigation and menu behavior.

Verification:

- Unit tests + lint + assemble on each small PR.
- For back-navigation changes, add/adjust focused JVM tests where possible.

Exit criteria:

- Lint errors reduced to zero without new failures.

### Stage 2: Play Console Edge-to-Edge Remediation

Scope:

- Remove dependency on deprecated edge-to-edge opt-out parameters.
- Standardize inset handling on activity roots and dialogs/surfaces touched by Material3 themes.
- Validate that top/bottom content is not obscured after changes.

Risk profile:

- Medium. Primarily UI/layout regression risk.

Suggested ordering:

1. Remove/replace deprecated style parameters in smallest affected theme set.
2. Verify shared inset helper behavior remains correct for all activity hosts.
3. Expand changes to remaining affected themes/screens only after prior gate is green.

Verification:

- Lint must stay green for edge/back categories.
- Build + unit tests every step.
- Keep changes split per screen/theme cluster to simplify rollback.

Exit criteria:

- No lint edge-related blockers.
- Play warning trend expected to improve on next Play analysis cycle.

### Stage 3: Warning Debt Reduction by Value and Safety

Scope waves:

1. Localization correctness wave: `Typos`, `MissingQuantity`, `StringFormatCount`, `PluralsCandidate`
2. UI/i18n wave: `RtlHardcoded`, `SetTextI18n`, `DefaultLocale`, `SimpleDateFormat`
3. Cleanup wave: `UnusedResources`, `Icon*`, `Obsolete*`

Risk profile:

- Low to medium depending on string/resource deletions.

Verification:

- Keep PRs small and category-focused.
- Re-run lint and tests after each wave.

Exit criteria:

- Warning count reduced materially with no behavior regressions.

## Change Types to Use in PRs

Recommended PR taxonomy:

1. `lint-fix/config` (property files, gradle/lint config)
2. `lint-fix/localization` (translations, typo and plural fixes)
3. `lint-fix/api-migration` (back navigation, restricted API replacements)
4. `lint-fix/ui-edge-to-edge` (insets, theme attributes)
5. `lint-fix/cleanup` (unused resources and low-risk debt)

Each PR should include:

- Before/after lint counts for the issue types touched.
- Scope declaration (what is intentionally not changed).
- Regression-sensitive areas to spot check.

## Conservative Plan: 11 Check-ins

Use this when minimizing regression risk is more important than speed. Keep each check-in narrow and independently reversible.

### Check-in 1: Baseline Snapshot

Scope:

- Re-run lint, tests, and assemble to confirm reproducible baseline numbers.
- Update this document with any count drift.

Gate:

- `./gradlew :app:lint --console=plain`
- `./gradlew test`
- `./gradlew assembleSideload`

### Check-in 2: Config-only Error Fixes

Scope:

- Fix `PropertyEscape` and any non-runtime config formatting issues.

Gate:

- Lint error count decreases with no new errors.

### Check-in 3: Missing Translation Errors

Scope:

- Add only the missing keys causing `MissingTranslation` errors.
- Do not perform broad translation rewrites in this pass.

Gate:

- `MissingTranslation` errors at zero.

### Check-in 4: Restricted API Replacement

Scope:

- Remove `RestrictedApi` usage in menu handling.
- Keep behavior equivalent and limited to the touched flow.

Gate:

- `RestrictedApi` errors at zero.
- Unit tests and assemble remain green.

### Check-in 5: AppCompat Custom View Migration

Scope:

- Address `AppCompatCustomView` by migrating the custom spinner inheritance.

Gate:

- `AppCompatCustomView` error at zero.

### Check-in 6: Predictive Back Migration

Scope:

- Migrate `onBackPressed` handling to `OnBackPressedDispatcher` patterns.
- Keep logic parity with existing back behavior.

Gate:

- `GestureBackNavigation` errors at zero.
- No regressions in back-stack behavior in existing tests.

### Check-in 7: Edge-to-edge Attribute Cleanup

Scope:

- Resolve `NewApi` findings tied to edge-to-edge style attributes.
- Prefer smallest viable theme-scope edits first.

Gate:

- `NewApi` lint errors at zero.
- No new edge/back lint regressions.

### Check-in 8: Play Console Alignment Pass

Scope:

- Validate that edge-to-edge handling is consistent across affected activity/theme surfaces.
- Tighten any remaining compatibility gaps identified by lint/report review.

Gate:

- Lint remains green for edge/back categories.
- Build and tests remain green.

### Check-in 9: Localization Warning Wave

Scope:

- Reduce high-volume localization warnings in one focused pass:
  `Typos`, `MissingQuantity`, `StringFormatCount`, `PluralsCandidate`.

Gate:

- Material warning reduction in those categories without new errors.

### Check-in 10: UI/i18n + Cleanup Wave

Scope:

- Address lower-risk UI/i18n and cleanup categories:
  `RtlHardcoded`, `SetTextI18n`, `DefaultLocale`, `SimpleDateFormat`,
  and selected `UnusedResources`/icon/obsolete findings.

Gate:

- Net warning count reduced.
- No lint error regressions.

### Check-in 11: Hygiene Automation and Merge Enforcement

Scope:

- Add repository automation so lint hygiene is continuously enforced for future feature and bug work.
- Introduce CI workflow(s) that run the same local quality gate stack on pull requests and main-branch changes:
  - `./gradlew :app:lint --console=plain`
  - `./gradlew test`
  - `./gradlew assembleSideload`
- Ensure existing custom guards are included through Gradle task wiring:
  - `checkNoHardcodedUiStrings`
  - `checkReminderLocalizationKeys`
- Add branch protection guidance to require passing CI checks before merge.
- Add baseline governance rules so lint baseline updates are explicit and reviewable.

Gate:

- CI runs successfully on a test PR.
- Required status checks are configured and block merge when failing.
- A deliberately introduced lint regression fails CI as expected.

Deliverables:

- CI workflow file(s) under `.github/workflows/`.
- Short contributor guidance for running the same commands locally before push.
- PR checklist item requiring before/after lint counts for touched categories.

Notes:

- If any check-in grows beyond a narrow scope, split it before merging.
- Record before/after counts for the categories targeted by that check-in.
- Keep each check-in independently rollback-safe.

## VS Code Tooling (Optional but Helpful)

There is no single first-party Android Lint extension in VS Code equivalent to Android Studio integration, but these help:

- `ms-sarifvscode.sarif-viewer`
  - Open `app/build/reports/lint-results-debug.sarif` with richer triage UI.

- `usernamehw.errorlens`
  - Surfaces diagnostics inline, which helps quick cleanup passes.

- `vscjava.vscode-gradle` (already installed)
  - Convenient task running for repeat lint/test/assemble loops.

Recommended workflow in VS Code:

1. Run `:app:lint` from Gradle or terminal.
2. Open SARIF output for grouped triage.
3. Fix one issue category per PR.
4. Re-run lint and unit tests before merge.
