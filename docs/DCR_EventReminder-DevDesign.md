# Dev Design: Event Reminder Rework Implementation

**Design ID:** DCR-2026-09-13-EventReminder-DevDesign
**Related DCR:** [DCR_EventReminderRework.md](DCR_EventReminderRework.md)
**Status:** Completed
**Author:** DaysSincePro Engineering
**Target release:** Implemented (PR1-PR4 complete)

**Implementation completed on:** 2026-09-13

Delivery status note:

1. PR 1 through PR 4 scopes in section 7 were implemented and validated.
2. The localization completeness gate is active and hard-fail in build/CI.
3. No open engineering design decisions remain.

---

## 1. Purpose

This document defines the implementation design for the reminder rework requirements in [DCR_EventReminderRework.md](DCR_EventReminderRework.md).

It focuses on:

1. Concrete code architecture.
2. Schema and migration execution details.
3. Phased pull request (PR) delivery plan.
4. Test strategy and release gates.

It does not redefine product requirements; the related DCR remains the source of truth for behavior.

---

## 2. Decision Status

### 2.1 Accepted and locked

These are treated as implementation invariants.

1. Clamping rule exactness:

- For Number of Days recurrence, effective lead days are clamped to interval days.
- Named recurrence types are not clamped by interval length.

2. Shared source of truth:

- One resolver computes effective lead days and is used by both notification and list-color urgency logic.

3. One-time event consumption semantics:

- A one-time notification is consumed after first successful fire.
- It is re-armed only when the user materially changes date or reminder configuration.

4. Migration/backfill behavior:

- `notify_enabled` is `INTEGER NOT NULL DEFAULT 1`.
- `notify_lead_days` is `INTEGER NULL`.
- No special per-row backfill beyond additive defaults/null semantics.

5. Time boundary and timezone policy:

- Reminder day-state and due/near-due evaluation use current device local date and timezone at evaluation time (floating local-day behavior).
- Timezone or manual clock/timezone setting changes must trigger reminder recalculation/rescheduling.
- Notification scheduling time remains local-device-time anchored (for example 08:00 local time), shifting with timezone changes.

6. Localization, CSV contract, and PR test ownership are mandatory release gates:

- Localization contract is fixed (required locales and required keys).
- CSV warning count semantics and placeholder ordering are fixed.
- Test ownership is fixed by PR as defined in sections 7 and 8.
- CI rollout policy is fixed: localization gate runs on every PR; warning-only for PR 1-2; hard-fail required from PR 3 onward.

7. Explicitly skipped optional rollout controls:

- No temporary debug toggle/log tag is required for reminder-resolution tracing in this DCR.
- No internal PR 2 kill-switch is required for reminder logic rollout in this DCR.

---

## 3. Data Model Design

### 3.1 Columns

Add to `event` table:

1. `notify_lead_days INTEGER NULL`
2. `notify_enabled INTEGER NOT NULL DEFAULT 1`

### 3.2 Semantics

1. `notify_lead_days = NULL` means inherited recurrence default.
2. `notify_lead_days != NULL` means explicit per-event custom lead days.
3. `notify_enabled = 0` suppresses notifications for that event only.
4. `notify_enabled` must not alter color urgency rendering.
5. Disabling notifications must not mutate or clear custom lead days.

### 3.3 Migration SQL

```sql
ALTER TABLE event ADD COLUMN notify_lead_days INTEGER;
ALTER TABLE event ADD COLUMN notify_enabled INTEGER NOT NULL DEFAULT 1;
```

### 3.4 Upgrade guarantees

1. Existing rows preserve behavior through inherited defaults (`notify_lead_days` remains null).
2. Existing rows become notification-enabled by default (`notify_enabled = 1`).
3. Migration chain must remain additive and non-destructive.

---

## 4. Runtime Logic Architecture

### 4.1 New shared utility surface

Create a small shared reminder domain utility (single source of truth), for example:

1. `ReminderDefaults`

- Returns recurrence default lead days.
- Includes Number of Days interval-based default mapping.

2. `ReminderLeadDaysResolver`

- Inputs: recurrence type/interval, event override, event notify enabled state.
- Output: effective lead days and source state (Default or Custom).
- Applies Number of Days clamping.

3. `ReminderUrgencyEvaluator`

- Inputs: event date context, recurrence context, effective lead days.
- Output: urgency state (normal, near due, due, overdue) consistent with existing visual model.

4. `ReminderEligibilityGate`

- Inputs: global notifications enabled, event notify enabled.
- Output: eligible/ineligible for notification emission.

### 4.2 Precedence flow

1. If global notifications are disabled, do not notify.
2. Else if event notifications are disabled, do not notify that event.
3. Else compute effective lead days via shared resolver.
4. Evaluate urgency using shared evaluator.
5. Emit notification only for eligible urgency states.

### 4.3 One-time event semantics

1. Near due: inside lead-days pre-event window.
2. Due: event day.
3. Overdue: after event day.
4. After successful one-time fire, mark consumed state to prevent repeat firing.
5. Re-arm only on material user edits (date or reminder configuration changes).

---

## 5. Code Touchpoint Plan

### 5.1 Persistence and migration

1. [app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java](../app/src/main/java/com/merware/dayssincepro/DatabaseHelper.java)

- Schema version bump.
- Add migration statements for new columns.
- Extend migration tests.

2. Event read/write paths (existing DAO/query call sites)

- Include both new columns in projection and persistence.
- Preserve null semantics for inherited defaults.

### 5.2 Notification path

1. [app/src/main/java/com/merware/dayssincepro/OnAlarmReceive.java](../app/src/main/java/com/merware/dayssincepro/OnAlarmReceive.java)

- Replace any local threshold logic with shared resolver/evaluator.
- Apply precedence gate before urgency check.
- Add one-time consumed/re-arm handling integration.

### 5.3 List urgency path

1. [app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java](../app/src/main/java/com/merware/dayssincepro/MyEventAdapter.java)

- Use same shared resolver/evaluator for color urgency.
- Keep color logic independent of `notify_enabled`.

### 5.4 Add/Edit UI path

1. [app/src/main/java/com/merware/dayssincepro/EditEventActivity.java](../app/src/main/java/com/merware/dayssincepro/EditEventActivity.java)

- Add notify-enabled toggle wiring.
- Add default/custom lead-days source-state behavior.
- Ensure recurrence-change behavior rules (inheritance recalculates, custom preserved unless explicit confirmation).
- Ensure field-open/focus does not commit values.

### 5.5 Settings path

1. [app/src/main/res/xml/options.xml](../app/src/main/res/xml/options.xml)
2. [app/src/main/res/values/arrays.xml](../app/src/main/res/values/arrays.xml)
3. [app/src/main/java/com/merware/dayssincepro/PrefActivity.java](../app/src/main/java/com/merware/dayssincepro/PrefActivity.java)

- Remove percent-based reminder preference and wiring.

### 5.6 Export warning path

1. [app/src/main/java/com/merware/dayssincepro/MainActivity.java](../app/src/main/java/com/merware/dayssincepro/MainActivity.java)

- Extend full CSV warning trigger/count query:
  - history entries
  - events with history
  - events with custom notify days
  - events with notifications disabled
- Compute counts once per request and reuse for message assembly.
- Keep existing user options unchanged.

### 5.7 Localization resources

1. [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml)
2. all supported `values-*` `strings.xml`

- Add/adjust required reminder-warning strings.
- Add/adjust source-state and notify-state labels.
- Ensure localization completeness gate can fail build when keys are missing.

Localization contract (fixed):

1. Required locale folders:

- `values`
- `values-de`
- `values-es`
- `values-fr`
- `values-hi`
- `values-it`
- `values-pt`
- `values-pt-rBR`
- `values-zh-rCN`

2. Required reminder warning keys:

- `csv_history_warning_title`
- `csv_history_warning_message`
- `csv_history_warning_continue`
- `csv_history_warning_export_db`

3. Required reminder state keys:

- `lead_days_source_default_from_recurrence`
- `lead_days_source_custom`
- `event_notifications_enabled`
- `event_notifications_disabled`

4. Placeholder contract for `csv_history_warning_message` is fixed:

- `%1$d` = history entries
- `%2$d` = events with history
- `%3$d` = events with custom notify days
- `%4$d` = events with notifications disabled

---

## 6. Validation and Guardrails

### 6.1 Input validation rules

1. Custom lead-days accepted range: 0 through 30 inclusive.
2. Number of Days effective lead-days clamped to interval.
3. Clearing custom value writes null, never default literal.

### 6.2 Invariant checks

1. Resolver parity invariant:

- Notification and list-color must always use the same effective lead-days result.

2. Persistence invariant:

- Disabling notifications never modifies stored custom lead-days value.

3. Migration invariant:

- Upgrade never drops user rows and defaults all existing rows to `notify_enabled = 1`.

4. Timezone invariant:

- Reminder evaluation uses current device local date/timezone consistently across notification and list-color paths.

5. CSV warning invariant:

- Warning always renders all four count lines, including zero values, in fixed placeholder order.

---

## 7. PR Delivery Plan

### PR 1: Data layer

Scope:

1. Schema bump and additive migration.
2. Persistence read/write support for new columns.
3. Migration and null/default semantics tests.

Exit criteria:

1. DB upgrade passes with existing data preserved.
2. Existing rows verified as `notify_enabled = 1`.
3. No behavior change yet in reminder timing.
4. Localization gate runs in CI (warning-only mode).

### PR 2: Core reminder logic

Scope:

1. Shared defaults/resolver/evaluator/gate utilities.
2. Notification path uses shared logic.
3. List urgency path uses shared logic.

Exit criteria:

1. Notification-color parity tests pass.
2. Number of Days clamp tests pass.
3. One-time consume/re-arm tests pass.
4. Localization gate runs in CI (warning-only mode).

### PR 3: UI and settings

Scope:

1. Edit-event notify toggle and default/custom lead-days UX.
2. Recurrence-change behavior and commit-boundary safeguards.
3. Remove percent reminder setting and obsolete bindings.

Exit criteria:

1. Edit/create persistence roundtrip tests pass.
2. Cancel/open/focus no-commit tests pass.
3. No runtime reads of removed percent settings.
4. Localization gate is hard-fail in CI for this and all subsequent PRs.

### PR 4: Export warning and localization gate

Scope:

1. Extend CSV full-export warning counts and copy.
2. Add all required locale strings.
3. Add/enable build-fail localization completeness gate.

Exit criteria:

1. Warning appears for each trigger condition and combinations.
2. Warning always shows all four count lines including zero.
3. Build fails if required localization keys are missing in any supported locale.
4. Localization gate remains hard-fail in CI.

---

## 8. Test Matrix

### 8.1 Unit tests

1. Resolver uses override when present.
2. Resolver falls back to recurrence default when null.
3. Number of Days clamping is correct.
4. Named recurrence types are not interval-clamped.
5. One-time near-due/due/overdue transitions are correct.
6. One-time consumed/re-arm semantics are correct.
7. Eligibility gate precedence is correct.

### 8.2 Migration tests

1. vN -> vN+1 migration adds columns.
2. Existing rows remain intact.
3. Existing rows receive `notify_enabled = 1`.
4. `notify_lead_days` remains null for existing rows.

### 8.3 Integration behavior tests

1. Add event with inherited defaults.
2. Add/edit custom lead days and persist.
3. Clear custom lead days back to inherited null state.
4. Disable and re-enable event notifications without losing custom lead value.
5. Recurrence change preserves custom lead unless explicitly changed.
6. Recurrence change recalculates inherited default automatically.
7. CSV warning triggers for each individual count dimension.
8. CSV warning triggers for combined dimensions.
9. CSV warning does not trigger when all counts are zero.
10. CSV warning renders all four count lines including zero values.

### 8.4 Localization gate tests

1. Positive: all required keys present -> build passes.
2. Negative: remove one required locale key -> build fails.
3. CI policy check: gate executes in every PR, with warning-only behavior validated for PR 1-2 and hard-fail behavior validated from PR 3 onward.

### 8.5 PR test ownership (fixed)

1. PR 1 owns:

- migration column add/default/null tests
- persistence null/custom lead-days roundtrip tests

2. PR 2 owns:

- resolver/evaluator/gate unit tests
- one-time consume/re-arm tests
- notification-color parity tests

3. PR 3 owns:

- edit/create reminder state transition tests
- recurrence-change inherited/custom behavior tests
- no-commit-on-open/focus/cancel tests
- percent-setting removal runtime tests

4. PR 4 owns:

- CSV warning count definition tests
- CSV warning fixed-line/fixed-placeholder rendering tests
- localization completeness pass/fail gate tests

---

## 9. Observability and Debug Notes

1. Add concise debug logging around reminder-resolution decisions where existing logging conventions allow.
2. Keep logs free of noisy per-row spam in production paths.
3. Ensure troubleshooting can answer:

- effective lead days source
- clamped/unclamped result
- notification eligibility reason

---

## 10. Rollout Risk and Rollback

### 10.1 Risks

1. Divergence between notification and color logic if any callsite bypasses shared resolver.
2. UX confusion if source state labels are not consistently shown.
3. Localization misses causing runtime mixed-language strings if not build-gated.

### 10.2 Mitigations

1. Shared resolver mandatory in both notification and adapter paths.
2. Explicit unit/integration parity tests.
3. Build-fail localization completeness gate.

### 10.3 Rollback strategy

1. If runtime logic regressions occur after PR 2 or PR 3, revert latest behavior PR while keeping additive schema intact.
2. Schema is additive and backward-tolerant for app logic that ignores new columns.

---

## 11. Definition of Done

1. All acceptance criteria in [DCR_EventReminderRework.md](DCR_EventReminderRework.md) map to passing tests.
2. No code path reads/writes obsolete percent reminder settings.
3. Reminder parity is guaranteed between notification and color urgency logic.
4. Migration safety validated on representative pre-upgrade data.
5. Localization completeness gate is enforced in CI/build.

---

## 12. Open Items

No open design decisions remain from items 1-6; accepted recommendations are locked in this document.

Operational note: optional rollout controls previously discussed as item 1 (temporary debug toggle/log tag) and item 2 (internal PR 2 kill-switch) were intentionally not adopted.
