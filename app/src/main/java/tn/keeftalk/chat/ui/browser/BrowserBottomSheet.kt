package tn.keeftalk.chat.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteInfoBottomSheet(
    url: String,
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val siteData = remember(url) { viewModel.getSiteInfoData(url) }
    val context = LocalContext.current

    var certOpen by remember { mutableStateOf(false) }
    var cookiesOpen by remember { mutableStateOf(false) }
    var dataOpen by remember { mutableStateOf(false) }
    var permsOpen by remember { mutableStateOf(false) }

    val cookiesList = remember { siteData.cookies }
    val permsMap = remember { mutableStateMapOf<String, String>().apply { putAll(siteData.permissions) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .heightIn(max = 600.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val hostFirstChar = siteData.host.firstOrNull()?.uppercase() ?: "W"
                val hash = viewModel.hash(siteData.host)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    listOf(Color(0xFFff9a3c), Color(0xFF2d9bf0), Color(0xFF7a5cff))[hash % 3],
                                    listOf(Color(0xFFff4f7b), Color(0xFF18a999), Color(0xFFe0447a))[hash % 3]
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(hostFirstChar, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(siteData.host, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF16181d), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (siteData.isSecure) "Connection is secure" else "Connection is not secure",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (siteData.isSecure) Color(0xFF168a4a) else Color(0xFFd12e2e)
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf0f1f4), contentColor = Color(0xFF16181d))
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // Trackers Banner
            val trkEnabled = viewModel.settings.collectAsState().value.trk
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (trkEnabled) Color(0xFFe8f6ee) else Color(0xFFf0f1f4))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = if (trkEnabled) "${siteData.trackersBlocked} trackers blocked on this page" else "Tracker blocking is off",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (trkEnabled) Color(0xFF168a4a) else Color(0xFF6b6e76)
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Section 1: Certificate
                item {
                    SectionHeader(
                        title = "Certificate",
                        isOpen = certOpen,
                        onClick = { certOpen = !certOpen }
                    )
                    AnimatedVisibility(visible = certOpen) {
                        Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (siteData.isSecure) {
                                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                KeyValueRow("Issued to", siteData.host)
                                KeyValueRow("Issued by", siteData.issuer)
                                KeyValueRow("Valid from", dateFormat.format(siteData.validFrom))
                                KeyValueRow("Expires", dateFormat.format(siteData.expiresTo))
                                KeyValueRow("Serial number", siteData.serialNumber)
                                KeyValueRow("Public key", siteData.publicKey)
                                KeyValueRow("Protocol", "TLS 1.3")
                                KeyValueRow("SHA-256", siteData.fingerprint)

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Fingerprint", siteData.fingerprint))
                                        viewModel.showToast("Fingerprint copied")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Text("Copy fingerprint", fontSize = 13.sp)
                                }
                            } else {
                                Text(
                                    "This page has no certificate, so it isn't encrypted. Others on the network could read what you send.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8a4b00)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFeceef2))
                }

                // Section 2: Cookies
                item {
                    SectionHeader(
                        title = "Cookies (${cookiesList.size})",
                        isOpen = cookiesOpen,
                        onClick = { cookiesOpen = !cookiesOpen }
                    )
                    AnimatedVisibility(visible = cookiesOpen) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            if (cookiesList.isEmpty()) {
                                Text("No cookies stored.", fontSize = 13.sp, color = Color(0xFF6b6e76))
                            } else {
                                cookiesList.forEachIndexed { idx, cookie ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cookie.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                "${if (cookie.isThirdParty) "Third-party" else "First-party"} · expires ${cookie.expires}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF6b6e76)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                cookiesList.removeAt(idx)
                                                viewModel.showToast("Cookie deleted")
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Delete", fontSize = 12.sp)
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        cookiesList.clear()
                                        viewModel.showToast("Cookies cleared")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFd12e2e)),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Clear all cookies", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFeceef2))
                }

                // Section 3: Site Data
                item {
                    val totalKb = siteData.siteStorage.values.sum()
                    SectionHeader(
                        title = "Site data (${totalKb} KB)",
                        isOpen = dataOpen,
                        onClick = { dataOpen = !dataOpen }
                    )
                    AnimatedVisibility(visible = dataOpen) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            siteData.siteStorage.forEach { (key, sizeKb) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key, fontSize = 13.sp)
                                    Text("${sizeKb} KB", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.showToast("Site data cleared")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFd12e2e)),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Clear site data", fontSize = 13.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFeceef2))
                }

                // Section 4: Permissions
                item {
                    SectionHeader(
                        title = "Permissions",
                        isOpen = permsOpen,
                        onClick = { permsOpen = !permsOpen }
                    )
                    AnimatedVisibility(visible = permsOpen) {
                        Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            permsMap.forEach { (perm, currentVal) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(perm, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFeef0f4))
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf("ask" to "Ask", "allow" to "Allow", "block" to "Block").forEach { (valKey, label) ->
                                            val isSel = currentVal == valKey
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) Color.White else Color.Transparent)
                                                    .clickable { permsMap[perm] = valKey }
                                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                                            ) {
                                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16181d))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historyList by viewModel.history.collectAsState()
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .heightIn(max = 500.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16181d))
                    Text("${historyList.size} pages", fontSize = 13.sp, color = Color(0xFF6b6e76))
                }

                if (historyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearHistory() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFd12e2e))
                    ) {
                        Text("Clear", fontSize = 13.sp)
                    }
                }
            }

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No history yet.", color = Color(0xFF6b6e76), fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyList) { (url, timestamp) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.navigateToUrl(url)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    viewModel.titleOf(url),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF16181d),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${viewModel.hostOf(url)} · ${timeFormat.format(Date(timestamp))}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6b6e76)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9aa0b4))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    isOpen: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF16181d))
        Icon(
            imageVector = if (isOpen) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF8a8e98)
        )
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF6b6e76), modifier = Modifier.width(104.dp))
        Text(value, fontSize = 13.sp, color = Color(0xFF16181d), modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsBottomSheet(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val downloads by viewModel.downloads.collectAsState()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .heightIn(max = 500.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Downloads", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16181d))
                    Text("${downloads.size} files", fontSize = 13.sp, color = Color(0xFF6b6e76))
                }

                if (downloads.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearDownloads() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFd12e2e))
                    ) {
                        Text("Clear", fontSize = 13.sp)
                    }
                }
            }

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No downloads yet. Download files or pictures while browsing.", color = Color(0xFF6b6e76), fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(downloads) { item ->
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFf6f7f9),
                            tonalElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF16181d),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (item.isComplete) "Complete" else "${(item.progress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (item.isComplete) Color(0xFF168a4a) else Color(0xFF0a7cff)
                                    )
                                }

                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { item.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color(0xFF0a7cff),
                                    trackColor = Color(0xFFe2e4ea)
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(item.timestamp)),
                                        fontSize = 11.sp,
                                        color = Color(0xFF6b6e76)
                                    )

                                    if (item.isComplete && item.localPath != null) {
                                        TextButton(
                                            onClick = {
                                                try {
                                                    val file = java.io.File(item.localPath)
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        context.packageName + ".fileprovider",
                                                        file
                                                    )
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    viewModel.showToast("Cannot open file: ${e.message}")
                                                }
                                            }
                                        ) {
                                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

