package tn.keeftalk.chat.ui.docs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow
import tn.keeftalk.chat.domain.model.Doc
import tn.keeftalk.chat.domain.model.DocFormat
import tn.keeftalk.chat.ui.components.FabActionType
import tn.keeftalk.chat.ui.docs.components.DocThumbnailGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(
    viewModel: DocsViewModel,
    onOpenDoc: (String) -> Unit,
    onSettings: () -> Unit,
    fabActionFlow: SharedFlow<FabActionType>? = null
) {
    val context = LocalContext.current
    val docs by viewModel.filteredDocs.collectAsState()
    val totalBytes by viewModel.totalStorageBytes.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    var selectedDocForActions by remember { mutableStateOf<Doc?>(null) }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importDocsFromUris(uris)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFolderFromUri(it) }
    }

    LaunchedEffect(fabActionFlow) {
        fabActionFlow?.collect { action ->
            when (action) {
                FabActionType.IMPORT_DOC_FILES -> multiFilePickerLauncher.launch(arrayOf("*/*"))
                FabActionType.IMPORT_DOC_FOLDER -> folderPickerLauncher.launch(null)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keeftalk", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.syncDocs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh & Sync")
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View"
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Storage Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${docs.size} Documents Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Storage used: ${Formatter.formatFileSize(context, totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    DocCategoryFilter.ALL to "All",
                    DocCategoryFilter.PDF to "PDF",
                    DocCategoryFilter.MARKDOWN to "Markdown",
                    DocCategoryFilter.HTML to "HTML",
                    DocCategoryFilter.EPUB_DOCX to "EPUB / DOCX",
                    DocCategoryFilter.CODE to "Code",
                    DocCategoryFilter.FAVORITES to "Favorites"
                )

                items(filters) { (filter, label) ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setSelectedFilter(filter) },
                        label = { Text(label) },
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Document Grid or List Content
            if (docs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No documents found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import PDF, Markdown, HTML, TXT, DOCX, EPUB, or code files using the Adaptive FAB menu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(docs, key = { it.id }) { doc ->
                        DocCardGrid(
                            doc = doc,
                            onClick = { onOpenDoc(doc.id) },
                            onMoreClick = { selectedDocForActions = doc },
                            onFavoriteClick = { viewModel.toggleFavorite(doc) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(docs, key = { it.id }) { doc ->
                        DocCardList(
                            doc = doc,
                            onClick = { onOpenDoc(doc.id) },
                            onMoreClick = { selectedDocForActions = doc },
                            onFavoriteClick = { viewModel.toggleFavorite(doc) }
                        )
                    }
                }
            }
        }

        selectedDocForActions?.let { doc ->
            DocActionSheet(
                doc = doc,
                context = context,
                onFavorite = {
                    viewModel.toggleFavorite(doc)
                    selectedDocForActions = null
                },
                onDelete = {
                    viewModel.deleteDoc(doc.id)
                    selectedDocForActions = null
                },
                onDismiss = { selectedDocForActions = null }
            )
        }
    }
}

@Composable
fun DocCardGrid(
    doc: Doc,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(doc) {
        thumbnailBitmap = DocThumbnailGenerator.getThumbnail(context, doc)
    }

    val formatColor = when (doc.format) {
        DocFormat.PDF -> Color(0xFFE53935)
        DocFormat.MARKDOWN -> Color(0xFF00ACC1)
        DocFormat.HTML -> Color(0xFFFB8C00)
        DocFormat.TXT -> Color(0xFF7CB342)
        DocFormat.DOCX -> Color(0xFF1E88E5)
        DocFormat.EPUB -> Color(0xFF8E24AA)
        DocFormat.CODE -> Color(0xFF43A047)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(formatColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = doc.format.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = formatColor
                    )
                }

                Row {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (doc.isFavorite) Color(0xFFFFB300) else LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            thumbnailBitmap?.let { bmp ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = doc.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = doc.authorOrSource.ifBlank { "Local File" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { doc.percentProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = formatColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${doc.percentProgress}% read",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DocCardList(
    doc: Doc,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val formatColor = when (doc.format) {
        DocFormat.PDF -> Color(0xFFE53935)
        DocFormat.MARKDOWN -> Color(0xFF00ACC1)
        DocFormat.HTML -> Color(0xFFFB8C00)
        DocFormat.TXT -> Color(0xFF7CB342)
        DocFormat.DOCX -> Color(0xFF1E88E5)
        DocFormat.EPUB -> Color(0xFF8E24AA)
        DocFormat.CODE -> Color(0xFF43A047)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(formatColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doc.format.name.take(3),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = formatColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.format.name} · ${doc.percentProgress}% read",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (doc.isFavorite) Color(0xFFFFB300) else LocalContentColor.current
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocActionSheet(
    doc: Doc,
    context: Context,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(doc.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Format: ${doc.format.name} · Size: ${Formatter.formatFileSize(context, doc.fileSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Added: ${dateFormat.format(Date(doc.createdAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            ListItem(
                headlineContent = { Text(if (doc.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                leadingContent = { Icon(if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null) },
                modifier = Modifier.clickable { onFavorite() }
            )

            ListItem(
                headlineContent = { Text("Share Document File") },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                modifier = Modifier.clickable {
                    val file = java.io.File(doc.filePath)
                    if (file.exists()) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = doc.mimeType.ifBlank { "*/*" }
                            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                    }
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Delete Document", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDelete() }
            )
        }
    }
}
