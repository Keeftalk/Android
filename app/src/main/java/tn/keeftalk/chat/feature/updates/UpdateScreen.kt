package tn.keeftalk.chat.feature.updates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.ui.settings.SettingsHeader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val channel by viewModel.channel.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState is UpdateUiState.Idle) {
            viewModel.checkForUpdates()
        }
    }

    Scaffold(
        topBar = {
            SettingsHeader(title = "Check for Updates", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Keeftalk Android",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Installed Version: v${viewModel.installedVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = { viewModel.checkForUpdates() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Release Channel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = channel == ReleaseChannel.STABLE,
                            onClick = { viewModel.setChannel(ReleaseChannel.STABLE) },
                            label = { Text("Stable") },
                            leadingIcon = if (channel == ReleaseChannel.STABLE) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = channel == ReleaseChannel.PRERELEASE,
                            onClick = { viewModel.setChannel(ReleaseChannel.PRERELEASE) },
                            label = { Text("Pre-release") },
                            leadingIcon = if (channel == ReleaseChannel.PRERELEASE) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    if (channel == ReleaseChannel.PRERELEASE) {
                        Text(
                            text = "Note: Pre-releases may contain experimental features or bugs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            when (val state = uiState) {
                is UpdateUiState.Idle, is UpdateUiState.Checking -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Checking for updates from GitHub...")
                        }
                    }
                }
                is UpdateUiState.UpToDate -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Text(
                                text = "You're up to date!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version v${state.installedVersion} is the latest available on the ${state.channel.name.lowercase()} channel.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is UpdateUiState.UpdateAvailable -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "New Update Available!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = state.availableVersion,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(text = "Asset: ${state.assetName} (${state.downloadSize / (1024 * 1024)} MB)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (!state.changelog.isNullOrBlank()) {
                                HorizontalDivider()
                                Text(text = "Release Notes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(text = state.changelog, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            when (val dState = downloadState) {
                                is DownloadUiState.Idle -> {
                                    Button(
                                        onClick = { viewModel.downloadUpdate() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download Update")
                                    }
                                }
                                is DownloadUiState.Downloading -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LinearProgressIndicator(
                                            progress = { dState.progress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Downloading... ${(dState.progress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            TextButton(onClick = { viewModel.cancelDownload() }) {
                                                Text("Cancel")
                                            }
                                        }
                                    }
                                }
                                is DownloadUiState.Completed -> {
                                    Button(
                                        onClick = { viewModel.installUpdate(context, dState.apkFile) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Icon(Icons.Default.InstallMobile, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Install Update")
                                    }
                                }
                                is DownloadUiState.Cancelled -> {
                                    OutlinedButton(
                                        onClick = { viewModel.downloadUpdate() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Resume / Retry Download")
                                    }
                                }
                                is DownloadUiState.Error -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "Error: ${dState.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                        Button(
                                            onClick = { viewModel.downloadUpdate() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Retry Download")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is UpdateUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "Update Check Failed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(text = state.message, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { viewModel.checkForUpdates() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}
