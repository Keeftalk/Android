package com.keeftalk.chat.util

import java.text.Bidi

object LanguageUtils {
    /**
     * Detects if the given text is primarily Right-to-Left (like Arabic, Hebrew).
     */
    fun isRtl(text: String): Boolean {
        if (text.isBlank()) return false
        val bidi = Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
        return bidi.isRightToLeft || bidi.baseLevel != 0
    }
}
