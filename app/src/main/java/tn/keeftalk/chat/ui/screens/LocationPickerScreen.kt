package tn.keeftalk.chat.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun LocationPickerScreen(
    onBack: () -> Unit,
    onLocationShared: (Double, Double, Boolean, String) -> Unit
) {
    onBack()
}
