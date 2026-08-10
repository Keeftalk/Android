package com.keeftalk.chat.ui.feed

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.domain.model.FeedArticle
import com.keeftalk.chat.ui.feed.components.*
import com.keeftalk.chat.ui.theme.FabGradient
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    onManageFeeds: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: FeedViewModel = viewModel {
        FeedViewModel(
            AppModule.provideFeedRepository(context),
            AppModule.provideUserPreferencesRepository(context)
        )
    }
    
    val articles by viewModel.filteredArticles.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val locationMode by viewModel.weatherLocationMode.collectAsState()
    val manualLocation by viewModel.weatherManualLocation.collectAsState()
    val readingArticle by viewModel.selectedArticle.collectAsState()
    val isArticleLoading by viewModel.isArticleLoading.collectAsState()

    var showLocationDialog by remember { mutableStateOf(false) }
    
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                if (locationMode == "AUTO" && !locationPermissionState.status.isGranted) {
                    locationPermissionState.launchPermissionRequest()
                }
                viewModel.refresh() 
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Weather Section
                item {
                    Box(modifier = Modifier.padding(16.dp)) {
                        WeatherCard(
                            weather = weather,
                            onLocationClick = { showLocationDialog = true }
                        )
                    }
                }

                // STICKY Category Chips (The "Containers" user referred to)
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = 4.dp // Add shadow to make it feel like it's above content
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { viewModel.setCategory(category) },
                                    label = { Text(category) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                if (articles.isEmpty()) {
                    item {
                        if (!isRefreshing) {
                            EmptyFeedOnboarding(onManageFeeds)
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                } else {
                    itemsIndexed(articles, key = { _, item -> item.id }) { index, article ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            when {
                                index == 0 && selectedCategory == "For You" -> {
                                    HeroArticleCard(article, onClick = { viewModel.selectArticle(article) })
                                }
                                index % 6 == 0 -> {
                                    StandardArticleCard(article, onClick = { viewModel.selectArticle(article) })
                                }
                                else -> {
                                    CompactArticleCard(article, onClick = { viewModel.selectArticle(article) })
                                }
                            }
                        }
                    }
                }
            }
        }

        // Location Dialog
        if (showLocationDialog) {
            WeatherLocationDialog(
                currentMode = locationMode,
                currentManualLocation = manualLocation ?: "",
                onDismiss = { showLocationDialog = false },
                onSave = { mode: String, location: String ->
                    if (mode == "AUTO") locationPermissionState.launchPermissionRequest()
                    viewModel.setWeatherMode(mode)
                    if (mode == "MANUAL") viewModel.setManualLocation(location)
                    showLocationDialog = false
                }
            )
        }

        // Native Article Reader
        if (readingArticle != null) {
            ArticleReader(
                article = readingArticle!!,
                isLoading = isArticleLoading,
                onDismiss = { viewModel.selectArticle(null) }
            )
        }
    }
}

@Composable
fun EmptyFeedOnboarding(onManageFeeds: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Your Feed, your interests.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Choose the topics and sources you want to follow to personalize your experience.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onManageFeeds,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text("Customize Feed", fontWeight = FontWeight.Bold)
        }
    }
}
