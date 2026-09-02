package com.merware.dayssincepro;

import java.util.Locale;

/**
 * Central policy surface for uncategorized semantics across selection and import flows.
 */
public final class CategorySelectionPolicy {

    public static final long UNCATEGORIZED_CAT_ID = 0L;
    public static final String UNCATEGORIZED_LABEL = "Uncategorized";

    private CategorySelectionPolicy() {
        // Utility class; prevent instantiation.
    }

    public static boolean shouldIncludeSyntheticUncategorized(long uncategorizedEventCount) {
        return uncategorizedEventCount > 0;
    }

    public static String normalizeCategoryToken(String rawCategory) {
        return rawCategory == null ? "" : rawCategory.trim();
    }

    public static boolean isUncategorizedToken(String rawCategory) {
        String normalized = normalizeCategoryToken(rawCategory);
        if (normalized.isEmpty()) {
            return true;
        }
        return normalized.toLowerCase(Locale.US)
                .equals(UNCATEGORIZED_LABEL.toLowerCase(Locale.US));
    }

    public static ImportCategoryDecision decideImportCategory(String rawCategory,
                                                              long defaultCategoryId,
                                                              boolean mapReservedTokenToSentinel) {
        String normalized = normalizeCategoryToken(rawCategory);

        if (normalized.isEmpty()) {
            return ImportCategoryDecision.useDefault(defaultCategoryId);
        }

        if (mapReservedTokenToSentinel && isUncategorizedToken(normalized)) {
            return ImportCategoryDecision.useUncategorizedSentinel();
        }

        return ImportCategoryDecision.resolveByName(normalized);
    }

    public static final class ImportCategoryDecision {
        public enum Kind {
            USE_DEFAULT,
            RESOLVE_BY_NAME,
            USE_UNCATEGORIZED_SENTINEL
        }

        private final Kind kind;
        private final long defaultCategoryId;
        private final String categoryName;

        private ImportCategoryDecision(Kind kind, long defaultCategoryId, String categoryName) {
            this.kind = kind;
            this.defaultCategoryId = defaultCategoryId;
            this.categoryName = categoryName;
        }

        public static ImportCategoryDecision useDefault(long defaultCategoryId) {
            return new ImportCategoryDecision(Kind.USE_DEFAULT, defaultCategoryId, null);
        }

        public static ImportCategoryDecision resolveByName(String categoryName) {
            return new ImportCategoryDecision(Kind.RESOLVE_BY_NAME, -1L, categoryName);
        }

        public static ImportCategoryDecision useUncategorizedSentinel() {
            return new ImportCategoryDecision(Kind.USE_UNCATEGORIZED_SENTINEL,
                    UNCATEGORIZED_CAT_ID, null);
        }

        public Kind getKind() {
            return kind;
        }

        public long getDefaultCategoryId() {
            return defaultCategoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }
    }
}
