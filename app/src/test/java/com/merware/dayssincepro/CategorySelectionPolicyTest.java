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
}
