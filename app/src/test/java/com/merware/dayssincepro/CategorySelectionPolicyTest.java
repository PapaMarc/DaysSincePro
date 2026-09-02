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
}
