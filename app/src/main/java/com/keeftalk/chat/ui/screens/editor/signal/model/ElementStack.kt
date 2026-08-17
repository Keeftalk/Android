package com.keeftalk.chat.ui.screens.editor.signal.model

import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class ElementStack(val limit: Int) : Parcelable {
    private val stack = Stack<ByteArray>()

    private constructor(`in`: Parcel) : this(`in`.readInt()) {
        val count = `in`.readInt()
        for (i in 0 until count) {
            stack.add(i, `in`.createByteArray())
        }
    }

    fun tryPush(element: EditorElement): Boolean {
        val bytes = getBytes(element)
        val push = stack.isEmpty() || !Arrays.equals(bytes, stack.peek())
        if (push) {
            stack.push(bytes)
            if (stack.size > limit) {
                stack.removeAt(1)
            }
        }
        return push
    }

    fun pop(element: EditorElement): EditorElement? {
        if (stack.empty()) return null
        val elementBytes = getBytes(element)
        var stackData: ByteArray? = null
        while (!stack.empty() && stackData == null) {
            val topData = stack.pop()
            if (!Arrays.equals(topData, elementBytes)) {
                stackData = topData
            }
        }
        if (stackData == null) return null
        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(stackData, 0, stackData.size)
            parcel.setDataPosition(0)
            parcel.readParcelable(EditorElement::class.java.classLoader)
        } finally {
            parcel.recycle()
        }
    }

    fun clear() {
        stack.clear()
    }

    fun stackContainsStateDifferentFrom(element: EditorElement): Boolean {
        if (stack.isEmpty()) return false
        val currentStateBytes = getBytes(element)
        for (item in stack) {
            if (!Arrays.equals(item, currentStateBytes)) return true
        }
        return false
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val persisted = entriesToPersist()
        dest.writeInt(limit)
        dest.writeInt(persisted.size)
        for (entry in persisted) {
            dest.writeByteArray(entry)
        }
    }

    private fun entriesToPersist(): List<ByteArray> {
        if (stack.size <= PERSISTED_LIMIT) return stack
        val persisted = ArrayList<ByteArray>(PERSISTED_LIMIT)
        persisted.add(stack[0])
        persisted.addAll(stack.subList(stack.size - (PERSISTED_LIMIT - 1), stack.size))
        return persisted
    }

    companion object {
        private const val PERSISTED_LIMIT = 10

        fun getBytes(parcelable: Parcelable): ByteArray {
            val parcel = Parcel.obtain()
            return try {
                parcel.writeParcelable(parcelable, 0)
                parcel.marshall()
            } finally {
                parcel.recycle()
            }
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<ElementStack> {
            override fun createFromParcel(`in`: Parcel): ElementStack = ElementStack(`in`)
            override fun newArray(size: Int): Array<ElementStack?> = arrayOfNulls(size)
        }
    }
}
