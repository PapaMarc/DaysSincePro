package com.merware.dayssincepro;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AboutDialogHeaderTextTest {

    @Test
    public void buildHeaderText_release_includesOnlyVersion() {
        String header = AboutDialog.buildHeaderText(
                "Version: 1.2.3",
                "Schema: v5",
                "SideLoad .apk: com.example.dev",
                "(currently running in DEBUG mode)",
                false,
                true);

        assertEquals("Version: 1.2.3", header);
    }

    @Test
    public void buildHeaderText_sideloadWithoutDebugger_omitsDebugLine() {
        String header = AboutDialog.buildHeaderText(
                "Version: 1.2.3",
                "Schema: v5",
                "SideLoad .apk: com.example.dev",
                "(currently running in DEBUG mode)",
                true,
                false);

        assertEquals(
                "Version: 1.2.3\nSchema: v5\nSideLoad .apk: com.example.dev",
                header);
    }

    @Test
    public void buildHeaderText_sideloadWithDebugger_includesDebugLineUnderPackage() {
        String header = AboutDialog.buildHeaderText(
                "Version: 1.2.3",
                "Schema: v5",
                "SideLoad .apk: com.example.dev",
                "(currently running in DEBUG mode)",
                true,
                true);

        assertEquals(
                "Version: 1.2.3\nSchema: v5\nSideLoad .apk: com.example.dev\n(currently running in DEBUG mode)",
                header);
    }
}
