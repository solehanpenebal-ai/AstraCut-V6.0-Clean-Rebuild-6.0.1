package com.astracut.v60

import com.astracut.v60.sync.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncEngineTest {
    @Test fun presentWithinTolerance() {
        assertEquals(FrameAction.PRESENT, SyncEngine().decide(SyncSample(1_000_000,995_000)))
    }
    @Test fun holdWhenVideoEarly() {
        assertEquals(FrameAction.HOLD, SyncEngine().decide(SyncSample(1_000_000,1_050_000)))
    }
    @Test fun dropWhenVideoLate() {
        assertEquals(FrameAction.DROP, SyncEngine().decide(SyncSample(1_100_000,1_000_000)))
    }
}
