package com.keeftalk.chat.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.VaultItem
import com.keeftalk.chat.ui.screens.ImageViewer
import com.keeftalk.chat.ui.screens.VideoViewer
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMediaViewer(
    item: VaultItem,
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var decryptedFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(item) {
        isLoading = true
        android.util.Log.d("VAULT_UI", "Opening media viewer for item: ${item.id} | title: ${item.title}")
        decryptedFile = viewModel.getDecryptedFile(item)
        if (decryptedFile == null) {
            android.util.Log.e("VAULT_UI", "Failed to get decrypted file for item: ${item.id}")
        } else {
            android.util.Log.i("VAULT_UI", "File decrypted successfully: ${decryptedFile!!.absolutePath}")
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else if (decryptedFile != null) {
            val uri = decryptedFile!!.absolutePath
            
            when {
                item.file?.fileType == com.keeftalk.chat.domain.model.FileType.IMAGE -> {
                    ImageViewer(uri = uri, onToggleControls = {})
                }
                item.file?.fileType == com.keeftalk.chat.domain.model.FileType.VIDEO -> {
                    VideoViewer(uri = uri, onToggleControls = {})
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(120.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(item.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { com.keeftalk.chat.util.FileUtils.openFile(context, decryptedFile!!) }) {
                            Text("Open in System Viewer")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Failed to decrypt file", color = Color.White)
                Text(
                    "Check Logcat for [FILE_PIPELINE] to see details", 
                    color = Color.Gray, 
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Top Bar
        TopAppBar(
            title = { Text(item.title, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { scope.launch { com.keeftalk.chat.ui.vault.shareFile(context, item, viewModel) } }) {
                    Icon(Icons.Default.Share, null, tint = Color.White)
                }
                IconButton(onClick = { viewModel.downloadFile(item) }) {
                    Icon(Icons.Default.Download, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}
