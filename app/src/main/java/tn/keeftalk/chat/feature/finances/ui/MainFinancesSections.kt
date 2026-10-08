package tn.keeftalk.chat.feature.finances.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.data.local.entities.FinanceAccountEntity
import tn.keeftalk.chat.data.local.entities.FinanceTransactionEntity
import tn.keeftalk.chat.feature.finances.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Composable
fun HeroBannerContainer(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hideAmounts = state.settings.hide
    val blurModifier = if (hideAmounts) Modifier.blur(8.dp) else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .weight(1f)
                .height(244.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 30.dp, bottomEnd = 10.dp))
        ) {
            when (state.activeTab) {
                "spend" -> SpendingHeroCard(state = state, viewModel = viewModel, blurModifier = blurModifier)
                "plan" -> PlanHeroCard(state = state, viewModel = viewModel, blurModifier = blurModifier)
                "wealth" -> WealthHeroCard(state = state, viewModel = viewModel, blurModifier = blurModifier)
                else -> OverviewHeroCard(state = state, viewModel = viewModel, blurModifier = blurModifier)
            }
        }

        // Section Rail
        FinanceSectionRail(
            activeTab = state.activeTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
fun SectionSummaryCard(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    modifier: Modifier = Modifier
) {
    val currentMonthTxs = viewModel.getMonthTransactions(0)
    val liquid = viewModel.computeLiquidMoney()
    val netWorth = viewModel.computeNetWorth()

    FinanceCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (state.activeTab) {
                        "spend" -> "Monthly Spending Overview"
                        "plan" -> "Financial Plan & Salary"
                        "wealth" -> "Net Worth & Accounts"
                        else -> "Financial Overview"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (state.activeTab) {
                        "spend" -> "${currentMonthTxs.size} transactions this month"
                        "plan" -> "${state.budgets.size} budgets · ${state.goals.size} savings goals"
                        "wealth" -> "${state.accounts.size} accounts/cards · ${state.assets.size} assets"
                        else -> "Available Liquid: ${viewModel.formatCurrency(liquid)}"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = viewModel.formatCurrency(netWorth),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun ToastOverlay(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (message != null) {
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2500)
                onDismiss()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewHeroCard(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    blurModifier: Modifier
) {
    val netWorth = viewModel.computeNetWorth()
    val (history, _) = viewModel.computeNetWorthHistory(7)
    val startVal = history.firstOrNull() ?: netWorth
    val diff = netWorth - startVal
    val liquid = viewModel.computeLiquidMoney()
    val assets = viewModel.computeAssetsTotal()
    val debt = viewModel.computeTotalDebtOwed()
    val tot = max(1.0, liquid + assets)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF5A52D9), Color(0xFF3C7FE0), Color(0xFF00B8C4))))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Net worth", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Text(
                    text = viewModel.formatCurrency(netWorth),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = blurModifier
                )
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${if (diff >= 0) "▲" else "▼"} ${viewModel.formatCurrency(Math.abs(diff))} this month",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            SparklineChart(data = history, modifier = Modifier.fillMaxWidth().height(48.dp))

            // Footer breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroStatItem("Liquid", viewModel.formatCompactCurrency(liquid), (liquid / tot).toFloat(), Modifier.weight(1f))
                HeroStatItem("Assets", viewModel.formatCompactCurrency(assets), (assets / tot).toFloat(), Modifier.weight(1f))
                HeroStatItem("Debt", viewModel.formatCompactCurrency(debt), (debt / max(1.0, debt + tot)).toFloat(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SpendingHeroCard(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    blurModifier: Modifier
) {
    val cmTxs = viewModel.getMonthTransactions(0)
    val pmTxs = viewModel.getMonthTransactions(1)
    val spentCM = viewModel.computeExpenses(cmTxs)
    val spentPM = viewModel.computeExpenses(pmTxs)
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val pace = (spentCM / max(1, dayOfMonth)) * 30
    val paceDiffPct = if (spentPM > 0) (((pace - spentPM) / spentPM) * 100).toInt() else 0

    val dailySpend = (1..dayOfMonth).map { d ->
        val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        val dateStr = String.format(Locale.US, "%s-%02d", ym, d)
        state.transactions.filter { it.type == "exp" && it.date == dateStr }.sumOf { it.amt }
    }
    val avgDaily = spentCM / max(1, dayOfMonth)
    val biggestSpend = cmTxs.filter { it.type == "exp" }.maxOfOrNull { it.amt } ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF7D5CFF), Color(0xFFB05CE0), Color(0xFFFF7AB6))))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Spent this month", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Text(
                    text = viewModel.formatCurrency(spentCM),
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = blurModifier
                )
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${if (paceDiffPct >= 0) "▲" else "▼"} ${Math.abs(paceDiffPct)}% vs last month's pace",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            DailySpendBarChart(dailyAmounts = dailySpend, average = avgDaily, modifier = Modifier.fillMaxWidth().height(48.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroStatItem("Income", viewModel.formatCompactCurrency(viewModel.computeIncome(cmTxs)), 1f, Modifier.weight(1f))
                HeroStatItem("Avg/day", viewModel.formatCurrency(avgDaily), 0.5f, Modifier.weight(1f))
                HeroStatItem("Biggest", viewModel.formatCurrency(biggestSpend), 0.8f, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PlanHeroCard(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    blurModifier: Modifier
) {
    val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val workStats = viewModel.computeWorkStats(ym)
    val mainSalary = state.salaries.firstOrNull { it.active }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF00B8C4), Color(0xFF329FE0), Color(0xFF7D5CFF))))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Work & Salary Status", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Text(
                    text = viewModel.formatCurrency(workStats.calculatedActualSalary),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = blurModifier
                )
                Text(
                    text = if (mainSalary != null) "Expected: ${viewModel.formatCurrency(mainSalary.amount)} (${mainSalary.employer})" else "No active employment configured",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroStatItem("Work days", "${workStats.workingDaysCount} days", (workStats.workingDaysCount / 22f).coerceIn(0f, 1f), Modifier.weight(1f))
                HeroStatItem("Absences", "${workStats.absentDaysCount} days", (workStats.absentDaysCount / 10f).coerceIn(0f, 1f), Modifier.weight(1f))
                HeroStatItem("Attendance", "${workStats.attendancePercentage.toInt()}%", (workStats.attendancePercentage / 100f).toFloat(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun WealthHeroCard(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    blurModifier: Modifier
) {
    val netWorth = viewModel.computeNetWorth()
    val liquid = viewModel.computeLiquidMoney()
    val debt = viewModel.computeTotalDebtOwed()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF2ECC71), Color(0xFF00CCCC), Color(0xFF329FE0))))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total Liquid & Wealth", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Text(
                    text = viewModel.formatCurrency(netWorth),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = blurModifier
                )
                Text(
                    text = "Liquid Cash: ${viewModel.formatCurrency(liquid)} · Total Debt: ${viewModel.formatCurrency(debt)}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroStatItem("Liquid", viewModel.formatCompactCurrency(liquid), 1f, Modifier.weight(1f))
                HeroStatItem("Assets", viewModel.formatCompactCurrency(viewModel.computeAssetsTotal()), 0.7f, Modifier.weight(1f))
                HeroStatItem("Debt Owed", viewModel.formatCompactCurrency(debt), 0.3f, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun HeroStatItem(
    label: String,
    value: String,
    ratio: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
    }
}

// ─── OVERVIEW CONTENT ───

@Composable
fun OverviewContent(
    state: FinancesUiState,
    viewModel: FinancesViewModel
) {
    val isYearly = state.period == "year"
    val countMonths = if (isYearly) 12 else 6
    val summaries = viewModel.computeMonthlySummaries(countMonths)
    val thisMonth = viewModel.computeThisMonthSummary()

    Column {
        // Toggle Period
        SegmentedToggle(
            options = listOf("month" to "Monthly", "year" to "Yearly"),
            selectedKey = state.period,
            onOptionSelected = { viewModel.setPeriod(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Available Money & Account Summary Banner
        FinanceCard {
            Text("Available Liquid Money", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (state.accounts.isEmpty()) {
                Text("No accounts configured yet. Tap + Add to add a cash or bank account.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Liquid Cash", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(viewModel.formatCurrency(viewModel.computeLiquidMoney()), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Credit Card Debt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(viewModel.formatCurrency(viewModel.computeCreditCardDebt()), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5C7A))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Account items breakdown
                state.accounts.take(4).forEach { acc ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = if (acc.kind == "cash") FinancesIcons.Income else if (acc.kind == "bank") FinancesIcons.Bank else FinancesIcons.Card
                            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${acc.title} (${acc.num ?: acc.kind})", fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = "${viewModel.formatCurrency(acc.bal)} ${acc.cur}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (acc.kind == "credit_card") Color(0xFFFF5C7A) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Widgets based on state.widgetsOrder
        state.widgetsOrder.filter { it.second }.forEach { (widgetId, _) ->
            when (widgetId) {
                "chart" -> {
                    FinanceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Income vs Expenses", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(if (isYearly) "Last 12 months" else "Last 6 months", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFECEBFF)) {
                                Text("Net Cashflow: ${viewModel.formatCurrency(thisMonth.netCashFlow)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7D5CFF), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        DualBarChart(data = summaries, modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF00CCCC)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Income", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF7D5CFF)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Expenses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                "insights" -> {
                    Text("Insights for you", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    viewModel.computeInsights().take(4).forEach { ins ->
                        val color = try { Color(android.graphics.Color.parseColor(ins.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        InsightCard(icon = ins.icon, title = ins.title, subtitle = ins.subtitle, borderLeftColor = color)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                "summary" -> {
                    FinanceCard {
                        Text("This Month Financial Ledger Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Salary Received", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.formatCurrency(thisMonth.salaryReceived), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Other Income", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.formatCurrency(thisMonth.otherIncome), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expenses", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("−${viewModel.formatCurrency(thisMonth.expenses)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5C7A))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Debt Payments", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("−${viewModel.formatCurrency(thisMonth.debtPayments)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─── SPENDING CONTENT ───

@Composable
fun SpendingContent(
    state: FinancesUiState,
    viewModel: FinancesViewModel
) {
    val currentMonthTxs = viewModel.getMonthTransactions(0)
    val avgPerDay = viewModel.computeExpenses(currentMonthTxs) / max(1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    val biggest = currentMonthTxs.filter { it.type == "exp" }.maxOfOrNull { it.amt } ?: 0.0

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(value = viewModel.formatCurrency(avgPerDay), label = "Avg per day", modifier = Modifier.weight(1f))
            StatTile(value = viewModel.formatCurrency(biggest), label = "Biggest spend", modifier = Modifier.weight(1f))
            StatTile(value = "${currentMonthTxs.size}", label = "Transactions", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search transactions or merchant") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Type Filter Toggle
        SegmentedToggle(
            options = listOf("all" to "All", "exp" to "Expenses", "inc" to "Income", "transfer" to "Transfers", "debt_payment" to "Debts"),
            selectedKey = state.typeFilter,
            onOptionSelected = { viewModel.setTypeFilter(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Account Chips Filter
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChipItem(label = "All Accounts", selected = state.accountFilter == "all", onClick = { viewModel.setAccountFilter("all") })
            }
            items(state.accounts) { acc ->
                FilterChipItem(
                    label = "${acc.title} (${acc.num ?: acc.kind})",
                    selected = state.accountFilter == acc.id,
                    onClick = { viewModel.setAccountFilter(acc.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtered Transactions List
        val filteredTxs = state.transactions.filter { t ->
            val matchesCategory = state.categoryFilter == "all" || t.cat == state.categoryFilter
            val matchesType = state.typeFilter == "all" || t.type == state.typeFilter
            val matchesAccount = state.accountFilter == "all" || t.sourceAccountId == state.accountFilter || t.destinationAccountId == state.accountFilter
            val q = state.searchQuery.lowercase()
            val matchesQuery = q.isEmpty() || t.title.lowercase().contains(q) || t.cat.lowercase().contains(q) || t.tags.lowercase().contains(q) || (t.note ?: "").lowercase().contains(q)
            matchesCategory && matchesType && matchesAccount && matchesQuery
        }.sortedByDescending { it.date }

        if (filteredTxs.isNotEmpty()) {
            val groupedByDate = filteredTxs.groupBy { it.date }
            groupedByDate.forEach { (dateStr, txsList) ->
                Text(
                    text = dateStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                FinanceCard {
                    txsList.forEach { t ->
                        val catMeta = FinancesViewModel.CATEGORIES[t.cat]
                        val iconBg = try { Color(android.graphics.Color.parseColor(catMeta?.colorHex ?: "#7D5CFF")).copy(alpha = 0.18f) } catch (e: Exception) { Color.LightGray }

                        val sourceAcc = state.accounts.find { it.id == t.sourceAccountId }
                        val destAcc = state.accounts.find { it.id == t.destinationAccountId }
                        val accBadgeText = when (t.type) {
                            "transfer" -> "⇄ ${sourceAcc?.title ?: "Source"} → ${destAcc?.title ?: "Dest"}"
                            "debt_payment" -> "💳 Debt Pay · ${sourceAcc?.title ?: "Paid"}"
                            "inc" -> "↓ ${destAcc?.title ?: "Account"}"
                            else -> sourceAcc?.title?.let { "• $it (${sourceAcc.num ?: sourceAcc.kind})" } ?: ""
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = catMeta?.icon ?: FinancesIcons.Shopping, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(t.title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(catMeta?.label ?: t.cat, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (accBadgeText.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(accBadgeText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    if (t.note != null) {
                                        Text(t.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val amountSign = if (t.type == "inc") "+" else if (t.type == "transfer") "⇄ " else "−"
                                val amountColor = if (t.type == "inc") Color(0xFF2ECC71) else if (t.type == "transfer") Color(0xFF329FE0) else MaterialTheme.colorScheme.onSurface
                                Text(
                                    text = "$amountSign${viewModel.formatCurrency(t.amt, 2)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = amountColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { viewModel.deleteTransaction(t.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(imageVector = FinancesIcons.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            FinanceCard {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No matching transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── PLAN CONTENT ───

@Composable
fun PlanContent(
    state: FinancesUiState,
    viewModel: FinancesViewModel
) {
    val cmTxs = viewModel.getMonthTransactions(0)
    val ym = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val workStats = viewModel.computeWorkStats(ym)
    val mainSalary = state.salaries.firstOrNull { it.active }

    Column {
        // Salary & Work Calendar Summary Card
        FinanceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Salary & Work Calendar", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.openToolScreen("t_cal") }) { Text("Full Calendar →", color = Color(0xFF7D5CFF)) }
            }

            if (mainSalary != null) {
                Text("Employer: ${mainSalary.employer} · Expected: ${viewModel.formatCurrency(mainSalary.amount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text("No salary/employment tracked yet.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Working Days", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workStats.workingDaysCount} days", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Absences", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workStats.absentDaysCount} days", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Attendance", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workStats.attendancePercentage.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                }
            }

            if (mainSalary != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        viewModel.recordSalaryReceived(mainSalary.id, mainSalary.amount, todayStr)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Record Salary Received (${viewModel.formatCurrency(mainSalary.amount)})", fontSize = 12.5.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Budgets Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Budgets", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.openSheet("budget") }) { Text("+ Add", color = Color(0xFF7D5CFF)) }
        }

        state.budgets.forEach { b ->
            val spent = viewModel.getSpentByCategory(cmTxs, b.cat)
            val ratio = (spent / b.limit).toFloat()
            val catMeta = FinancesViewModel.CATEGORIES[b.cat]
            val trackColor = if (ratio >= 1f) Color(0xFFFF5C7A) else if (ratio >= 0.8f) Color(0xFFFFB020) else Color(0xFF2ECC71)

            FinanceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = catMeta?.icon ?: FinancesIcons.Roadmap, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(catMeta?.label ?: b.cat, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text(
                                text = if (spent > b.limit) "${viewModel.formatCurrency(spent - b.limit)} over" else "${viewModel.formatCurrency(b.limit - spent)} left",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(viewModel.formatCurrency(spent), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text("of ${viewModel.formatCurrency(b.limit)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ProgressBarTrack(progress = ratio, color = trackColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Goals Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Savings goals", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.openSheet("goal") }) { Text("+ Add", color = Color(0xFF7D5CFF)) }
        }

        state.goals.forEach { g ->
            val ratio = (g.saved / g.target).toFloat()

            FinanceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = FinancesIcons.Goal, contentDescription = null, tint = Color(0xFF00CCCC), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(g.title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text("${viewModel.formatCurrency(g.saved)} of ${viewModel.formatCurrency(g.target)} · ${(ratio * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { viewModel.fundGoal(g.id, 100.0) }) {
                        Text("+${viewModel.formatCurrency(100.0)}", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ProgressBarTrack(progress = ratio, color = Color(0xFF00CCCC))
            }
        }
    }
}

// ─── WEALTH CONTENT ───

@Composable
fun WealthContent(
    state: FinancesUiState,
    viewModel: FinancesViewModel
) {
    val totalLiquid = viewModel.computeLiquidMoney()
    val totalAssets = viewModel.computeAssetsTotal()
    val totalDebt = viewModel.computeTotalDebtOwed()

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(value = viewModel.formatCurrency(totalLiquid), label = "Liquid Cash", modifier = Modifier.weight(1f))
            StatTile(value = viewModel.formatCurrency(totalAssets), label = "Assets", modifier = Modifier.weight(1f))
            StatTile(value = viewModel.formatCurrency(totalDebt), label = "Debts Owed", valueColor = Color(0xFFFF5C7A), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Accounts & Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Accounts & Cards", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.openToolScreen("t_wallets") }) { Text("Manage →", color = Color(0xFF7D5CFF)) }
        }

        FinanceCard {
            if (state.accounts.isEmpty()) {
                Text("No accounts added yet.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.accounts.forEach { acc ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = if (acc.kind == "cash") FinancesIcons.Income else if (acc.kind == "bank") FinancesIcons.Bank else FinancesIcons.Card
                            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(acc.title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                Text("${acc.num ?: acc.kind} · ${acc.cur}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = viewModel.formatCurrency(acc.bal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = if (acc.kind == "credit_card") Color(0xFFFF5C7A) else Color(0xFF2ECC71)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Assets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Assets & investments", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.openSheet("asset") }) { Text("+ Add", color = Color(0xFF7D5CFF)) }
        }

        FinanceCard {
            state.assets.forEach { a ->
                val icon = FinancesIcons.getAssetKindIcon(a.kind)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(a.title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text(a.kind, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(viewModel.formatCurrency(a.value), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Personal Debts & Loans
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Personal Debts & Loans", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { viewModel.openSheet("debt") }) { Text("+ Add", color = Color(0xFF7D5CFF)) }
        }

        state.debts.forEach { d ->
            val paidPct = (1.0 - (d.bal / max(1.0, d.orig))) * 100.0

            FinanceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = FinancesIcons.Card, contentDescription = null, tint = Color(0xFFFF5C7A), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(d.personOrOrg.ifBlank { d.title }, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text("${if (d.direction == "i_owe") "I owe them" else "They owe me"}${d.dueDate?.let { " · Due $it" } ?: ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(viewModel.formatCurrency(d.bal), fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = Color(0xFFFF5C7A))
                        Text("${paidPct.toInt()}% paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
