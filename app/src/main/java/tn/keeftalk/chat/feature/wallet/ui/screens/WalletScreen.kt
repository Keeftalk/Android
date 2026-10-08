package tn.keeftalk.chat.feature.wallet.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import tn.keeftalk.chat.feature.finances.ui.WalletsCardsScreen
import tn.keeftalk.chat.feature.finances.viewmodel.FinancesViewModel

@Composable
fun WalletScreen(
    viewModel: FinancesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    WalletsCardsScreen(
        state = state,
        viewModel = viewModel,
        onBack = onBack
    )
}
