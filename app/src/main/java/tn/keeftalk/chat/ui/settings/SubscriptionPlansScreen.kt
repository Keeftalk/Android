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
import tn.keeftalk.chat.data.billing.PurchaseResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    viewModel: SettingsViewModel,
    billingManager: BillingManager,
    onBack: () -> Unit,
) {
    val profile by viewModel.currentUserProfile.collectAsState()
    val productDetails by billingManager.productDetails.collectAsState()
    val isQuerying by billingManager.isQuerying.collectAsState()
    val isServiceConnected by billingManager.isServiceConnected.collectAsState()
    val activity = LocalActivity.current ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Prepaid, 1 = Postpaid, 2 = Pricing Rates
    var mockPrepaidBalance by remember { mutableStateOf(5.00) }

    LaunchedEffect(Unit) {
        billingManager.purchaseEvents.collect { result ->
            when (result) {
                is PurchaseResult.Success -> {
                    mockPrepaidBalance += 5.00
                    snackbarHostState.showSnackbar("Prepaid cloud balance topped up successfully!")
                }
                is PurchaseResult.Error -> {
                    snackbarHostState.showSnackbar("Top-up Error: ${result.message}")
                }
                is PurchaseResult.Cancelled -> {}
            }
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Available Prepaid Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$${String.format("%.2f", mockPrepaidBalance)} USD", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    color = if (mockPrepaidBalance > 2.0) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        if (mockPrepaidBalance > 2.0) "Active • Low balance alert threshold: $2.00" else "Low Balance Warning • Cloud uploads paused when $0",
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
                                            if (topup5 != null) billingManager.launchPurchaseFlow(activity, topup5)
                                            else mockPrepaidBalance += 5.0
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+$5")
                                    }
                                    Button(
                                        onClick = {
                                            val topup10 = productDetails.find { it.productId == BillingManager.BALANCE_TOPUP_10 }
                                            if (topup10 != null) billingManager.launchPurchaseFlow(activity, topup10)
                                            else mockPrepaidBalance += 10.0
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+$10")
                                    }
                                    Button(
                                        onClick = {
                                            val topup25 = productDetails.find { it.productId == BillingManager.BALANCE_TOPUP_25 }
                                            if (topup25 != null) billingManager.launchPurchaseFlow(activity, topup25)
                                            else mockPrepaidBalance += 25.0
                                        },
                                        modifier = Modifier.weight(1f)
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
                                Text("Credit Limit: $50.00 USD", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Spending Ceiling: $100.00 USD", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(onClick = {
                                    scope.launch { snackbarHostState.showSnackbar("Monthly invoice statements are generated on the 1st of each month.") }
                                }) {
                                    Text("View Past Invoices")
                                }
                            }
                        }
                    }
                } else {
                    // AUTHORITATIVE RATES TABLE
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Authoritative Backend Pricing Formula", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Customer Rate = Base Provider Cost + 12% Maintenance Allocation + 5% Founder Margin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                RateRow("Cloud Storage", "$0.0150 / GB-mo", "$0.0176 / GB-mo")
                                RateRow("Egress Bandwidth", "$0.0200 / GB", "$0.0235 / GB")
                                RateRow("Write Operations (Class A)", "$0.0045 / 1k ops", "$0.0053 / 1k ops")
                                RateRow("Read Operations (Class B)", "$0.00036 / 1k ops", "$0.00042 / 1k ops")
                                RateRow("Encrypted Backups", "$0.0200 / GB-mo", "$0.0235 / GB-mo")
                            }
                        }
                    }
                }

                // REALTIME CONSUMPTION BREAKDOWN
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Current Cloud Consumption", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            ConsumptionMetric(icon = Icons.Rounded.Storage, label = "Active Cloud Storage", value = "0.42 GB")
                            ConsumptionMetric(icon = Icons.Rounded.CloudDownload, label = "Network Bandwidth Transferred", value = "1.15 GB")
                            ConsumptionMetric(icon = Icons.Rounded.CloudUpload, label = "API Read/Write Operations", value = "1,420 Ops")
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
