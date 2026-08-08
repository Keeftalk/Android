package com.keeftalk.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.ui.legal.LegalUiState
import com.keeftalk.chat.ui.legal.LegalViewModel
import com.keeftalk.chat.ui.theme.LocalAppIcons
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalViewerModal(
    type: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: LegalViewModel = viewModel {
        LegalViewModel(AppModule.provideLegalRepository(context))
    }
    val icons = LocalAppIcons.current

    LaunchedEffect(type) {
        viewModel.loadDocument(type)
    }

    val uiState by viewModel.uiState.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            when (type) {
                                "terms" -> "Terms of Service"
                                "privacy" -> "Privacy Policy"
                                else -> "Legal Document"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(icons.back, contentDescription = "Close")
                        }
                    }
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = uiState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "LegalContentTransition"
                    ) { state ->
                        when (state) {
                            is LegalUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is LegalUiState.Success -> {
                                val document = state.document
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                                ) {
                                    if (document.isFromCache) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    "You're viewing the last downloaded version.",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = document.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Version ${document.version} | Last updated: ${formatDate(document.updatedAt)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    MarkdownText(
                                        markdown = document.content,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(32.dp))
                                }
                            }
                            is LegalUiState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "Failed to load document",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = { viewModel.loadDocument(type) },
                                        modifier = Modifier.padding(top = 24.dp)
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
