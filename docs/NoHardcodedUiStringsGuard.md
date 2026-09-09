# No Hardcoded UI Strings Guard

Purpose: enforce the Phase 1 policy that new user-facing text must come from resources instead of Java/Kotlin string literals.

## Command

Run from repository root:

- `./gradlew :app:checkNoHardcodedUiStrings`
- `./gradlew :app:updateNoHardcodedUiStringsBaseline` (intentional maintenance action)

## What It Checks

The guard scans:

- `app/src/main/java`
- `app/src/main/kotlin`

It fails if it finds string literals in common UI sinks, including:

- `setText("...")`
- `setTitle("...")`
- `setMessage("...")`
- `setHint("...")`
- `setError("...")`
- `setSummary("...")`
- `setPositiveButton("...")`
- `setNegativeButton("...")`
- `setNeutralButton("...")`
- `Toast.makeText(..., "...", ...)`
- `showToast("...")`
- `Snackbar.make(..., "...", ...)`

## Exclusions

The scanner excludes:

- `app/src/test`
- `app/src/androidTest`
- `build` folders

Approved non-localizable code patterns are also ignored:

- Empty-string UI resets, for example `setText("")`.
- Wrapper punctuation composition, for example `setText("(" + value + ")")`.

## Intentional Baseline Example

One literal is intentionally kept in baseline as a known working verification example for the guard/bootstrap workflow:

- `showToast("Failed to open output stream")` in `CategoriesActivity`.

Reason it remains:

- It is a low-frequency fallback toast.
- It demonstrates that baseline behavior is functioning (known debt is tracked while new debt is blocked).

## Notes

- This is a targeted regex guard for fast local/CI feedback.
- False positives/negatives are still possible; human review remains the final backstop.
- The check fails only for violations not listed in `scripts/no_hardcoded_ui_strings.baseline`.
- Baseline updates should be rare and reviewed: run the update task only when intentionally accepting existing debt or after broad cleanup/refactoring.
