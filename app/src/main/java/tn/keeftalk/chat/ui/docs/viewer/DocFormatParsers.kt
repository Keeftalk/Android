package tn.keeftalk.chat.ui.docs.viewer

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipFile

data class DocChapter(
    val title: String,
    val paragraphs: List<String>
)

data class DocOutlineItem(
    val level: Int,
    val title: String,
    val lineNumber: Int,
    val id: String = ""
)

object DocFormatParsers {
    private const val TAG = "DocFormatParsers"

    // ---------------------------------------------------------------------------------------------
    // Heading ids. slugify() MUST stay byte-for-byte equivalent to slugify() in assets/doc_viewer.js
    // (same character class, same whitespace rule, same "-N" counter) - rule 4 of the project notes.
    // The WebView also reports the real outline from the rendered DOM (DocMarkdownView.onOutlineReady),
    // so this Kotlin outline is the fallback used before/without a render.
    // ---------------------------------------------------------------------------------------------
    private val SLUG_STRIP = Regex("[^\\p{L}\\p{M}\\p{N} \\t-]")
    private val SLUG_SPACES = Regex("[ \\t]+")
    private val SLUG_DASHES = Regex("-+")

    fun slugify(raw: String): String {
        val s = raw.lowercase()
            .replace(SLUG_STRIP, "")
            .trim()
            .replace(SLUG_SPACES, "-")
            .replace(SLUG_DASHES, "-")
        return s.ifEmpty { "heading" }
    }

    /** Same rule as FRONT_MATTER in doc_viewer.js (YAML block at the very top must contain "key:"). */
    private val FRONT_MATTER =
        Regex("^\\uFEFF?---[ \\t]*\\r?\\n(?=[\\w\"'-][^\\r\\n]*:)[\\s\\S]*?\\r?\\n(?:---|\\.\\.\\.)[ \\t]*(?:\\r?\\n|$)")
    private val ATX = Regex("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?(?:[ \\t]+#+)?[ \\t]*$")
    private val FENCE = Regex("^ {0,3}(`{3,}|~{3,})(.*)$")
    private val SETEXT = Regex("^ {0,3}(=+|-+)[ \\t]*$")
    private val HR = Regex("^ {0,3}([-*_])(?:[ \\t]*\\1){2,}[ \\t]*$")
    private val LIST_ITEM = Regex("^ {0,3}(?:[-*+]|\\d{1,9}[.)])(?:[ \\t]|$)")
    private val MD_LINK = Regex("!?\\[([^\\]]*)]\\([^)]*\\)")
    private val MD_NOISE = Regex("[*_`~]|<[^>]+>")

    /**
     * Builds the Table of Contents from Markdown headings.
     * Handles: front matter, fenced code (``` and ~~~), HTML comments, indented code, ATX headings
     * (needs a space after the #'s, so "#hashtag" is NOT a heading), setext headings, closing #'s,
     * and unique ids with the same "-1, -2" counter as the renderer.
     */
    fun extractMarkdownOutline(markdownText: String): List<DocOutlineItem> {
        if (markdownText.isBlank()) return emptyList()

        val items = ArrayList<DocOutlineItem>()
        val used = HashSet<String>()
        val lines = markdownText.lines()

        fun addHeading(level: Int, rawText: String, lineNo: Int) {
            val base = slugify(rawText)
            var id = base
            var counter = 1
            while (!used.add(id)) {          // add() == false -> already taken
                id = "$base-$counter"
                counter++
            }
            val title = rawText.replace(MD_LINK, "\$1").replace(MD_NOISE, "").trim()
            if (title.isNotEmpty()) items.add(DocOutlineItem(level, title, lineNo, id))
        }

        var i = 0
        FRONT_MATTER.find(markdownText)?.let { m ->
            i = m.value.count { it == '\n' } + (if (m.value.endsWith("\n")) 0 else 1)
        }

        var fenceChar = ' '
        var fenceLen = 0
        var inFence = false
        var inComment = false
        val para = ArrayList<String>()
        var paraStart = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            if (inFence) {
                if (trimmed.length >= fenceLen && trimmed.all { it == fenceChar }) inFence = false
                i++; continue
            }
            if (inComment) {
                if (line.contains("-->")) inComment = false
                i++; continue
            }
            if (trimmed.isEmpty()) { para.clear(); i++; continue }

            val atx = ATX.matchEntire(line)
            if (atx != null) {
                addHeading(atx.groupValues[1].length, atx.groupValues[2], i)
                para.clear(); i++; continue
            }

            val fence = FENCE.matchEntire(line)
            if (fence != null && !(fence.groupValues[1][0] == '`' && fence.groupValues[2].contains('`'))) {
                inFence = true
                fenceChar = fence.groupValues[1][0]
                fenceLen = fence.groupValues[1].length
                para.clear(); i++; continue
            }

            if (trimmed.startsWith("<!--")) {
                if (!trimmed.contains("-->")) inComment = true
                para.clear(); i++; continue
            }

            val setext = SETEXT.matchEntire(line)
            if (setext != null && para.isNotEmpty()) {
                val level = if (setext.groupValues[1][0] == '=') 1 else 2
                addHeading(level, para.joinToString("\n"), paraStart)
                para.clear(); i++; continue
            }

            val indent = line.length - line.trimStart().length
            val isBlockStart = trimmed.startsWith(">") || trimmed.startsWith("|") || trimmed.startsWith("<") ||
                    HR.matches(line) || LIST_ITEM.containsMatchIn(line)
            if ((indent >= 4 && para.isEmpty()) || isBlockStart) {
                para.clear(); i++; continue
            }

            if (para.isEmpty()) paraStart = i
            para.add(trimmed)
            i++
        }
        return items
    }

    // ---------------------------------------------------------------------------------------------
    // XML helpers (namespace-agnostic: "w:p" and "p" both match local name "p")
    // ---------------------------------------------------------------------------------------------
    private fun xml(s: InputStream): XmlPullParser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(s, null)
    }

    private fun local(p: XmlPullParser): String = (p.name ?: "").substringAfter(':')

    private fun attr(p: XmlPullParser, local: String): String? {
        for (i in 0 until p.attributeCount) {
            if ((p.getAttributeName(i) ?: "").substringAfter(':') == local) return p.getAttributeValue(i)
        }
        return null
    }

    private fun skipSubtree(p: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // EPUB: chapters in SPINE order (container.xml -> OPF -> spine), not zip order.
    // ---------------------------------------------------------------------------------------------
    fun extractEpubChapters(file: File): List<DocChapter> {
        if (!file.exists() || !file.name.endsWith(".epub", ignoreCase = true)) return emptyList()
        return try {
            ZipFile(file).use { zip ->
                val names = epubSpine(zip).ifEmpty {
                    zip.entries().asSequence()
                        .filter { !it.isDirectory && isHtmlName(it.name) }
                        .map { it.name }.sorted().toList()
                }
                val out = ArrayList<DocChapter>()
                for (name in names) {
                    val entry = zip.getEntry(name) ?: continue
                    val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val chapter = htmlToChapter(html, out.size + 1) ?: continue
                    out.add(chapter)
                }
                out
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting EPUB text", e)
            emptyList()
        }
    }

    private fun isHtmlName(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith(".html") || n.endsWith(".xhtml") || n.endsWith(".htm")
    }

    private fun epubSpine(zip: ZipFile): List<String> {
        val container = zip.getEntry("META-INF/container.xml") ?: return emptyList()
        var opfPath: String? = null
        zip.getInputStream(container).use { s ->
            val p = xml(s)
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.START_TAG && local(p) == "rootfile") {
                    opfPath = attr(p, "full-path")
                    break
                }
            }
        }
        val opf = opfPath ?: return emptyList()
        val opfEntry = zip.getEntry(opf) ?: return emptyList()
        val opfDir = opf.substringBeforeLast('/', "")

        val manifest = HashMap<String, String>()
        val navIds = HashSet<String>()
        val spine = ArrayList<String>()
        zip.getInputStream(opfEntry).use { s ->
            val p = xml(s)
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType != XmlPullParser.START_TAG) continue
                when (local(p)) {
                    "item" -> {
                        val id = attr(p, "id") ?: continue
                        val href = attr(p, "href") ?: continue
                        if ((attr(p, "media-type") ?: "").contains("html")) {
                            manifest[id] = resolveZipPath(opfDir, href)
                            if ((attr(p, "properties") ?: "").split(' ').contains("nav")) navIds.add(id)
                        }
                    }
                    "itemref" -> attr(p, "idref")?.let { spine.add(it) }
                }
            }
        }
        return spine.filter { it !in navIds }.mapNotNull { manifest[it] }.distinct()
    }

    private fun resolveZipPath(baseDir: String, href: String): String {
        val path = href.substringBefore('#')
        val decoded = try {
            URLDecoder.decode(path.replace("+", "%2B"), "UTF-8")
        } catch (e: Exception) {
            path
        }
        val parts = ArrayList<String>()
        val joined = if (baseDir.isEmpty()) decoded else "$baseDir/$decoded"
        for (seg in joined.split('/')) {
            when (seg) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts.add(seg)
            }
        }
        return parts.joinToString("/")
    }

    private val RX_HEAD = Regex("<head\\b.*?</head>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val RX_SCRIPT_STYLE = Regex("<(script|style)\\b.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val RX_TITLE = Regex("<title\\b[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val RX_H123 = Regex("<h[1-3]\\b[^>]*>(.*?)</h[1-3]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val RX_BR = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val RX_BLOCK_END = Regex("</(p|div|h[1-6]|li|tr|blockquote|section|article|pre|table)>", RegexOption.IGNORE_CASE)
    private val RX_TAG = Regex("<[^>]*>", RegexOption.DOT_MATCHES_ALL)
    private val RX_WS = Regex("\\s+")

    private fun htmlToChapter(html: String, index: Int): DocChapter? {
        val headTitle = RX_TITLE.find(html)?.groupValues?.get(1)
        val body = html.replace(RX_HEAD, "").replace(RX_SCRIPT_STYLE, "")
        val headingTitle = RX_H123.find(body)?.groupValues?.get(1)

        val text = decodeEntities(
            body.replace(RX_BR, "\n").replace(RX_BLOCK_END, "\n\n").replace(RX_TAG, "")
        )
        val paragraphs = text.split('\n')
            .map { it.replace('\uFFFC', ' ').replace(RX_WS, " ").trim() }
            .filter { it.isNotEmpty() }
        if (paragraphs.isEmpty()) return null

        fun clean(s: String?): String? =
            s?.let { decodeEntities(it.replace(RX_TAG, "")).replace(RX_WS, " ").trim() }?.takeIf { it.isNotEmpty() }

        val title = clean(headingTitle) ?: clean(headTitle)
        ?: paragraphs.firstOrNull { it.length < 80 } ?: "Chapter $index"
        return DocChapter(title, paragraphs)
    }

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "mdash" to "—", "ndash" to "–", "hellip" to "…", "lsquo" to "‘", "rsquo" to "’",
        "ldquo" to "“", "rdquo" to "”", "laquo" to "«", "raquo" to "»", "copy" to "©",
        "reg" to "®", "deg" to "°", "middot" to "·", "bull" to "•", "times" to "×"
    )
    private val ENTITY = Regex("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")

    private fun codePointToString(cp: Int?): String? =
        if (cp != null && cp in 1..0x10FFFF) String(Character.toChars(cp)) else null

    private fun decodeEntities(s: String): String = ENTITY.replace(s) { m ->
        val g = m.groupValues[1]
        when {
            g.startsWith("#x") || g.startsWith("#X") -> codePointToString(g.substring(2).toIntOrNull(16)) ?: m.value
            g.startsWith("#") -> codePointToString(g.substring(1).toIntOrNull()) ?: m.value
            else -> NAMED_ENTITIES[g] ?: m.value
        }
    }

    // ---------------------------------------------------------------------------------------------
    // DOCX: real XML walk (the old regex version merged paragraphs, lost tabs/breaks/tables and
    // showed "&amp;" literally). Output is GitHub-flavoured Markdown so DOCX renders through the
    // same WebView pipeline (headings, bold/italic, lists, tables, links, images, TOC).
    // ---------------------------------------------------------------------------------------------
    private class DocxSeg(
        val text: String,
        val bold: Boolean,
        val italic: Boolean,
        val strike: Boolean,
        val href: String?,
        val image: String? = null
    )

    private class DocxBlock(val text: String, val isListItem: Boolean)

    /** Plain-text paragraphs (used by the legacy DocDocxView fallback). */
    fun extractDocxParagraphs(file: File): List<String> =
        convertDocx(file, markdown = false).map { it.text }.filter { it.isNotBlank() }

    /** Markdown for the WebView renderer. Blank string if the file can't be read. */
    fun extractDocxMarkdown(file: File): String {
        val blocks = convertDocx(file, markdown = true)
        val sb = StringBuilder()
        blocks.forEachIndexed { i, b ->
            if (i > 0) sb.append(if (b.isListItem && blocks[i - 1].isListItem) "\n" else "\n\n")
            sb.append(b.text)
        }
        return sb.toString()
    }

    private fun convertDocx(file: File, markdown: Boolean): List<DocxBlock> {
        if (!file.exists() || !file.name.endsWith(".docx", ignoreCase = true)) return emptyList()
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry("word/document.xml") ?: return@use emptyList<DocxBlock>()
                val walker = DocxWalker(readDocxStyles(zip), readDocxNumbering(zip), readDocxRels(zip), markdown)
                zip.getInputStream(entry).use { walker.walk(xml(it)) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting DOCX", e)
            emptyList()
        }
    }

    private fun readDocxStyles(zip: ZipFile): Map<String, String> {
        val out = HashMap<String, String>()
        val e = zip.getEntry("word/styles.xml") ?: return out
        zip.getInputStream(e).use { s ->
            val p = xml(s)
            var current: String? = null
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType != XmlPullParser.START_TAG) continue
                when (local(p)) {
                    "style" -> current = attr(p, "styleId")
                    "name" -> current?.let { id -> attr(p, "val")?.let { out[id] = it } }
                }
            }
        }
        return out
    }

    /** numId -> (ilvl -> numFmt) e.g. "bullet", "decimal". */
    private fun readDocxNumbering(zip: ZipFile): Map<String, Map<Int, String>> {
        val e = zip.getEntry("word/numbering.xml") ?: return emptyMap()
        val abstractLevels = HashMap<String, MutableMap<Int, String>>()
        val numToAbstract = HashMap<String, String>()
        zip.getInputStream(e).use { s ->
            val p = xml(s)
            var absId: String? = null
            var numId: String? = null
            var lvl = 0
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.END_TAG) {
                    when (local(p)) {
                        "abstractNum" -> absId = null
                        "num" -> numId = null
                    }
                    continue
                }
                if (p.eventType != XmlPullParser.START_TAG) continue
                when (local(p)) {
                    "abstractNum" -> absId = attr(p, "abstractNumId")
                    "lvl" -> lvl = attr(p, "ilvl")?.toIntOrNull() ?: 0
                    "numFmt" -> absId?.let { a ->
                        abstractLevels.getOrPut(a) { HashMap() }[lvl] = attr(p, "val") ?: "decimal"
                    }
                    "num" -> numId = attr(p, "numId")
                    "abstractNumId" -> numId?.let { n -> attr(p, "val")?.let { numToAbstract[n] = it } }
                }
            }
        }
        val out = HashMap<String, Map<Int, String>>()
        numToAbstract.forEach { (n, a) -> abstractLevels[a]?.let { out[n] = it } }
        return out
    }

    /** relationship id -> (target, isExternal) */
    private fun readDocxRels(zip: ZipFile): Map<String, Pair<String, Boolean>> {
        val out = HashMap<String, Pair<String, Boolean>>()
        val e = zip.getEntry("word/_rels/document.xml.rels") ?: return out
        zip.getInputStream(e).use { s ->
            val p = xml(s)
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                if (p.eventType == XmlPullParser.START_TAG && local(p) == "Relationship") {
                    val id = attr(p, "Id") ?: continue
                    val target = attr(p, "Target") ?: continue
                    out[id] = Pair(target, attr(p, "TargetMode") == "External")
                }
            }
        }
        return out
    }

    private val MD_ESCAPE = Regex("([\\\\`*_\\[\\]<>|\$~])")
    private val BLOCK_START = Regex("^(#{1,6}(?:\\s|$)|[-+](?:\\s|$)|\\d+[.)](?:\\s|$))")
    private val IMAGE_EXT = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg")
    private val HEADING_NAME = Regex("^heading\\s*(\\d)")

    private fun mdEscape(s: String): String = s.replace(MD_ESCAPE, "\\\\\$1")

    private fun escapeBlockStart(t: String): String {
        val m = BLOCK_START.find(t) ?: return t
        val g = m.groupValues[1]
        if (!g[0].isDigit()) return "\\" + t
        val idx = g.indexOfFirst { it == '.' || it == ')' }
        return t.substring(0, idx) + "\\" + t.substring(idx)
    }

    private class DocxWalker(
        val styleNames: Map<String, String>,
        val numFormats: Map<String, Map<Int, String>>,
        val rels: Map<String, Pair<String, Boolean>>,
        val markdown: Boolean
    ) {
        val blocks = ArrayList<DocxBlock>()

        // paragraph state
        var segs = ArrayList<DocxSeg>()
        var pStyle: String? = null
        var numId: String? = null
        var ilvl = 0
        var outlineLvl = -1
        var inPPr = false

        // run state
        var inRun = false
        var inRPr = false
        var bold = false
        var italic = false
        var strike = false
        var href: String? = null

        // table state (only the outermost table becomes a Markdown table; nested ones flatten into the cell)
        var tblDepth = 0
        var rows = ArrayList<List<String>>()
        var rowCells = ArrayList<String>()
        var cellParas = ArrayList<String>()
        var cellSpan = 1

        fun walk(p: XmlPullParser): List<DocxBlock> {
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) onStart(p) else if (ev == XmlPullParser.END_TAG) onEnd(p)
                ev = p.next()
            }
            return blocks
        }

        private fun onOff(p: XmlPullParser): Boolean {
            val v = attr(p, "val")
            return v == null || !(v == "0" || v == "false" || v == "off")
        }

        private fun addText(t: String) {
            if (t.isNotEmpty()) segs.add(DocxSeg(t, bold, italic, strike, href))
        }

        private fun onStart(p: XmlPullParser) {
            when (local(p)) {
                "Fallback" -> skipSubtree(p)               // mc:AlternateContent duplicates text-box content
                "tbl" -> { tblDepth++; if (tblDepth == 1) rows = ArrayList() }
                "tr" -> if (tblDepth == 1) rowCells = ArrayList()
                "tc" -> if (tblDepth == 1) { cellParas = ArrayList(); cellSpan = 1 }
                "gridSpan" -> if (tblDepth == 1) cellSpan = attr(p, "val")?.toIntOrNull() ?: 1
                "p" -> { segs = ArrayList(); pStyle = null; numId = null; ilvl = 0; outlineLvl = -1; inPPr = false }
                "pPr" -> inPPr = true
                "pStyle" -> if (inPPr) pStyle = attr(p, "val")
                "numId" -> if (inPPr) numId = attr(p, "val")
                "ilvl" -> if (inPPr) ilvl = attr(p, "val")?.toIntOrNull() ?: 0
                "outlineLvl" -> if (inPPr) outlineLvl = attr(p, "val")?.toIntOrNull() ?: -1
                "r" -> { inRun = true; bold = false; italic = false; strike = false }
                "rPr" -> if (inRun) inRPr = true
                "b" -> if (inRPr) bold = onOff(p)
                "i" -> if (inRPr) italic = onOff(p)
                "strike" -> if (inRPr) strike = onOff(p)
                "hyperlink" -> {
                    val rid = attr(p, "id")
                    href = if (rid != null) rels[rid]?.takeIf { it.second }?.first else null
                }
                "t" -> if (inRun) addText(p.nextText() ?: "")
                "tab" -> if (inRun && !inPPr) addText(" ")
                "br", "cr" -> if (inRun && attr(p, "type") != "page") segs.add(DocxSeg("\n", false, false, false, null))
                "noBreakHyphen" -> if (inRun) addText("-")
                "blip" -> if (markdown) {
                    val target = attr(p, "embed")?.let { rels[it] }
                    if (target != null && !target.second &&
                        IMAGE_EXT.contains(target.first.substringAfterLast('.', "").lowercase())
                    ) {
                        segs.add(DocxSeg("", false, false, false, null, "/docx/" + target.first))
                    }
                }
            }
        }

        private fun onEnd(p: XmlPullParser) {
            when (local(p)) {
                "pPr" -> inPPr = false
                "rPr" -> inRPr = false
                "r" -> { inRun = false; inRPr = false }
                "hyperlink" -> href = null
                "p" -> endParagraph()
                "tc" -> if (tblDepth == 1) {
                    rowCells.add(cellParas.joinToString(if (markdown) "<br>" else " "))
                    repeat(cellSpan - 1) { rowCells.add("") }
                }
                "tr" -> if (tblDepth == 1) rows.add(rowCells)
                "tbl" -> { if (tblDepth == 1) emitTable(); tblDepth-- }
            }
        }

        private fun headingLevel(): Int {
            val style = pStyle
            val name = (style?.let { styleNames[it] } ?: style ?: "").lowercase()
            HEADING_NAME.find(name)?.let { return it.groupValues[1].toInt().coerceIn(1, 6) }
            if (name == "title") return 1
            if (outlineLvl in 0..5) return outlineLvl + 1
            return 0
        }

        private fun endParagraph() {
            val level = headingLevel()
            val inCell = tblDepth >= 1
            val text = renderSegs(level > 0, if (inCell) "<br>" else "  \n")
            segs = ArrayList()
            if (text.isBlank()) return
            if (inCell) { cellParas.add(text); return }

            val nid = numId
            when {
                level > 0 -> blocks.add(DocxBlock(if (markdown) "#".repeat(level) + " " + text else text, false))
                nid != null && nid != "0" -> {
                    val fmt = numFormats[nid]?.get(ilvl) ?: "bullet"
                    val indent = "    ".repeat(ilvl.coerceAtLeast(0))
                    val marker = if (!markdown) "•" else if (fmt == "bullet" || fmt == "none") "-" else "1."
                    blocks.add(DocxBlock("$indent$marker $text", true))
                }
                else -> blocks.add(DocxBlock(if (markdown) escapeBlockStart(text) else text, false))
            }
        }

        private fun renderSegs(isHeading: Boolean, breakStr: String): String {
            val merged = ArrayList<DocxSeg>()
            for (s in segs) {
                val last = merged.lastOrNull()
                if (last != null && last.image == null && s.image == null && last.text != "\n" && s.text != "\n" &&
                    last.bold == s.bold && last.italic == s.italic && last.strike == s.strike && last.href == s.href
                ) {
                    merged[merged.lastIndex] = DocxSeg(last.text + s.text, s.bold, s.italic, s.strike, s.href)
                } else {
                    merged.add(s)
                }
            }
            val sb = StringBuilder()
            for (s in merged) {
                when {
                    s.image != null -> sb.append("![](").append(s.image).append(")")
                    s.text == "\n" -> sb.append(if (markdown) breakStr else "\n")
                    !markdown -> sb.append(s.text)
                    else -> sb.append(formatSeg(s, isHeading))
                }
            }
            return sb.toString().trim()
        }

        private fun formatSeg(s: DocxSeg, isHeading: Boolean): String {
            val raw = s.text
            val core = raw.trim()
            if (core.isEmpty()) return if (raw.isEmpty()) "" else " "
            val lead = raw.substring(0, raw.length - raw.trimStart().length)
            val trail = raw.substring(raw.trimEnd().length)
            var t = mdEscape(core)
            if (s.strike) t = "~~$t~~"
            if (!isHeading) {
                t = when {
                    s.bold && s.italic -> "***$t***"
                    s.bold -> "**$t**"
                    s.italic -> "*$t*"
                    else -> t
                }
            }
            val link = s.href
            if (link != null) t = "[$t](" + link.replace(" ", "%20").replace(")", "%29") + ")"
            return lead + t + trail
        }

        private fun emitTable() {
            val data = rows.filter { r -> r.any { it.isNotBlank() } }
            if (data.isEmpty()) return
            if (!markdown) {
                data.forEach { blocks.add(DocxBlock(it.joinToString(" | "), false)) }
                return
            }
            val cols = data.maxOf { it.size }.coerceAtLeast(1)
            fun row(r: List<String>) = "| " + (0 until cols).joinToString(" | ") { i -> r.getOrElse(i) { "" } } + " |"
            val sb = StringBuilder()
            sb.append(row(data[0])).append('\n')
            sb.append("|").append(" --- |".repeat(cols)).append('\n')
            data.drop(1).forEach { sb.append(row(it)).append('\n') }
            blocks.add(DocxBlock(sb.toString().trimEnd(), false))
        }
    }
}