package tn.keeftalk.chat.extension.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.extension.model.ExtensionManifest
import tn.keeftalk.chat.extension.model.SecurityReviewReport
import tn.keeftalk.chat.extension.model.VerificationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionReviewScreen(
    manifest: ExtensionManifest,
    report: SecurityReviewReport,
    onConfirmInstall: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extension Security Review") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirmInstall,
                        enabled = report.isEligibleForInstallation,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (report.isPublisherTrusted) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Install Extension")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = manifest.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(manifest.type.toDisplayName(), fontSize = 11.sp) }
                            )
                            Text(
                                text = "v${manifest.version}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Description & Publisher Info
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (manifest.description.isNotBlank()) {
                        Text(
                            text = manifest.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Publisher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (report.isPublisherTrusted) {
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF30D158), modifier = Modifier.size(16.dp))
                            }
                            Text(report.publisherName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Package Size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${"%.2f".format(report.totalSizeBytes / 1024.0 / 1024.0)} MB", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Package ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(manifest.id, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }

            // Security Audit Summary
            Text("Security & Verification Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            VerificationStatusRow("Manifest Format", report.formatStatus, report.formatDetails)
            VerificationStatusRow("Publisher Signature", report.signatureStatus, report.signatureDetails)
            VerificationStatusRow("Asset Integrity", report.assetIntegrityStatus, report.assetIntegrityDetails)
            VerificationStatusRow("App Compatibility", report.compatibilityStatus, report.compatibilityDetails)
            VerificationStatusRow("Online Revocation", report.onlineRevocationStatus, report.onlineRevocationDetails)

            // Warnings List
            if (report.warnings.isNotEmpty()) {
                Text("Security Findings & Warnings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                report.warnings.forEach { warning ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (warning.isCritical) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (warning.isCritical) Icons.Default.Error else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (warning.isCritical) Color.Red else Color(0xFFF57F17)
                            )
                            Column {
                                Text(
                                    text = warning.title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (warning.isCritical) Color.Red else Color(0xFFF57F17),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = warning.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationStatusRow(
    label: String,
    status: VerificationResult,
    details: String
) {
    val (statusText, statusColor, icon) = when (status) {
        VerificationResult.VERIFIED, VerificationResult.PASSED -> Triple("PASSED", Color(0xFF2E7D32), Icons.Default.CheckCircle)
        VerificationResult.WARNING -> Triple("WARNING", Color(0xFFF57F17), Icons.Default.Warning)
        VerificationResult.FAILED -> Triple("FAILED", Color(0xFFC62828), Icons.Default.Cancel)
        VerificationResult.NOT_CHECKED -> Triple("OFFLINE / UNCHECKED", Color.Gray, Icons.Default.Info)
        VerificationResult.UNSUPPORTED -> Triple("UNSUPPORTED", Color.DarkGray, Icons.AutoMirrored.Filled.Help)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
            if (details.isNotBlank()) {
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
