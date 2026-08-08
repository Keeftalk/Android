package com.keeftalk.chat.feature.wallet.ui.screens

import androidx.compose.runtime.Composable
import com.keeftalk.chat.feature.wallet.viewmodel.WalletViewModel
import com.keeftalk.chat.ui.components.FabActionType
import kotlinx.coroutines.flow.MutableSharedFlow

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    fabActionFlow: MutableSharedFlow<FabActionType>
) {}
