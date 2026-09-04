package com.merware.dayssincepro;

import java.util.Locale;

/**
 * Central policy surface for uncategorized semantics across selection and import flows.
 */
public final class CategorySelectionPolicy {

    public static final long UNCATEGORIZED_CAT_ID = 0L;
    public static final String UNCATEGORIZED_LABEL = "Uncategorized";
    public static final long ACTION_ADD_NEW_CATEGORY_ID = -100L;
    public static final String ACTION_ADD_NEW_CATEGORY_LABEL = "<Add New Category>";

    private CategorySelectionPolicy() {
        // Utility class; prevent instantiation.
    }

    public static boolean shouldIncludeSyntheticUncategorized(long uncategorizedEventCount) {
        return uncategorizedEventCount > 0;
    }

    public static boolean shouldShowCategoryCreationNudge(long realCategoryCount) {
        return realCategoryCount <= 0;
    }

    public static boolean isAddNewCategoryActionId(long categoryId) {
        return categoryId == ACTION_ADD_NEW_CATEGORY_ID;
    }

    public static boolean isPersistableCategoryId(long categoryId) {
        return categoryId >= 0;
    }

    public static boolean shouldDefaultToAddNewCategoryAction(boolean isAddMode,
                                                              long requestedCategoryId) {
        return isAddMode && requestedCategoryId == UNCATEGORIZED_CAT_ID;
    }

    public static boolean shouldBootstrapFilterContextAfterAdd(long selectedCategoryId,
                                                               String categoryIdsPreference,
                                                               String categoriesPreference,
                                                               boolean hasExplicitFilterSelection,
                                                               boolean addLaunchedFromUncategorizedContext,
                                                               boolean addLaunchedWithNoEvents) {
        if (selectedCategoryId <= UNCATEGORIZED_CAT_ID) {
            return false;
        }

        boolean uncategorizedFilterContext = isPristineUncategorizedFilterContext(
                categoryIdsPreference,
                categoriesPreference);
        if (!uncategorizedFilterContext) {
            return false;
        }

        if (!hasExplicitFilterSelection) {
            return true;
        }

        return addLaunchedFromUncategorizedContext && addLaunchedWithNoEvents;
    }

    public static boolean shouldSwitchFilterToNewlyCreatedCategoryAfterAdd(
            long selectedCategoryId,
            boolean categoryWasCreatedInlineDuringAddFlow) {
        return categoryWasCreatedInlineDuringAddFlow && selectedCategoryId > UNCATEGORIZED_CAT_ID;
    }

    public static boolean isPristineUncategorizedFilterContext(String categoryIdsPreference,
                                                                String categoriesPreference) {
        String ids = normalizeCategoryToken(categoryIdsPreference);
        String categories = normalizeCategoryToken(categoriesPreference);

        boolean idsPristine = ids.isEmpty()
                || "[]".equals(ids)
                || "[0]".equals(ids)
                || "0".equals(ids);

        boolean categoriesPristine = categories.isEmpty() || isUncategorizedToken(categories);
        return idsPristine && categoriesPristine;
    }

    public static String formatSingleSelectedCategoryIds(long categoryId) {
        return "[" + categoryId + "]";
    }

    public static String normalizeCategoryNameForLookup(String rawCategoryName) {
        return normalizeCategoryToken(rawCategoryName).toLowerCase(Locale.US);
    }

    public static boolean areCategoryNamesEquivalent(String first, String second) {
        return normalizeCategoryNameForLookup(first)
                .equals(normalizeCategoryNameForLookup(second));
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

    public static boolean isReservedCategoryName(String rawCategoryName) {
        String normalized = normalizeCategoryToken(rawCategoryName);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.toLowerCase(Locale.US)
                .equals(UNCATEGORIZED_LABEL.toLowerCase(Locale.US));
    }

    public static String getUncategorizedDisplayLabel() {
        return UNCATEGORIZED_LABEL;
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
