package com.keeftalk.chat.ui.components

import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@Composable
fun PdfPageImage(
    file: File?,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file, pageIndex) {
        if (file == null || !file.exists()) return@LaunchedEffect
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex < renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                val scale = 1.2f
                                val bmp = Bitmap.createBitmap(
                                    (page.width * scale).toInt(),
                                    (page.height * scale).toInt(),
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap = bmp
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfPageImage", "Error rendering page $pageIndex", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

/**
 * Renders a preview of a PDF file with manual paging controls.
 * 
 * IMPORTANT: Manual paging via buttons is REQUIRED. Do NOT re-enable HorizontalPager or 
 * any swipe-based navigation here, as it conflicts with chat list gestures and 
 * attachment review workflows.
 */
@Composable
fun PdfPreviewContent(
    uri: String,
    modifier: Modifier = Modifier,
    thumbnailUri: String? = null,
    isLocked: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    // CRITICAL DESIGN DECISION: We use a manual currentPage state instead of HorizontalPager.
    // This is to prevent swipe conflicts with the parent chat list and to fulfill 
    // the requirement for explicit button-based navigation. Do NOT refactor to Pager.
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var isLoadingFile by remember { mutableStateOf(true) }
    var errorOccurred by remember { mutableStateOf(false) }

    val resolvedFile = remember(uri) { mutableStateOf<File?>(null) }
    
    LaunchedEffect(uri) {
        if (uri.isBlank()) {
            resolvedFile.value = null
            isLoadingFile = false
            return@LaunchedEffect
        }
        isLoadingFile = true
        errorOccurred = false
        withContext(Dispatchers.IO) {
            try {
                val file = when {
                    uri.startsWith("content://") -> {
                        val tempFile = File(context.cacheDir, "pdf_preview_${uri.hashCode()}.pdf")
                        if (!tempFile.exists()) {
                            context.contentResolver.openInputStream(uri.toUri())?.use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        tempFile
                    }
                    uri.startsWith("http") -> {
                        val tempFile = File(context.cacheDir, "pdf_remote_${uri.hashCode()}.pdf")
                        if (!tempFile.exists()) {
                            val connection = java.net.URL(uri).openConnection()
                            connection.connect()
                            connection.getInputStream().use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        tempFile
                    }
                    else -> File(uri)
                }
                if (file.exists()) {
                    resolvedFile.value = file
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfPreviewContent", "Error resolving file", e)
                errorOccurred = true
            } finally {
                isLoadingFile = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        if (isLocked) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PDF Locked", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            if (pageCount > 0) {
                PdfPageImage(file = resolvedFile.value, pageIndex = currentPage)
            } else if (thumbnailUri != null && !errorOccurred) {
                AsyncImage(
                    model = thumbnailUri,
                    contentDescription = "PDF Thumbnail",
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentScale = ContentScale.Fit
                )
            }

            if (isLoadingFile) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }

            if (errorOccurred) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
                    Text("Failed to load PDF", color = Color.Red.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            // Overlay Navigation (Manual Only)
            if (pageCount > 1 && !isLoadingFile) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        modifier = Modifier.size(32.dp),
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    
                    Text(
                        text = "${currentPage + 1} / $pageCount",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { if (currentPage < pageCount - 1) currentPage++ },
                        modifier = Modifier.size(32.dp),
                        enabled = currentPage < pageCount - 1
                    ) {
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        
        // PDF Badge
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            color = Color.Red,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "PDF",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
