package com.keeftalk.chat.ui.screens.editor.signal.model

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class EditorFlags(flags: Int = ASPECT_LOCK or SELECTABLE or VISIBLE or CHILDREN_VISIBLE or EDITABLE) {
    private var flags: Int = flags
    private var markedFlags: Int = 0
    private var persistedFlags: Int = flags

    fun setRotateLocked(rotateLocked: Boolean): EditorFlags {
        setFlag(ROTATE_LOCK, rotateLocked)
        return this
    }

    fun isRotateLocked(): Boolean = isFlagSet(ROTATE_LOCK)

    fun setAspectLocked(aspectLocked: Boolean): EditorFlags {
        setFlag(ASPECT_LOCK, aspectLocked)
        return this
    }

    fun isAspectLocked(): Boolean = isFlagSet(ASPECT_LOCK)

    fun setSelectable(selectable: Boolean): EditorFlags {
        setFlag(SELECTABLE, selectable)
        return this
    }

    fun isSelectable(): Boolean = isFlagSet(SELECTABLE)

    fun setEditable(canEdit: Boolean): EditorFlags {
        setFlag(EDITABLE, canEdit)
        return this
    }

    fun isEditable(): Boolean = isFlagSet(EDITABLE)

    fun setVisible(visible: Boolean): EditorFlags {
        setFlag(VISIBLE, visible)
        return this
    }

    fun isVisible(): Boolean = isFlagSet(VISIBLE)

    fun setChildrenVisible(childrenVisible: Boolean): EditorFlags {
        setFlag(CHILDREN_VISIBLE, childrenVisible)
        return this
    }

    fun isChildrenVisible(): Boolean = isFlagSet(CHILDREN_VISIBLE)

    private fun setFlag(flag: Int, set: Boolean) {
        if (set) {
            this.flags = this.flags or flag
        } else {
            this.flags = this.flags and flag.inv()
        }
    }

    private fun isFlagSet(flag: Int): Boolean = (flags and flag) != 0

    fun asInt(): Int = persistedFlags

    fun getCurrentState(): Int = flags

    fun persist() {
        persistedFlags = flags
    }

    fun reset() {
        restoreState(persistedFlags)
    }

    fun restoreState(flags: Int) {
        this.flags = flags
    }

    fun mark() {
        markedFlags = flags
    }

    fun restore() {
        flags = markedFlags
    }

    fun set(from: EditorFlags) {
        this.persistedFlags = from.persistedFlags
        this.flags = from.flags
    }

    companion object {
        private const val ASPECT_LOCK = 1
        private const val ROTATE_LOCK = 2
        private const val SELECTABLE = 4
        private const val VISIBLE = 8
        private const val CHILDREN_VISIBLE = 16
        private const val EDITABLE = 32
    }
}
