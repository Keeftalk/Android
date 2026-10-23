package tn.keeftalk.chat.feature.finances.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.data.local.entities.FinanceAccountEntity
import tn.keeftalk.chat.data.local.entities.FinanceDebtEntity
import tn.keeftalk.chat.feature.finances.util.*
import tn.keeftalk.chat.feature.finances.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        ) {
            Icon(imageVector = FinancesIcons.Back, contentDescription = "Back", modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

val TOOLS_CATALOG = listOf(
    Tuple4("t_wallets", FinancesIcons.Card, "Wallets & cards", "Accounts, cards, balances"),
    Tuple4("t_cal", FinancesIcons.Calendar, "Work & Calendar", "Work days, salary, timeline"),
    Tuple4("t_debt", FinancesIcons.DebtPayoff, "Debts & Payoff", "Personal debts, loans, payments"),
    Tuple4("t_fc", FinancesIcons.Forecast, "Forecast", "Balance in 7–365 days"),
    Tuple4("t_sim", FinancesIcons.Simulator, "What-if", "Simulate and compare"),
    Tuple4("t_health", FinancesIcons.HealthFire, "Health & FIRE", "Score, safety net, freedom"),
    Tuple4("t_subs", FinancesIcons.Subscriptions, "Subscriptions", "Annual cost, detection"),
    Tuple4("t_shared", FinancesIcons.SharedExpenses, "Shared expenses", "Split and settle up"),
    Tuple4("t_chal", FinancesIcons.Challenges, "Challenges", "Save on a streak"),
    Tuple4("t_nw", FinancesIcons.NetWorth, "Net-worth history", "Scrub through time"),
    Tuple4("t_road", FinancesIcons.Roadmap, "Goals roadmap", "What comes first"),
    Tuple4("t_ai", FinancesIcons.Assistant, "Assistant", "Ask about your money"),
    Tuple4("t_docs", FinancesIcons.Documents, "Receipts & docs", "Scan, OCR, vault"),
    Tuple4("t_trend", FinancesIcons.Trends, "Trends", "Anomalies and patterns")
)

data class Tuple4(val key: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val subtitle: String)

@Composable
fun ToolsGridScreen(
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Tools & Features", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            gridItemsIndexed(TOOLS_CATALOG) { _, tool ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.openToolScreen(tool.key) }
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(imageVector = tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = tool.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = tool.subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ─── WALLETS & CARDS SCREEN ───

@Composable
fun WalletsCardsScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var accountToReconcile by remember { mutableStateOf<FinanceAccountEntity?>(null) }
    var reconcileAmountText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Wallets & Accounts", onBack = onBack)

        // Security card
        FinanceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isVaultUnlocked) "🔓 Details unlocked" else "🔒 Details locked",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (state.isVaultUnlocked) "Tap to copy numbers" else "Enter PIN to reveal full numbers",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.isVaultUnlocked) {
                    Button(onClick = { viewModel.lockVault() }) { Text("Lock", fontSize = 12.sp) }
                } else {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                pinInput = it
                                if (it.length == 4) {
                                    if (viewModel.checkPin(it)) pinInput = ""
                                }
                            }
                        },
                        placeholder = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cards Carousel
        if (state.accounts.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(state.accounts) { index, account ->
                    VaultCardItem(
                        account = account,
                        isUnlocked = state.isVaultUnlocked,
                        index = index,
                        onCopy = { text, label ->
                            clipboardManager.setText(AnnotatedString(text))
                            viewModel.showToast("Copied $label")
                        }
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No accounts saved yet. Add cash, bank, or card.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.openSheet("account") },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))))
        ) {
            Text("+ Add a card or account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Accounts Detail & Reconciliation List
        Text("Account Balances & Reconciliation", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(modifier = Modifier.height(6.dp))

        state.accounts.forEach { acc ->
            FinanceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(acc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${acc.num ?: acc.kind} · Native Currency: ${acc.cur}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (acc.kind == "credit_card") {
                            Text("Amount Owed: ${acc.bal} ${acc.cur} (Limit: ${acc.creditLimit})", fontSize = 12.sp, color = Color(0xFFFF5C7A))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${acc.bal} ${acc.cur}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (acc.kind == "credit_card") Color(0xFFFF5C7A) else Color(0xFF2ECC71)
                        )
                        OutlinedButton(
                            onClick = {
                                accountToReconcile = acc
                                reconcileAmountText = acc.bal.toString()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Reconcile", fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Reconcile Dialog
    if (accountToReconcile != null) {
        val targetAcc = accountToReconcile!!
        AlertDialog(
            onDismissRequest = { accountToReconcile = null },
            title = { Text("Reconcile ${targetAcc.title}") },
            text = {
                Column {
                    Text("Current Keeftalk Balance: ${targetAcc.bal} ${targetAcc.cur}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter actual bank/cash balance:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = reconcileAmountText,
                        onValueChange = { reconcileAmountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val actualVal = reconcileAmountText.toDoubleOrNull()
                    if (actualVal != null) {
                        viewModel.reconcileAccount(targetAcc.id, actualVal)
                    }
                    accountToReconcile = null
                }) {
                    Text("Apply Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToReconcile = null }) { Text("Cancel") }
            }
        )
    }
}

val GRADIENTS = listOf(
    listOf(Color(0xFF7D5CFF), Color(0xFF00A9C4)),
    listOf(Color(0xFF14142B), Color(0xFF5A52D9)),
    listOf(Color(0xFFFF7AB6), Color(0xFF7D5CFF)),
    listOf(Color(0xFF00A9C4), Color(0xFF2ECC71)),
    listOf(Color(0xFF3C7FE0), Color(0xFF14142B))
)

@Composable
fun VaultCardItem(
    account: FinanceAccountEntity,
    isUnlocked: Boolean,
    index: Int,
    onCopy: (String, String) -> Unit
) {
    val gradientColors = GRADIENTS[index % GRADIENTS.size]

    Surface(
        modifier = Modifier
            .width(310.dp)
            .height(180.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 8.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(account.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.25f)) {
                        Text(
                            text = "${account.kind.replaceFirstChar { it.uppercase() }} · ${account.cur}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Column {
                    val num = account.num ?: ""
                    val displayNum = if (num.isNotBlank()) {
                        if (isUnlocked) num else "•••• •••• •••• " + num.filter { it.isDigit() }.takeLast(4)
                    } else "No card number"

                    Text(displayNum, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Holder", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(account.holder ?: "—", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Balance", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text("${account.bal} ${account.cur}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ─── WORK & FINANCIAL CALENDAR SCREEN ───

@Composable
fun FinancialCalendarScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    val cal = Calendar.getInstance()
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val ym = state.selectedCalendarMonth
    val workStats = viewModel.computeWorkStats(ym)

    val selectedDateStr = String.format(Locale.US, "%s-%02d", ym, state.selectedCalendarDay)
    val currentDayWorkLog = state.workLogs.find { it.date == selectedDateStr }

    var workStatusKey by remember(selectedDateStr) { mutableStateOf(currentDayWorkLog?.status ?: "working") }
    var hoursText by remember(selectedDateStr) { mutableStateOf(currentDayWorkLog?.workHours?.toString() ?: "8.0") }
    var overtimeText by remember(selectedDateStr) { mutableStateOf(currentDayWorkLog?.overtimeHours?.toString() ?: "0.0") }
    var isPaidChecked by remember(selectedDateStr) { mutableStateOf(currentDayWorkLog?.isPaid ?: true) }
    var workNoteText by remember(selectedDateStr) { mutableStateOf(currentDayWorkLog?.note ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Work & Financial Calendar", onBack = onBack)

        // Monthly Work Statistics Banner
        FinanceCard {
            Text("Work & Attendance Summary ($ym)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Working Days", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workStats.workingDaysCount} days", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Absences", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workStats.absentDaysCount} days", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5C7A))
                }
                Column {
                    Text("Paid / Unpaid", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workStats.paidLeaveCount} / ${workStats.unpaidLeaveCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Attendance", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${workStats.attendancePercentage.toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2ECC71))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Calculated Salary: ${viewModel.formatCurrency(workStats.calculatedActualSalary)} (Expected: ${viewModel.formatCurrency(workStats.expectedMonthlySalary)})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month Calendar View
        FinanceCard {
            Text(ym, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                    Text(d, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val daysList = (1..daysInMonth).toList()
            daysList.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    week.forEach { day ->
                        val dayStr = String.format(Locale.US, "%s-%02d", ym, day)
                        val log = state.workLogs.find { it.date == dayStr }
                        val isSelected = state.selectedCalendarDay == day
                        val isToday = day == cal.get(Calendar.DAY_OF_MONTH)

                        val bg = if (isSelected) Color(0xFF7D5CFF) else when (log?.status) {
                            "working" -> Color(0xFF2ECC71).copy(alpha = 0.2f)
                            "absent" -> Color(0xFFFF5C7A).copy(alpha = 0.25f)
                            "vacation" -> Color(0xFF329FE0).copy(alpha = 0.25f)
                            "sick" -> Color(0xFFFFB020).copy(alpha = 0.25f)
                            else -> Color.Transparent
                        }

                        val fg = if (isSelected) Color.White else if (isToday) Color(0xFF7D5CFF) else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { viewModel.setSelectedCalendarDay(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "$day", fontSize = 13.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = fg)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Work Log Entry Form for Selected Date
        FinanceCard {
            Text("Work Status for $selectedDateStr", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            KindDropdown(
                options = listOf(
                    "working" to "Working Day",
                    "absent" to "Absent",
                    "vacation" to "Vacation / Paid Leave",
                    "sick" to "Sick Leave",
                    "holiday" to "Official Holiday",
                    "custom" to "Custom Status"
                ),
                selected = workStatusKey,
                onSelected = { workStatusKey = it }
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Work Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = overtimeText,
                    onValueChange = { overtimeText = it },
                    label = { Text("Overtime Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isPaidChecked, onCheckedChange = { isPaidChecked = it })
                Text("Paid Status (Leave is paid)", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val hVal = hoursText.toDoubleOrNull() ?: 8.0
                    val otVal = overtimeText.toDoubleOrNull() ?: 0.0
                    viewModel.setWorkLog(
                        date = selectedDateStr,
                        status = workStatusKey,
                        workHours = hVal,
                        overtimeHours = otVal,
                        isPaid = isPaidChecked,
                        note = workNoteText.ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Work Day Log")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Financial Timeline on Selected Date
        val selectedDayTxs = state.transactions.filter { it.date == selectedDateStr }
        FinanceCard {
            Text("Financial Timeline for Day ${state.selectedCalendarDay}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedDayTxs.isNotEmpty()) {
                selectedDayTxs.forEach { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(t.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(t.cat, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${if (t.type == "inc") "+" else "−"}${viewModel.formatCurrency(t.amt, 2)}",
                            fontWeight = FontWeight.Bold,
                            color = if (t.type == "inc") Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Text("No financial transactions on this date.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ForecastScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    val forecast = viewModel.computeForecast(state.forecastDays)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Cash-flow forecast", onBack = onBack)

        FinanceCard {
            Text("Forecast next ${state.forecastDays} days", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LineChart(data = forecast, modifier = Modifier.fillMaxWidth().height(140.dp))
        }
    }
}

@Composable
fun SimulatorScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("What-if simulator", onBack = onBack)

        FinanceCard {
            SliderRow("Extra savings/mo", viewModel.formatCurrency(state.extraSavings), state.extraSavings.toFloat(), 0f..2000f) {
                viewModel.setExtraSavings(it.toDouble())
            }
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    valueStr: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(valueStr, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = currentValue, onValueChange = onValueChange, valueRange = range)
    }
}

@Composable
fun HealthFireScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    val health = viewModel.computeHealthScore()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Health & FIRE", onBack = onBack)

        FinanceCard {
            Text("Health Score: ${health.score}/100", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            health.factors.forEach { factor ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(factor.label, fontSize = 13.sp)
                    Text(factor.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── DEBT PAYOFF & PERSONAL DEBTS SCREEN ───

@Composable
fun DebtPayoffScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    var debtToPay by remember { mutableStateOf<FinanceDebtEntity?>(null) }
    var payAmountText by remember { mutableStateOf("") }
    var payAccountSourceId by remember { mutableStateOf(state.accounts.firstOrNull()?.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Debts & Payoff Planner", onBack = onBack)

        // Personal Debts & Owed Money
        Text("Personal Debts & Money Owed", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(modifier = Modifier.height(6.dp))

        if (state.debts.isEmpty()) {
            FinanceCard {
                Text("No personal debts recorded yet. Tap + Add to record money owed.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            state.debts.forEach { debt ->
                FinanceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(debt.personOrOrg.ifBlank { debt.title }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "${if (debt.direction == "i_owe") "I owe them" else "They owe me"}${debt.dueDate?.let { " · Due $it" } ?: ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(viewModel.formatCurrency(debt.bal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFF5C7A))
                            Button(
                                onClick = {
                                    debtToPay = debt
                                    payAmountText = debt.bal.toString()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Record Payment", fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Payoff Simulation
        Text("Payoff Strategy Simulator", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChipItem(label = "Avalanche (High APR)", selected = state.debtMethod == "avl", onClick = { viewModel.setDebtMethod("avl") })
            FilterChipItem(label = "Snowball (Low Bal)", selected = state.debtMethod == "snow", onClick = { viewModel.setDebtMethod("snow") })
        }

        Spacer(modifier = Modifier.height(10.dp))

        FinanceCard {
            SliderRow("Extra payment per month", viewModel.formatCurrency(state.extraDebtPayment), state.extraDebtPayment.toFloat(), 0f..800f) {
                viewModel.setExtraDebtPayment(it.toDouble())
            }
        }
    }

    // Payment Dialog
    if (debtToPay != null) {
        val debt = debtToPay!!
        AlertDialog(
            onDismissRequest = { debtToPay = null },
            title = { Text("Record Payment for ${debt.personOrOrg.ifBlank { debt.title }}") },
            text = {
                Column {
                    Text("Remaining Balance: ${viewModel.formatCurrency(debt.bal)}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    FormFieldLabelText("Payment Amount")
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormFieldLabelText("Paid From Account")
                    AccountDropdown(
                        accounts = state.accounts,
                        selectedAccountId = payAccountSourceId,
                        onSelected = { payAccountSourceId = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pAmt = payAmountText.toDoubleOrNull()
                    val accId = payAccountSourceId ?: state.accounts.firstOrNull()?.id
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    if (pAmt != null && pAmt > 0 && accId != null) {
                        viewModel.recordDebtPayment(
                            debtId = debt.id,
                            amt = pAmt,
                            date = todayStr,
                            sourceAccountId = accId
                        )
                    }
                    debtToPay = null
                }) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToPay = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SubscriptionsScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    val subs = state.recurring.filter { it.kind == "subscription" }
    val totalAnnual = subs.filter { it.on }.sumOf { it.amt } * 12

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Subscriptions", onBack = onBack)

        FinanceCard {
            Text("Annual Subscription Cost", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(viewModel.formatCurrency(totalAnnual), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        FinanceCard {
            subs.forEach { r ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text("${viewModel.formatCurrency(r.amt * 12)}/yr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { viewModel.toggleRecurring(r.id) }) {
                        Text(if (r.on) "Pause" else "Resume", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SharedExpensesScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Shared Expenses", onBack = onBack)

        FinanceCard {
            state.sharedPeople.forEach { p ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(if (p.bal > 0) "owes you" else if (p.bal < 0) "you owe" else "settled", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(viewModel.formatCurrency(abs(p.bal)), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChallengesScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Savings Challenges", onBack = onBack)

        state.challenges.forEach { c ->
            FinanceCard {
                Text(c.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${viewModel.formatCurrency(c.saved)} of ${viewModel.formatCurrency(c.target)}", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun NetWorthHistoryScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    val (history, _) = viewModel.computeNetWorthHistory(state.netWorthMonths)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Net Worth History", onBack = onBack)

        FinanceCard {
            Text(viewModel.formatCurrency(viewModel.computeNetWorth()), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LineChart(data = history, modifier = Modifier.fillMaxWidth().height(120.dp))
        }
    }
}

@Composable
fun GoalsRoadmapScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Goals Roadmap", onBack = onBack)

        state.goals.forEach { g ->
            FinanceCard {
                Text(g.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${viewModel.formatCurrency(g.saved)} of ${viewModel.formatCurrency(g.target)}", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FinancesAssistantScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    var questionInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        BackHeader("Assistant", onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.chatHistory) { chat ->
                val isMe = chat.sender == "me"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = chat.text,
                            fontSize = 14.sp,
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = questionInput,
                onValueChange = { questionInput = it },
                placeholder = { Text("Ask about your finances…") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (questionInput.isNotBlank()) {
                    viewModel.askAi(questionInput)
                    questionInput = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun ReceiptsDocsScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Receipts & Documents", onBack = onBack)

        state.docs.forEach { doc ->
            FinanceCard {
                Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${doc.kind} · ${doc.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TrendsScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Trends & Analysis", onBack = onBack)

        FinanceCard {
            Text("Monthly Spending Trends", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Anomalies and patterns are analyzed automatically from your financial ledger.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FinancesSettingsScreen(
    state: FinancesUiState,
    viewModel: FinancesViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackHeader("Settings", onBack = onBack)

        tn.keeftalk.chat.ui.settings.components.FeatureCloudSyncCard(
            featureId = tn.keeftalk.chat.domain.model.entitlement.FeatureId.FINANCES,
            description = "Sync your financial data securely across your devices.",
            onEnableClick = { },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        FinanceCard {
            Text("Display Currency", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(6.dp))
            KindDropdown(
                options = FinancesViewModel.EXCHANGE_RATES.keys.map { it to it },
                selected = state.settings.cur,
                onSelected = { viewModel.setCurrency(it) }
            )
        }
    }
}
