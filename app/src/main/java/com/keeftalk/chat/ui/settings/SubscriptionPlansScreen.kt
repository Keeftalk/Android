package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.data.billing.BillingManager
import com.keeftalk.chat.data.billing.PurchaseResult
import com.keeftalk.chat.domain.model.SubscriptionPlan
import com.keeftalk.chat.util.BandwidthPolicy
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

    LaunchedEffect(Unit) {
        billingManager.purchaseEvents.collect { result ->
            when (result) {
                is PurchaseResult.Success -> {
                    snackbarHostState.showSnackbar("Purchase successful! Your plan will be updated shortly.")
                }
                is PurchaseResult.Error -> {
                    snackbarHostState.showSnackbar("Error: ${result.message}")
                }
                is PurchaseResult.Cancelled -> {
                    // Silent cancellation is often preferred, but we could log it
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keeftalk Plans", fontWeight = FontWeight.Bold) },
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
                if (!isServiceConnected && !isQuerying) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Billing service unavailable. Please check your internet connection or Play Store account.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                item {
                    PlanCard(
                        title = SubscriptionPlan.FREE.displayName,
                        price = "$0",
                        description = "The complete messenger",
                        features = listOf(
                            "${SubscriptionPlan.FREE.formatStorageLimit()} encrypted storage",
                            "Compressed media",
                            "${SubscriptionPlan.FREE.formatMaxFileSize()} max upload",
                            "Up to ${BandwidthPolicy.formatLimit(BandwidthPolicy.getUploadLimit(SubscriptionPlan.FREE))} upload",
                            "Up to ${BandwidthPolicy.formatLimit(BandwidthPolicy.getDownloadLimit(SubscriptionPlan.FREE))} download"
                        ),
                        isActive = profile?.planType == SubscriptionPlan.FREE,
                        isLoading = false,
                        onSelect = {}
                    )
                }

                item {
                    val plusMonthly = productDetails.find { it.productId == BillingManager.PLUS_MONTHLY }
                    PlanCard(
                        title = SubscriptionPlan.PLUS_MONTHLY.displayName,
                        price = plusMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$4.99/mo",
                        description = "For users who need more space",
                        features = listOf(
                            "${SubscriptionPlan.PLUS_MONTHLY.formatStorageLimit()} encrypted storage",
                            "Original quality media",
                            "${SubscriptionPlan.PLUS_MONTHLY.formatMaxFileSize()} max upload",
                            "Up to ${BandwidthPolicy.formatLimit(BandwidthPolicy.getUploadLimit(SubscriptionPlan.PLUS_MONTHLY))} upload",
                            "Up to ${BandwidthPolicy.formatLimit(BandwidthPolicy.getDownloadLimit(SubscriptionPlan.PLUS_MONTHLY))} download"
                        ),
                        isActive = (profile?.planType == SubscriptionPlan.PLUS_MONTHLY || profile?.planType == SubscriptionPlan.PLUS_YEARLY),
                        isLoading = isQuerying && plusMonthly == null,
                        onSelect = { 
                            plusMonthly?.let { 
                                billingManager.launchPurchaseFlow(activity, it) 
                            } ?: run {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Subscription plan details not found. Please try again later.")
                                }
                            }
                        }
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val proMonthly = productDetails.find { it.productId == BillingManager.PRO_MONTHLY }
                        PlanCard(
                            modifier = Modifier.weight(1f),
                            title = SubscriptionPlan.PRO_MONTHLY.displayName,
                            price = proMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$14.99/mo",
                            description = "Power user tools",
                            features = listOf(
                                "${SubscriptionPlan.PRO_MONTHLY.formatStorageLimit()} storage",
                                "AI Assistant",
                                "${SubscriptionPlan.PRO_MONTHLY.formatMaxFileSize()} max upload",
                                "Unlimited transfer speed"
                            ),
                            isActive = profile?.planType == SubscriptionPlan.PRO_MONTHLY,
                            isLoading = isQuerying && proMonthly == null,
                            onSelect = { 
                                proMonthly?.let { 
                                    billingManager.launchPurchaseFlow(activity, it) 
                                } ?: run {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Subscription plan details not found.")
                                    }
                                }
                            }
                        )

                        val familyMonthly = productDetails.find { it.productId == BillingManager.FAMILY_MONTHLY }
                        PlanCard(
                            modifier = Modifier.weight(1f),
                            title = SubscriptionPlan.FAMILY_MONTHLY.displayName,
                            price = familyMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$14.99/mo",
                            description = "Shared family pool",
                            features = listOf(
                                "${SubscriptionPlan.FAMILY_MONTHLY.formatStorageLimit()} shared pool",
                                "Up to 6 users",
                                "Plus features for all",
                                "Unlimited transfer speed"
                            ),
                            isActive = profile?.planType == SubscriptionPlan.FAMILY_MONTHLY,
                            isLoading = isQuerying && familyMonthly == null,
                            onSelect = { 
                                familyMonthly?.let { 
                                    billingManager.launchPurchaseFlow(activity, it) 
                                } ?: run {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Subscription plan details not found.")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (isQuerying) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    description: String,
    features: List<String>,
    isActive: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(price, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive && !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text(if (isActive) "Current Plan" else "Select Plan")
                }
            }
        }
    }
}
