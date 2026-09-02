package com.merware.dayssincepro;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CreateCategoryFlowWiringTest {

    @Test
    public void editEvent_launchesDedicatedCreateCategoryActivity() throws IOException {
        String source = readFile("src/main/java/com/merware/dayssincepro/EditEventActivity.java");

        assertTrue(source.contains("new Intent(this, CreateCategoryActivity.class)"));
        assertTrue(source.contains("CreateCategoryActivity.EXTRA_CREATED_CATEGORY_ID"));
        assertFalse(source.contains("new Intent(this, CategoriesActivity.class)"));
        assertFalse(source.contains("CategoriesActivity.EXTRA_AUTO_OPEN_ADD_CATEGORY"));
    }

    @Test
    public void manifest_registersCreateCategoryActivity() throws IOException {
        String manifest = readFile("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\".CreateCategoryActivity\""));
    }

    private static String readFile(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }
}
