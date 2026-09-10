# Design Companion: Localization Dev Design

**Companion To:** [DCR_LocalizationPlan.md](DCR_LocalizationPlan.md)  
**Target Phase:** Phase 1 through Phase 4 implementation  
**Status:** Proposed  
**Audience:** Implementation and review

---

## 1. Hardcoded String Extraction Checklist (Implementation Scope)

Extract user-facing string literals to `res/values/strings.xml` for Phase 1 classes already identified in the plan:

1. `AboutDialog`
2. `CategorySelectionPolicy`
3. `CsvImporter`
4. `CsvExportResult`
5. `MainActivity` leftover dev-only toast copy

Include new About dialog paragraph resources for translation/support feedback contact copy.

Out of scope for Phase 1 extraction:

1. Widget copy (feature disabled/deferred)
2. CSV schema/header identifiers used for interchange stability
3. Internal logs/debug-only diagnostics

Implementation rule: no new user-facing string literals may be introduced in Java/Kotlin during extraction.

In short: file format remains canonical English; app feedback about that file format is localized.
This boundary is a cross-phase contract: CSV schema/header identifiers remain canonical; user-visible import/export text localizes.

---

## 2. Resource Key and Naming Conventions

Use stable, translator-friendly key conventions during extraction to avoid future churn:

1. lowercase snake case only
2. Prefix by feature/surface where helpful (`about_`, `settings_`, `history_`, `import_`, `export_`)
3. One semantic meaning per key (do not reuse one key for unrelated contexts)
4. Avoid UI-position-based names (`label1`, `text_top`) that become ambiguous over time
5. Preserve existing key names unless there is a clear quality reason to rename
6. Cross-phase key stability policy: once a key is merged, do not rename/delete it during Phases 1-4 unless it is objectively defective

If a rename is necessary, do it in the same PR as all call-site updates.
If a rename is necessary, update all active locale files in that same PR so no locale is left behind on stale keys.

---

## 3. Formatting, Placeholders, and Plurals Rules

Localization-safe formatting standards for all new/updated strings:

1. Use numbered placeholders (`%1$s`, `%2$d`) to allow language-specific reordering
2. Use `<plurals>` for counted units (day/days, month/months, year/years, week/weeks)
3. Avoid user-facing string concatenation in Java/Kotlin
4. Keep punctuation and spacing in resources, not code
5. Keep grammar/phrasing in resources, not locale-specific conditionals in business logic

Policy timing: this standard is effective immediately in Phase 1 extraction work and remains mandatory for Phases 2-4.
Phase 2 specifically applies it to `DaysSinceCalculations` term/plural refactoring.

---

## 4. Language Picker UX Contract (Phase 1-3 Behavior)

Language picker behavior should be explicit and deterministic:

1. Include first option: "Use device language"
2. Include only currently exposed locales from the release allow-list
3. Persist language selection through `AppCompatDelegate.setApplicationLocales(...)`
4. Locale precedence: app-selected locale > device locale > default English
5. Language change effect in Phases 1-3: apply after restart prompt acceptance (restart-first policy for reliability)

Recommended restart prompt text:

"Restart app to apply language update."

---

## 5. Exposure/Rollback Control Model

Keep translation assets and release exposure decoupled:

1. Translation folders may exist in source even if not exposed yet
2. User-visible locale list is controlled by one policy class plus build-type platform locale config files
3. In-app picker and Android system App Language lists must be intentionally kept in sync
4. Rollback path: remove a locale from exposure controls in the next build without deleting translation files

This model supports Wave 2 preparation while shipping only approved locales.

Implemented control surface (operator view):

1. In-app language picker policy: `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`
2. Release platform locale list (Android Settings -> App Language): `app/src/main/res/xml/locales_config.xml`
3. Sideload platform locale list override: `app/src/sideload/res/xml/locales_config.xml`

Operator guide: common enable/disable actions

1. Hide pseudolocales in release, but show in sideload (current default)
   - Keep `en-XA` and `ar-XB` out of `RELEASE_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Keep `en-XA` and `ar-XB` in `SIDELOAD_ALWAYS_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Keep `en-XA` and `ar-XB` absent from `app/src/main/res/xml/locales_config.xml`.
   - Keep `en-XA` and `ar-XB` present in `app/src/sideload/res/xml/locales_config.xml`.

2. Disable a normal locale everywhere without deleting translations (example: `it`)
   - Remove `it` from `RELEASE_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Remove `<locale android:name="it"/>` from `app/src/main/res/xml/locales_config.xml`.
   - Remove `<locale android:name="it"/>` from `app/src/sideload/res/xml/locales_config.xml`.
   - Do not delete `app/src/main/res/values-it/`; keep translation assets intact for future re-enable.

3. Re-enable a normal locale everywhere (example: `it`)
   - Add `it` back to `RELEASE_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Add `<locale android:name="it"/>` to `app/src/main/res/xml/locales_config.xml` and `app/src/sideload/res/xml/locales_config.xml`.

4. Expose a QA-only locale in sideload only (example: `en-XA`)
   - Add `en-XA` to `SIDELOAD_ALWAYS_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Add `<locale android:name="en-XA"/>` only in `app/src/sideload/res/xml/locales_config.xml`.
   - Keep `en-XA` out of `RELEASE_EXPOSED_LOCALES` and out of `app/src/main/res/xml/locales_config.xml`.

5. Promote a sideload-only locale to release (example: `en-XA` for a temporary release experiment)
   - Add `en-XA` to `RELEASE_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Add `<locale android:name="en-XA"/>` to `app/src/main/res/xml/locales_config.xml`.
   - Keep or remove it from `SIDELOAD_ALWAYS_EXPOSED_LOCALES` depending on whether sideload should force it visible even if later removed from release.

Recommended guardrail:

1. Add a drift guard test/check that compares `LocaleExposureConfig` effective lists with locale tags declared in `app/src/main/res/xml/locales_config.xml` and `app/src/sideload/res/xml/locales_config.xml`.

---

## 6. Phase 1 Definition of Done (DoD)

Phase 1 is complete only when all criteria below are satisfied:

1. Scope strings extracted for listed classes (Section 1)
2. No new hardcoded user-facing strings introduced
3. Build/compile passes
4. Existing JVM unit tests pass
5. No blocker defects in Wave 1 smoke verification
6. About dialog feedback paragraph present and linked to translation/support channels

Blockers are defined in the primary localization DCR.

---

## 7. Minimal Smoke Verification Matrix (Wave 1)

Perform lightweight per-locale smoke checks for Wave 1 locales on core user paths:

1. Main list screens and navigation labels
2. Add/Edit event flows
3. Settings language picker flow and restart prompt
4. About dialog paragraph and contact links
5. Notification-facing strings for newly scheduled notifications

Record issues using the feedback template from [DCR_LocalizationPlan.md](DCR_LocalizationPlan.md).

---

## 8. Pseudolocale Checks (Informational Only)

This section is informational and not a release gate for Phase 1.

1. `en-XA` pass helps detect expansion/concatenation vulnerabilities
2. `ar-XB` pass helps surface bidi/RTL-sensitive assumptions early
3. Findings should be logged for backlog prioritization, not treated as Phase 1 blockers unless they also reproduce in shipped locales

---

## 9. Feedback Severity and Triage Rubric

Use this four-level severity rubric for localization feedback:

1. `P0-ShipStopper`: crash, unusable flow, or severe text defect that blocks normal use.
2. `P1-MustHave`: major issue that does not fully block use but must be fixed before release.
3. `P2-ShouldHave`: meaningful quality issue that should be fixed when feasible.
4. `P3-NiceToHave`: minor polish or preference-level feedback.

Minimum intake fields per report:

1. Locale
2. Screen/feature
3. App version
4. Screenshot (when possible)
5. Severity (`P0`/`P1`/`P2`/`P3`)

No-hardcoded-UI-string enforcement guard:

1. Keep the policy rule: no new user-facing string literals in Java/Kotlin.
2. Add an automated pre-commit/CI check (regex or script) for common UI-string sinks in Java/Kotlin (for example `setText("...")`, `showToast("...")`, `setTitle("...")`, dialog builder text literals).
3. Exclude test sources and known non-UI constants paths from this check.
4. Treat any new violation as `P1-MustHave` until triaged.
5. Keep human review as a backstop for false negatives/positives.

Owner-managed triage workflow:

1. Assign severity (`P0`/`P1`/`P2`/`P3`) on intake.
2. Validate reproducibility in the reported locale and one control locale (`en`).
3. Record disposition: fix now, fix later, or reject with rationale.
4. For `P0` and `P1`, prioritize into the next shipping build.
5. For `P2` and `P3`, track in backlog and batch by locale/surface.

---

## 10. English Regression Baseline (12 Strings)

Before and after Phase 1 extraction, verify these English strings remain unchanged unless intentionally edited:

1. `dayssince` -> `Days Since` (tab label)
2. `daysuntil` -> `Until Next` (tab label)
3. `sincelast` -> `Since Last` (tab label)
4. `add_new` -> `Add` (button/menu label)
5. `edit` -> `Edit` (button/menu label)
6. `remove` -> `Remove` (button/menu label)
7. `search` -> `Search` (menu/toolbar)
8. `about` -> `About` (menu)
9. `days_diff` -> `Days Between Calculator` (main overflow menu)
10. `config` -> `Options` (main overflow menu)
11. `daily_notifications` -> `Daily Notifications` (settings)
12. `are_you_sure` -> `Are you sure?` (confirmation dialog)

If any of these change unexpectedly during extraction-only work, treat as regression and review before merge.

---

## 11. French Mapping Prep for Phase 2

Before removing legacy locale branching in `DaysSinceCalculations`, prepare a one-to-one mapping table in implementation notes/PR description:

1. Legacy hardcoded French token/phrase
2. New resource key name
3. French value placed into `values-fr/strings.xml` (or `<plurals>` entry)
4. Matching English default key/value in `values/strings.xml`

Goal: preserve currently working French wording behavior while moving from code-branch strings to resource-driven localization.

---

## 12. Implementation Notes

1. Keep changes small and reviewable by grouping extraction logically (About, category, import/export, Main toast) rather than one giant mixed commit.
2. Avoid touching date-format behavior in Phase 1; that remains deferred by policy.
3. Preserve current behavior while changing only string sourcing mechanics.

---

## 13. Phase 3A Definition of Done (Stabilization + Guards)

Phase 3A is complete only when all criteria below are satisfied:

1. `DaysSinceCalculations` localization behavior remains resource-driven with no locale-branching reintroduction.
2. Hardcoded UI string guard runs as a required verification gate (not ad hoc/manual-only).
3. Compile/resource verification passes.
4. Targeted JVM tests for date-calculation cutover and recurrence urgency pass.
5. Guard baseline remains intentional and unchanged unless explicitly reviewed and approved.

Required commands (or equivalent CI wiring):

1. `./gradlew :app:compileDebugJavaWithJavac :app:processDebugResources`
2. `./gradlew :app:testDebugUnitTest --tests com.merware.dayssincepro.DaysSinceCalculationsCutoverTest --tests com.merware.dayssincepro.SimpleDateProlepticGregorianTest --tests com.merware.dayssincepro.OnAlarmReceiveUrgencyTest`
3. `./gradlew :app:checkNoHardcodedUiStrings`

---

## 14. Deferred Constructor Hardening Policy (Indefinite)

Work definition:

1. Remove legacy no-context `DaysSinceCalculations` constructors.
2. Require Context-aware construction in all production call paths.
3. Update test helpers/callers to explicit context-safe construction strategy.

Current decision:

1. Deferred with no fixed delivery date.

Implications while deferred:

1. API surface remains broader than ideal.
2. Fallback no-context behavior remains available for compatibility.
3. Localization correctness is still preserved in migrated production paths.

When to revisit:

1. Dedicated technical-debt hardening pass.
2. Any future broad call-site refactor that already touches constructor usage.
3. If defects are traced to legacy constructor fallback behavior.

Can this remain deferred indefinitely?

1. Yes, provided compatibility overloads stay stable, tests remain green, and no regressions are attributed to the legacy path.
