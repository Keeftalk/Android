package com.keeftalk.chat.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.keeftalk.chat.R
import java.net.URL

object NotificationAvatarHelper {
    private const val TAG = "NotificationAvatar"

    /**
     * Attempts to fetch an avatar from the URL. If it fails or is null,
     * generates a bitmap with initials.
     */
    fun getAvatarBitmap(context: Context, avatarUrl: String?, name: String): Bitmap {
        if (!avatarUrl.isNullOrBlank()) {
            try {
                val inputStream = URL(avatarUrl).openStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    return getCircleBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download notification avatar: $avatarUrl", e)
            }
        }

        // Fallback to initials
        return generateInitialsBitmap(context, name)
    }

    /**
     * Generates a circular bitmap with initials and a seed-based background color.
     */
    private fun generateInitialsBitmap(context: Context, name: String): Bitmap {
        val size = 128 // Sufficient size for notification LargeIcon
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val color = AvatarUtils.getAvatarColorInt(name)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Text
        val initials = AvatarUtils.getInitials(name)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            this.textSize = size * 0.45f
            this.textAlign = Paint.Align.CENTER
            this.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val xPos = size / 2f
        val yPos = (size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initials, xPos, yPos, textPaint)

        return bitmap
    }

    /**
     * Crops a bitmap into a circle.
     */
    private fun getCircleBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}
