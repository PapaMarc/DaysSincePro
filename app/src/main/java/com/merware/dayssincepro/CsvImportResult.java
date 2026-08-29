package com.merware.dayssincepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result object encapsulating the outcome of a CSV import operation.
 *
 * Provides callers with comprehensive summary statistics including total rows
 * processed, successfully imported rows, skipped/malformed rows, newly created
 * categories, and detailed diagnostic error/warning messages.
 */
public class CsvImportResult {

    private final boolean success;
    private final int totalRows;
    private final int importedCount;
    private final int skippedCount;
    private final int categoriesCreated;
    private final List<String> errors;

    public CsvImportResult(boolean success, int totalRows, int importedCount,
                           int skippedCount, int categoriesCreated, List<String> errors) {
        this.success = success;
        this.totalRows = totalRows;
        this.importedCount = importedCount;
        this.skippedCount = skippedCount;
        this.categoriesCreated = categoriesCreated;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<String>();
    }

    public static CsvImportResult success(int totalRows, int importedCount,
                                          int skippedCount, int categoriesCreated, List<String> errors) {
        return new CsvImportResult(true, totalRows, importedCount, skippedCount, categoriesCreated, errors);
    }

    public static CsvImportResult failure(String errorMessage) {
        List<String> errs = new ArrayList<>();
        if (errorMessage != null) {
            errs.add(errorMessage);
        }
        return new CsvImportResult(false, 0, 0, 0, 0, errs);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getCategoriesCreated() {
        return categoriesCreated;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Builds a human-readable summary string for display in UI toasts or dialogs.
     */
    public String getSummaryMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Imported ").append(importedCount).append(" event");
        if (importedCount != 1) {
            sb.append("s");
        }
        if (categoriesCreated > 0) {
            sb.append(" (").append(categoriesCreated).append(" new categor").append(categoriesCreated == 1 ? "y" : "ies").append(")");
        }
        if (skippedCount > 0) {
            sb.append(", ").append(skippedCount).append(" skipped");
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CsvImportResult{" +
                "success=" + success +
                ", totalRows=" + totalRows +
                ", importedCount=" + importedCount +
                ", skippedCount=" + skippedCount +
                ", categoriesCreated=" + categoriesCreated +
                ", errors=" + errors +
                '}';
    }
}
