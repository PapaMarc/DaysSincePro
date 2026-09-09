# Design Companion: Localization Phase 1 Dev Design

**Companion To:** [DCR_LocalizationPlan.md](DCR_LocalizationPlan.md)  
**Target Phase:** Phase 1 implementation  
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

---

## 2. Resource Key and Naming Conventions

Use stable, translator-friendly key conventions during extraction to avoid future churn:

1. lowercase snake case only
2. Prefix by feature/surface where helpful (`about_`, `settings_`, `history_`, `import_`, `export_`)
3. One semantic meaning per key (do not reuse one key for unrelated contexts)
4. Avoid UI-position-based names (`label1`, `text_top`) that become ambiguous over time
5. Preserve existing key names unless there is a clear quality reason to rename

If a rename is necessary, do it in the same PR as all call-site updates.

---

## 3. Formatting, Placeholders, and Plurals Rules

Localization-safe formatting standards for all new/updated strings:

1. Use numbered placeholders (`%1$s`, `%2$d`) to allow language-specific reordering
2. Use `<plurals>` for counted units (day/days, month/months, year/years, week/weeks)
3. Avoid user-facing string concatenation in Java/Kotlin
4. Keep punctuation and spacing in resources, not code
5. Keep grammar/phrasing in resources, not locale-specific conditionals in business logic

Phase 2 will apply these rules when refactoring `DaysSinceCalculations` term handling.

---

## 4. Language Picker UX Contract (Phase 1 Behavior)

Language picker behavior should be explicit and deterministic:

1. Include first option: "Use device language"
2. Include only currently exposed locales from the release allow-list
3. Persist language selection through `AppCompatDelegate.setApplicationLocales(...)`
4. Locale precedence: app-selected locale > device locale > default English
5. Language change effect in Phase 1: apply after restart prompt acceptance

Recommended restart prompt text:

"Restart app to apply language update."

---

## 5. Exposure/Rollback Control Model

Keep translation assets and release exposure decoupled:

1. Translation folders may exist in source even if not exposed yet
2. User-visible locale list is controlled by a single release allow-list
3. `locales_config.xml` and in-app picker must use the same allow-list source
4. Rollback path: remove a locale from allow-list in next build without deleting translation files

This model supports Wave 2 preparation while shipping only approved locales.

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

## Implementation Notes

1. Keep changes small and reviewable by grouping extraction logically (About, category, import/export, Main toast) rather than one giant mixed commit.
2. Avoid touching date-format behavior in Phase 1; that remains deferred by policy.
3. Preserve current behavior while changing only string sourcing mechanics.
