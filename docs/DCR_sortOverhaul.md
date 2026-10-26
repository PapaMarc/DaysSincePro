# DCR: Event Sort Overhaul

Document ID: DCR-2026-10-24-SORT
Status: Implemented
Author: DaysSincePro Architecture

## 1. Decision Summary

Replace the current five event-sort choices with three sort families:

- **Entry Order**
- **Alphabetical**
- **By Tab Value**

Each family has an ascending and descending direction. The selected sort family is configured in Settings. The active tab displays an up/down direction indicator and can be tapped to reverse the current direction:

- Tapping an inactive tab changes to that tab.
- Tapping the already-selected tab reverses the current sort direction.

The default should be **By Tab Value, ascending**.

The three tabs give the third sort family its meaning:

- **Days Since:** elapsed days since the event's original date.
- **Since Last:** elapsed days since the effective last occurrence or History date.
- **Until Next:** days until the effective next occurrence or planned date.

No implementation is included in this document. This document defines the target behavior and implementation order.

## 2. Motivation

The existing sort choices were designed when the primary event list was effectively a Days Since view:

- Input Order
- Alphabetize
- Reverse Alphabetize
- Oldest First
- Newest First

This is an important historical distinction. The original sort implementation predates the current multi-tab model: Days Since was the primary/namesake view, Since Last did not yet exist, and Until Next was not yet a complete working presentation of the next valid occurrence. Consequently, the original Oldest/Newest choices were implemented around the event's primary stored date, which was a reasonable approximation for the original Days Since-focused product but is not a suitable definition for all current tabs.

Since Last and Until Next now have distinct date semantics. A sort by the event's original stored date is no longer necessarily useful in those tabs. For example:

- A recurring birthday entered decades ago should be ordered in Until Next by its next upcoming birthday, not by the year it was entered.
- Since Last should be ordered by the most recent relevant occurrence or History entry.
- A future one-time event with a planned date should be ordered by the date that Until Next displays.

The revised model keeps the useful name and entry-order choices while making the date/value sort relative to the active tab.

## 3. Current Implementation

### 3.1 Settings representation

The event sort preference is `event_sort_order`. Its current values are defined in `app/src/main/res/values/arrays.xml`:

| Value | Current label       |
| ----: | ------------------- |
|   `0` | Input Order         |
|   `1` | Alphabetize         |
|   `2` | Reverse Alphabetize |
|   `3` | Oldest First        |
|   `4` | Newest First        |

The preference is declared in `app/src/main/res/xml/options.xml`.

Existing preference values should not be assumed to map directly to the new model. A migration or compatibility mapping must be defined before changing the resource values.

### 3.2 Historical context

The current implementation should be evaluated in the context in which it was created:

- Days Since was the original and primary event-list concept.
- Since Last had not yet been introduced.
- Until Next was not yet a properly functioning next-occurrence view.
- The event's stored `date` was therefore a reasonable common sort basis for the available behavior.

The implementation was not designed as a tab-aware sorting system. The overhaul is not intended to characterize the original choices as incorrect for their time; it updates their meaning to match the now-established Since Last and Until Next views.

### 3.3 Normal list ordering

`PastFutureListFragment` builds the event query and appends the result of `getOrderBy()` to the SQL statement.

The current mapping is:

```text
0 -> no explicit order (`ORDER BY NULL`)
1 -> event ASC
2 -> event DESC
3 -> date ASC
4 -> date DESC
```

The same SQL order is used for Days Since, Since Last, and Until Next. The selected tab changes the candidate-event filter and the row presentation, but not the SQL sort key.

### 3.4 Search ordering

Search results use a separate SQL builder, `buildSearchSql(...)`, in `PastFutureListFragment`. Search intentionally does not apply the normal tab date filters, but it still receives the selected SQL order.

Any new sorting implementation must preserve the intended search behavior while ensuring that normal lists and search results use the same sort-family and direction semantics.

### 3.5 Row date/value calculation

`MyEventAdapter` calculates the displayed event dates and explanations. It uses:

- `EventTimeline` for Days Since, Since Last, recurrence, History, and end-date semantics.
- `RecurrenceCycle` for the current and next recurrence occurrences.
- `planned_date` handling for future one-time events.

The displayed values are therefore richer than the raw `event.date` column used by the current Oldest/Newest SQL ordering.

## 4. Strengths of the Existing Implementation

The current implementation has several useful characteristics:

- Alphabetical ordering is simple and understandable.
- The existing SQL query handles normal and filtered event lists efficiently for small and moderate data sets.
- The adapter already centralizes much of the event display calculation.
- Recurrence and History behavior has been centralized in `EventTimeline` and `RecurrenceCycle`.
- Existing preference values make the current behavior easy to persist.
- Search and category filtering already share the event projection and can be brought into a common ordering pipeline.

These strengths should be preserved where possible. The overhaul should not replace recurrence or History calculations with a second simplified implementation.

## 5. Weaknesses and Risks in the Existing Implementation

### 5.1 Input Order is not deterministic

The current option `0` uses no meaningful ordering. `ORDER BY NULL` does not guarantee insertion order. The observed order may currently resemble creation order because of SQLite's row layout, but it is not a durable contract.

Entry order should be made explicit with `_id ASC` for first-entered-first. Descending entry order should use `_id DESC`.

### 5.2 Oldest/Newest sort the wrong date in some tabs

Oldest First and Newest First sort by the stored event `date`. That is a reasonable interpretation for the original Days Since view, but it does not match the relevant date in the other tabs:

- Since Last is concerned with the latest relevant occurrence or History date.
- Until Next is concerned with the next valid occurrence or future planned date.

### 5.3 SQL ordering cannot represent all current date semantics safely

The effective date/value can depend on:

- recurrence intervals;
- calendar-month and calendar-year behavior;
- the current local date;
- latest History;
- off-time History used as a recurrence anchor;
- end dates;
- future one-time planned dates.

Reproducing all of these rules in SQLite would create a second recurrence implementation and would risk divergence from the displayed row.

### 5.4 No deterministic tie-breaking

Rows with equal names or equal date values do not currently have an explicit secondary order. Their relative positions may change between refreshes.

The target implementation should use `_id ASC` as a final tie-breaker, with event name as an appropriate intermediate tie-breaker where useful.

### 5.5 Display and ordering can diverge

Until Next has special handling for a future one-time `planned_date`. A sorter that uses only `EventTimeline.untilNextReferenceDate` without incorporating the adapter's planned-date behavior could order an event by one date while displaying another.

The eventual implementation must create one authoritative effective-value calculation and use it for both sorting and display.

## 6. Target User Experience

### 6.1 Settings choices

The event sort preference should expose:

```text
By Tab Value
Alphabetical
Entry Order
```

These are sort families, not five independent direction choices.

### 6.2 Direction behavior

The initial direction for a family/view combination that has not been used before is ascending:

- Entry Order: first-entered-first.
- Alphabetical: A-Z.
- By Tab Value: lower numeric value first.

Tapping the selected tab reverses the direction:

- Entry Order: first-entered-first <-> last-entered-first.
- Alphabetical: A-Z <-> Z-A.
- By Tab Value: ascending <-> descending.

The selected direction should be visible on the active tab with an up/down indicator.

Direction is remembered independently for each sort-family/view combination. Changing
the global family in Settings does not reset any saved directions. Returning to a
previous family restores the direction last used for the selected view.

### 6.2.1 Recurring day-zero boundary exception

When a recurring event's effective occurrence date is the user's local current date,
the event receives a special priority in the Since Last and Until Next views when the
sort family is By Tab Value. It is placed in the leading same-day group for both
directions, even when ordinary numeric ordering would place a zero value at the
bottom. The event appears once; it is not duplicated at both ends of one list.

The displayed occurrence follows the selected view and direction:

| View/direction        | Displayed occurrence                   |
| --------------------- | -------------------------------------- |
| Since Last ascending  | Current occurrence, displayed as Today |
| Since Last descending | Previous recurrence occurrence         |
| Until Next ascending  | Current occurrence, displayed as Today |
| Until Next descending | Next recurrence occurrence             |

The adjacent occurrence is calculated from the event's existing recurrence interval,
not hard-coded as one year. Weekly, monthly, quarterly, semiannual, annual, and
N-day recurrences therefore use their own cycle. One-time events, including one-time
events dated today, do not receive this treatment.

The day-zero test uses the existing `EventTimeline`/`EventPresentation` recurrence
semantics and compares calendar dates in the user's local date context rather than
raw UTC timestamps. Leap-day behavior remains whatever the existing recurrence
implementation defines; this change does not introduce a new February 29 policy.
The current date is captured once per presentation/sort refresh.

Filtering occurs before ordering, so the same rule applies consistently to normal,
search, category-filtered, and combined search/category results. All qualifying
same-day recurring events are grouped together and then ordered by case-insensitive
event name followed by `_id ASC`. Remaining rows retain the selected ordinary
ascending/descending tab-value ordering.

This is an intentional exception to strict numeric sort/display correspondence:
the event is prioritized because it occurs today, while its displayed date may be
the previous or next cycle occurrence for the selected boundary direction.

The explanation text must use that same direction-aware occurrence calculation:
Today for the current occurrence, or the appropriate cycle distance such as
1 year for the previous/next occurrence. Date text and explanation text must not
be calculated independently.

### 6.3 Tab behavior

- Tapping an inactive tab changes to that tab.
- Tapping the active tab toggles the direction.
- No app-specific double-tap gesture is required.
- The normal user action is a single tap.

The active tab should make the behavior discoverable through a direction arrow and, where practical, an accessibility description or announcement.

Suggested accessibility wording:

```text
Days Since, selected, ascending. Activate to sort descending.
```

“Activate” is intentional. TalkBack may use a double-tap as its platform activation gesture, but the application should not require or describe a special double-tap interaction.

### 6.4 Direction state

The initial target design uses one selected sort family and one current direction. Direction is reset to ascending when the user changes sort family.

Per-tab direction memory may be considered later, but it should not be introduced unless there is a demonstrated need. Global direction keeps the first implementation easier to understand, persist, and test.

### 6.5 As Entered versus Entry Order

The preferred label is **Entry Order**. It communicates that both directions are valid:

- ascending entry order: first-created event at the top;
- descending entry order: last-created event at the top.

The underlying implementation should use the event `_id`, because it is the database's creation-order surrogate.

## 7. Target Sort Semantics

### 7.1 Entry Order

Primary key:

```text
event._id
```

Ascending:

```sql
_id ASC
```

Descending:

```sql
_id DESC
```

This mode does not require recurrence calculation.

### 7.2 Alphabetical

Primary key:

```text
event name
```

Ascending is A-Z; descending is Z-A.

The implementation should decide explicitly whether comparison is case-insensitive. A likely target is case-insensitive user-facing alphabetical sorting, with `_id ASC` as the final tie-breaker.

### 7.3 By Tab Value: Days Since

Use the event's original Days Since reference date. For recurring events, this remains the inception/start date, consistent with the existing Days Since semantics.

Ascending:

```text
fewest elapsed days first
```

Descending:

```text
most elapsed days first
```

### 7.4 By Tab Value: Since Last

Use the effective Since Last reference date, which may be:

- the latest relevant History date;
- the latest recurrence occurrence;
- the end date for an ended event;
- another date selected by the existing timeline rules.

Ascending:

```text
most recently happened first
```

Descending:

```text
longest ago first
```

The sort must use the same History and recurrence semantics used to render Since Last.

### 7.5 By Tab Value: Until Next

Use the effective Until Next date/value, which may be:

- the next recurrence occurrence;
- the original date for a future one-time event;
- a future planned date;
- no valid future occurrence when an event has ended.

Ascending:

```text
soonest upcoming event first
```

Descending:

```text
farthest upcoming event first
```

The planned-date path must be included so that sorting matches the date displayed by Until Next.

## 8. Design Options

### Option A: SQL-only ordering

Keep all sorting in the SQL `ORDER BY` clause.

Appropriate for:

- Entry Order;
- Alphabetical;
- simple original-date ordering.

Not appropriate as the general solution for By Tab Value because it would either duplicate recurrence logic in SQL or sort by incomplete proxies.

**Decision:** do not use as the primary design.

### Option B: Java-calculated keys with a reordered cursor

1. Query the same candidate rows currently used by the tab.
2. Convert rows into typed event records.
3. Calculate the effective presentation values and sort keys.
4. Sort the records in Java.
5. Provide the ordered rows through a cursor compatible with the existing adapter, such as a carefully constructed `MatrixCursor`.

Advantages:

- Reuses the existing list and row layout.
- Keeps all complex date logic in Java.
- Makes normal, category-filtered, and search results use one ordering path.
- Avoids generating a second SQL ordering expression.

Risks:

- Requires copying every column expected by `MyEventAdapter`.
- Adds cursor lifecycle and projection-maintenance concerns.
- Retains a cursor-oriented adapter while the ordering becomes model-oriented.

**Decision:** preferred transitional integration if the current adapter is retained.

### Option C: Java-calculated ordered IDs followed by a second SQL query

1. Query candidate rows without dynamic ordering.
2. Calculate and sort event IDs in Java.
3. Query the same projection again using an explicit ID order.

Advantages:

- The existing adapter receives a normal SQLite cursor.
- Avoids copying rows into a `MatrixCursor`.

Risks:

- Requires two queries and duplicated filter/projection coordination.
- Requires generated ordering SQL.
- Makes search/category parity more fragile.
- Adds complexity without improving the underlying sort-key design.

**Decision:** viable fallback, but not preferred over a single calculated ordering pipeline.

### Option D: Move to an in-memory event model and adapter

Replace the `SimpleCursorAdapter` surface with a typed event list and custom adapter.

Advantages:

- Cleanest long-term model.
- Sorting and rendering naturally share calculated values.
- Avoids `MatrixCursor`.
- Makes future sort modes easier to add.

Risks:

- Larger UI refactor.
- More surface area for accidental behavior changes.
- Requires additional testing for row rendering, IDs, context actions, and refresh behavior.

**Decision:** desirable long-term direction, but not required for the first sort overhaul.

## 9. Recommended Technical Direction

Use a shared event-presentation calculation as the source of truth for both display and ordering.

Conceptually, each candidate row should produce a presentation record containing:

- event ID;
- event name;
- original event date;
- effective Days Since date/value;
- effective Since Last date/value;
- effective Until Next date/value;
- data needed for row rendering;
- any future/end-date state required by the display.

The exact class and fields should follow existing project patterns. The key requirement is that the adapter and sorter do not independently reconstruct the same recurrence rules.

The first implementation should retain the current list surface where practical:

1. Query candidate rows without relying on Oldest/Newest SQL ordering.
2. Calculate one presentation record per row.
3. Calculate the selected mode's primary sort value.
4. Sort in Java.
5. Apply stable tie-breakers.
6. Provide the ordered result to the existing adapter.

Entry Order and Alphabetical can remain SQL-optimized if doing so does not create separate behavior, but a single pipeline for all modes is preferable for consistency and testability.

## 10. State and Compatibility

New installations default to By Tab Value, ascending. Existing installations receive a one-time migration to that same default when they still contain legacy or invalid event-sort state. This intentionally does not preserve the old sort choice; it gives all users the new recommended behavior while ensuring their later choices are not reset.

The current five values do not map one-to-one to the three target families:

| Existing value | Existing meaning    | One-time migration result |
| -------------: | ------------------- | ------------------------- |
|            `0` | Input Order         | By Tab Value, ascending   |
|            `1` | Alphabetize         | By Tab Value, ascending   |
|            `2` | Reverse Alphabetize | By Tab Value, ascending   |
|            `3` | Oldest First        | By Tab Value, ascending   |
|            `4` | Newest First        | By Tab Value, ascending   |

The migration writes the symbolic `tab_value` family and initializes the family/view direction matrix, then records migration version 2. It runs only while that version marker is absent. The prior global direction initializes all By Tab Value views; all other family/view combinations start ascending. Already-modern symbolic values are preserved and only receive the marker, so users who have already selected a new choice are not reset. The existing compatibility parser remains as defensive handling for any legacy value encountered outside the normal migration path.

The model persists:

```text
sort family
direction for each sort family and view
```

The direction keys are namespaced by family and view so process death, activity recreation, tab switching, and family switching all restore the same state.

## 11. Tie-Breaking and Edge Cases

Every mode should have deterministic ordering:

1. selected primary sort key;
2. event name ascending, where appropriate;
3. `_id ASC`.

The implementation must explicitly test and define:

- two events with the same event name;
- two events with equal tab values;
- an event due today;
- a future one-time event;
- a future one-time event with a planned date;
- an ended one-time event;
- an ended recurring event;
- recurring events with latest History;
- recurring events with off-time History;
- monthly and annual recurrence;
- category filtering;
- search results;
- empty result sets.

## 12. Rollout Plan

### Phase 1: Characterize current behavior

Add focused tests and diagnostics before changing the settings:

- current preference-value mapping;
- current normal-list and search-list projections;
- current event-name ordering;
- current insertion-order assumptions;
- existing `EventTimeline` and recurrence behavior.

### Phase 2: Centralize effective presentation values

Extract or extend a shared calculation so that row rendering and sorting use the same effective dates/values, including planned-date handling.

Do not alter visible behavior beyond correcting duplicated or inconsistent calculations required for the shared source of truth.

### Phase 3: Make simple modes deterministic

Implement:

- Entry Order with `_id ASC`/`_id DESC`;
- Alphabetical with explicit case behavior and tie-breakers.

Keep preference compatibility and add tests for normal and search paths.

### Phase 4: Implement By Tab Value

Add Java-calculated sort keys for:

- Days Since;
- Since Last;
- Until Next.

Verify that sort values match the displayed row values for recurring, historical, planned, and ended events.

### Phase 5: Add direction interaction

Add active-tab indicators and click behavior:

- inactive tab click changes tabs;
- active tab click reverses direction.

Add accessibility descriptions/announcements using “activate” wording rather than describing an app-specific double-tap.

### Phase 6: Migrate settings and compatibility

Replace the five current choices with the three sort families, migrate existing preference values, and update localized strings/resources.

Document any intentional change in the meaning of Oldest/Newest for existing users.

### Phase 7: Validate and consider follow-up refactoring

Run focused unit tests, build validation, and manual checks across all tabs and settings combinations.

Evaluate whether the list should later move from `SimpleCursorAdapter` to a typed in-memory adapter. That should be a separate change unless the transitional cursor approach proves unsafe or overly complex.

## 13. Verification Requirements

Before implementation is considered complete, verify:

- Entry Order ascending and descending are deterministic.
- Alphabetical ascending and descending have expected case behavior.
- By Tab Value uses the active tab's semantics.
- Since Last orders by effective history/occurrence age.
- Until Next orders by effective next occurrence/planned date.
- Days Since preserves inception-date semantics.
- Search and category-filtered results use the same mode and direction.
- Changing tabs does not apply the wrong tab's sort key.
- Tapping an inactive tab does not unexpectedly reverse order.
- Tapping the active tab reverses order exactly once.
- The arrow always reflects the current direction.
- Accessibility services receive the current selected state and direction.
- Existing event rendering, context actions, and refresh behavior remain intact.

## 14. Finalized Design Decisions

The following recommendations are accepted for the initial implementation:

1. The sort family selected in Settings is global across tabs.
2. Direction is persisted independently for each sort-family/view combination.
3. Changing the sort family does not reset previously saved directions.
4. Alphabetical comparison is case-insensitive.
5. The settings choices are **By Tab Value**, **Alphabetical**, and **Entry Order**.
6. The active-tab arrow represents the current direction and is the primary visible indication that the active tab is sortable.
7. Accessibility wording may use mode-specific explanations such as “A-Z,” “first entered first,” or “soonest first.”
8. Existing legacy preference values are reset once to By Tab Value, ascending; modern symbolic values are preserved.
9. `_id` is the permanent Entry Order key.
10. The initial implementation uses a Java-calculated presentation record and a reordered `MatrixCursor`, or an equivalent ordered-row bridge if implementation constraints require it.
11. Accessibility state is exposed through a stateful tab content description. A concise accessibility announcement after a direction toggle may be added if testing shows it improves discoverability without causing duplicate announcements.
12. Accessibility wording uses “Activate” rather than describing an app-specific double-tap gesture.
13. A later migration to a typed in-memory adapter remains a separate follow-up decision.

## 15. Implementation Checklist

The following are implementation details rather than product-direction questions:

1. Implement the one-time preference migration from section 10 while retaining the legacy parser as a defensive fallback.
2. **Complete in Milestone 4 for settings labels:** Add and translate the three settings labels across all supported event-sort array resources. Direction and accessibility strings remain part of Milestone 6.
3. **Complete in Milestone 3:** Implement the reordered `MatrixCursor` bridge, centralizing the event projection so all columns required by `MyEventAdapter` are preserved.
4. **Complete in Milestone 6:** Use stateful tab content descriptions with localized direction text. No additional explicit announcement is used by default to avoid duplicate accessibility output.
5. **Complete in Milestone 10:** Apply the recurring day-zero boundary exception through the shared presentation and Java ordering pipeline.
6. **Complete in Milestone 11:** Use the direction-aware boundary occurrence for both date text and explanation text.

## 16. Milestone Tracking

This section is updated as implementation progresses. A milestone should be marked **Complete** only after its implementation and verification work has been finished. Documentation updates that record completion are part of the milestone close-out.

### Milestone 0: Creating the sort-overhaul DCR

**Status: Complete**

Created this DCR, documented the historical Days Since-focused origin of the existing sorting behavior, evaluated implementation options, and recorded the target product and technical decisions.

### Milestone 1: Adding characterization and sort-model tests

**Status: Complete — 2026-10-24 13:04 EDT**

Added the UI-independent `EventSortSpec` model for the three agreed sort families and family/view direction state. Tests cover the By Tab Value ascending default, direction reversal, preservation of direction when changing family, and invalid null state rejection. Existing `EventTimelineTest` and `RecurrenceCycleTest` coverage remains part of the characterization baseline for recurrence, History, end-date, and current/next occurrence semantics.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest --tests com.merware.dayssincepro.EventSortSpecTest --tests com.merware.dayssincepro.EventTimelineTest --tests com.merware.dayssincepro.RecurrenceCycleTest
```

### Milestone 2: Centralizing presentation and sort-key calculation

**Status: Complete — 2026-10-24 13:12 EDT**

Added `EventPresentation` as the shared event-date calculation used by `MyEventAdapter` and future sort-key generation. It centralizes recurrence, History, off-time History, end-date, planned one-time date, and effective Until Next semantics without changing the existing list-ordering behavior.

Added focused tests for recurring presentation references, future planned one-time events, and off-time History recurrence anchors. The adapter now consumes the shared presentation values while retaining its existing display and explanation paths.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest
```

### Milestone 3: Implementing the Java ordering bridge

**Status: Complete — 2026-10-24 13:22 EDT**

Added `EventRowSorter`, which calculates effective event presentations in Java, applies Entry Order, Alphabetical, or By Tab Value ordering, applies the accepted ascending/descending direction, and preserves deterministic tie-breaking. Normal and search list paths now use the reordered `MatrixCursor` while retaining every column required by `MyEventAdapter`.

The existing legacy preference values are temporarily mapped through `EventSortSpec.fromLegacyPreference(...)` so the bridge is active before the settings-resource migration in Milestone 4. The parser remains as a defensive fallback; the one-time upgrade migration now resets legacy values to the new By Tab Value ascending default.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest
```

### Milestone 4: Migrating the preference model

**Status: Complete — 2026-10-24 13:27 EDT**

Replaced the five legacy event-sort choices with By Tab Value, Alphabetical, and Entry Order. Updated the shared event-sort values to three entries while retaining the defensive legacy mapping in `EventSortSpec.fromLegacyPreference(...)`. Updated all supported localized `entries` resources, including German, Spanish, French, Hindi, Italian, Portuguese, Brazilian Portuguese, and Simplified Chinese. By Tab Value ascending remains the new default.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

### Milestone 5: Adding active-tab direction interaction

**Status: Complete — 2026-10-24 13:41 EDT**

Implemented inactive-tab selection through the existing TabLayout/ViewPager behavior and active-tab direction reversal through `onTabReselected`. Direction was initially global and persisted separately from the selected sort family; the later family/view direction matrix is recorded in Milestone 9. The selected tab displays an up or down arrow; accessibility descriptions and announcements remain in Milestone 6.

The new symbolic preference values distinguish the three settings choices from legacy numeric values, while `EventSortSpec` continues to map legacy values during compatibility migration.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

### Milestone 6: Completing localization and accessibility

**Status: Complete — 2026-10-24 13:53 EDT**

Added localized ascending/descending tab content descriptions across all supported language resources. The selected tab now exposes its current direction and the action that will occur when it is activated. The visible arrow remains the sighted-user indicator. No separate accessibility announcement is added by default because the updated content description should be read by TalkBack without duplicate output.

The `en-XA` and `ar-XB` pseudo-locales inherit the updated default resources and are generated by the sideload build's `pseudoLocalesEnabled` configuration. The localized resource parity check now includes both new accessibility keys.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleSideload
```

### Milestone 7: Final validation and DCR close-out

**Status: Complete — 2026-10-24 14:05 EDT**

Automated validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug :app:assembleSideload :app:bundleRelease
git diff --check
```

The repository-wide `:app:check` command continues to report pre-existing lint findings unrelated to this overhaul. The first reported error is `CreateCategoryActivity.java:91` (`MissingSuperCall`); the report also includes existing findings in `MainActivity.java`, including `RestrictedApi` and `GestureBackNavigation`. The lint baseline is explicitly accepted for this DCR, and no unrelated lint fixes were made. The generated report is at `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`.

Manual device validation was not available in this environment. Active-tab reversal, direction arrows, TalkBack content descriptions, search/category ordering, and locale coverage including `en-XA` and `ar-XB` remain follow-up checks and do not block implementation status.

### Milestone 8: Applying the one-time preference migration and display-order update

**Status: Complete — 2026-10-24 14:28 EDT**

Updated the Settings order to By Tab Value, Alphabetical, and Entry Order across all supported localized resources. Added a versioned migration that resets legacy or invalid event-sort values to By Tab Value ascending exactly once, preserves already-modern symbolic values, and prevents future user choices from being overwritten. Added focused migration tests.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest --tests com.merware.dayssincepro.EventSortSpecTest :app:assembleDebug
git diff --check
```

### Milestone 9: Persisting direction per sort family and view

**Status: Complete — 2026-10-24 14:52 EDT**

Expanded direction persistence into a family/view matrix while keeping the Settings sort-family selection global. Each active tab now reverses only its current family/view direction, and switching families or tabs restores the previously saved direction for that combination. Existing global direction state is migrated once into all By Tab Value views; other combinations start ascending.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest --tests com.merware.dayssincepro.EventSortSpecTest :app:assembleDebug
git diff --check
```

### Milestone 10: Prioritizing recurring day-zero boundary events

**Status: Complete — 2026-10-26 18:32 EDT**

Added the shared recurring day-zero boundary exception. Recurring events whose
effective occurrence is today are grouped first in By Tab Value sorting for Since
Last and Until Next, regardless of ascending or descending direction. The adapter
displays today, the previous recurrence, or the next recurrence according to the
active view and direction. The implementation preserves existing recurrence,
History, planned-date, local-calendar, leap-day, filtering, and deterministic
tie-breaking semantics, and excludes one-time events.

Added focused coverage for annual boundary dates, adjacent occurrence display,
one-time-event exclusion, and day-zero priority.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest --tests com.merware.dayssincepro.EventPresentationTest --tests com.merware.dayssincepro.EventSortSpecTest :app:assembleDebug
git diff --check
```

### Milestone 11: Aligning day-zero explanation text with boundary dates

**Status: Complete — 2026-10-26 19:06 EDT**

Updated the adapter to calculate explanation text from the same direction-aware
boundary occurrence used for the displayed date. Day-zero recurring events now
show Today when the current occurrence is displayed and the appropriate cycle
distance, such as 1 year, when the previous or next occurrence is displayed.
Ordinary events, one-time events, reminder urgency, and non-By-Tab-Value behavior
remain unchanged.

Added focused assertions for the four Since Last/Until Next direction combinations.

Validation completed successfully:

```text
.\gradlew.bat :app:testDebugUnitTest --tests com.merware.dayssincepro.EventPresentationTest --tests com.merware.dayssincepro.EventSortSpecTest :app:assembleDebug
git diff --check
```
