package com.astracut.v60

import com.astracut.v60.animation.*
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyframeEngineTest {
    @Test fun linearInterpolation() {
        val t=KeyframeTrack(listOf(Keyframe(0,0f),Keyframe(1_000_000,10f)))
        assertEquals(5f,t.valueAt(500_000)!!,0.0001f)
    }
}
