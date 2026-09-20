package com.astracut.v60.edit

import com.astracut.v60.core.Project

interface EditCommand { fun apply(project: Project): Project; fun undo(project: Project): Project }

class History(private val limit: Int = 200) {
    private val undo = ArrayDeque<EditCommand>()
    private val redo = ArrayDeque<EditCommand>()
    fun execute(project: Project, command: EditCommand): Project {
        val next = command.apply(project)
        undo.addLast(command); if (undo.size > limit) undo.removeFirst()
        redo.clear()
        return next
    }
    fun undo(project: Project): Project? =
        undo.removeLastOrNull()?.also { redo.addLast(it) }?.undo(project)
    fun redo(project: Project): Project? =
        redo.removeLastOrNull()?.also { undo.addLast(it) }?.apply(project)
}
