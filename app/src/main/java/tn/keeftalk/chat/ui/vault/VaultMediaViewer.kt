package tn.keeftalk.chat.ui.vault

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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import tn.keeftalk.chat.domain.model.VaultItem
import tn.keeftalk.chat.ui.screens.ImageViewer
import tn.keeftalk.chat.ui.screens.VideoViewer
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                item.file?.fileType == tn.keeftalk.chat.domain.model.FileType.IMAGE -> {
                    ImageViewer(uri = uri, onToggleControls = {})
                }
                item.file?.fileType == tn.keeftalk.chat.domain.model.FileType.VIDEO -> {
                    VideoViewer(uri = uri, onToggleControls = {})
                }
                else -> {
                    val isPdf = item.title.endsWith(".pdf", ignoreCase = true) || decryptedFile?.name?.endsWith(".pdf", ignoreCase = true) == true || item.file?.mimeType == "application/pdf"
                    val ext = item.title.substringAfterLast(".", "").lowercase()
                    val isCodeFile = setOf("kt", "java", "py", "js", "cpp", "json", "xml", "html", "css", "ts", "rs", "go", "c", "h", "md", "txt", "log").contains(ext)
                    
                    if (isPdf) {
                        VaultPdfViewer(file = decryptedFile!!, title = item.title, onBack = onBack)
                    } else if (isCodeFile) {
                        tn.keeftalk.chat.extension.renderer.UnifiedCodeViewer(file = decryptedFile!!, title = item.title, onBack = onBack)
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(120.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(item.title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = { tn.keeftalk.chat.util.FileUtils.openFile(context, decryptedFile!!) }) {
                                Text("Open in System Viewer")
                            }
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
                IconButton(onClick = { scope.launch { tn.keeftalk.chat.ui.vault.shareFile(context, item, viewModel) } }) {
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

@Composable
fun PdfVaultPageItem(file: File, index: Int, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file, index) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        if (index < renderer.pageCount) {
                            renderer.openPage(index).use { page ->
                                val width = (page.width * 1.5f).toInt()
                                val height = (page.height * 1.5f).toInt()
                                val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfVaultPageItem", "Error rendering page $index", e)
            } finally {
                isLoading = false
            }
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.7f)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun VaultPdfViewer(
    file: File,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var pageCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VaultPdfViewer", "Error getting page count", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
        } else if (pageCount == 0) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Failed to load PDF ($title)", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { tn.keeftalk.chat.util.FileUtils.openFile(context, file) }) {
                    Text("Open in System Viewer")
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 80.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pageCount) { index ->
                    PdfVaultPageItem(file = file, index = index)
                }
            }
        }
    }
}
