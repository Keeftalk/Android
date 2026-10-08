package tn.keeftalk.chat.feature.finances.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.data.local.entities.FinanceAccountEntity
import tn.keeftalk.chat.data.local.entities.FinanceDebtEntity
import tn.keeftalk.chat.feature.finances.util.*
import tn.keeftalk.chat.feature.finances.viewmodel.FinancesViewModel
import java.text.SimpleDateFormat
import java.util.*

data class FormTypeMeta(val key: String, val icon: ImageVector, val label: String)

val ALL_FORMS = listOf(
    FormTypeMeta("expense", FinancesIcons.Bills, "Expense"),
    FormTypeMeta("income", FinancesIcons.Income, "Income"),
    FormTypeMeta("salary", FinancesIcons.Roadmap, "Salary"),
    FormTypeMeta("transfer", FinancesIcons.Repeat, "Transfer"),
    FormTypeMeta("debt_payment", FinancesIcons.Card, "Debt Pay"),
    FormTypeMeta("account", FinancesIcons.Bank, "Card / Account"),
    FormTypeMeta("debt", FinancesIcons.Card, "Personal Debt"),
    FormTypeMeta("budget", FinancesIcons.Roadmap, "Budget"),
    FormTypeMeta("goal", FinancesIcons.Goal, "Goal"),
    FormTypeMeta("asset", FinancesIcons.Stock, "Asset"),
    FormTypeMeta("recurring", FinancesIcons.Repeat, "Recurring"),
    FormTypeMeta("shared", FinancesIcons.SharedExpenses, "Split"),
    FormTypeMeta("challenge", FinancesIcons.Challenges, "Challenge"),
    FormTypeMeta("doc", FinancesIcons.Documents, "Document")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinancesSheet(
    initialForm: String,
    viewModel: FinancesViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedForm by remember { mutableStateOf(initialForm) }
    var quickAddText by remember { mutableStateOf("") }

    // Form fields state
    var titleText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var destAmountText by remember { mutableStateOf("") }
    var categoryKey by remember { mutableStateOf(uiState.settings.lastUsedCategory ?: "food") }
    var dateText by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var tagsText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    // Selected Accounts & Debts
    var selectedSourceAccId by remember { mutableStateOf(uiState.settings.lastUsedAccountId ?: uiState.accounts.firstOrNull()?.id) }
    var selectedDestAccId by remember { mutableStateOf(uiState.accounts.getOrNull(1)?.id ?: uiState.accounts.firstOrNull()?.id) }
    var selectedDebtId by remember { mutableStateOf(uiState.debts.firstOrNull()?.id) }

    // Employment / Salary fields
    var employerText by remember { mutableStateOf("") }
    var payPeriodKey by remember { mutableStateOf("monthly") }
    var payDayText by remember { mutableStateOf("25") }
    var scheduleText by remember { mutableStateOf("Mon-Fri (8h/day)") }
    var raisePctText by remember { mutableStateOf("") }

    // Goal / Asset / Debt extra fields
    var targetText by remember { mutableStateOf("") }
    var savedText by remember { mutableStateOf("0") }
    var monthlyText by remember { mutableStateOf("") }
    var kindKey by remember { mutableStateOf("fund") }
    var aprText by remember { mutableStateOf("") }
    var minPaymentText by remember { mutableStateOf("") }
    var dayText by remember { mutableStateOf("1") }
    var holderText by remember { mutableStateOf("") }
    var numText by remember { mutableStateOf("") }
    var ribText by remember { mutableStateOf("") }
    var bicText by remember { mutableStateOf("") }
    var expText by remember { mutableStateOf("") }
    var currencyKey by remember { mutableStateOf(uiState.settings.cur) }
    var creditLimitText by remember { mutableStateOf("") }
    var directionKey by remember { mutableStateOf("i_owe") }
    var dueDateText by remember { mutableStateOf("") }
    var withPersonText by remember { mutableStateOf("") }
    var paidByText by remember { mutableStateOf("me") }
    var daysText by remember { mutableStateOf("90") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Add to My Finances",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick add field
            Text(
                text = "Quick add — type and press Enter",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = quickAddText,
                onValueChange = { quickAddText = it },
                placeholder = { Text("coffee 4.5 food visa  or  paid Ahmed 50 cash") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.quickAdd(quickAddText)
                        onDismiss()
                    }
                }),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
            )

            // Form selection chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(ALL_FORMS) { meta ->
                    FilterChipItem(
                        label = meta.label,
                        selected = selectedForm == meta.key,
                        onClick = { selectedForm = meta.key }
                    )
                }
            }

            // Dynamic Form Fields based on selectedForm
            when (selectedForm) {
                "expense" -> {
                    FormFieldLabelText("What was it?")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormFieldLabelText("Paid with")
                    AccountDropdown(
                        accounts = uiState.accounts,
                        selectedAccountId = selectedSourceAccId,
                        onSelected = { selectedSourceAccId = it }
                    )

                    FormFieldLabelText("Category")
                    CategoryDropdown(selectedCategory = categoryKey, onSelected = { categoryKey = it })

                    FormFieldLabelText("Date")
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Tags (optional)")
                    OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Note (optional)")
                    OutlinedTextField(value = noteText, onValueChange = { noteText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "income" -> {
                    FormFieldLabelText("Source / Employer")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormFieldLabelText("Deposit into")
                    AccountDropdown(
                        accounts = uiState.accounts,
                        selectedAccountId = selectedSourceAccId,
                        onSelected = { selectedSourceAccId = it }
                    )

                    FormFieldLabelText("Income Category")
                    KindDropdown(
                        options = listOf("income" to "Salary / Base Income", "bonus" to "Bonus", "freelance" to "Freelance", "overtime" to "Overtime", "other_income" to "Other Income"),
                        selected = categoryKey,
                        onSelected = { categoryKey = it }
                    )

                    FormFieldLabelText("Date")
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "salary" -> {
                    FormFieldLabelText("Employer / Company Name")
                    OutlinedTextField(value = employerText, onValueChange = { employerText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Salary Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormFieldLabelText("Pay Frequency")
                    KindDropdown(
                        options = listOf("monthly" to "Monthly Salary", "weekly" to "Weekly Salary", "daily" to "Daily Pay", "hourly" to "Hourly Pay"),
                        selected = payPeriodKey,
                        onSelected = { payPeriodKey = it }
                    )

                    FormFieldLabelText("Pay Day (e.g. 25th of month)")
                    OutlinedTextField(value = payDayText, onValueChange = { payDayText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Working Schedule")
                    OutlinedTextField(value = scheduleText, onValueChange = { scheduleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Target Bank Account for Deposit")
                    AccountDropdown(
                        accounts = uiState.accounts,
                        selectedAccountId = selectedSourceAccId,
                        onSelected = { selectedSourceAccId = it }
                    )

                    FormFieldLabelText("Expected Salary Raise % (optional)")
                    OutlinedTextField(value = raisePctText, onValueChange = { raisePctText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "transfer" -> {
                    FormFieldLabelText("Source Account (Transfer From)")
                    AccountDropdown(
                        accounts = uiState.accounts,
                        selectedAccountId = selectedSourceAccId,
                        onSelected = { selectedSourceAccId = it }
                    )

                    FormFieldLabelText("Destination Account (Transfer To)")
                    AccountDropdown(
                        accounts = uiState.accounts,
                        selectedAccountId = selectedDestAccId,
                        onSelected = { selectedDestAccId = it }
                    )

                    FormFieldLabelText("Amount")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormFieldLabelText("Destination Amount (if multi-currency, optional)")
                    OutlinedTextField(
                        value = destAmountText,
                        onValueChange = { destAmountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormFieldLabelText("Date")
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "debt_payment" -> {
                    FormFieldLabelText("Select Debt / Person")
                    DebtDropdown(
                        debts = uiState.debts,
                        selectedDebtId = selectedDebtId,
                        onSelected = { selectedDebtId = it }
                    )

                    FormFieldLabelText("Amount to Pay")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormFieldLabelText("Paid from Account")
                    AccountDropdown(
                        accounts = uiState.accounts,
                        selectedAccountId = selectedSourceAccId,
                        onSelected = { selectedSourceAccId = it }
                    )

                    FormFieldLabelText("Date")
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "budget" -> {
                    FormFieldLabelText("Category")
                    CategoryDropdown(selectedCategory = categoryKey, onSelected = { categoryKey = it })

                    FormFieldLabelText("Monthly Limit")
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "goal" -> {
                    FormFieldLabelText("Goal Name")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Target Amount")
                    OutlinedTextField(value = targetText, onValueChange = { targetText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Already Saved")
                    OutlinedTextField(value = savedText, onValueChange = { savedText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Monthly Contribution")
                    OutlinedTextField(value = monthlyText, onValueChange = { monthlyText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "asset" -> {
                    FormFieldLabelText("Asset Name")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Current Value")
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Type")
                    KindDropdown(
                        options = listOf("fund" to "Fund", "stock" to "Stock", "crypto" to "Crypto", "cash" to "Cash", "property" to "Property"),
                        selected = kindKey,
                        onSelected = { kindKey = it }
                    )
                }
                "debt" -> {
                    FormFieldLabelText("Person or Organization")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Type / Direction")
                    KindDropdown(
                        options = listOf("i_owe" to "I owe them money", "they_owe" to "They owe me money"),
                        selected = directionKey,
                        onSelected = { directionKey = it }
                    )

                    FormFieldLabelText("Remaining Amount / Balance")
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Due Date (e.g. YYYY-MM-DD)")
                    OutlinedTextField(value = dueDateText, onValueChange = { dueDateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Interest Rate (APR %)")
                    OutlinedTextField(value = aprText, onValueChange = { aprText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Monthly Installment / Minimum Payment")
                    OutlinedTextField(value = minPaymentText, onValueChange = { minPaymentText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "recurring" -> {
                    FormFieldLabelText("Name")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Amount")
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Day of month (1-31)")
                    OutlinedTextField(value = dayText, onValueChange = { dayText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Type")
                    KindDropdown(
                        options = listOf("subscription" to "Subscription", "bill" to "Bill", "income" to "Income"),
                        selected = kindKey,
                        onSelected = { kindKey = it }
                    )
                }
                "account" -> {
                    FormFieldLabelText("Account / Card Type")
                    KindDropdown(
                        options = listOf("cash" to "Cash in pocket", "bank" to "Bank Account", "credit_card" to "Credit Card", "debit_card" to "Debit Card", "wallet" to "Digital Wallet", "other" to "Other Account"),
                        selected = kindKey,
                        onSelected = { kindKey = it }
                    )

                    FormFieldLabelText("Account / Card Name")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Holder Name (optional)")
                    OutlinedTextField(value = holderText, onValueChange = { holderText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Card Number / Account / IBAN (optional)")
                    OutlinedTextField(
                        value = numText,
                        onValueChange = { input ->
                            val clean = input.replace("\\s".toRegex(), "")
                            numText = if (clean.take(2).matches("^[A-Za-z]{2}$".toRegex())) {
                                OfflineBankDetector.formatIban(input)
                            } else if (clean.all { it.isDigit() } && clean.length <= 19) {
                                OfflineCardDetector.formatCardNumber(input)
                            } else {
                                input
                            }
                        },
                        placeholder = { Text("e.g. 4000 1234 5678 9010") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (kindKey == "credit_card") {
                        FormFieldLabelText("Credit Line Limit")
                        OutlinedTextField(value = creditLimitText, onValueChange = { creditLimitText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    }

                    FormFieldLabelText("Current Balance / Amount Owed")
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Currency")
                    KindDropdown(
                        options = FinancesViewModel.EXCHANGE_RATES.keys.map { it to it },
                        selected = currencyKey,
                        onSelected = { currencyKey = it }
                    )
                }
                "shared" -> {
                    FormFieldLabelText("What was it?")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Total")
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Split with")
                    OutlinedTextField(value = withPersonText, onValueChange = { withPersonText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Who paid?")
                    KindDropdown(
                        options = listOf("me" to "I paid", "them" to "They paid"),
                        selected = paidByText,
                        onSelected = { paidByText = it }
                    )
                }
                "challenge" -> {
                    FormFieldLabelText("Name")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Amount to save")
                    OutlinedTextField(value = targetText, onValueChange = { targetText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Days")
                    OutlinedTextField(value = daysText, onValueChange = { daysText = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                "doc" -> {
                    FormFieldLabelText("Title")
                    OutlinedTextField(value = titleText, onValueChange = { titleText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    FormFieldLabelText("Type")
                    KindDropdown(
                        options = listOf("invoice" to "Invoice", "payslip" to "Payslip", "tax" to "Tax", "contract" to "Contract", "warranty" to "Warranty", "receipt" to "Receipt"),
                        selected = kindKey,
                        onSelected = { kindKey = it }
                    )

                    FormFieldLabelText("Date")
                    OutlinedTextField(value = dateText, onValueChange = { dateText = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action Button
            Button(
                onClick = {
                    val amtVal = amountText.toDoubleOrNull() ?: 0.0
                    val destAmtVal = destAmountText.toDoubleOrNull()
                    val targetVal = targetText.toDoubleOrNull() ?: 0.0
                    val savedVal = savedText.toDoubleOrNull() ?: 0.0
                    val monthlyVal = monthlyText.toDoubleOrNull() ?: 0.0
                    val aprVal = aprText.toDoubleOrNull() ?: 0.0
                    val minPaymentVal = minPaymentText.toDoubleOrNull() ?: 0.0
                    val creditLimitVal = creditLimitText.toDoubleOrNull() ?: 0.0
                    val payDayVal = payDayText.toIntOrNull() ?: 25
                    val raisePctVal = raisePctText.toDoubleOrNull() ?: 0.0
                    val dayVal = dayText.toIntOrNull() ?: 1
                    val daysVal = daysText.toIntOrNull() ?: 90

                    when (selectedForm) {
                        "expense" -> if (titleText.isNotBlank() && amtVal > 0) {
                            viewModel.addExpense(
                                title = titleText,
                                amt = amtVal,
                                cat = categoryKey,
                                date = dateText,
                                sourceAccountId = selectedSourceAccId,
                                tags = tagsText,
                                note = noteText
                            )
                        }
                        "income" -> if (titleText.isNotBlank() && amtVal > 0) {
                            viewModel.addIncome(
                                source = titleText,
                                amt = amtVal,
                                date = dateText,
                                destinationAccountId = selectedSourceAccId,
                                cat = categoryKey
                            )
                        }
                        "salary" -> if (employerText.isNotBlank() && amtVal > 0) {
                            viewModel.addSalary(
                                employer = employerText,
                                amount = amtVal,
                                payPeriod = payPeriodKey,
                                currency = currencyKey,
                                payDay = payDayVal,
                                workingSchedule = scheduleText,
                                targetAccountId = selectedSourceAccId,
                                expectedRaisePct = raisePctVal
                            )
                        }
                        "transfer" -> if (amtVal > 0 && selectedSourceAccId != null && selectedDestAccId != null) {
                            viewModel.addTransfer(
                                title = "Transfer",
                                amt = amtVal,
                                date = dateText,
                                sourceAccountId = selectedSourceAccId!!,
                                destinationAccountId = selectedDestAccId!!,
                                destinationAmount = destAmtVal
                            )
                        }
                        "debt_payment" -> if (amtVal > 0 && selectedDebtId != null && selectedSourceAccId != null) {
                            viewModel.recordDebtPayment(
                                debtId = selectedDebtId!!,
                                amt = amtVal,
                                date = dateText,
                                sourceAccountId = selectedSourceAccId!!
                            )
                        }
                        "budget" -> if (amtVal > 0) viewModel.addBudget(categoryKey, amtVal)
                        "goal" -> if (titleText.isNotBlank() && targetVal > 0) viewModel.addGoal(titleText, targetVal, savedVal, monthlyVal)
                        "asset" -> if (titleText.isNotBlank() && amtVal >= 0) viewModel.addAsset(titleText, kindKey, amtVal)
                        "debt" -> if (titleText.isNotBlank() && amtVal > 0) viewModel.addDebt(titleText, amtVal, directionKey, aprVal, minPaymentVal, dueDateText)
                        "recurring" -> if (titleText.isNotBlank() && amtVal > 0) viewModel.addRecurring(titleText, amtVal, dayVal, kindKey)
                        "account" -> if (titleText.isNotBlank()) {
                            viewModel.addAccount(
                                title = titleText,
                                kind = kindKey,
                                cur = currencyKey,
                                bal = amtVal,
                                num = numText,
                                rib = ribText,
                                bic = bicText,
                                exp = expText,
                                cvv = null, // NEVER persist CVV
                                holder = holderText,
                                creditLimit = creditLimitVal
                            )
                        }
                        "shared" -> if (titleText.isNotBlank() && amtVal > 0) viewModel.addShared(titleText, amtVal, withPersonText, paidByText)
                        "challenge" -> if (titleText.isNotBlank() && targetVal > 0) viewModel.addChallenge(titleText, targetVal, daysVal)
                        "doc" -> if (titleText.isNotBlank()) viewModel.addDoc(titleText, kindKey, dateText)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF))))
            ) {
                Text(
                    text = "Save ${ALL_FORMS.find { it.key == selectedForm }?.label?.lowercase() ?: "item"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AccountDropdown(
    accounts: List<FinanceAccountEntity>,
    selectedAccountId: String?,
    onSelected: (String) -> Unit,
    label: String = "Select Account"
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAcc = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { expanded = true }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp)),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (selectedAcc != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${selectedAcc.title} (${selectedAcc.num ?: selectedAcc.kind})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedAcc.bal} ${selectedAcc.cur}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (accounts.isEmpty()) {
                DropdownMenuItem(text = { Text("No accounts yet") }, onClick = { expanded = false })
            } else {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("${acc.title} (${acc.num ?: acc.kind})", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${acc.bal} ${acc.cur}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            onSelected(acc.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DebtDropdown(
    debts: List<FinanceDebtEntity>,
    selectedDebtId: String?,
    onSelected: (String) -> Unit,
    label: String = "Select Debt"
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDebt = debts.find { it.id == selectedDebtId } ?: debts.firstOrNull()

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { expanded = true }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp)),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (selectedDebt != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = selectedDebt.personOrOrg.ifBlank { selectedDebt.title },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedDebt.bal} remaining",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (debts.isEmpty()) {
                DropdownMenuItem(text = { Text("No active debts") }, onClick = { expanded = false })
            } else {
                debts.forEach { d ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(d.personOrOrg.ifBlank { d.title }, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${d.bal} left", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        },
                        onClick = {
                            onSelected(d.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FormFieldLabelText(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
fun CategoryDropdown(
    selectedCategory: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { expanded = true }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp)),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                val meta = FinancesViewModel.CATEGORIES[selectedCategory]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (meta != null) {
                        Icon(imageVector = meta.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = meta?.label ?: selectedCategory,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FinancesViewModel.EXPENSE_CATEGORIES.forEach { catKey ->
                val meta = FinancesViewModel.CATEGORIES[catKey]
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (meta != null) {
                                Icon(imageVector = meta.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(meta?.label ?: catKey)
                        }
                    },
                    onClick = {
                        onSelected(catKey)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun KindDropdown(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable { expanded = true }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp)),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                val label = options.find { it.first == selected }?.second ?: selected
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(key)
                        expanded = false
                    }
                )
            }
        }
    }
}
