package com.keeftalk.chat.util

import androidx.compose.foundation.text.InlineTextContent
import com.mohamedrejeb.richeditor.model.RichTextState
import java.util.regex.Pattern

object NotesUtils {
    /**
     * WORKAROUND: The RichText library rc14 has a bug where it doesn't pass inlineContentMap 
     * to the underlying BasicTextField. This reflection hack extracts it.
     */
    @Suppress("UNCHECKED_CAST")
    fun getInlineContentMap(state: RichTextState): Map<String, InlineTextContent> {
        return try {
            val field = state.javaClass.getDeclaredField("inlineContentMap")
            field.isAccessible = true
            val map = field.get(state) as Map<String, InlineTextContent>
            NotesLogger.v("FIX", "Successfully extracted inlineContentMap using reflection. Size: ${map.size}")
            
            // Debug check for the map contents
            map.forEach { (key, value) ->
                NotesLogger.v("MEDIA_DEBUG", "  Map Entry: key=$key, content=$value")
            }
            
            map
        } catch (e: Exception) {
            NotesLogger.e("FIX", "Failed to extract inlineContentMap using reflection", throwable = e)
            emptyMap()
        }
    }

    /**
     * Detection for corruption characters
     */
    fun checkForCorruption(content: String, location: String) {
        if (content.contains("\uFFFD")) {
            NotesLogger.e("MEDIA_DEBUG", "[CORRUPTION_FOUND] Found REPLACEMENT CHARACTER (\\uFFFD) at $location")
            logSurroundingContent(content, "\uFFFD")
        }
        if (content.contains("\uFFFC")) {
            NotesLogger.w("MEDIA_DEBUG", "[CORRUPTION_FOUND] Found OBJECT REPLACEMENT CHARACTER (\\uFFFC) at $location")
            logSurroundingContent(content, "\uFFFC")
        }
    }

    private fun logSurroundingContent(content: String, char: String) {
        val index = content.indexOf(char)
        val start = (index - 50).coerceAtLeast(0)
        val end = (index + 50).coerceAtMost(content.length)
        NotesLogger.d("MEDIA_DEBUG", "Context around corruption: ...${content.substring(start, end)}...")
    }

    /**
     * Removes invalid dimensions and normalizes HTML tags to prevent rendering issues.
     * Targets width="0.0" and height="0.0" which cause images to disappear.
     * Converts float dimensions (1200.0) to integers (1200).
     * Now supports responsive images by preserving data-attributes and style.
     */
    fun sanitizeHtml(html: String): String {
        if (html.isBlank()) return html
        
        var sanitized = html

        // 1. Remove Object Replacement Characters (\uFFFC) and Replacement Characters (\uFFFD)
        val replacementCharsPattern = "[\uFFFC\uFFFD]".toRegex()
        if (sanitized.contains(replacementCharsPattern)) {
            sanitized = sanitized.replace(replacementCharsPattern, "")
        }

        // 2. Remove width="0" or height="0" variants entirely
        val zeroPattern = """\s(width|height)=["']0(\.0+)?["']""".toRegex(RegexOption.IGNORE_CASE)
        sanitized = zeroPattern.replace(sanitized, "")

        // 3. Normalize float dimensions to integers
        sanitized = sanitized.replace(".0\"", "\"").replace(".0'", "'")

        try {
            val floatAttrPattern = Pattern.compile("""\s(width|height)=["'](\d+)\.\d+["']""", Pattern.CASE_INSENSITIVE)
            val matcher = floatAttrPattern.matcher(sanitized)
            val sb = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(sb, " ${matcher.group(1)}=\"${matcher.group(2)}\"")
            }
            matcher.appendTail(sb)
            sanitized = sb.toString()
        } catch (e: Exception) {
            NotesLogger.e("HTML_SANITIZER", "Error during float dimension normalization", throwable = e)
        }

        // 4. Force all <video> tags to <img> tags for consistent parser support
        sanitized = sanitized.replace("<video", "<img")
        sanitized = sanitized.replace("</video>", "")

        // 5. Fix potential malformed HTML from the library's toHtml() output
        sanitized = sanitized.replace(Regex("""(<img[^>]+>)\s*</p>"""), "$1")

        // 6. Ensure alt attribute is present
        if (sanitized.contains("<img ") && !sanitized.contains("alt=")) {
            sanitized = sanitized.replace("<img ", "<img alt=\"Media\" ")
        }

        // 7. Ensure img tags have a space before attributes
        sanitized = sanitized.replace("<imgsrc=", "<img src=")

        // 8. Responsive support: If width/height are missing, we check for our responsive attributes.
        // The library parser usually needs some dimensions to create a placeholder.
        if (sanitized.contains("<img") && !sanitized.contains("width=") && !sanitized.contains("height=")) {
            // Check if it has our data-original-width
            if (sanitized.contains("data-original-width=")) {
                 // It's a responsive image. Default to 25% width for new images as requested.
                 sanitized = sanitized.replace("<img", "<img width=\"25%\" ")
                 if (!sanitized.contains("style=")) {
                     sanitized = sanitized.replace("<img", "<img style=\"max-width:25%;height:auto;\" ")
                 }
            } else {
                // Old image, force a default but make it look better
                sanitized = sanitized.replace("<img", "<img width=\"300\" height=\"200\" ")
            }
        }

        if (sanitized != html) {
            NotesLogger.v("HTML_SANITIZER", "Sanitized HTML. Original: ${html.length} chars, New: ${sanitized.length} chars")
        }

        return sanitized
    }

}
