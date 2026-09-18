package com.merware.dayssincepro;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class EditEventKeyboardPolicyContractTest {

    @Test
    public void editEvent_nonTextInteractionsDismissKeyboard() throws IOException {
        String source = readFile("src/main/java/com/merware/dayssincepro/EditEventActivity.java");

        assertTrue(source.contains("catSpinner.setOnTouchListener(hideKeyboardTouchListener);"));
        assertTrue(source.contains("recurSpinner.setOnTouchListener(hideKeyboardTouchListener);"));
        assertTrue(source.contains("dismissKeyboardAndClearFocus();"));
        assertTrue(source.contains("launchAddCategoryFromPicker();"));
        assertTrue(source.contains("long initial = DatePickerSupport.utcMillis"));
        assertTrue(source.contains("openEndDatePicker(false);"));
        assertTrue(source.contains("showNotifyLeadDaysDialog();"));
        assertTrue(source.contains("if (!globalNotificationsEnabled)"));
        assertTrue(source.contains("if (cbEndDay.isChecked())"));
        assertTrue(source.contains("restoreEventTitleInputAfterCategoryResult();"));
        assertTrue(source.contains("eventText.getText().toString().trim().isEmpty()"));
        assertTrue(source.contains("eventText.requestFocus();"));
        assertTrue(source.contains("imm.showSoftInput(eventText, InputMethodManager.SHOW_IMPLICIT);"));
        assertTrue(source.contains("dismissKeyboardAndClearFocus();"));
    }

    @Test
    public void createCategory_textFirstSurfaceAutoFocusesInput() throws IOException {
        String source = readFile("src/main/java/com/merware/dayssincepro/CreateCategoryActivity.java");

        assertTrue(source.contains("categoryInput.requestFocus();"));
        assertTrue(source.contains("imm.showSoftInput(categoryInput, InputMethodManager.SHOW_IMPLICIT);"));
        assertTrue(source.contains("categoryInput.clearFocus();"));
        assertTrue(source.contains("SOFT_INPUT_STATE_HIDDEN"));
        assertTrue(source.contains("imm.hideSoftInputFromWindow(windowToken, 0);"));
    }

    private static String readFile(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relativePath)), StandardCharsets.UTF_8);
    }
}
