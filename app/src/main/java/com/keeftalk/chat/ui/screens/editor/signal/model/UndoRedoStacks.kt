package com.keeftalk.chat.ui.screens.editor.signal.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
internal class UndoRedoStacks(
    val undoStack: ElementStack,
    val redoStack: ElementStack,
    unchangedState: ByteArray? = null
) : Parcelable {
    var unchangedState: ByteArray = unchangedState ?: ByteArray(0)
        private set

    constructor(limit: Int) : this(ElementStack(limit), ElementStack(limit), null)

    fun pushState(element: EditorElement) {
        if (undoStack.tryPush(element)) {
            redoStack.clear()
        }
    }

    fun clear(element: EditorElement) {
        undoStack.clear()
        redoStack.clear()
        unchangedState = ElementStack.getBytes(element)
    }

    fun isChanged(element: EditorElement): Boolean = !Arrays.equals(ElementStack.getBytes(element), unchangedState)

    fun canUndo(currentState: EditorElement): Boolean = undoStack.stackContainsStateDifferentFrom(currentState)

    fun canRedo(currentState: EditorElement): Boolean = redoStack.stackContainsStateDifferentFrom(currentState)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(undoStack, flags)
        dest.writeParcelable(redoStack, flags)
        dest.writeByteArray(unchangedState)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<UndoRedoStacks> {
            override fun createFromParcel(`in`: Parcel): UndoRedoStacks {
                return UndoRedoStacks(
                    `in`.readParcelable(ElementStack::class.java.classLoader)!!,
                    `in`.readParcelable(ElementStack::class.java.classLoader)!!,
                    `in`.createByteArray()
                )
            }
            override fun newArray(size: Int): Array<UndoRedoStacks?> = arrayOfNulls(size)
        }
    }
}
