package tn.keeftalk.chat.ui.settings

import androidx.compose.runtime.*
import tn.keeftalk.chat.ui.books.components.BooksSettingsScreen as BookCompSettings

@Composable
fun BooksSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.fullSettings.collectAsState()
    val bookSettings = settings.bookSettings

    BookCompSettings(
        settings = bookSettings,
        onBack = onBack,
        onSettingsChanged = { newSettings ->
            viewModel.updateBookSettings { newSettings }
        }
    )
}
