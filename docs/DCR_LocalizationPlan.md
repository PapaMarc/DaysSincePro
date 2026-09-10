# Design Change Request (DCR): Localization Plan

**Document ID:** DCR-2026-09-09-A  
**Target Component:** String resources, `DaysSinceCalculations`, Settings (`PrefActivity`), locale/language selection  
**Status:** Proposed (no code changes made yet)  
**Author:** DaysSincePro Architecture

---

## 1. Motivation

The app currently has partial, inconsistent localization support: a `values-fr/` resource folder exists but is nearly empty, while the actual French wording is hardcoded in Java as an if/else branch that duplicates (and bypasses) Android's own resource system. Before adding more languages, the app needs a sound architectural foundation so each new language is a low-risk, mostly-translation-only addition rather than a repeated code change.

---

## 2. Current State Assessment

- **Partial resource-based localization already exists.** Most UI strings flow through `res/values/strings.xml` and `getString(R.string.x)` (e.g. [HistoryActivity.java](../app/src/main/java/com/merware/dayssincepro/HistoryActivity.java), [CreateCategoryActivity.java](../app/src/main/java/com/merware/dayssincepro/CreateCategoryActivity.java), [DaysDiffActivity.java](../app/src/main/java/com/merware/dayssincepro/DaysDiffActivity.java)).
- **`values-fr/strings.xml` exists but only overrides 3 strings** (`about_maintained`, `about_republished`, `about_maintained_and_republished`). It is not a functioning French translation of the app.
- **The real French support is hand-rolled and bypasses resources entirely.** [DaysSinceCalculations.java](../app/src/main/java/com/merware/dayssincepro/DaysSinceCalculations.java)'s `setTerms()` checks `Locale.getDefault().getLanguage().equals("fr")` and hardcodes French words (`"demain"`, `"aujourd'hui"`, `"jours"`, etc.) directly in Java against a hardcoded English else-branch. This is the core anti-pattern to remove: it does not scale past two languages, has no real pluralization rules, and cannot be driven by an in-app language override without adding more hardcoded branches.
- **Other user-visible strings are still hardcoded in Java**, not resources: [AboutDialog.java](../app/src/main/java/com/merware/dayssincepro/AboutDialog.java) (`"Originally written by "`), [CategorySelectionPolicy.java](../app/src/main/java/com/merware/dayssincepro/CategorySelectionPolicy.java) (`"Uncategorized"`), [ConfigWidgetActivity.java](../app/src/main/java/com/merware/dayssincepro/ConfigWidgetActivity.java) (`APP_NAME` literal), and various `CsvImporter`/`CsvExportResult` messages. A known leftover dev-only toast ("ADD new now go set alarm...") in `MainActivity`'s add-activity result path is also flagged as needing removal/localization (see repo memory).
- **`DateFormat.java`** implements a manual US/UK/M-D-Y user preference. This is a related but separate concern from language localization — it is not locale-driven, it is a hand-picked format string, and is not addressed by this plan except as a noted future option.
- **Dependencies already support the recommended approach.** `app/build.gradle` has `androidx.appcompat:appcompat:1.7.0` and `minSdk 30` — both comfortably support AndroidX's per-app-language API with no version bump required.

**Verdict:** the app is roughly half-localizable. The resource plumbing (`R.string`, `values-fr/`) exists but is bypassed by hardcoded logic in exactly the place that matters most for a "days since" app's everyday output.

---

## 3. Architectural Recommendation

Do **not** build a custom language-list/strings-section mechanism (e.g. a hand-rolled language map or an in-app settings switch that swaps string tables manually). Use Android's existing localization system, which already does this work:

1. **Resource qualifiers, not sections in one file.** Each supported language gets its own `res/values-<lang>/strings.xml` (and `values-<lang>-r<REGION>/` for regional variants) containing only the strings that differ from the default `res/values/strings.xml`. Android resolves missing keys by falling back to the default file automatically — no custom mapping code is needed.
2. **Per-app language switching via `AppCompatDelegate.setApplicationLocales(LocaleListCompat)`.** This is the standard AndroidX mechanism, not something to build from scratch:
   - Works uniformly across all API levels back to 21 (well below this app's `minSdk 30`).
   - On API 33+, declaring `android:localeConfig="@xml/locales_config"` in the manifest integrates it with the system's own **Settings → App → Language** screen for free.
   - AndroidX persists the chosen locale for you — no custom SharedPreferences scheme needed for "selected language."
   - System-locale detection and "no match → fall back to default (English)" is Android's default resource-resolution behavior — no detection logic needs to be written.
3. **Settings UI**: add one "Language" entry to the existing Settings screen (`PrefActivity`) that either launches `Settings.ACTION_APP_LOCALE_SETTINGS` (API 33+) or shows a simple in-app list (for completeness on API 30-32) that calls `AppCompatDelegate.setApplicationLocales(...)`.
4. **Pluralization via `<plurals>` (quantity strings)**, not manual singular/plural string picks. This directly replaces the ad hoc `s_day`/`s_days`, `s_month`/`s_months` fields in `DaysSinceCalculations` and is required for languages with different plural rules than English (e.g. French's binary singular/plural differs from Russian/Polish's three-way plural forms).

---

## 4. Phased Work Plan

| Phase                 | Work                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Risk                                                                                                                                           |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| 1                     | Sweep remaining hardcoded Java strings into `values/strings.xml` (`AboutDialog`, `CategorySelectionPolicy`, `CsvImporter`/`CsvExportResult` messages, the leftover dev toast). Include the new About-dialog translation feedback paragraph with translation/support email contact string. Also pre-populate `values-fr/strings.xml` with resource-mapped equivalents of currently working hardcoded French terms used in `DaysSinceCalculations` as a behavior-preservation bridge before Phase 2 branch removal. No behavior change. | Low — mechanical, additive                                                                                                                     |
| 2                     | Refactor `DaysSinceCalculations.setTerms()` to stop hardcoding French; source all terms from string/plural resources (thread a `Context` in, or have the caller resolve strings and pass them in). Delete the hardcoded French branch entirely.                                                                                                                                                                                                                                                                                       | Medium — this class has had real date-math bugs fixed before; needs care and JVM test coverage per existing repo test conventions before/after |
| 3                     | Wire up `AppCompatDelegate.setApplicationLocales()`, add a Settings "Language" entry, and add `res/xml/locales_config.xml` + manifest `android:localeConfig` reference                                                                                                                                                                                                                                                                                                                                                                | Low-medium — new but small, well-documented AndroidX API                                                                                       |
| 4                     | Populate real `values-<lang>/strings.xml` translations for Tier 1 languages (Section 5) using machine translation as baseline plus community/volunteer feedback loops for iterative correction                                                                                                                                                                                                                                                                                                                                        | Not a code risk — translation content/accuracy risk only                                                                                       |
| 5 (deferred/optional) | Locale-aware date formatting (replace manual US/UK/M-D-Y picker with a "System Default" option using `java.time`/`DateFormat.getDateInstance(Locale)`); widget localization (feature currently disabled, may be rewritten before re-enable); RTL layout audit if an RTL language is added later; Traditional Chinese (`zh-TW`) variant if needed                                                                                                                                                                                      | Low priority — only needed if scope expands beyond this plan                                                                                   |

### 4.1 Phase 3 Execution Split (Current)

Phase 3 is executed in two slices to keep risk low and scope explicit:

1. **Phase 3A (current milestone):** stabilization and enforcement gates.
   - Confirm no new hardcoded UI strings policy regressions.
   - Keep guard checks as required verification for local/CI workflows.
   - Run compile + targeted JVM tests for `DaysSinceCalculations` and recurrence/notification behavior.
2. **Phase 3B (feature wiring):** in-app language picker + `AppCompatDelegate.setApplicationLocales(...)` + `locales_config` integration.
   - This remains planned work for broader locale rollout.
   - It can be scheduled independently once non-EN/FR release exposure is imminent.

**Biggest risk concentration:** Phase 2, because `DaysSinceCalculations` mixes localization with the app's core day-count business logic. Keep the string-resolution refactor isolated from any date-math changes, and extend JVM tests before touching it (see repo memory conventions: `DaysSinceCalculationsCutoverTest`, etc.).

---

## 5. Tier 1 Language Plan

Tier 1 languages were selected because they are all Latin, Cyrillic, or CJK/Devanagari scripts renderable by Android's bundled system fonts (no custom font work), are all LTR (no RTL mirroring work), and — aside from Hindi, which is flagged for a visual QA pass as the first complex/shaped script — carry only ordinary translated-text-length variation, not the German/Russian/Polish "moderate expansion" tier being unusually large.

| Language             | Tag(s)                | Script/Font Risk                                                                                          | Layout Expansion Risk                                                       | Notes                                                                                                                                                    |
| -------------------- | --------------------- | --------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| English (default)    | `en` (base `values/`) | None                                                                                                      | Baseline                                                                    | Existing default                                                                                                                                         |
| Spanish              | `es`                  | None — Latin                                                                                              | Low-moderate                                                                |                                                                                                                                                          |
| Portuguese           | `pt`                  | None — Latin                                                                                              | Low-moderate                                                                | Generic, covers Portugal/Africa                                                                                                                          |
| Portuguese (Brazil)  | `pt-rBR`              | None — Latin                                                                                              | Low-moderate                                                                | Thin diff over `pt`, only strings that actually differ                                                                                                   |
| French               | `fr`                  | None — Latin                                                                                              | Low-moderate                                                                | Already has a stub folder; needs to be completed and reconciled with Phase 2's removal of the hardcoded French branch                                    |
| German               | `de`                  | None — Latin (incl. ß, umlauts)                                                                           | **Moderate-high** — ~30-35% longer strings, compound words                  | Verify no `singleLine` truncation on translated (not user-entered) labels                                                                                |
| Russian              | `ru`                  | None — Cyrillic, full system-font coverage on API 30+                                                     | Moderate                                                                    |                                                                                                                                                          |
| Chinese (Simplified) | `zh-rCN`              | None — CJK, full system-font coverage                                                                     | Low (compact glyphs) but no inter-word spacing — verify line-break behavior |                                                                                                                                                          |
| Japanese             | `ja`                  | None — CJK                                                                                                | Low, same line-break note as above                                          |                                                                                                                                                          |
| Korean               | `ko`                  | None — Hangul                                                                                             | Low-moderate                                                                |                                                                                                                                                          |
| Italian              | `it`                  | None — Latin                                                                                              | Low-moderate                                                                |                                                                                                                                                          |
| Dutch                | `nl`                  | None — Latin                                                                                              | Moderate — occasional long compound words, same tier as German              |                                                                                                                                                          |
| Hindi                | `hi`                  | None — Devanagari; system-font (Noto Sans Devanagari) rendering solid since API 21, well past `minSdk 30` | Low (word length)                                                           | First complex/shaped script added — deliberate visual QA pass recommended (conjunct consonants, matra line-height), not expected to require code changes |
| Indonesian           | `id`                  | None — Latin                                                                                              | Low-moderate                                                                |                                                                                                                                                          |
| Polish               | `pl`                  | None — Latin Extended-A (ą, ć, ę, ł, ń, ó, ś, ź, ż), full system-font coverage                            | Moderate                                                                    |                                                                                                                                                          |

### 5.1 Planned Release Waves (Current Decision)

- **Wave 1 (initial localization release):** `es`, `fr`, `de`, `pt`, `pt-rBR`, `it`, `zh-rCN`, `hi`.
- **Wave 2 (follow-on release):** `ru`, `ja`, `ko`, `nl`, `id`, `pl`.

Wave 1 intentionally includes `zh-rCN` and `hi` to capture early tester feedback in the first release cycle on CJK wrap behavior (no spaces) and Devanagari vertical rendering/line-height behavior. The expected impact is still low and primarily QA-observational, not architectural.

### 5.2 Pseudolocale QA Coverage

- **Wave 1 QA pseudolocale:** `en-XA` (accented/expanded English) for string expansion and concatenation checks.
- **Wave 2 QA pseudolocale:** `ar-XB` (bidi pseudolocale) for early RTL-surface detection.

These are QA locales and do not require adding translation folders.

Exposure policy for pseudolocales:

- They can be exposed/hidden using the same allow-list mechanism as standard locales.
- Release builds should default to hiding pseudolocales.
- Sideload/internal builds may keep pseudolocales always visible in the language picker for ongoing test utility.

---

## 6. Proposed Folder and File Structure

Additive only — no existing files move or restructure. New sibling folders under `app/src/main/res/`:

```
app/src/main/res/
    values/                     ← existing default (English) - source of truth for all string keys
        strings.xml
    values-fr/                  ← existing stub, to be completed (Phase 4)
        strings.xml
    values-es/                  ← new: Spanish
        strings.xml
    values-pt/                  ← new: Portuguese (generic)
        strings.xml
    values-pt-rBR/              ← new: Brazilian Portuguese (diff-only over values-pt)
        strings.xml
    values-de/                  ← new: German
        strings.xml
    values-ru/                  ← new: Russian
        strings.xml
    values-zh-rCN/              ← new: Chinese (Simplified)
        strings.xml
    values-ja/                  ← new: Japanese
        strings.xml
    values-ko/                  ← new: Korean
        strings.xml
    values-it/                  ← new: Italian
        strings.xml
    values-nl/                  ← new: Dutch
        strings.xml
    values-hi/                  ← new: Hindi
        strings.xml
    values-id/                  ← new: Indonesian
        strings.xml
    values-pl/                  ← new: Polish
        strings.xml
    xml/
        locales_config.xml      ← new (Phase 3): declares supported locale tags for android:localeConfig
app/src/sideload/res/
    xml/
        locales_config.xml      ← sideload-only locale overlay (adds test/pseudolocale exposure)
app/src/main/java/com/merware/dayssincepro/
    LocaleExposureConfig.java   ← in-app picker exposure policy (release + sideload additions)
```

No changes to `layout/`, `layout-v14/`, `layout-land/`, or `layout-w820dp/` folders are anticipated for Tier 1 languages (see Section 5's risk notes and Section 8).

Language enable/disable policy (implemented): all locale resource folders can exist in source while only a subset is exposed by a three-file control surface: `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java` (in-app picker policy), `app/src/main/res/xml/locales_config.xml` (release platform App Language list), and `app/src/sideload/res/xml/locales_config.xml` (sideload platform overlay). This applies to both production locales and pseudolocales, with profile-specific defaults (release can hide pseudolocales; sideload/internal can keep them visible).

### 6.1 Locale Exposure Operator Guide

Use these concrete edit patterns to expose/hide locales without deleting translation folders.

1. Hide pseudolocales in release, keep visible in sideload (default)
   - In `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`: keep `en-XA` and `ar-XB` only in `SIDELOAD_ALWAYS_EXPOSED_LOCALES`, not in `RELEASE_EXPOSED_LOCALES`.
   - In `app/src/main/res/xml/locales_config.xml`: omit `<locale android:name="en-XA"/>` and `<locale android:name="ar-XB"/>`.
   - In `app/src/sideload/res/xml/locales_config.xml`: include both tags.

2. Disable one normal locale in both release and sideload (example: `it`)
   - Remove `it` from `RELEASE_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Remove `<locale android:name="it"/>` from `app/src/main/res/xml/locales_config.xml`.
   - Remove `<locale android:name="it"/>` from `app/src/sideload/res/xml/locales_config.xml`.
   - Keep `app/src/main/res/values-it/strings.xml` in source.

3. Enable one normal locale in both release and sideload (example: `it`)
   - Add `it` to `RELEASE_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Add `<locale android:name="it"/>` to both XML files.

4. Add a QA-only locale in sideload (example: `en-XA`)
   - Add `en-XA` to `SIDELOAD_ALWAYS_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Add `<locale android:name="en-XA"/>` to `app/src/sideload/res/xml/locales_config.xml` only.
   - Keep it absent from `RELEASE_EXPOSED_LOCALES` and from `app/src/main/res/xml/locales_config.xml`.

5. Promote sideload-only locale to release (example: `en-XA`)
   - Add `en-XA` to `RELEASE_EXPOSED_LOCALES` in `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`.
   - Add `<locale android:name="en-XA"/>` to `app/src/main/res/xml/locales_config.xml`.
   - Optionally keep it in `SIDELOAD_ALWAYS_EXPOSED_LOCALES` if sideload should continue forcing visibility.

---

## 7. Top 20 Android Language Prioritization

Directional ranking based on common global Android usage/native-speaker population, intended as a prioritization reference, not a precise analytics-derived order (this app has no in-app locale analytics yet). "Tier" reflects this plan's recommendation, not overall importance.

| Rank | Language              | Tag             | Tier             | Reason if Post-Tier 1                                                                                                                                                                                                                          |
| ---- | --------------------- | --------------- | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | English               | `en`            | Tier 1 (default) | —                                                                                                                                                                                                                                              |
| 2    | Chinese (Simplified)  | `zh-rCN`        | Tier 1           | —                                                                                                                                                                                                                                              |
| 3    | Hindi                 | `hi`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 4    | Spanish               | `es`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 5    | Arabic                | `ar`            | Post-Tier 1      | RTL — requires `supportsRtl`, start/end layout audit, mirrored icons                                                                                                                                                                           |
| 6    | Portuguese (+ Brazil) | `pt` / `pt-rBR` | Tier 1           | —                                                                                                                                                                                                                                              |
| 7    | Russian               | `ru`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 8    | Japanese              | `ja`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 9    | German                | `de`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 10   | French                | `fr`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 11   | Korean                | `ko`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 12   | Italian               | `it`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 13   | Turkish               | `tr`            | Post-Tier 1      | Latin script but the Turkish dotted/dotless "I" casing problem (`i`/`İ` vs `ı`/`I`) can break naive `String.toUpperCase()`/`equalsIgnoreCase()` locale-sensitive code paths; needs an explicit audit of any case-folding logic before enabling |
| 14   | Vietnamese            | `vi`            | Post-Tier 1      | Latin script but heavy stacked-diacritic density (tone marks + vowel modifiers); higher font-rendering/kerning risk than plain Latin Extended, warrants a dedicated rendering QA pass                                                          |
| 15   | Thai                  | `th`            | Post-Tier 1      | No inter-word spacing plus stacking vowel/tone marks above and below the baseline; line-break and vertical-clipping risk in fixed-height rows needs dedicated layout QA                                                                        |
| 16   | Polish                | `pl`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 17   | Dutch                 | `nl`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 18   | Indonesian            | `id`            | Tier 1           | —                                                                                                                                                                                                                                              |
| 19   | Persian (Farsi)       | `fa`            | Post-Tier 1      | RTL                                                                                                                                                                                                                                            |
| 20   | Hebrew                | `he`            | Post-Tier 1      | RTL                                                                                                                                                                                                                                            |

---

## 8. Layout / Script Considerations Recap

- **RTL (Arabic, Persian, Hebrew, Urdu):** none of the Tier 1 languages require this. Deferred entirely until/unless a Post-Tier 1 RTL language is scheduled. Would require `android:supportsRtl="true"`, converting `left`/`right` layout attributes to `start`/`end`, and a mirrored-icon audit.
- **Font/glyph coverage:** no custom `fontFamily`/typeface overrides exist in [styles.xml](../app/src/main/res/values/styles.xml); system default fonts are used everywhere, which on `minSdk 30` already include full coverage for Latin Extended, Cyrillic, CJK, and Devanagari. No font bundling work needed for Tier 1.
- **Text expansion:** German and Dutch are the two Tier 1 languages most likely to produce unusually long strings/words. Existing `singleLine="true"` usages ([categories.xml](../app/src/main/res/layout/categories.xml), [create_category.xml](../app/src/main/res/layout/create_category.xml)) apply to user-entered content, not translated labels, so risk is low; spinner dropdown layouts already use `ellipsize="marquee"` ([spinner_dropdown_item.xml](../app/src/main/res/layout/spinner_dropdown_item.xml)), which is the safe pattern.
- **CJK/Hindi line-breaking:** Chinese/Japanese/Korean lack inter-word spaces; Devanagari uses shaped conjuncts and above/below-baseline matras. Android's default line breaker and system fonts handle both correctly out of the box; recommended as a visual QA checkpoint rather than an anticipated code change.
- **Locale-selection precedence:** app-selected locale (if explicitly set) overrides device locale; if no app locale is set, device locale is used; if unsupported, fallback is default English resources.
- **Language-switch runtime behavior:** Phase 1 uses explicit restart prompt after language change for reliability ("Restart app to apply language update"). Existing notifications already scheduled stay as-is; newly scheduled notifications use the language active at scheduling/display time.
- **Date/number formatting:** intentionally out of scope for this plan (see Section 4, Phase 5) — `DateFormat.java`'s manual US/UK/M-D-Y preference is unaffected by language selection.

---

## 9. Explicitly Out of Scope / Deferred

1. RTL support (Arabic, Hebrew, Persian, Urdu) and any associated layout mirroring.
2. Traditional Chinese (`zh-rTW`) or other regional Chinese variants beyond Simplified.
3. Locale-aware date/number formatting as a replacement for the manual US/UK/M-D-Y setting.
4. Turkish casing audit, Vietnamese diacritic rendering QA, and Thai line-break/vertical-clipping QA — each called out in Section 7 as a prerequisite before promoting those languages to Tier 1.
5. CSV import/export format strings (headers, column names) — these are data-interchange identifiers, not user-facing UI copy, and must remain stable/unlocalized to avoid breaking round-trip compatibility.
6. Widget localization (feature currently disabled; defer indefinitely pending any future re-enable/redesign).
7. Formalized translation workflow tooling/process automation (labels, templates, structured triage) beyond owner-managed intake.
8. **Strict constructor/API hardening in `DaysSinceCalculations` (Context-only constructors): deferred at present, with no fixed deadline.**
   - Current decision: keep no-context constructor overloads as compatibility shims.
   - Rationale: these overloads support existing tests and low-risk call patterns while localization behavior is already resource-driven in production paths.
   - Implications: API surface remains larger than ideal and allows non-Context fallback usage where not needed.
   - Trigger to revisit: when preparing a dedicated technical-debt hardening pass, or when future work already requires broad call-site edits.
   - Deferability: may be deferred indefinitely as long as shims remain stable, tests pass, and no defects are attributed to legacy overload usage.

---

## 10. Decisions (Current)

1. **Translation source model:** machine translation baseline, then iterative correction via community/volunteer feedback encouraged from active users/testers.
2. **Release sequencing:** two waves. Wave 1 includes `es`, `fr`, `de`, `pt`, `pt-rBR`, `it`, `zh-rCN`, `hi`; Wave 2 follows with the remaining Tier 1 languages.
3. **API 30-32 behavior:** expose the in-app "Language" setting on API 30-32 as well (same capability as API 33+ users), applying locales through `AppCompatDelegate.setApplicationLocales(...)`.
4. **Release posture during closed testing:** ship planned locales and observe real usage/feedback; no minimum feedback-count threshold is required before release. Users can always switch back to English.
5. **Fallback/rollback posture (recommended):** keep translation folders in source, but gate user exposure through the implemented control surface: `app/src/main/java/com/merware/dayssincepro/LocaleExposureConfig.java`, `app/src/main/res/xml/locales_config.xml`, and `app/src/sideload/res/xml/locales_config.xml`. If a locale regresses, remove it from exposure in those files for the next build without deleting translation work. The same mechanism governs pseudolocale exposure.
6. **Ownership and intake:** product owner reviews and incorporates localization feedback directly (email and/or GitHub PR), with process formalization deferred until scale requires it.
7. **No new hardcoded UI strings rule:** no new user-facing string literals are allowed in Java/Kotlin code. All user-facing copy must come from resources.
8. **No locale-branching in business logic rule:** avoid `if (Locale...)` branches inside domain/business classes for phrasing/grammar. Locale-specific wording must live in string/plural resources and be resolved via Android resource APIs.
9. **QA baseline:** include `en-XA` in Wave 1 test pass and `ar-XB` in Wave 2 test pass. Pseudolocales may be visible in sideload/internal language lists and hidden in release lists.
10. **Feedback channels in About dialog:** add the paragraph with `mailto:` link for translation feedback and general support as provided in section11.
11. **Language-change application policy (Phase 1):** apply on app restart for ease/reliability. Dynamic in-place re-render may be evaluated later, but is not required for Phase 1.
12. **Date-format policy scope:** locale-aware date/number formatting is explicitly not required for Phase 1 (Wave 1 and Wave 2 locales). It is required as a precursor policy gate before launching post-Phase-1 locale expansion.
13. **Constructor hardening policy:** strict Context-only constructor enforcement for `DaysSinceCalculations` is explicitly deferred with no fixed date; retain compatibility overloads unless/until a dedicated hardening change is scheduled.
14. **Phase 3A guard policy:** no-hardcoded-UI-string checks are mandatory milestone gates (local and CI-oriented), not optional spot checks.

## 11. Localization Feedback Template (Operational)

Use this template when asking testers/reporters for actionable localization feedback:

1. App version/build number:
2. Language selected in app (or device language if using device default):
3. Screen/feature where issue appears:
4. Expected text (if known) and actual text shown:
5. Is this blocking normal use? (Yes/No):
6. Device model and Android version:
7. Screenshot attached (required when possible):
8. Optional suggested wording:

Contact copy for About dialog:

"Any questions or feedback may be sent to support@merware.com, and any translation assistance, whether a brief email or deeper collaboration on a strings.xml file, is sincerely appreciated."

## 12. Remaining Open Questions

None.
