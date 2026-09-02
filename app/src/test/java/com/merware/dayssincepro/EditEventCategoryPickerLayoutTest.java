package com.merware.dayssincepro;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditEventCategoryPickerLayoutTest {

    @Test
    public void mainLayout_usesSelectAgainSpinnerForCategoryPicker() throws IOException {
        String xml = readFile("src/main/res/layout/edit_event.xml");
        assertContainsCategorySelectAgainSpinner(xml);
    }

    @Test
    public void v14Layout_usesSelectAgainSpinnerForCategoryPicker() throws IOException {
        String xml = readFile("src/main/res/layout-v14/edit_event.xml");
        assertContainsCategorySelectAgainSpinner(xml);
    }

    private static String readFile(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }

    private static void assertContainsCategorySelectAgainSpinner(String xml) {
        assertTrue(xml.contains("<com.merware.dayssincepro.SelectAgainSpinner"));
        assertTrue(xml.contains("android:id=\"@+id/catSpinner\""));
        assertFalse(xml.contains("android:id=\"@+id/checkBox1\""));

        int categorySpinnerIndex = xml.indexOf("android:id=\"@+id/catSpinner\"");
        int detailsInputIndex = xml.indexOf("android:id=\"@+id/editDetails\"");
        assertTrue(categorySpinnerIndex >= 0);
        assertTrue(detailsInputIndex > categorySpinnerIndex);
    }
}
