package com.merware.dayssincepro;

import java.io.File;

/**
 * Result object encapsulating the outcome of a CSV export operation.
 *
 * Provides callers with structured success/failure status, row count,
 * target file reference, and diagnostic error information.
 */
public class CsvExportResult {

    private final boolean success;
    private final int rowsExported;
    private final File exportedFile;
    private final String errorMessage;

    public CsvExportResult(boolean success, int rowsExported, File exportedFile, String errorMessage) {
        this.success = success;
        this.rowsExported = rowsExported;
        this.exportedFile = exportedFile;
        this.errorMessage = errorMessage;
    }

    public static CsvExportResult success(int rowsExported, File exportedFile) {
        return new CsvExportResult(true, rowsExported, exportedFile, null);
    }

    public static CsvExportResult failure(String errorMessage) {
        return new CsvExportResult(false, 0, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getRowsExported() {
        return rowsExported;
    }

    public File getExportedFile() {
        return exportedFile;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "CsvExportResult{" +
                "success=" + success +
                ", rowsExported=" + rowsExported +
                ", exportedFile=" + (exportedFile != null ? exportedFile.getAbsolutePath() : "null") +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
