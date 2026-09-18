package com.merware.dayssincepro;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditEventReminderDisclosureContractTest {

    private static final String[] SUMMARY_RESOURCE_FILES = {
            "src/main/res/values/strings.xml",
            "src/main/res/values-de/strings.xml",
            "src/main/res/values-es/strings.xml",
            "src/main/res/values-fr/strings.xml",
            "src/main/res/values-hi/strings.xml",
            "src/main/res/values-it/strings.xml",
            "src/main/res/values-pt/strings.xml",
            "src/main/res/values-pt-rBR/strings.xml",
            "src/main/res/values-zh-rCN/strings.xml",
            "src/sideload/res/values-ar-rXB/strings.xml",
            "src/sideload/res/values-en-rXA/strings.xml"
    };

    @Test
    public void mainLayouts_useTappableSummaryInsteadOfReminderGroup() throws IOException {
        assertSummaryRowLayout("src/main/res/layout/edit_event.xml");
        assertSummaryRowLayout("src/main/res/layout-v14/edit_event.xml");
    }

    @Test
    public void reminderDialog_retainsExistingEditorControls() throws IOException {
        String xml = readFile("src/main/res/layout/reminder_editor_dialog.xml");

        assertTrue(xml.contains("@+id/reminder_editor_content"));
        assertTrue(xml.contains("@+id/eventNotifyEnabledCheckbox"));
        assertTrue(xml.contains("@+id/buttonPickRecur"));
        assertTrue(xml.contains("@+id/buttonEditNotifyLeadDays"));
        assertTrue(xml.contains("@+id/notify_global_disabled_hint"));
    }

    @Test
    public void summaryResources_coverAllSupportedLocales() throws IOException {
        for (String path : SUMMARY_RESOURCE_FILES) {
            String xml = readFile(path);
            assertTrue(path, xml.contains("name=\"reminders_off\""));
            assertTrue(path, xml.contains("name=\"reminders_on_summary\""));
            assertTrue(path, xml.contains("%1$s"));
            assertTrue(path, xml.contains("%2$d"));
        }
    }

    @Test
    public void activity_opensDialogFromFullSummaryRowAndUpdatesSummary() throws IOException {
        String source = readFile("src/main/java/com/merware/dayssincepro/EditEventActivity.java");

        assertTrue(source.contains("reminderSummaryRow.setOnClickListener"));
        assertTrue(source.contains("showReminderEditor();"));
        assertTrue(source.contains("R.plurals.reminders_on_summary"));
        assertTrue(source.contains("eventNotifyEnabledCheckbox.setOnClickListener"));
        assertFalse(source.contains("eventNotifyEnabledCheckbox.setOnClickListener(v -> {\n            dismissKeyboardAndClearFocus();"));
    }

    private static void assertSummaryRowLayout(String relativePath) throws IOException {
        String xml = readFile(relativePath);

        assertTrue(xml.contains("@+id/reminderSummaryRow"));
        assertTrue(xml.contains("@+id/reminderSummaryValue"));
        assertTrue(xml.contains("@drawable/ic_chevron_end"));
        assertTrue(xml.contains("android:clickable=\"true\""));
        assertFalse(xml.contains("@+id/reminderSettingsGroup"));
    }

    private static String readFile(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }
}