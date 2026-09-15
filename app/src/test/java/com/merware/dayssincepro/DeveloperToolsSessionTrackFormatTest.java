package com.merware.dayssincepro;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DeveloperToolsSessionTrackFormatTest {

    @Test
    public void withTrack_formatsTrackPrefixForTrackA() {
        String line = DeveloperToolsSession.withTrack(DeveloperToolsSession.TRACK_A, "event=test");
        assertEquals("TRACK_A | event=test", line);
    }

    @Test
    public void withTrack_formatsTrackPrefixForTrackB() {
        String line = DeveloperToolsSession.withTrack(DeveloperToolsSession.TRACK_B, "event=locale_apply_start");
        assertEquals("TRACK_B | event=locale_apply_start", line);
    }
}
