# Design Change Request (DCR): Widget Roadmap and Stabilization

**Document ID:** DCR-2026-09-06-Widget-A  
**Target Component:** Home-screen widget (`DaysSinceAppWidgetProvider`) and widget configuration flow  
**Status:** Active (Containment Applied)  
**Author:** DaysSincePro Maintenance

---

## 1. Context

The app includes a legacy home-screen widget pipeline:

- provider: `DaysSinceAppWidgetProvider`
- config host: `ConfigWidgetActivity`
- widget metadata: `res/xml/widget1_info.xml`
- widget layout: `res/layout/widget1.xml`

During Mini-B validation, emulator-level instability was observed around launcher/widget behavior (black screens, home/recents controls not behaving predictably), especially after widget interaction.

Current log review did not show a clear app-process crash (`FATAL EXCEPTION`) in `com.merware.dayssincepro`, suggesting host-side instability and/or aggressive widget update behavior as a likely contributor.

---

## 2. Current Intent (As Implemented)

The widget appears intended to:

1. Display a selected event on the launcher with date + relative-time text.
2. Allow widget-specific settings:
   - event selection
   - color
   - opaque/transparent background
   - display style (years/months/days, days only, etc.)
3. Open the widget config screen when the widget text is tapped.

---

## 3. Observed Risk and Technical Findings

1. Widget update frequency is configured to `1000` ms (`updatePeriodMillis`), which is unusually aggressive for launcher widgets.
2. Provider update path performs repeated DB and formatting work during updates.
3. Legacy widget rendering and preference paths are tightly coupled and difficult to reason about.
4. Behavior appears fragile in emulator launcher-host conditions.

---

## 4. Short-Term Policy

To protect reliability and unblock current UIModernization verification, widget UX exposure is intentionally removed for now.

1. Widget installation and configuration paths are hard-disabled in manifest:
   - `DaysSinceAppWidgetProvider` disabled (`android:enabled="false"`)
   - `ConfigWidgetActivity` disabled (`android:enabled="false"`)
2. Widget remains in codebase for decision follow-up, but is not on the active product code path.
3. Widget behavior is out-of-scope for Mini-B sign-off unless explicitly re-enabled under a dedicated widget phase.

Immediate containment outcome:

1. Widget should no longer appear as installable in launcher widget picker.
2. Widget should not perform periodic provider updates in normal operation.
3. Existing code is retained to support either controlled rewrite or removal.

---

## 5. Product Decision Track

Two viable paths are recognized.

### Option A: Deprecate and Remove Widget

Use when widget value is low and maintenance cost/risk is high.

- keep provider/config disabled while planning removal
- remove widget config UI and resources in a cleanup phase
- document removal in release notes

### Option B: Rewrite Widget (Preferred if Widget Value Is Confirmed)

Use when widget value is meaningful to users.

- redesign requirements first (what should be shown, when it updates, what interactions are needed)
- rebuild with an event-driven update model
- avoid high-frequency periodic updates
- isolate formatting/data reads behind testable helper methods

---

## 6. Recommendation

Default recommendation: **keep hard-disabled now, then decide remove vs rewrite before any re-enable**.

Reasoning:

1. Existing implementation mixes legacy patterns and high-frequency update behavior.
2. Incremental patching likely preserves structural fragility.
3. A focused rewrite can be smaller, clearer, and safer if widget value justifies it.
4. Hard-disable contains user-facing risk immediately without deleting potentially useful historical code.

---

## 7. Open Questions

1. Is the widget still a desired user-facing feature in the maintained product vision?
2. What is the minimum useful widget behavior (single event, summary, actionable tap target)?
3. What update cadence is acceptable (event-driven only vs periodic at larger intervals)?
4. Should widget theming align with main app themes or use an intentionally separate style?

---

## 8. Proposed Follow-On Work Items

1. Add a dedicated widget decision issue: keep/remove/rewrite.
2. If keeping, create a widget requirements brief before implementation.
3. If rewriting, implement behind a scoped phase with explicit acceptance criteria:
   - no launcher-host instability during smoke test
   - deterministic update behavior
   - low battery/CPU impact baseline
   - light/dark visual sanity pass
4. If removing, execute a clean deprecation/removal checklist in one scoped PR.

---

## 9. Exit Criteria for This DCR

This DCR is complete when:

1. A product decision is made (remove or rewrite).
2. The chosen path is scheduled into a phase plan.
3. Widget behavior no longer blocks or confounds non-widget UI modernization verification.
