package com.astracut.v60.animation

enum class Property { X, Y, SCALE_X, SCALE_Y, ROTATION, OPACITY }
enum class Easing { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

data class Keyframe(val timeUs: Long, val value: Float, val easing: Easing = Easing.LINEAR)

class KeyframeTrack(private val frames: List<Keyframe>) {
    fun valueAt(timeUs: Long): Float? {
        val f = frames.sortedBy { it.timeUs }
        if (f.isEmpty()) return null
        if (timeUs <= f.first().timeUs) return f.first().value
        if (timeUs >= f.last().timeUs) return f.last().value
        val i = f.indexOfFirst { it.timeUs >= timeUs }
        val a = f[i - 1]; val b = f[i]
        val t = ((timeUs-a.timeUs).toDouble()/(b.timeUs-a.timeUs)).toFloat()
        val e = when (b.easing) {
            Easing.LINEAR -> t
            Easing.EASE_IN -> t*t
            Easing.EASE_OUT -> 1f-(1f-t)*(1f-t)
            Easing.EASE_IN_OUT -> if (t<.5f) 2f*t*t else 1f-((-2f*t+2f)*(-2f*t+2f))/2f
        }
        return a.value + (b.value-a.value)*e
    }
}

data class BezierCurve(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    fun sample(t: Float): Float {
        val u = 1f-t
        return 3*u*u*t*y1 + 3*u*t*t*y2 + t*t*t
    }
}
