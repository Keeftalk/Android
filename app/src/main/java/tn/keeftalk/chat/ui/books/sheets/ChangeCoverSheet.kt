package tn.keeftalk.chat.ui.books.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.Book
import tn.keeftalk.chat.domain.model.CustomCover
import tn.keeftalk.chat.ui.books.components.BookCover

data class VaultCoverItem(
    val name: String,
    val colors: List<String>
)

val VAULT_COVERS = listOf(
    VaultCoverItem("aurora.jpg", listOf("#00B894", "#2D3436")),
    VaultCoverItem("dusk.jpg", listOf("#E17055", "#6C2A6A")),
    VaultCoverItem("ocean.png", listOf("#0984E3", "#052A4F")),
    VaultCoverItem("paper.png", listOf("#B2A58A", "#5A4C33")),
    VaultCoverItem("rose.jpg", listOf("#FD79A8", "#6C1D45")),
    VaultCoverItem("midnight.png", listOf("#4834D4", "#0A0A2A"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCoverSheet(
    book: Book,
    onDismiss: () -> Unit,
    onCoverSelected: (CustomCover?) -> Unit
) {
    var viewMode by remember { mutableStateOf("menu") } // "menu", "cb", "cv"
    val context = androidx.compose.ui.platform.LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "custom_cover_${book.id}_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } catch (_: Exception) {
                uri.toString()
            }
            onCoverSelected(CustomCover(imageUrl = localPath))
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewMode != "menu") {
                    IconButton(onClick = { viewMode = "menu" }) {
                        Text(text = "‹", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Text(
                    text = when (viewMode) {
                        "cb" -> "Choose from Book"
                        "cv" -> "Choose from Vault"
                        else -> "Change Cover"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Text(text = "✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            when (viewMode) {
                "cb" -> {
                    // Custom Color Gradients derived from Book
                    val options = listOf(
                        "Nebula" to listOf("#6C63FF", "#00CCCC"),
                        "Sunset" to listOf("#FF7675", "#6C5CE7"),
                        "Emerald" to listOf("#00B894", "#00CEC9"),
                        "Midnight" to listOf("#2D3436", "#0984E3")
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(options) { (title, colors) ->
                            val coverOpt = CustomCover(colors = colors)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onCoverSelected(coverOpt)
                                    onDismiss()
                                }
                            ) {
                                BookCover(book = book, overrideCover = coverOpt, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                "cv" -> {
                    // Vault cover options
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(VAULT_COVERS) { item ->
                            val vaultCover = CustomCover(colors = item.colors)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onCoverSelected(vaultCover)
                                    onDismiss()
                                }
                            ) {
                                BookCover(book = book, overrideCover = vaultCover, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = item.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                else -> {
                    // Main Cover Source Menu
                    ActionSheetRow(icon = Icons.Default.Book, label = "From book", hasChevron = true, onClick = { viewMode = "cb" })
                    ActionSheetRow(icon = Icons.Default.Image, label = "Upload image", hasChevron = true, onClick = { imagePickerLauncher.launch("image/*") })
                    ActionSheetRow(icon = Icons.Default.Lock, label = "From vault", hasChevron = true, onClick = { viewMode = "cv" })
                    if (book.customCover != null) {
                        ActionSheetRow(icon = Icons.Default.Refresh, label = "Reset to original", onClick = {
                            onCoverSelected(null)
                            onDismiss()
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
