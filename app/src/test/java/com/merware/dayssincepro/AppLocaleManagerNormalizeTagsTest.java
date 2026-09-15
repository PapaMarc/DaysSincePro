package com.merware.dayssincepro;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppLocaleManagerNormalizeTagsTest {

    @Test
    public void normalizeTagsForLog_nullBecomesSystem() {
        assertEquals("system", AppLocaleManager.normalizeTagsForLog(null));
    }

    @Test
    public void normalizeTagsForLog_blankBecomesSystem() {
        assertEquals("system", AppLocaleManager.normalizeTagsForLog("   "));
    }

    @Test
    public void normalizeTagsForLog_nonBlankUnchanged() {
        assertEquals("fr", AppLocaleManager.normalizeTagsForLog("fr"));
        assertEquals("pt-BR", AppLocaleManager.normalizeTagsForLog("pt-BR"));
    }
}
