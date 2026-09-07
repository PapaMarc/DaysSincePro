package com.merware.dayssincepro;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MyEventAdapterDateTokenWrapTest {

    @Test
    public void stabilizeDateTokenWrapping_keepsDateTokenUnbreakable() {
        assertEquals(
                "2026\u201109\u201105",
                MyEventAdapter.stabilizeDateTokenWrapping("2026-09-05")
        );
    }

    @Test
    public void stabilizeDateTokenWrapping_handlesMultipleDates() {
        assertEquals(
                "1989\u201105\u201108 2001\u201101\u201101",
                MyEventAdapter.stabilizeDateTokenWrapping("1989-05-08 2001-01-01")
        );
    }

    @Test
    public void stabilizeDateTokenWrapping_leavesNonDateSeparatorsAlone() {
        assertEquals(
                "Apr-2026",
                MyEventAdapter.stabilizeDateTokenWrapping("Apr-2026")
        );
    }
}
