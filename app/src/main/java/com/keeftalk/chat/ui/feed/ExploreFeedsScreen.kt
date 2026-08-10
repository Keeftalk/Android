package com.keeftalk.chat.ui.feed

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.CuratedCategory
import com.keeftalk.chat.domain.model.CuratedFeed
import com.keeftalk.chat.domain.model.CuratedFeedProvider
import com.keeftalk.chat.ui.components.KeeftalkFeatureTopBar
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFeedsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: FeedViewModel = viewModel {
        FeedViewModel(
            AppModule.provideFeedRepository(context),
            AppModule.provideUserPreferencesRepository(context)
        )
    }
    
    val existingSources by viewModel.sources.collectAsState()
    val selectedFeeds = remember { mutableStateMapOf<String, CuratedFeed>() }
    
    // Initialize selection from existing sources
    LaunchedEffect(existingSources) {
        existingSources.forEach { source ->
            val curated = CuratedFeedProvider.categories.flatMap { it.feeds }.find { it.url == source.url }
            if (curated != null) {
                selectedFeeds[curated.url] = curated
            }
        }
    }

    Scaffold(
        topBar = {
            KeeftalkFeatureTopBar(
                title = "Explore Feeds",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        viewModel.addCuratedSources(selectedFeeds.values.toList())
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save My Feed (${selectedFeeds.size})", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Text(
                    "Choose your interests",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(CuratedFeedProvider.categories) { category ->
                CategoryExpandableItem(
                    category = category,
                    selectedFeeds = selectedFeeds,
                    onToggleFeed = { feed, selected ->
                        if (selected) selectedFeeds[feed.url] = feed
                        else selectedFeeds.remove(feed.url)
                    }
                )
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun CategoryExpandableItem(
    category: CuratedCategory,
    selectedFeeds: Map<String, CuratedFeed>,
    onToggleFeed: (CuratedFeed, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val icons = LocalAppIcons.current
    val categorySelectedCount = category.feeds.count { selectedFeeds.containsKey(it.url) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(16.dp),
            color = if (expanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            border = if (!expanded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
            tonalElevation = if (expanded) 2.dp else 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getCategoryIcon(category.icon, icons), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (categorySelectedCount > 0) {
                        Text("$categorySelectedCount selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(onClick = { category.feeds.forEach { onToggleFeed(it, true) } }) {
                        Text("Select All")
                    }
                    TextButton(onClick = { category.feeds.forEach { onToggleFeed(it, false) } }) {
                        Text("Clear All")
                    }
                }
                
                category.feeds.forEach { feed ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggleFeed(feed, !selectedFeeds.containsKey(feed.url)) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFeeds.containsKey(feed.url),
                            onCheckedChange = { onToggleFeed(feed, it) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(feed.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(key: String, icons: com.keeftalk.chat.ui.theme.AppIcons) = when(key) {
    "globe" -> Icons.Default.Public
    "cpu" -> Icons.Default.Memory
    "brain" -> Icons.Default.Psychology
    "flask" -> Icons.Default.Biotech
    "rocket" -> Icons.Default.RocketLaunch
    "plane" -> Icons.Default.Flight
    "briefcase" -> Icons.Default.BusinessCenter
    "trending-up" -> Icons.AutoMirrored.Filled.TrendingUp
    "code" -> Icons.Default.Terminal
    "shield" -> Icons.Default.Security
    "gamepad" -> Icons.Default.SportsEsports
    "car" -> Icons.Default.DirectionsCar
    "trophy" -> Icons.Default.EmojiEvents
    "leaf" -> Icons.Default.Eco
    "book" -> Icons.Default.School
    else -> icons.info
}
