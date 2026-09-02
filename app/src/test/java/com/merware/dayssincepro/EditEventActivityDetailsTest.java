package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EditEventActivityDetailsTest {

    @Test
    public void normalizeDetails_null_returnsNull() {
        assertNull(EditEventActivity.normalizeDetails(null));
    }

    @Test
    public void normalizeDetails_blank_returnsNull() {
        assertNull(EditEventActivity.normalizeDetails("   \n  \t  "));
    }

    @Test
    public void normalizeDetails_trimsAndPreservesContent() {
        assertEquals("Oersted discovers electromagnetism.",
                EditEventActivity.normalizeDetails("  Oersted discovers electromagnetism.  "));
    }

    @Test
    public void normalizeDetails_truncatesTo256Characters() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            builder.append('a');
        }

        String normalized = EditEventActivity.normalizeDetails(builder.toString());
        assertEquals(256, normalized.length());
    }
}
