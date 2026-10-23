package tn.keeftalk.chat.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun LocationDetailScreen(
    latitude: Double,
    longitude: Double,
    title: String? = null,
    isLive: Boolean = false,
    onBack: () -> Unit
) {
    onBack()
}
