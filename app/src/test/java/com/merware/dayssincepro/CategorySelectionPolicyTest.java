package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CategorySelectionPolicyTest {

    @Test
    public void shouldIncludeSyntheticUncategorized_onlyWhenCountPositive() {
        assertFalse(CategorySelectionPolicy.shouldIncludeSyntheticUncategorized(0));
        assertFalse(CategorySelectionPolicy.shouldIncludeSyntheticUncategorized(-1));
        assertTrue(CategorySelectionPolicy.shouldIncludeSyntheticUncategorized(1));
    }

    @Test
    public void normalizeCategoryToken_trimsAndHandlesNull() {
        assertEquals("", CategorySelectionPolicy.normalizeCategoryToken(null));
        assertEquals("", CategorySelectionPolicy.normalizeCategoryToken("   "));
        assertEquals("LifeDocs", CategorySelectionPolicy.normalizeCategoryToken("  LifeDocs  "));
    }

    @Test
    public void isUncategorizedToken_acceptsBlankAndCaseVariants() {
        assertTrue(CategorySelectionPolicy.isUncategorizedToken(null));
        assertTrue(CategorySelectionPolicy.isUncategorizedToken(""));
        assertTrue(CategorySelectionPolicy.isUncategorizedToken("   "));
        assertTrue(CategorySelectionPolicy.isUncategorizedToken("Uncategorized"));
        assertTrue(CategorySelectionPolicy.isUncategorizedToken(" uncategorized "));
        assertFalse(CategorySelectionPolicy.isUncategorizedToken("LifeDocs"));
    }

    @Test
    public void decideImportCategory_blankUsesDefault() {
        CategorySelectionPolicy.ImportCategoryDecision decision =
                CategorySelectionPolicy.decideImportCategory("   ", 42L, false);

        assertNotNull(decision);
        assertEquals(CategorySelectionPolicy.ImportCategoryDecision.Kind.USE_DEFAULT, decision.getKind());
        assertEquals(42L, decision.getDefaultCategoryId());
    }

    @Test
    public void decideImportCategory_namedCategoryResolvesByName_whenReservedMappingDisabled() {
        CategorySelectionPolicy.ImportCategoryDecision decision =
                CategorySelectionPolicy.decideImportCategory("Uncategorized", 7L, false);

        assertNotNull(decision);
        assertEquals(CategorySelectionPolicy.ImportCategoryDecision.Kind.RESOLVE_BY_NAME, decision.getKind());
        assertEquals("Uncategorized", decision.getCategoryName());
    }

    @Test
    public void decideImportCategory_reservedTokenMapsToSentinel_whenEnabled() {
        CategorySelectionPolicy.ImportCategoryDecision decision =
                CategorySelectionPolicy.decideImportCategory(" uncategorized ", 7L, true);

        assertNotNull(decision);
        assertEquals(CategorySelectionPolicy.ImportCategoryDecision.Kind.USE_UNCATEGORIZED_SENTINEL, decision.getKind());
        assertEquals(CategorySelectionPolicy.UNCATEGORIZED_CAT_ID, decision.getDefaultCategoryId());
    }

    @Test
    public void isReservedCategoryName_blocksUncategorizedLiteralCaseInsensitive() {
        assertTrue(CategorySelectionPolicy.isReservedCategoryName("Uncategorized"));
        assertTrue(CategorySelectionPolicy.isReservedCategoryName(" uncategorized "));
        assertFalse(CategorySelectionPolicy.isReservedCategoryName(""));
        assertFalse(CategorySelectionPolicy.isReservedCategoryName("LifeDocs"));
    }

    @Test
    public void shouldShowCategoryCreationNudge_onlyWhenNoRealCategories() {
        assertTrue(CategorySelectionPolicy.shouldShowCategoryCreationNudge(0));
        assertTrue(CategorySelectionPolicy.shouldShowCategoryCreationNudge(-1));
        assertFalse(CategorySelectionPolicy.shouldShowCategoryCreationNudge(1));
        assertFalse(CategorySelectionPolicy.shouldShowCategoryCreationNudge(3));
    }

    @Test
    public void addNewCategoryActionId_isRecognizedAndNonPersistable() {
        assertTrue(CategorySelectionPolicy.isAddNewCategoryActionId(
                CategorySelectionPolicy.ACTION_ADD_NEW_CATEGORY_ID));
        assertFalse(CategorySelectionPolicy.isPersistableCategoryId(
                CategorySelectionPolicy.ACTION_ADD_NEW_CATEGORY_ID));
    }

    @Test
    public void isPersistableCategoryId_acceptsSentinelAndRealIds() {
        assertTrue(CategorySelectionPolicy.isPersistableCategoryId(0));
        assertTrue(CategorySelectionPolicy.isPersistableCategoryId(7));
        assertFalse(CategorySelectionPolicy.isPersistableCategoryId(-1));
    }

    @Test
    public void shouldDefaultToAddNewCategoryAction_onlyForAddModeFromUncategorizedContext() {
        assertTrue(CategorySelectionPolicy.shouldDefaultToAddNewCategoryAction(true, 0));
        assertFalse(CategorySelectionPolicy.shouldDefaultToAddNewCategoryAction(false, 0));
        assertFalse(CategorySelectionPolicy.shouldDefaultToAddNewCategoryAction(true, 4));
    }

    @Test
    public void normalizeCategoryNameForLookup_isTrimmedAndCaseInsensitive() {
        assertEquals("postoption2", CategorySelectionPolicy.normalizeCategoryNameForLookup(" PostOption2 "));
        assertEquals("", CategorySelectionPolicy.normalizeCategoryNameForLookup("   "));
    }

    @Test
    public void areCategoryNamesEquivalent_matchesTrimmedCaseVariants() {
        assertTrue(CategorySelectionPolicy.areCategoryNamesEquivalent("PostOption2", " postoption2 "));
        assertFalse(CategorySelectionPolicy.areCategoryNamesEquivalent("PostOption2", "PostOption3"));
    }

    @Test
    public void shouldBootstrapFilterContextAfterAdd_trueForPristineFreshState() {
        assertTrue(CategorySelectionPolicy.shouldBootstrapFilterContextAfterAdd(
                5L,
                "",
                "",
                false,
                true,
                true));
    }

    @Test
    public void shouldBootstrapFilterContextAfterAdd_trueForUncategorizedPlaceholderState() {
        assertTrue(CategorySelectionPolicy.shouldBootstrapFilterContextAfterAdd(
                5L,
                "[0]",
                "Uncategorized",
                false,
                true,
                true));
    }

    @Test
    public void shouldBootstrapFilterContextAfterAdd_trueForAlternatePristinePlaceholders() {
        assertTrue(CategorySelectionPolicy.shouldBootstrapFilterContextAfterAdd(
                9L,
                "[]",
                " uncategorized ",
                false,
                true,
                true));
    }

    @Test
    public void shouldBootstrapFilterContextAfterAdd_falseWhenExplicitSelectionExists() {
        assertFalse(CategorySelectionPolicy.shouldBootstrapFilterContextAfterAdd(
                5L,
                "",
                "",
                true,
                false,
                false));
    }

    @Test
    public void shouldBootstrapFilterContextAfterAdd_trueWhenExplicitSelectionExistsButAddStartedFromEmptyUncategorized() {
        assertTrue(CategorySelectionPolicy.shouldBootstrapFilterContextAfterAdd(
                5L,
                "[0]",
                "Uncategorized",
                true,
                true,
                true));
    }

    @Test
    public void shouldBootstrapFilterContextAfterAdd_falseWhenSelectedIsUncategorized() {
        assertFalse(CategorySelectionPolicy.shouldBootstrapFilterContextAfterAdd(
                0L,
                "",
                "",
                false,
                true,
                true));
    }

    @Test
    public void shouldBootstrapFilterContextAfterAdd_falseWhenFilterAlreadySet() {
        assertFalse(CategorySelectionPolicy.shouldBootstrapFilterContextAfterAdd(
                5L,
                "[3]",
                "SomeCategory",
                false,
                true,
                true));
    }

    @Test
    public void isPristineUncategorizedFilterContext_trueForUncategorizedDefaults() {
        assertTrue(CategorySelectionPolicy.isPristineUncategorizedFilterContext("", ""));
        assertTrue(CategorySelectionPolicy.isPristineUncategorizedFilterContext("[0]", "Uncategorized"));
        assertTrue(CategorySelectionPolicy.isPristineUncategorizedFilterContext("[]", " uncategorized "));
    }

    @Test
    public void isPristineUncategorizedFilterContext_falseForRealCategorySelection() {
        assertFalse(CategorySelectionPolicy.isPristineUncategorizedFilterContext("[3]", "Life"));
    }

    @Test
    public void formatSingleSelectedCategoryIds_formatsAsBracketedSingleValue() {
        assertEquals("[7]", CategorySelectionPolicy.formatSingleSelectedCategoryIds(7L));
    }

    @Test
    public void shouldSwitchFilterToNewlyCreatedCategoryAfterAdd_trueWhenInlineCreationAndRealCategory() {
        assertTrue(CategorySelectionPolicy.shouldSwitchFilterToNewlyCreatedCategoryAfterAdd(5L, true));
    }

    @Test
    public void shouldSwitchFilterToNewlyCreatedCategoryAfterAdd_falseWhenInlineCreationFlagMissing() {
        assertFalse(CategorySelectionPolicy.shouldSwitchFilterToNewlyCreatedCategoryAfterAdd(5L, false));
    }

    @Test
    public void shouldSwitchFilterToNewlyCreatedCategoryAfterAdd_falseForUncategorizedSentinel() {
        assertFalse(CategorySelectionPolicy.shouldSwitchFilterToNewlyCreatedCategoryAfterAdd(0L, true));
    }
}
