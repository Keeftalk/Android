package tn.keeftalk.chat.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.data.billing.BillingManager
import tn.keeftalk.chat.data.billing.CloudPricing
import tn.keeftalk.chat.data.billing.CloudBillingState
import tn.keeftalk.chat.data.billing.InvoiceSummary
import tn.keeftalk.chat.data.billing.ResourceUsage
import tn.keeftalk.chat.data.billing.CostEstimate
import tn.keeftalk.chat.data.billing.PurchaseResult
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    viewModel: SettingsViewModel,
    billingManager: BillingManager,
    onBack: () -> Unit,
) {
    val billingState by viewModel.cloudBillingState.collectAsState()
    val pricingState by viewModel.cloudPricing.collectAsState()
    val invoices by viewModel.invoiceHistory.collectAsState()
    val isLoading by viewModel.isBillingLoading.collectAsState()
    val billingMsg by viewModel.billingMessage.collectAsState()

    val productDetails by billingManager.productDetails.collectAsState()
    val activity = LocalActivity.current ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Prepaid, 1 = Postpaid, 2 = Pricing Rates

    LaunchedEffect(Unit) {
        viewModel.loadBillingData()

        billingManager.purchaseEvents.collect { result ->
            when (result) {
                is PurchaseResult.Success -> {
                    val purchaseToken = result.purchase.purchaseToken
                    val productId = result.purchase.products.firstOrNull() ?: ""
                    val amount = when (productId) {
                        BillingManager.BALANCE_TOPUP_10 -> 10.0
                        BillingManager.BALANCE_TOPUP_25 -> 25.0
                        else -> 5.0
                    }
                    viewModel.topUpBalance(amount, purchaseToken, productId) { success, msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "Prepaid cloud balance topped up and verified with server!"
                                else "Top-up verification warning: $msg"
                            )
                        }
                    }
                }
                is PurchaseResult.Error -> {
                    snackbarHostState.showSnackbar("Top-up Error: ${result.message}")
                }
                is PurchaseResult.Cancelled -> {}
            }
        }
    }

    LaunchedEffect(billingMsg) {
        billingMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.billingMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Usage & Billing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadBillingData() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Billing")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner: 100% Free & Open Source
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("100% Free & Open-Source App Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "All 19+ Keeftalk tools (Chats, Notes, Vault, Books, Habits, Maps, Browser, Secrets) are 100% free and work fully offline without subscriptions or ads. You only pay for actual cloud resources consumed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Tab Switcher
                item {
                    TabRow(selectedTabIndex = activeTab) {
                        Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                            Text("Prepaid Balance", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                        }
                        Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                            Text("Postpaid Invoicing", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                        }
                        Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                            Text("Authoritative Rates", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (activeTab == 0) {
                    // PREPAID BALANCE CARD
                    item {
                        val currentBalance = billingState?.prepaidBalance ?: 0.0
                        val threshold = billingState?.lowBalanceThreshold ?: 2.0
                        val isHealthy = currentBalance > threshold

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Available Prepaid Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "$${String.format(Locale.US, "%.2f", currentBalance)} USD",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    color = if (isHealthy) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        if (isHealthy) "Active • Low balance alert threshold: $${String.format(Locale.US, "%.2f", threshold)}"
                                        else "Low Balance Warning • Cloud uploads paused when $0.00",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Top-up Balance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val topup5 = productDetails.find { it.productId == BillingManager.BALANCE_TOPUP_5 }
                                            if (topup5 != null) {
                                                billingManager.launchPurchaseFlow(activity, topup5)
                                            } else {
                                                viewModel.topUpBalance(5.0, "topup_5_${System.currentTimeMillis()}", BillingManager.BALANCE_TOPUP_5)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoading
                                    ) {
                                        Text("+$5")
                                    }
                                    Button(
                                        onClick = {
                                            val topup10 = productDetails.find { it.productId == BillingManager.BALANCE_TOPUP_10 }
                                            if (topup10 != null) {
                                                billingManager.launchPurchaseFlow(activity, topup10)
                                            } else {
                                                viewModel.topUpBalance(10.0, "topup_10_${System.currentTimeMillis()}", BillingManager.BALANCE_TOPUP_10)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoading
                                    ) {
                                        Text("+$10")
                                    }
                                    Button(
                                        onClick = {
                                            val topup25 = productDetails.find { it.productId == BillingManager.BALANCE_TOPUP_25 }
                                            if (topup25 != null) {
                                                billingManager.launchPurchaseFlow(activity, topup25)
                                            } else {
                                                viewModel.topUpBalance(25.0, "topup_25_${System.currentTimeMillis()}", BillingManager.BALANCE_TOPUP_25)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoading
                                    ) {
                                        Text("+$25")
                                    }
                                }
                            }
                        }
                    }
                } else if (activeTab == 1) {
                    // POSTPAID INVOICING CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Postpaid Monthly Billing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Consume resources throughout your billing period and pay periodic invoices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                val mode = billingState?.paymentMode ?: "PREPAID"
                                val credit = billingState?.creditLimit ?: 50.0
                                val spending = billingState?.spendingLimit ?: 100.0
                                val alertThresh = billingState?.spendingAlertThreshold ?: 80.0

                                Text("Billing Mode: $mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Credit Limit: $${String.format(Locale.US, "%.2f", credit)} USD", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Spending Ceiling: $${String.format(Locale.US, "%.2f", spending)} USD (Alert at $${String.format(Locale.US, "%.2f", alertThresh)})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Past Invoice Statements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                if (invoices.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("No past invoices for this billing period.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        invoices.forEach { invoice ->
                                            InvoiceItemCard(invoice = invoice)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // AUTHORITATIVE RATES TABLE
                    item {
                        val pricing = pricingState ?: CloudPricing()
                        val maintPct = pricing.maintenanceAllocationPct
                        val marginPct = pricing.founderMarginPct
                        val multiplier = 1.0 + maintPct + marginPct

                        val storageRate = pricing.storageRatePerGbMonth
                        val bandwidthRate = pricing.bandwidthRatePerGb
                        val writeRate1k = pricing.writeOpRatePer10k / 10.0
                        val readRate1k = pricing.readOpRatePer10k / 10.0
                        val backupRate = pricing.backupRatePerGbMonth

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Authoritative Backend Pricing Formula (${pricing.versionName})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Customer Rate = Base Provider Cost + ${(maintPct * 100).toInt()}% Maintenance Allocation + ${(marginPct * 100).toInt()}% Founder Margin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                RateRow(
                                    label = "Cloud Storage",
                                    baseRate = "$${String.format(Locale.US, "%.4f", storageRate)} / GB-mo",
                                    finalRate = "$${String.format(Locale.US, "%.4f", storageRate * multiplier)} / GB-mo"
                                )
                                RateRow(
                                    label = "Egress Bandwidth",
                                    baseRate = "$${String.format(Locale.US, "%.4f", bandwidthRate)} / GB",
                                    finalRate = "$${String.format(Locale.US, "%.4f", bandwidthRate * multiplier)} / GB"
                                )
                                RateRow(
                                    label = "Write Operations (Class A)",
                                    baseRate = "$${String.format(Locale.US, "%.4f", writeRate1k)} / 1k ops",
                                    finalRate = "$${String.format(Locale.US, "%.4f", writeRate1k * multiplier)} / 1k ops"
                                )
                                RateRow(
                                    label = "Read Operations (Class B)",
                                    baseRate = "$${String.format(Locale.US, "%.5f", readRate1k)} / 1k ops",
                                    finalRate = "$${String.format(Locale.US, "%.5f", readRate1k * multiplier)} / 1k ops"
                                )
                                RateRow(
                                    label = "Encrypted Backups",
                                    baseRate = "$${String.format(Locale.US, "%.4f", backupRate)} / GB-mo",
                                    finalRate = "$${String.format(Locale.US, "%.4f", backupRate * multiplier)} / GB-mo"
                                )
                            }
                        }
                    }
                }

                // REALTIME CONSUMPTION & CHARGES BREAKDOWN
                item {
                    val usage = billingState?.currentUsage ?: ResourceUsage()
                    val est = billingState?.estimatedCharges ?: CostEstimate()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Current Cloud Consumption", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            ConsumptionMetric(icon = Icons.Rounded.Storage, label = "Active Cloud Storage", value = formatBytes(usage.storageBytes))
                            ConsumptionMetric(icon = Icons.Rounded.CloudDownload, label = "Network Bandwidth Transferred", value = formatBytes(usage.bandwidthBytes))
                            ConsumptionMetric(icon = Icons.Rounded.CloudUpload, label = "API Read/Write Operations", value = "${usage.readOperations} Reads / ${usage.writeOperations} Writes")

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Real-Time Estimated Charges Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Base Resource Cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${String.format(Locale.US, "%.6f", est.baseCost)} ${est.currency}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Maintenance Allocation (12%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${String.format(Locale.US, "%.6f", est.maintenanceFee)} ${est.currency}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Founder Margin (5%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${String.format(Locale.US, "%.6f", est.founderMargin)} ${est.currency}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Estimated Cost", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("$${String.format(Locale.US, "%.6f", est.totalEstimated)} ${est.currency}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // SELF HOSTING LINK
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Self-Host Keeftalk Infrastructure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Prefer operating your own independent cloud? Keeftalk is 100% open-source and easy to self-host using Supabase and Cloudflare R2.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceItemCard(invoice: InvoiceSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Invoice #${invoice.invoiceNumber.ifBlank { invoice.id.take(8) }}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (invoice.billingPeriodStart.isNotBlank()) {
                    Text("${invoice.billingPeriodStart.take(10)} - ${invoice.billingPeriodEnd.take(10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$${String.format(Locale.US, "%.2f", invoice.total)} ${invoice.currency}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (invoice.status.uppercase() == "PAID") Color(0xFF1B5E20) else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = invoice.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RateRow(label: String, baseRate: String, finalRate: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(finalRate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Text("Base provider cost: $baseRate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ConsumptionMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.2f KB".format(Locale.US, kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.2f MB".format(Locale.US, mb)
    val gb = mb / 1024.0
    return "%.3f GB".format(Locale.US, gb)
}
