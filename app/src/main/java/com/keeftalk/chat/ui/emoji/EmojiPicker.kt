package com.keeftalk.chat.ui.emoji

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.launch

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentEmojiManager = remember { RecentEmojiManager(context) }
    val recentEmojis by recentEmojiManager.recentEmojis.collectAsState(initial = emptyList())
    val frequentEmojis by recentEmojiManager.frequentEmojis.collectAsState(initial = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Emoji>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }
    var skinToneEmoji by remember { mutableStateOf<Emoji?>(null) }
    
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var isDataSourceReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        EmojiDataSource.initialize(context)
        isDataSourceReady = true
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            searchResults = EmojiDataSource.search(searchQuery)
        } else {
            searchResults = emptyList()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 8.dp
    ) {
        if (!isDataSourceReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box {
                Column {
                    // Header / Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp),
                            placeholder = { Text("Search emojis...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            shape = CircleShape,
                            singleLine = true
                        )
                    }

                    // Category Tabs
                    if (searchQuery.isEmpty()) {
                        ScrollableTabRow(
                            selectedTabIndex = EmojiCategory.entries.indexOf(selectedCategory),
                            edgePadding = 8.dp,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[EmojiCategory.entries.indexOf(selectedCategory)]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            EmojiCategory.entries.forEach { category ->
                                Tab(
                                    selected = selectedCategory == category,
                                    onClick = { 
                                        selectedCategory = category
                                        scope.launch { gridState.scrollToItem(0) }
                                    },
                                    text = { Text(category.icon, fontSize = 20.sp) }
                                )
                            }
                        }
                    }

                    // Emoji Grid
                    val displayEmojis = if (searchQuery.isNotEmpty()) {
                        searchResults
                    } else {
                        when (selectedCategory) {
                            EmojiCategory.RECENT -> recentEmojis.map { Emoji(it, "", emptyList(), EmojiCategory.RECENT) }
                            EmojiCategory.FREQUENT -> frequentEmojis.map { Emoji(it, "", emptyList(), EmojiCategory.FREQUENT) }
                            else -> EmojiDataSource.getEmojisByCategory(selectedCategory)
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        state = gridState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(displayEmojis, key = { it.unicode + it.category.name }) { emoji ->
                            EmojiCell(
                                emoji = emoji,
                                onClick = {
                                    onEmojiSelected(emoji.unicode)
                                    scope.launch { recentEmojiManager.addEmoji(emoji.unicode) }
                                },
                                onLongClick = {
                                    if (emoji.skinToneSupport) {
                                        skinToneEmoji = emoji
                                    }
                                }
                            )
                        }
                    }
                }

                // Skin Tone Popup
                if (skinToneEmoji != null) {
                    SkinToneSelector(
                        baseEmoji = skinToneEmoji!!,
                        onToneSelected = { tonedEmoji ->
                            onEmojiSelected(tonedEmoji)
                            scope.launch { recentEmojiManager.addEmoji(tonedEmoji) }
                            skinToneEmoji = null
                        },
                        onDismiss = { skinToneEmoji = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiCell(
    emoji: Emoji,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji.unicode,
            fontSize = 32.sp
        )
    }
}

@Composable
fun SkinToneSelector(
    baseEmoji: Emoji,
    onToneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tones = listOf("", "🏻", "🏼", "🏽", "🏾", "🏿")
    Popup(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 16.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Row(modifier = Modifier.padding(8.dp)) {
                tones.forEach { tone ->
                    val tonedEmoji = baseEmoji.unicode + tone
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onToneSelected(tonedEmoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tonedEmoji, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}
