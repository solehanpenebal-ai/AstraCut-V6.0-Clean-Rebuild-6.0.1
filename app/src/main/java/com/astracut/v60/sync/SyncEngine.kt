package com.astracut.v60.sync

interface MasterClock { fun positionUs(): Long; fun running(): Boolean }

enum class FrameAction { PRESENT, HOLD, DROP }

data class SyncSample(val masterUs:Long,val frameUs:Long)
class SyncEngine(private val earlyUs:Long=20_000,private val lateUs:Long=45_000) {
    fun decide(s:SyncSample):FrameAction=when {
        s.masterUs-s.frameUs < -earlyUs -> FrameAction.HOLD
        s.masterUs-s.frameUs > lateUs -> FrameAction.DROP
        else -> FrameAction.PRESENT
    }
}
