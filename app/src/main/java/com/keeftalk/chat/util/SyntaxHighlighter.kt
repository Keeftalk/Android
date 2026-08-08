package com.keeftalk.chat.util

import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.util.Locale

/**
 * Ultimate, high-performance syntax highlighter.
 * Optimized for LazyColumn virtualization and real-time editing.
 */
object SyntaxHighlighter {

    private val cache = LruCache<String, AnnotatedString>(200)

    enum class Token {
        COMMENT, STRING, KEYWORD, CONTROL_FLOW, TYPE, FUNCTION, CONSTANT, 
        NUMBER, OPERATOR, DELIMITER, ANNOTATION, TAG, ATTRIBUTE, 
        PREPROCESSOR, BUILTIN, ESCAPE, COMPOSE, ANDROID,
        MD_HEADER, MD_BOLD, MD_ITALIC, MD_LINK, MD_CODE, MD_BLOCK_START
    }

    private fun getStyle(token: Token, isDark: Boolean): SpanStyle {
        return when (token) {
            Token.COMMENT -> if (isDark) SpanStyle(color = Color(0xFF5C6370), fontStyle = FontStyle.Italic) 
                            else SpanStyle(color = Color(0xFFA0A1A7), fontStyle = FontStyle.Italic)
            
            Token.STRING -> if (isDark) SpanStyle(color = Color(0xFF98C379)) else SpanStyle(color = Color(0xFF50A14F))
            
            Token.KEYWORD -> if (isDark) SpanStyle(color = Color(0xFFC678DD), fontWeight = FontWeight.Bold) 
                            else SpanStyle(color = Color(0xFFA626A4), fontWeight = FontWeight.Bold)
            
            Token.CONTROL_FLOW -> if (isDark) SpanStyle(color = Color(0xFFC678DD), fontWeight = FontWeight.Black)
                                 else SpanStyle(color = Color(0xFFA626A4), fontWeight = FontWeight.Black)
            
            Token.TYPE -> if (isDark) SpanStyle(color = Color(0xFFE5C07B)) else SpanStyle(color = Color(0xFFC18401))
            
            Token.FUNCTION -> if (isDark) SpanStyle(color = Color(0xFF61AFEF)) else SpanStyle(color = Color(0xFF4078F2))
            
            Token.CONSTANT -> if (isDark) SpanStyle(color = Color(0xFFD19A66), fontWeight = FontWeight.Bold)
                              else SpanStyle(color = Color(0xFF986801), fontWeight = FontWeight.Bold)
            
            Token.NUMBER -> if (isDark) SpanStyle(color = Color(0xFFD19A66)) else SpanStyle(color = Color(0xFF986801))
            
            Token.OPERATOR -> if (isDark) SpanStyle(color = Color(0xFF56B6C2)) else SpanStyle(color = Color(0xFF0184BC))
            
            Token.DELIMITER -> if (isDark) SpanStyle(color = Color(0xFFABB2BF)) else SpanStyle(color = Color(0xFF383A42))
            
            Token.ANNOTATION -> if (isDark) SpanStyle(color = Color(0xFFE06C75)) else SpanStyle(color = Color(0xFFE45649))
            
            Token.TAG -> if (isDark) SpanStyle(color = Color(0xFFE06C75)) else SpanStyle(color = Color(0xFFE45649))
            
            Token.ATTRIBUTE -> if (isDark) SpanStyle(color = Color(0xFFD19A66)) else SpanStyle(color = Color(0xFF986801))
            
            Token.PREPROCESSOR -> if (isDark) SpanStyle(color = Color(0xFFC678DD)) else SpanStyle(color = Color(0xFFA626A4))
            
            Token.BUILTIN -> if (isDark) SpanStyle(color = Color(0xFF56B6C2), fontWeight = FontWeight.Bold)
                             else SpanStyle(color = Color(0xFF0184BC), fontWeight = FontWeight.Bold)
            
            Token.ESCAPE -> if (isDark) SpanStyle(color = Color(0xFF56B6C2)) else SpanStyle(color = Color(0xFF0184BC))

            Token.COMPOSE -> if (isDark) SpanStyle(color = Color(0xFF7EE787), fontWeight = FontWeight.Bold)
                            else SpanStyle(color = Color(0xFF22863A), fontWeight = FontWeight.Bold)
            
            Token.ANDROID -> if (isDark) SpanStyle(color = Color(0xFF61AFEF), fontStyle = FontStyle.Italic)
                            else SpanStyle(color = Color(0xFF4078F2), fontStyle = FontStyle.Italic)

            Token.MD_HEADER -> SpanStyle(color = Color(0xFFE06C75), fontWeight = FontWeight.Black)
            Token.MD_BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            Token.MD_ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            Token.MD_LINK -> SpanStyle(color = Color(0xFF61AFEF), textDecoration = TextDecoration.Underline)
            Token.MD_CODE -> if (isDark) SpanStyle(color = Color(0xFFE5C07B), background = Color(0xFF2C313A))
                             else SpanStyle(color = Color(0xFFC18401), background = Color(0xFFF0F0F0))
            Token.MD_BLOCK_START -> if (isDark) SpanStyle(color = Color(0xFF5C6370), fontWeight = FontWeight.Bold)
                                   else SpanStyle(color = Color(0xFFA0A1A7), fontWeight = FontWeight.Bold)
        }
    }

    private val ControlFlowKeywords = "if|else|while|for|return|break|continue|switch|case|default|try|catch|finally|throw|when|do|yield|await|async"
    private val CommonKeywords = "class|interface|struct|enum|fun|function|def|var|val|let|const|public|private|protected|internal|static|final|abstract|override|open|import|package|namespace|using|new|this|super|type|trait|impl|pub|fn|mod|use|by"
    private val Builtins = "println|print|log|assert|map|filter|reduce|ArrayList|HashMap|HashSet|String|Int|Long|Double|Float|Boolean|Any|Unit|Nothing|null|true|false"
    
    private val ComposeKeywords = "Composable|remember|LaunchedEffect|SideEffect|DisposableEffect|rememberUpdatedState|rememberCoroutineScope|mutableStateOf|mutableIntStateOf|derivedStateOf|collectAsState|CompositionLocalProvider|Modifier"
    private val AndroidKeywords = "Activity|Fragment|Context|Intent|Bundle|ViewModel|Lifecycle|LiveData|Flow|StateFlow|SharedFlow|NavHost|NavController|Scaffold|TopAppBar|Button|Text|Column|Row|Box|LazyColumn|LazyRow"

    private val combinedRegexes = mutableMapOf<String, Regex>()

    private fun getCombinedRegex(ext: String): Regex {
        return combinedRegexes.getOrPut(ext) {
            val isMarkdown = ext == "md" || ext == "markdown"
            val isSql = ext == "sql"
            val isWeb = ext == "html" || ext == "xml" || ext == "svg"
            val isCss = ext == "css"

            val parts = mutableListOf<String>()

            if (isMarkdown) {
                parts.add("^(#+.*)") // MD_HEADER (1)
                parts.add("(\\*\\*[^*]+\\*\\*|__[^_]+__)") // MD_BOLD (2)
                parts.add("(\\*[^*]+\\*|_[^_]+_)") // MD_ITALIC (3)
                parts.add("(\\[[^\\]]+\\]\\([^\\)]+\\))") // MD_LINK (4)
                parts.add("(`[^`]+`)") // MD_CODE (5)
                parts.add("(^```\\w*)") // MD_BLOCK_START (6)
                // Dummy for rest
                repeat(Token.entries.size - 6) { parts.add("(a^)") }
            } else {
                // Token.COMMENT (1)
                parts.add("(${if (ext == "py" || ext == "sh") "#.*" else if (ext == "sql") "--.*|/\\*[\\s\\S]*?\\*/" else if (isWeb) "<!--[\\s\\S]*?-->" else "//.*|/\\*[\\s\\S]*?\\*/"})")
                // Token.STRING (2)
                parts.add("(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')")
                // Token.KEYWORD (3)
                parts.add("(\\b(?:$CommonKeywords)\\b)")
                // Token.CONTROL_FLOW (4)
                parts.add("(\\b(?:$ControlFlowKeywords)\\b)")
                // Token.TYPE (5)
                parts.add("(\\b[A-Z][a-zA-Z0-9]*\\b)")
                // Token.FUNCTION (6)
                parts.add("(\\b[a-z_][a-zA-Z0-9_]*\\b(?=\\s*\\())")
                // Token.CONSTANT (7)
                parts.add("(\\b[A-Z_][A-Z0-9_]{2,}\\b)")
                // Token.NUMBER (8)
                parts.add("(\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b|0x[0-9a-fA-F]+)")
                // Token.OPERATOR (9)
                parts.add("([+\\-*/%&|^~!=<>?:]+)")
                // Token.DELIMITER (10)
                parts.add("([{}()\\[\\];,])")
                // Token.ANNOTATION (11)
                parts.add(if (!isWeb) "(@[a-zA-Z0-9_\\.]+)" else "(a^)")
                // Token.TAG (12)
                parts.add(if (isWeb) "(</?[a-zA-Z][\\w-]*|/?>|&\\w+;)" else "(a^)")
                // Token.ATTRIBUTE (13)
                parts.add(if (isWeb) "(\\b[a-zA-Z0-9_-]+(?==))" else if (isCss) "([a-zA-Z-]+(?=\\s*:))" else "(a^)")
                // Token.PREPROCESSOR (14)
                parts.add(if (ext == "c" || ext == "cpp") "(#\\w+)" else "(a^)")
                // Token.BUILTIN (15)
                parts.add("(\\b(?:$Builtins)\\b)")
                // Token.ESCAPE (16)
                parts.add("(\\\\[nrt\"'\\\\])")
                // Token.COMPOSE (17)
                parts.add("(\\b(?:$ComposeKeywords)\\b)")
                // Token.ANDROID (18)
                parts.add("(\\b(?:$AndroidKeywords)\\b)")
                
                repeat(Token.entries.size - 18) { parts.add("(a^)") }
            }

            Regex(parts.joinToString("|"), if (isSql) setOf(RegexOption.IGNORE_CASE) else emptySet())
        }
    }

    /**
     * Highlights a single line of code.
     * contextExt allows forcing a language (useful for MD blocks)
     */
    fun highlightLine(line: String, ext: String, isDark: Boolean): AnnotatedString {
        if (line.isEmpty()) return AnnotatedString("")
        
        val cacheKey = "L|$ext|$isDark|${line.hashCode()}"
        val cached = cache.get(cacheKey)
        if (cached != null) return cached

        val regex = getCombinedRegex(ext)
        val result = buildAnnotatedString {
            var lastIndex = 0
            regex.findAll(line).forEach { match ->
                if (match.range.first > lastIndex) {
                    append(line.substring(lastIndex, match.range.first))
                }
                
                var matched = false
                for (i in 0 until match.groups.size - 1) {
                    val group = match.groups[i + 1]
                    if (group != null) {
                        pushStyle(getStyle(Token.entries[i], isDark))
                        append(match.value)
                        pop()
                        matched = true
                        break
                    }
                }
                if (!matched) append(match.value)
                lastIndex = match.range.last + 1
            }
            if (lastIndex < line.length) append(line.substring(lastIndex))
        }
        
        cache.put(cacheKey, result)
        return result
    }

    /**
     * Highlights the entire document.
     * Intelligent enough to handle MD code blocks with nested highlighting.
     */
    fun highlight(code: String, extension: String, isDark: Boolean): AnnotatedString {
        if (code.isBlank()) return AnnotatedString("")
        val ext = extension.lowercase(Locale.ROOT)
        
        // For Markdown, we do a multi-pass to handle blocks
        if (ext == "md" || ext == "markdown") {
            return highlightMarkdownWithBlocks(code, isDark)
        }

        // Standard high-performance path
        val cacheKey = "D|$ext|$isDark|${code.hashCode()}"
        val cached = cache.get(cacheKey)
        if (cached != null) return cached

        val regex = getCombinedRegex(ext)
        val result = buildAnnotatedString {
            var lastIndex = 0
            regex.findAll(code).forEach { match ->
                if (match.range.first > lastIndex) {
                    append(code.substring(lastIndex, match.range.first))
                }
                var matched = false
                for (i in 0 until match.groups.size - 1) {
                    val group = match.groups[i + 1]
                    if (group != null) {
                        pushStyle(getStyle(Token.entries[i], isDark))
                        append(match.value)
                        pop()
                        matched = true
                        break
                    }
                }
                if (!matched) append(match.value)
                lastIndex = match.range.last + 1
            }
            if (lastIndex < code.length) append(code.substring(lastIndex))
        }
        
        cache.put(cacheKey, result)
        return result
    }

    private fun highlightMarkdownWithBlocks(code: String, isDark: Boolean): AnnotatedString {
        val lines = code.lines()
        return buildAnnotatedString {
            var currentBlockExt: String? = null
            
            lines.forEachIndexed { index, line ->
                if (line.startsWith("```")) {
                    pushStyle(getStyle(Token.MD_BLOCK_START, isDark))
                    append(line)
                    pop()
                    currentBlockExt = if (currentBlockExt == null) {
                        line.removePrefix("```").trim().takeIf { it.isNotEmpty() } ?: "txt"
                    } else {
                        null
                    }
                } else if (currentBlockExt != null) {
                    // Nested highlighting inside code block
                    append(highlightLine(line, currentBlockExt!!, isDark))
                } else {
                    // Standard MD highlighting
                    append(highlightLine(line, "md", isDark))
                }
                
                if (index < lines.size - 1) append("\n")
            }
        }
    }
}
