package com.keeftalk.chat.ui.emoji

object EmojiUtils {
    fun isSingleEmoji(text: String): Boolean {
        if (text.isEmpty()) return false
        
        // This is a simplified check. A production version would use EmojiCompat or 
        // a more complex regex to handle ZWJ sequences and skin tones.
        val emojiRegex = Regex("""(\u00a9|\u00ae|[\u2000-\u3300]|\ud83c[\ud000-\udfff]|\ud83d[\ud000-\udfff]|\ud83e[\ud000-\udfff])""")
        val matches = emojiRegex.findAll(text).toList()
        
        // If it's one emoji and no other text (other than whitespace)
        return matches.size == 1 && text.trim() == matches[0].value
    }

    fun getAnimatedEmojiUrl(unicode: String): String {
        // In a real app, this would return a URL to a GIF/Lottie file for the specific emoji
        // For demonstration, we'll use a public CDN or a specific mapping
        val hex = unicode.codePoints().toArray().joinToString("-") { Integer.toHexString(it) }
        return "https://fonts.gstatic.com/s/e/notoemoji/latest/$hex/512.gif"
    }
}
