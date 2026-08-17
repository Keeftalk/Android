package com.keeftalk.chat.ui.screens.editor.signal.model

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Parcel
import java.util.UUID

/**
 * Ported from Signal Android (AGPL-3.0)
 */
object ParcelUtils {
    fun writeMatrix(dest: Parcel, matrix: Matrix) {
        val values = FloatArray(9)
        matrix.getValues(values)
        dest.writeFloatArray(values)
    }

    fun readMatrix(matrix: Matrix, `in`: Parcel) {
        val values = FloatArray(9)
        `in`.readFloatArray(values)
        matrix.setValues(values)
    }

    fun readMatrix(`in`: Parcel): Matrix {
        val matrix = Matrix()
        readMatrix(matrix, `in`)
        return matrix
    }

    fun writeRect(dest: Parcel, rect: RectF) {
        dest.writeFloat(rect.left)
        dest.writeFloat(rect.top)
        dest.writeFloat(rect.right)
        dest.writeFloat(rect.bottom)
    }

    fun readRectF(`in`: Parcel): RectF {
        val left = `in`.readFloat()
        val top = `in`.readFloat()
        val right = `in`.readFloat()
        val bottom = `in`.readFloat()
        return RectF(left, top, right, bottom)
    }

    fun readUUID(`in`: Parcel): UUID {
        return UUID(`in`.readLong(), `in`.readLong())
    }

    fun writeUUID(dest: Parcel, uuid: UUID) {
        dest.writeLong(uuid.mostSignificantBits)
        dest.writeLong(uuid.leastSignificantBits)
    }
}
