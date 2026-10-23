package tn.keeftalk.chat.ui.docs.viewer

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.docs.sheets.DocSettingsSheet
import tn.keeftalk.chat.util.SyntaxHighlighter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocViewerScreen(
    viewModel: DocViewerViewModel,
    onBack: () -> Unit,
    onOpenInBrowser: (String) -> Unit
) {
    val doc by viewModel.docFlow.collectAsState()
    val settings by viewModel.docSettingsFlow.collectAsState()
    val bookmarks by viewModel.bookmarksFlow.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val rawContent by viewModel.rawContent.collectAsState()
    val epubChapters by viewModel.epubChapters.collectAsState()
    val docxParagraphs by viewModel.docxParagraphs.collectAsState()
    val outlineItems by viewModel.outlineItems.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showOutlineSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }
    var isPreviewMode by remember { mutableStateOf(true) }
    var isDocumentDarkTheme by remember { mutableStateOf(false) }
    var scrollToHeadingId by remember { mutableStateOf<String?>(null) }

    val themeBgColor = if (isDocumentDarkTheme) Color(0xFF1E1E1E) else Color.White
    val themeTextColor = if (isDocumentDarkTheme) Color.White else Color.Black
    val isDarkTheme = isDocumentDarkTheme

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search document...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = doc?.title ?: "Document Viewer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            doc?.format?.let {
                                Text(
                                    text = "${it.name} · ${doc?.percentProgress ?: 0}% read",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Document-Only Theme Toggle (Sun/Moon icon)
                        IconButton(onClick = { isDocumentDarkTheme = !isDocumentDarkTheme }) {
                            Icon(
                                imageVector = if (isDocumentDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Document Theme",
                                tint = LocalContentColor.current
                            )
                        }

                        if (doc?.format == DocFormat.MARKDOWN || doc?.format == DocFormat.HTML) {
                            IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                                Icon(
                                    imageVector = if (isPreviewMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPreviewMode) "Switch to Code View" else "Switch to Preview Mode",
                                    tint = if (isPreviewMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showOutlineSheet = true }) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = "Outline / Bookmarks")
                        }
                        IconButton(onClick = {
                            val current = doc?.lastReadPage ?: 1
                            viewModel.toggleBookmark(current, doc?.title ?: "")
                        }) {
                            val isBookmarked = bookmarks.any { it.pageOrPosition == (doc?.lastReadPage ?: 1) }
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(themeBgColor)
                .navigationBarsPadding()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                val currentDoc = doc
                if (currentDoc != null) {
                    when (currentDoc.format) {
                        DocFormat.PDF -> {
                            DocPdfView(
                                filePath = currentDoc.filePath,
                                isDark = isDarkTheme,
                                onPageChange = { page, total ->
                                    viewModel.updateProgress(page, total)
                                }
                            )
                        }
                        DocFormat.MARKDOWN -> {
                            if (isPreviewMode) {
                                DocMarkdownView(
                                    content = rawContent ?: "",
                                    textColor = themeTextColor,
                                    fontSizeSp = settings.fontSizeSp,
                                    isDark = isDarkTheme,
                                    scrollToHeadingId = scrollToHeadingId,
                                    onOpenInBrowser = { url -> pendingExternalUrl = url }
                                )
                            } else {
                                DocTextView(
                                    content = rawContent ?: "",
                                    format = currentDoc.format,
                                    settings = settings,
                                    textColor = themeTextColor,
                                    isDark = isDarkTheme,
                                    searchQuery = searchQuery,
                                    onCopy = { text ->
                                        clipboardManager.setText(AnnotatedString(text))
                                    }
                                )
                            }
                        }
                        DocFormat.HTML -> {
                            if (isPreviewMode) {
                                DocHtmlView(
                                    content = rawContent ?: "",
                                    filePath = currentDoc.filePath,
                                    isDark = isDarkTheme,
                                    onOpenInBrowser = { url -> pendingExternalUrl = url }
                                )
                            } else {
                                DocTextView(
                                    content = rawContent ?: "",
                                    format = currentDoc.format,
                                    settings = settings,
                                    textColor = themeTextColor,
                                    isDark = isDarkTheme,
                                    searchQuery = searchQuery,
                                    onCopy = { text ->
                                        clipboardManager.setText(AnnotatedString(text))
                                    }
                                )
                            }
                        }
                        DocFormat.CODE, DocFormat.TXT, DocFormat.OTHER -> {
                            DocTextView(
                                content = rawContent ?: "",
                                format = currentDoc.format,
                                settings = settings,
                                textColor = themeTextColor,
                                isDark = isDarkTheme,
                                searchQuery = searchQuery,
                                onCopy = { text ->
                                    clipboardManager.setText(AnnotatedString(text))
                                }
                            )
                        }
                        DocFormat.EPUB -> {
                            DocEpubView(
                                chapters = epubChapters,
                                textColor = themeTextColor,
                                fontSizeSp = settings.fontSizeSp,
                                onChapterChange = { index, total ->
                                    viewModel.updateProgress(index + 1, total)
                                }
                            )
                        }
                        DocFormat.DOCX -> {
                            DocDocxView(
                                paragraphs = docxParagraphs,
                                textColor = themeTextColor,
                                fontSizeSp = settings.fontSizeSp,
                                onParagraphChange = { index, total ->
                                    viewModel.updateProgress(index + 1, total)
                                }
                            )
                        }
                    }
                }
            }

            if (showSettingsSheet) {
                DocSettingsSheet(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onDismiss = { showSettingsSheet = false }
                )
            }

            if (showOutlineSheet) {
                DocOutlineSheet(
                    outlineItems = outlineItems,
                    bookmarks = bookmarks,
                    onItemClick = { id -> scrollToHeadingId = id },
                    onDismiss = { showOutlineSheet = false }
                )
            }

            // External Link Confirmation Dialog
            pendingExternalUrl?.let { url ->
                AlertDialog(
                    onDismissRequest = { pendingExternalUrl = null },
                    title = { Text("Open External Link") },
                    text = { Text("Do you want to open this link in Keeftalk's My Browser?\n\n$url") },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingExternalUrl = null
                            onOpenInBrowser(url)
                        }) {
                            Text("Open in My Browser")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingExternalUrl = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DocPdfView(
    filePath: String,
    isDark: Boolean,
    onPageChange: (Int, Int) -> Unit
) {
    var pageCount by remember { mutableIntStateOf(0) }
    val file = remember(filePath) { File(filePath) }

    LaunchedEffect(file) {
        if (file.exists()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                        }
                    }
                }
            }
        }
    }

    if (pageCount == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pageCount) { index ->
                LaunchedEffect(index) {
                    onPageChange(index + 1, pageCount)
                }
                PdfPageCard(file = file, pageIndex = index, isDark = isDark)
            }
        }
    }
}

@Composable
fun PdfPageCard(file: File, pageIndex: Int, isDark: Boolean) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file, pageIndex) {
        withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex < renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                val width = (page.width * 1.5f).toInt()
                                val height = (page.height * 1.5f).toInt()
                                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            }
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.7f)
                .background(if (isDark) Color(0xFF121212) else Color.White),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } ?: CircularProgressIndicator()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DocMarkdownView(
    content: String,
    textColor: Color,
    fontSizeSp: Int,
    isDark: Boolean,
    scrollToHeadingId: String?,
    onOpenInBrowser: (String) -> Unit
) {
    val context = LocalContext.current
    
    val isMermaidNeeded = remember(content) { content.contains("```mermaid", ignoreCase = true) }
    var isMermaidInstalled by remember { mutableStateOf(MermaidDownloadManager.isMermaidInstalled(context)) }
    var showMermaidPrompt by remember { mutableStateOf(isMermaidNeeded && !isMermaidInstalled && !MermaidDownloadManager.isDeclined()) }
    val mermaidFile = remember(isMermaidInstalled) { MermaidDownloadManager.getMermaidFile(context) }
    val mermaidScriptUri = if (isMermaidInstalled) "file://${mermaidFile.absolutePath}" else ""

    val isKatexNeeded = remember(content) {
        content.contains("$") || content.contains("\\(") || content.contains("\\[") || content.contains("\\begin{equation}")
    }
    var isKatexInstalled by remember {
        mutableStateOf(DocAssetDownloadManager.isAssetInstalled(context, DocAssetDownloadManager.ASSET_KATEX_JS) &&
                       DocAssetDownloadManager.isAssetInstalled(context, DocAssetDownloadManager.ASSET_KATEX_CSS))
    }
    var showKatexPrompt by remember {
        mutableStateOf(isKatexNeeded && !isKatexInstalled && !DocAssetDownloadManager.isDeclined(DocAssetDownloadManager.ASSET_KATEX_JS))
    }
    val katexJsFile = remember(isKatexInstalled) { DocAssetDownloadManager.getAssetFile(context, DocAssetDownloadManager.ASSET_KATEX_JS) }
    val katexCssFile = remember(isKatexInstalled) { DocAssetDownloadManager.getAssetFile(context, DocAssetDownloadManager.ASSET_KATEX_CSS) }
    val katexJsUri = if (isKatexInstalled) "file://${katexJsFile.absolutePath}" else ""
    val katexCssUri = if (isKatexInstalled) "file://${katexCssFile.absolutePath}" else ""

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(scrollToHeadingId) {
        scrollToHeadingId?.let { id ->
            webViewInstance?.evaluateJavascript("document.getElementById('$id')?.scrollIntoView({behavior: 'smooth'});", null)
        }
    }

    val processedHtml = remember(content, isDark, fontSizeSp, isMermaidInstalled, isKatexInstalled, textColor) {
        val bg = if (isDark) "#1E1E1E" else "#FFFFFF"
        val fg = String.format("#%02x%02x%02x", (textColor.red * 255).toInt(), (textColor.green * 255).toInt(), (textColor.blue * 255).toInt())
        val codeBg = if (isDark) "#2A2A2A" else "#F1F1F1"
        val preBg = if (isDark) "#2A2A2A" else "#F8F8F8"
        val borderColor = if (isDark) "#444" else "#ddd"

        val escapedContent = content
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
            ${if (isMermaidInstalled) "<script src=\"$mermaidScriptUri\"></script>" else ""}
            ${if (isKatexInstalled) """
            <link rel="stylesheet" href="$katexCssUri">
            <script src="$katexJsUri"></script>
            """ else ""}
            <style>
                body {
                    background-color: $bg;
                    color: $fg;
                    font-family: sans-serif;
                    padding: 16px;
                    line-height: 1.6;
                    font-size: ${fontSizeSp}sp;
                }
                img { max-width: 100%; height: auto; }
                table { border-collapse: collapse; width: 100%; margin: 16px 0; }
                th, td { border: 1px solid $borderColor; padding: 8px; }
                code { background-color: $codeBg; padding: 2px 4px; border-radius: 4px; font-family: monospace; }
                pre code { background-color: transparent; padding: 0; }
                pre { background-color: $preBg; padding: 12px; border-radius: 6px; overflow-x: auto; }
                .callout {
                    padding: 12px 16px;
                    margin: 16px 0;
                    border-left: 4px solid #4a90e2;
                    background-color: ${if (isDark) "#252525" else "#f8f9fa"};
                    border-radius: 0 6px 6px 0;
                }
                .callout-title { margin-bottom: 4px; font-weight: bold; }
                .callout-note { border-left-color: #4a90e2; }
                .callout-tip { border-left-color: #2ecc71; }
                .callout-important { border-left-color: #9b59b6; }
                .callout-warning { border-left-color: #f39c12; }
                .callout-caution { border-left-color: #e74c3c; }
                details {
                    background-color: ${if (isDark) "#252525" else "#f8f9fa"};
                    border: 1px solid $borderColor;
                    padding: 12px;
                    border-radius: 6px;
                    margin: 16px 0;
                }
                summary {
                    font-weight: bold;
                    cursor: pointer;
                }
                .mermaid {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    margin: 16px 0;
                    background-color: $preBg;
                    padding: 12px;
                    border-radius: 6px;
                }
                .mermaid-error {
                    background-color: #ffebee;
                    color: #c62828;
                    border: 1px solid #ef9a9a;
                    padding: 12px;
                    border-radius: 8px;
                    font-family: monospace;
                    font-size: 13px;
                    white-space: pre-wrap;
                }
            </style>
            <script>
                function parseTables(md) {
                    let lines = md.split('\n');
                    let inTable = false;
                    let tableHtml = [];
                    let result = [];

                    for (let i = 0; i < lines.length; i++) {
                        let line = lines[i].trim();
                        if (line.startsWith('|') && line.endsWith('|')) {
                            let cells = line.split('|').slice(1, -1).map(c => c.trim());
                            if (cells.every(c => /^[:\-]+$/.test(c))) {
                                continue;
                            }
                            if (!inTable) {
                                inTable = true;
                                tableHtml = ['<table>', '<thead><tr>'];
                                cells.forEach(c => tableHtml.push('<th>' + c + '</th>'));
                                tableHtml.push('</tr></thead><tbody>');
                            } else {
                                tableHtml.push('<tr>');
                                cells.forEach(c => tableHtml.push('<td>' + c + '</td>'));
                                tableHtml.push('</tr>');
                            }
                        } else {
                            if (inTable) {
                                tableHtml.push('</tbody></table>');
                                result.push(tableHtml.join(''));
                                inTable = false;
                                tableHtml = [];
                            }
                            result.push(lines[i]);
                        }
                    }
                    if (inTable) {
                        tableHtml.push('</tbody></table>');
                        result.push(tableHtml.join(''));
                    }
                    return result.join('\n');
                }

                function parseMarkdown(md) {
                    if (!md) return '';
                    let html = md
                        .replace(/&/g, '&amp;')
                        .replace(/</g, '&lt;')
                        .replace(/>/g, '&gt;');

                    html = html.replace(/```([a-z]*)\n([\s\S]*?)\n```/g, (match, lang, code) => {
                        const l = (lang || '').trim().toLowerCase();
                        if (l === 'mermaid') {
                            return '<div class="mermaid">' + code.trim() + '</div>';
                        }
                        return '<pre><code class="language-' + l + '">' + code + '</code></pre>';
                    });

                    html = parseTables(html);

                    html = html.replace(/^&gt;\s*\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]([\s\S]*?)(?=^(?:&gt;|\n\n|$))/gim, (match, type, content) => {
                        const t = type.charAt(0) + type.slice(1).toLowerCase();
                        const cls = 'callout-' + type.toLowerCase();
                        return '<div class="callout ' + cls + '"><div class="callout-title"><strong>' + t + '</strong></div>' + content.replace(/^&gt;\s?/gm, '') + '</div>';
                    });

                    html = html.replace(/^&gt;\s?(.*)$/gm, '<blockquote>$1</blockquote>');

                    const usedIds = new Set();
                    html = html.replace(/^(#{1,6})\s+(.+)$/gm, (match, hashes, text) => {
                        const level = hashes.length;
                        const cleanText = text.replace(/<[^>]*>/g, '');
                        let clean = cleanText.toLowerCase()
                            .replace(/[^a-z0-9\s-]/g, '')
                            .trim()
                            .replace(/\s+/g, '-')
                            .replace(/-+/g, '-');
                        if (!clean) clean = 'heading';
                        let id = clean;
                        let counter = 1;
                        while (usedIds.has(id)) {
                            id = clean + '-' + counter;
                            counter++;
                        }
                        usedIds.add(id);
                        return '<h' + level + ' id="' + id + '">' + text + '</h' + level + '>';
                    });

                    html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1">');
                    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
                    html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');
                    html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
                    html = html.replace(/_([^_]+)_/g, '<em>$1</em>');
                    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
                    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');
                    html = html.replace(/^---$/gm, '<hr>');

                    if (typeof katex !== 'undefined') {
                        html = html.replace(/\$\$([\s\S]*?)\$\$|\\\[([\s\S]*?)\\\]/g, (match, p1, p2) => {
                            const expr = p1 || p2;
                            try { return katex.renderToString(expr.trim(), { displayMode: true, throwOnError: false }); } catch (e) { return match; }
                        });
                        html = html.replace(/\$([^$\n]+?)\$|\\\(([^)]+?)\\\)/g, (match, p1, p2) => {
                            const expr = p1 || p2;
                            try { return katex.renderToString(expr.trim(), { displayMode: false, throwOnError: false }); } catch (e) { return match; }
                        });
                    }

                    html = html.split(/\n\n+/).map(para => {
                        para = para.trim();
                        if (!para) return '';
                        if (para.startsWith('<h') || para.startsWith('<div') || para.startsWith('<pre') || para.startsWith('<blockquote') || para.startsWith('<hr') || para.startsWith('<table')) {
                            return para;
                        }
                        return '<p>' + para.replace(/\n/g, '<br>') + '</p>';
                    }).join('\n');

                    return html;
                }
            </script>
        </head>
        <body>
            <div id="markdown-body"></div>
            <script>
                try {
                    const rawMarkdown = `$escapedContent`;
                    document.getElementById('markdown-body').innerHTML = parseMarkdown(rawMarkdown);
                } catch (err) {
                    document.getElementById('markdown-body').innerHTML = '<pre>' + rawMarkdown + '</pre>';
                }
                
                ${if (isMermaidInstalled) """
                window.addEventListener('DOMContentLoaded', async () => {
                    if (typeof mermaid !== 'undefined') {
                        mermaid.initialize({
                            startOnLoad: false,
                            theme: '${if (isDark) "dark" else "default"}',
                            securityLevel: 'loose',
                            flowchart: { htmlLabels: true, curve: 'basis' }
                        });
                        const mermaidElements = document.querySelectorAll('.mermaid');
                        for (let i = 0; i < mermaidElements.length; i++) {
                            const el = mermaidElements[i];
                            try {
                                const { svg } = await mermaid.render('mermaid-svg-' + i, el.textContent.trim());
                                el.innerHTML = svg;
                            } catch (err) {
                                console.error('Mermaid render error:', err);
                                el.innerHTML = '<div class="mermaid-error"><strong>Mermaid Syntax Error:</strong>\\n' + (err.message || err) + '</div>';
                            }
                        }
                    }
                });
                """ else ""}
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showMermaidPrompt) {
            AssetDownloadBanner(
                title = "Download Mermaid Engine",
                description = "This document contains Mermaid diagrams. Download the rendering engine once for secure offline rendering.",
                assetKeys = listOf(DocAssetDownloadManager.ASSET_MERMAID),
                onDownloaded = {
                    showMermaidPrompt = false
                    isMermaidInstalled = MermaidDownloadManager.isMermaidInstalled(context)
                }
            )
        }

        if (showKatexPrompt) {
            AssetDownloadBanner(
                title = "Download KaTeX Math Engine",
                description = "This document contains mathematical equations. Download KaTeX once for offline rendering.",
                assetKeys = listOf(DocAssetDownloadManager.ASSET_KATEX_JS, DocAssetDownloadManager.ASSET_KATEX_CSS),
                onDownloaded = {
                    showKatexPrompt = false
                    isKatexInstalled = DocAssetDownloadManager.isAssetInstalled(context, DocAssetDownloadManager.ASSET_KATEX_JS) &&
                                      DocAssetDownloadManager.isAssetInstalled(context, DocAssetDownloadManager.ASSET_KATEX_CSS)
                }
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                onOpenInBrowser(url)
                                return true
                            } else if (url.contains("#")) {
                                val fragment = request.url?.fragment
                                if (fragment != null && fragment.isNotEmpty()) {
                                    view?.evaluateJavascript("document.getElementById('$fragment')?.scrollIntoView({behavior: 'smooth'});", null)
                                    return true
                                }
                            }
                            return false
                        }
                    }
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
                webView.loadDataWithBaseURL("file:///android_asset/", processedHtml, "text/html", "UTF-8", null)
            }
        )
    }
}

@Composable
fun AssetDownloadBanner(
    title: String,
    description: String,
    assetKeys: List<String>,
    onDownloaded: () -> Unit
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (isDownloading) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("Downloading... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            } else if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    assetKeys.forEach { DocAssetDownloadManager.setDeclined(it, true) }
                    onDownloaded()
                }) {
                    Text("Not Now")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    isDownloading = true
                    coroutineScope.launch {
                        var success = true
                        for (key in assetKeys) {
                            DocAssetDownloadManager.downloadAsset(context, key) { p ->
                                progress = p
                            }.onFailure {
                                success = false
                                error = it.localizedMessage
                            }
                        }
                        isDownloading = false
                        if (success) {
                            onDownloaded()
                        }
                    }
                }) {
                    Text("Download")
                }
            }
        }
    }
}

@Composable
fun DocHtmlView(
    content: String,
    filePath: String,
    isDark: Boolean,
    onOpenInBrowser: (String) -> Unit
) {
    val file = remember(filePath) { File(filePath) }
    val parentUri = remember(file) {
        if (file.exists()) "file://${file.parentFile?.absolutePath}/" else null
    }

    val styledContent = remember(content, isDark) {
        val bg = if (isDark) "#1E1E1E" else "#FFFFFF"
        val fg = if (isDark) "#FFFFFF" else "#000000"
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { background-color: $bg; color: $fg; font-family: sans-serif; padding: 16px; line-height: 1.6; }
                img { max-width: 100%; height: auto; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid ${if (isDark) "#444" else "#ddd"}; padding: 8px; }
            </style>
        </head>
        <body>$content</body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            onOpenInBrowser(url)
                            return true
                        }
                        return false
                    }
                }
            }
        },
        update = { webView ->
            if (parentUri != null) {
                webView.loadDataWithBaseURL(parentUri, styledContent, "text/html", "UTF-8", null)
            } else {
                webView.loadDataWithBaseURL(null, styledContent, "text/html", "UTF-8", null)
            }
        }
    )
}

@Composable
fun DocTextView(
    content: String,
    format: DocFormat,
    settings: DocSettings,
    textColor: Color,
    isDark: Boolean,
    searchQuery: String,
    onCopy: (String) -> Unit
) {
    val lines = remember(content) { content.lines() }
    val lazyListState = rememberLazyListState()
    val ext = if (format == DocFormat.CODE) "kt" else "txt"

    Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().widthIn(min = 400.dp)
        ) {
            itemsIndexed(lines) { index, line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopy(line) }
                        .padding(vertical = 2.dp, horizontal = 8.dp)
                ) {
                    if (settings.showLineNumbers) {
                        Text(
                            text = (index + 1).toString(),
                            modifier = Modifier.width(42.dp),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (isDark) Color.Gray else Color.LightGray
                            )
                        )
                    }

                    val highlightedText = remember(line, ext, isDark) {
                        if (format == DocFormat.CODE) {
                            SyntaxHighlighter.highlightLine(line, ext, isDark)
                        } else {
                            AnnotatedString(line)
                        }
                    }

                    Text(
                        text = highlightedText,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = settings.fontSizeSp.sp,
                            color = textColor
                        ),
                        softWrap = settings.wordWrap
                    )
                }
            }
        }
    }
}

@Composable
fun DocEpubView(
    chapters: List<DocChapter>,
    textColor: Color,
    fontSizeSp: Int,
    onChapterChange: (Int, Int) -> Unit
) {
    var currentChapterIndex by remember { mutableIntStateOf(0) }

    if (chapters.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No readable EPUB content found.", color = textColor)
        }
    } else {
        val chapter = chapters[currentChapterIndex.coerceIn(0, chapters.lastIndex)]
        LaunchedEffect(currentChapterIndex) {
            onChapterChange(currentChapterIndex, chapters.size)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = currentChapterIndex > 0,
                    onClick = { currentChapterIndex-- }
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Chapter")
                }
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                IconButton(
                    enabled = currentChapterIndex < chapters.lastIndex,
                    onClick = { currentChapterIndex++ }
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Chapter")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(chapter.paragraphs) { _, p ->
                    Text(
                        text = p,
                        style = TextStyle(
                            fontSize = fontSizeSp.sp,
                            color = textColor,
                            lineHeight = (fontSizeSp * 1.5).sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DocDocxView(
    paragraphs: List<String>,
    textColor: Color,
    fontSizeSp: Int,
    onParagraphChange: (Int, Int) -> Unit
) {
    val lazyListState = rememberLazyListState()

    if (paragraphs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No text found in DOCX file.", color = textColor)
        }
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(paragraphs) { index, p ->
                LaunchedEffect(index) {
                    onParagraphChange(index, paragraphs.size)
                }
                Text(
                    text = p,
                    style = TextStyle(
                        fontSize = fontSizeSp.sp,
                        color = textColor,
                        lineHeight = (fontSizeSp * 1.5).sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocOutlineSheet(
    outlineItems: List<DocOutlineItem>,
    bookmarks: List<DocBookmark>,
    onItemClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Table of Contents & Bookmarks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (outlineItems.isEmpty() && bookmarks.isEmpty()) {
                Text("No headings or bookmarks available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bookmarks.isNotEmpty()) {
                        item {
                            Text("Bookmarks", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        itemsIndexed(bookmarks) { _, bm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDismiss() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Page/Section ${bm.pageOrPosition} - ${bm.chapterTitle.ifBlank { "Bookmark" }}")
                            }
                        }
                    }

                    if (outlineItems.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Headings Outline", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        itemsIndexed(outlineItems) { _, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onItemClick(item.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 6.dp)
                                    .padding(start = ((item.level - 1) * 16).dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (item.level == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
