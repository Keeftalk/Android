package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.settings.previews.*

/**
 * A screen that allows previewing UI-only components and screens.
 * This is intended for development and AI-driven UI design.
 */
@Composable
fun TestScreensPreviewScreen(
    onBack: () -> Unit
) {
    var selectedScreenId by remember { mutableStateOf<String?>(null) }
    
    if (selectedScreenId == null) {
        MainPreviewList(
            onBack = onBack,
            onSelectScreen = { selectedScreenId = it }
        )
    } else {
        PreviewContainer(
            screenId = selectedScreenId!!,
            onBack = { selectedScreenId = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainPreviewList(
    onBack: () -> Unit,
    onSelectScreen: (String) -> Unit
) {
    Scaffold(
        topBar = {
            SettingsHeader(title = "Test Screens Preview", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Select a screen to preview its UI. These are UI-only implementations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(PreviewRegistry.previews) { preview ->
                PreviewListItem(
                    title = preview.title,
                    description = preview.description,
                    onClick = { onSelectScreen(preview.id) }
                )
            }
            
            if (PreviewRegistry.previews.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No previews registered yet.")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewListItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewContainer(
    screenId: String,
    onBack: () -> Unit
) {
    val preview = PreviewRegistry.previews.find { it.id == screenId }
    
    Scaffold(
        topBar = {
            SettingsHeader(
                title = preview?.title ?: "Preview",
                onBack = onBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            preview?.content?.invoke() ?: Text("Preview not found", modifier = Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Registry for UI previews. 
 * AI agents can add new previews here by adding to the 'previews' list.
 */
object PreviewRegistry {
    val previews = mutableStateListOf<PreviewItem>(
        PreviewItem(
            id = "sample_screen",
            title = "Sample New Screen",
            description = "A placeholder to demonstrate how previews work.",
            content = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Science, null, modifier = Modifier.size(64.dp))
                        Text("This is a UI-only preview of a new screen.")
                        Button(onClick = {}) {
                            Text("Mock Button")
                        }
                    }
                }
            }
        ),
        PreviewItem(
            id = "ultimate_premium_profile",
            title = "Ultimate Premium Profile",
            description = "A high-fidelity, production-ready profile experience.",
            content = { UltimatePremiumProfile() }
        )
    )

    fun register(item: PreviewItem) {
        previews.add(item)
    }
}

data class PreviewItem(
    val id: String,
    val title: String,
    val description: String,
    val content: @Composable () -> Unit
)
