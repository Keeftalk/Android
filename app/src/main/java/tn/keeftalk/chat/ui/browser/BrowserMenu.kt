package tn.keeftalk.chat.ui.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MenuItemData(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun BrowserMenuPopup(
    viewModel: BrowserViewModel,
    onOpenSettings: () -> Unit,
    onOpenSiteInfo: () -> Unit
) {
    val isOpen by viewModel.isMenuOpen.collectAsState()
    val isHome = viewModel.isHome
    val context = LocalContext.current

    val menuItems = if (!isHome) {
        listOf(
            MenuItemData("find", "Find in page", Icons.Default.Search),
            MenuItemData("reader", if (viewModel.isReaderMode.collectAsState().value) "Exit reader mode" else "Reader mode", Icons.AutoMirrored.Filled.Article),
            MenuItemData("downloads", "Downloads", Icons.Default.Star),
            MenuItemData("share", "Share link", Icons.Default.Share),
            MenuItemData("site", "Site information", Icons.Default.Info),
            MenuItemData("save", "Save to quick links", Icons.Default.Star),
            MenuItemData("read", "Add to reading list", Icons.AutoMirrored.Filled.MenuBook),
            MenuItemData("hist", "History", Icons.Default.History),
            MenuItemData("set", "Settings", Icons.Default.Settings),
            MenuItemData("close", "Close tab", Icons.Default.Delete)
        )
    } else {
        listOf(
            MenuItemData("new", "New tab", Icons.Default.Add),
            MenuItemData("downloads", "Downloads", Icons.Default.Star),
            MenuItemData("hist", "History", Icons.Default.History),
            MenuItemData("set", "Settings", Icons.Default.Settings),
            MenuItemData("all", "Close all tabs", Icons.Default.Delete)
        )
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.width(220.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                    menuItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.isMenuOpen.value = false
                                    when (item.id) {
                                        "find" -> viewModel.isFindInPageOpen.value = true
                                        "reader" -> {
                                            viewModel.isReaderMode.value = !viewModel.isReaderMode.value
                                            viewModel.showToast(if (viewModel.isReaderMode.value) "Reader mode on" else "Reader mode off")
                                        }
                                        "share" -> {
                                            val tab = viewModel.tabs.value.getOrNull(viewModel.currentTabIndex.value)
                                            val url = tab?.currentUrl ?: ""
                                            if (url.isNotEmpty()) {
                                                try {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, url)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share link"))
                                                } catch (_: Exception) {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Link", url))
                                                    viewModel.showToast("Link copied to share")
                                                }
                                            }
                                        }
                                        "downloads" -> viewModel.isDownloadsSheetOpen.value = true
                                        "site" -> onOpenSiteInfo()
                                        "save" -> viewModel.saveCurrentPageToQuickLinks()
                                        "read" -> viewModel.addCurrentPageToReadingList()
                                        "hist" -> viewModel.isHistorySheetOpen.value = true
                                        "set" -> onOpenSettings()
                                        "close" -> viewModel.closeTab(viewModel.currentTabIndex.value)
                                        "new" -> viewModel.addNewTab()
                                        "all" -> viewModel.closeAllTabs()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(item.icon, contentDescription = null, tint = Color(0xFF16181d), modifier = Modifier.size(20.dp))
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF16181d),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
    }
}
