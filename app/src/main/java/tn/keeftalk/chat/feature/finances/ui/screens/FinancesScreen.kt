package tn.keeftalk.chat.feature.finances.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import tn.keeftalk.chat.feature.finances.ui.*
import tn.keeftalk.chat.feature.finances.viewmodel.FinancesViewModel
import tn.keeftalk.chat.ui.components.FabActionType

@Composable
fun FinancesScreen(
    viewModel: FinancesViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    fabActionFlow: MutableSharedFlow<FabActionType>,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Listen to global AdaptiveFab actions for My Finances
    LaunchedEffect(Unit) {
        fabActionFlow.collect { action ->
            when (action) {
                FabActionType.ADD_FINANCES -> {
                    val targetForm = when (state.activeTab) {
                        "wealth" -> "asset"
                        "plan" -> "budget"
                        else -> "expense"
                    }
                    viewModel.openSheet(targetForm)
                }
                FabActionType.FINANCES_TOOLS -> {
                    viewModel.openToolScreen("tools")
                }
                else -> {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Content Area
            if (state.activeToolScreen != null) {
                // Render Sub-view / Tool Screen
                Box(modifier = Modifier.weight(1f)) {
                    when (state.activeToolScreen) {
                        "tools" -> ToolsGridScreen(viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_wallets" -> WalletsCardsScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_cal" -> FinancialCalendarScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_fc" -> ForecastScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_sim" -> SimulatorScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_health" -> HealthFireScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_debt" -> DebtPayoffScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_subs" -> SubscriptionsScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_shared" -> SharedExpensesScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_chal" -> ChallengesScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_nw" -> NetWorthHistoryScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_road" -> GoalsRoadmapScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_ai" -> FinancesAssistantScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_docs" -> ReceiptsDocsScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "t_trend" -> TrendsScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        "settings" -> FinancesSettingsScreen(state = state, viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                        else -> ToolsGridScreen(viewModel = viewModel, onBack = { viewModel.closeToolScreen() })
                    }
                }
            } else {
                // Render Main Section Tab Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp)
                ) {
                    HeroBannerContainer(
                        state = state,
                        viewModel = viewModel,
                        onTabSelected = { viewModel.selectTab(it) }
                    )

                    SectionSummaryCard(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        when (state.activeTab) {
                            "spend" -> SpendingContent(state = state, viewModel = viewModel)
                            "plan" -> PlanContent(state = state, viewModel = viewModel)
                            "wealth" -> WealthContent(state = state, viewModel = viewModel)
                            else -> OverviewContent(state = state, viewModel = viewModel)
                        }
                    }
                }
            }
        }

        // Toast Overlay
        ToastOverlay(
            message = state.toastMessage,
            onDismiss = { viewModel.clearToast() }
        )

        // Universal "+ Add" Bottom Sheet
        if (state.activeSheet != null) {
            AddFinancesSheet(
                initialForm = state.activeSheet!!,
                viewModel = viewModel,
                onDismiss = { viewModel.closeSheet() }
            )
        }
    }
}
