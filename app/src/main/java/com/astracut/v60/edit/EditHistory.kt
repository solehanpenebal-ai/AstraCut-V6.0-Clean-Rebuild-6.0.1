package com.astracut.v60.edit

import android.net.Uri

data class Clip(
    val uri: Uri,
    var startMs: Long = 0L,
    var endMs: Long = -1L,
    var label: String = "Clip",
    var rotationDegrees: Float = 0f,
    var cropLeft: Float = 0f,
    var cropRight: Float = 0f,
    var cropTop: Float = 0f,
    var cropBottom: Float = 0f
) {
    fun effectiveEnd(durationMs: Long): Long =
        if (endMs > 0L) endMs else durationMs

    fun lengthMs(durationMs: Long): Long =
        (effectiveEnd(durationMs) - startMs).coerceAtLeast(0L)
}

data class ProjectSnapshot(
    val clips: List<Clip>,
    val selected: Int
)

class EditHistory(private val maxDepth: Int = 50) {
    private val undoStack = ArrayDeque<ProjectSnapshot>()
    private val redoStack = ArrayDeque<ProjectSnapshot>()

    fun push(snapshot: ProjectSnapshot) {
        undoStack.addLast(snapshot)
        while (undoStack.size > maxDepth) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: ProjectSnapshot): ProjectSnapshot? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: ProjectSnapshot): ProjectSnapshot? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        return next
    }

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()
}
