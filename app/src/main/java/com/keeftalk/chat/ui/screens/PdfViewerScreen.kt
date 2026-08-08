package com.keeftalk.chat.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfPageItem(file: File, index: Int, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file, index) {
        withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (index < renderer.pageCount) {
                            renderer.openPage(index).use { page ->
                                // Higher resolution for full screen viewer (2x)
                                val width = (page.width * 1.5f).toInt()
                                val height = (page.height * 1.5f).toInt()
                                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfPageItem", "Error rendering page $index", e)
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
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: PdfViewerViewModel,
    myId: String,
    onBack: () -> Unit
) {
    val message by viewModel.message.collectAsState()
    val localFile by viewModel.localFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var pageCount by remember { mutableIntStateOf(0) }
    var isCheckingPageCount by remember { mutableStateOf(false) }

    LaunchedEffect(localFile) {
        val file = localFile
        if (file != null && file.exists()) {
            isCheckingPageCount = true
            withContext(Dispatchers.IO) {
                try {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfViewerScreen", "Error getting page count", e)
                }
            }
            isCheckingPageCount = false
        }
    }

    Scaffold(
        topBar = {
            val isMe = message?.senderId == myId
            val isLocked = message?.mediaLocked == true
            
            TopAppBar(
                title = { 
                    Column {
                        Text("PDF Viewer", style = MaterialTheme.typography.titleMedium)
                        if (pageCount > 0) {
                            Text("$pageCount pages", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isMe || !isLocked) {
                        IconButton(onClick = { viewModel.download() }) {
                            Icon(Icons.Default.Download, "Download", tint = Color.White)
                        }
                    }
                    if (isMe) {
                        IconButton(onClick = { viewModel.toggleLock() }) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle Lock",
                                tint = if (isLocked) Color.Red else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
            if (isLoading || isCheckingPageCount) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (pageCount == 0) {
                Text(error ?: "Failed to load PDF", color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else {
                localFile?.let { file ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pageCount) { index ->
                            PdfPageItem(file = file, index = index)
                        }
                    }
                }
            }
        }
    }
}
