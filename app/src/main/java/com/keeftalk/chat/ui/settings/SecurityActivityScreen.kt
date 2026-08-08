package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityActivityScreen(
    viewModel: SecuritySettingsViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.securityEvents.collectAsState()
    val icons = LocalAppIcons.current

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        topBar = {
            SettingsHeader(title = "Security Activity", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(
                        icons.shield,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No security events recorded",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingsSection(title = "Recent Events") {
                        events.forEachIndexed { index, event ->
                            SecurityEventItem(
                                event = event,
                                showDivider = index != events.size - 1
                            )
                        }
                    }
                }
            }
        }
    }
}

