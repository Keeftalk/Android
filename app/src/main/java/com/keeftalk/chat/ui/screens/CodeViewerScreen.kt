package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.util.FileUtils
import com.keeftalk.chat.util.SyntaxHighlighter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeViewerScreen(
    viewModel: CodeViewerViewModel,
    myId: String,
    onToggleLock: () -> Unit,
    onBack: () -> Unit
) {
    val message by viewModel.message.collectAsState()
    val codeContent by viewModel.codeContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var currentSearchMatchIndex by remember { mutableIntStateOf(0) }
    var searchMatches by remember { mutableStateOf<List<Pair<Int, IntRange>>>(emptyList()) }
    
    LaunchedEffect(codeContent, searchQuery) {
        if (searchQuery.length < 2) {
            searchMatches = emptyList()
            return@LaunchedEffect
        }
        
        withContext(Dispatchers.Default) {
            val lines = codeContent?.lines() ?: emptyList()
            val matches = mutableListOf<Pair<Int, IntRange>>()
            lines.forEachIndexed { lineIndex, line ->
                var start = 0
                while (start < line.length) {
                    val index = line.indexOf(searchQuery, start, ignoreCase = true)
                    if (index == -1) break
                    matches.add(lineIndex to index..(index + searchQuery.length - 1))
                    start = index + searchQuery.length
                }
            }
            searchMatches = matches
        }
    }

    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            val isMe = message?.senderId == myId
            val isLocked = message?.mediaLocked == true
            val canExport = !isLocked || isMe

            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; currentSearchMatchIndex = 0 },
                            placeholder = { Text("Search code...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = ""; isSearchActive = false }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        if (searchMatches.isNotEmpty()) {
                            Text(
                                text = "${currentSearchMatchIndex + 1}/${searchMatches.size}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = {
                                if (currentSearchMatchIndex > 0) currentSearchMatchIndex--
                                scope.launch { lazyListState.animateScrollToItem(searchMatches[currentSearchMatchIndex].first) }
                            }) {
                                Icon(Icons.Default.KeyboardArrowUp, null)
                            }
                            IconButton(onClick = {
                                if (currentSearchMatchIndex < searchMatches.size - 1) currentSearchMatchIndex++
                                scope.launch { lazyListState.animateScrollToItem(searchMatches[currentSearchMatchIndex].first) }
                            }) {
                                Icon(Icons.Default.KeyboardArrowDown, null)
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = message?.fileName ?: "Code Viewer",
                                style = MaterialTheme.typography.titleMedium
                            )
                            message?.fileSize?.let {
                                Text(
                                    text = "${it / 1024} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { 
                            codeContent?.let { 
                                clipboardManager.setText(AnnotatedString(it))
                                android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        
                        if (isMe) {
                            IconButton(onClick = onToggleLock) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Toggle Lock",
                                    tint = if (isLocked) Color.Red else Color.White
                                )
                            }
                        }

                        if (canExport) {
                            IconButton(onClick = { viewModel.download() }) {
                                Icon(Icons.Default.Download, contentDescription = "Download")
                            }
                            IconButton(onClick = { 
                                message?.localFilePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) {
                                        FileUtils.openFile(context, file)
                                    }
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open with...")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else if (codeContent != null) {
                val lines = remember(codeContent) { codeContent!!.lines() }
                val extension = message?.fileName?.substringAfterLast(".", "") ?: ""
                val isDark = isSystemInDarkTheme()
                val horizontalScrollState = rememberScrollState()

                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp.dp

                Box(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxHeight().widthIn(min = screenWidth)
                    ) {
                        itemsIndexed(lines) { lineIndex, line ->
                            CodeLine(
                                lineIndex = lineIndex,
                                lineText = line,
                                extension = extension,
                                isDark = isDark,
                                searchMatches = searchMatches.filter { it.first == lineIndex },
                                currentMatchIndex = if (searchMatches.getOrNull(currentSearchMatchIndex)?.first == lineIndex) currentSearchMatchIndex else -1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeLine(
    lineIndex: Int,
    lineText: String,
    extension: String,
    isDark: Boolean,
    searchMatches: List<Pair<Int, IntRange>>,
    currentMatchIndex: Int
) {
    val highlightedLine = remember(lineText, extension, isDark) {
        SyntaxHighlighter.highlightLine(lineText, extension, isDark)
    }
    
    val lineWithMatches = remember(highlightedLine, searchMatches, currentMatchIndex) {
        if (searchMatches.isEmpty()) highlightedLine
        else {
            buildAnnotatedString {
                append(highlightedLine)
                searchMatches.forEach { (_, range) ->
                    addStyle(
                        style = SpanStyle(background = Color(0xFFFFCC00).copy(alpha = 0.6f), color = Color.Black),
                        start = range.first,
                        end = range.last + 1
                    )
                }
            }
        }
    }

    val guideColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .drawBehind {
                // Indentation guides
                val tabWidthPx = 32.dp.toPx()
                val spaceCount = lineText.takeWhile { it == ' ' }.length
                val tabs = spaceCount / 4
                for (i in 1..tabs) {
                    val x = 60.dp.toPx() + (i * tabWidthPx)
                    drawLine(
                        color = guideColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
    ) {
        // Gutter
        Text(
            text = (lineIndex + 1).toString(),
            modifier = Modifier
                .width(52.dp)
                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF3F3F3))
                .padding(end = 8.dp, top = 2.dp, bottom = 2.dp),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = if (isDark) Color.Gray else Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Code
        Text(
            text = lineWithMatches,
            modifier = Modifier.padding(vertical = 2.dp),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (isDark) Color.White else Color.Black
            ),
            softWrap = false
        )
    }
}
