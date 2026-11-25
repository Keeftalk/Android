package tn.keeftalk.chat.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object AvatarUtils {
    fun getInitials(name: String?): String {
        if (name.isNullOrBlank() || name.equals("Unknown", ignoreCase = true) || name.equals("Unknown Number", ignoreCase = true) || name.equals("Private Number", ignoreCase = true) || name == "-1") return "?"
        return name.trim().firstOrNull()?.toString()?.uppercase() ?: "?"
    }

    fun getAvatarColor(seed: String?): Color {
        if (seed.isNullOrBlank()) return Color(0xFF78909C)
        val colors = listOf(
            Color(0xFFFF8A80), // Premium Coral
            Color(0xFFFF80AB), // Premium Pink
            Color(0xFFEA80FC), // Premium Lavender
            Color(0xFFB388FF), // Premium Violet
            Color(0xFF8C9EFF), // Premium Indigo
            Color(0xFF82B1FF), // Premium Sky Blue
            Color(0xFF80D8FF), // Premium Light Blue
            Color(0xFF84FFFF), // Premium Aquamarine
            Color(0xFFA7FFEB), // Premium Mint
            Color(0xFFB9F6CA), // Premium Emerald
            Color(0xFFCCFF90), // Premium Lime
            Color(0xFFF4FF81), // Premium Lemon
            Color(0xFFFFE57F), // Premium Gold
            Color(0xFFFFD180), // Premium Orange
            Color(0xFFFF9E80)  // Premium Deep Orange
        )
        val index = Math.abs(seed.hashCode()) % colors.size
        return colors[index]
    }

    fun getAvatarColorInt(seed: String?): Int {
        return getAvatarColor(seed).toArgb()
    }

    fun getTextColorForBackground(backgroundColor: Color): Color {
        // Use a simple luminance check for better contrast
        val luminance = 0.299 * backgroundColor.red + 0.587 * backgroundColor.green + 0.114 * backgroundColor.blue
        return if (luminance > 0.6) Color(0xFF1A1A1A) else Color.White
    }

    /**
     * Converts a public CDN URL to its `compressed` variant if available.
     * Example: https://public.cdn.keeftalk.com/{user_id}/avatars/{picture_id}/original
     *       -> https://public.cdn.keeftalk.com/{user_id}/avatars/{picture_id}/compressed
     */
    fun toCompressedUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        if (url.endsWith("/original")) {
            return url.substringBeforeLast("/original") + "/compressed"
        }
        return url
    }

    /**
     * Converts a public CDN URL to its `original` variant if available.
     * Example: https://public.cdn.keeftalk.com/{user_id}/avatars/{picture_id}/compressed
     *       -> https://public.cdn.keeftalk.com/{user_id}/avatars/{picture_id}/original
     */
    fun toOriginalUrl(url: String?): String? {
        if (url.isNullOrBlank()) return url
        if (url.endsWith("/compressed")) {
            return url.substringBeforeLast("/compressed") + "/original"
        }
        return url
    }

    /**
     * Downscales and compresses an image byte array to JPEG format.
     */
    fun compressImage(bytes: ByteArray, maxDim: Int, quality: Int): ByteArray {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return bytes

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDim * 2 || options.outHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return bytes

            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (aspect >= 1f) maxDim else (maxDim * aspect).toInt().coerceAtLeast(1)
                val targetH = if (aspect <= 1f) maxDim else (maxDim / aspect).toInt().coerceAtLeast(1)
                android.graphics.Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }

            val out = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        } catch (e: Exception) {
            bytes
        }
    }

    /**
     * Generates (original, compressed) avatar variants.
     * original: Max 1024px, 90% quality
     * compressed: Max 256px, 75% quality
     */
    fun createAvatarVariants(bytes: ByteArray): Pair<ByteArray, ByteArray> {
        val original = compressImage(bytes, maxDim = 1024, quality = 90)
        val compressed = compressImage(bytes, maxDim = 256, quality = 75)
        return Pair(original, compressed)
    }

    /**
     * Generates (original, compressed) wallpaper variants.
     * original: Max 1920px, 85% quality
     * compressed: Max 512px, 70% quality
     */
    fun createWallpaperVariants(bytes: ByteArray): Pair<ByteArray, ByteArray> {
        val original = compressImage(bytes, maxDim = 1920, quality = 85)
        val compressed = compressImage(bytes, maxDim = 512, quality = 70)
        return Pair(original, compressed)
    }
}
