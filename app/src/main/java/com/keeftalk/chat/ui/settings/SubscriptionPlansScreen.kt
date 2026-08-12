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
import com.keeftalk.chat.domain.model.SubscriptionPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    viewModel: SettingsViewModel,
    billingManager: BillingManager,
    onBack: () -> Unit,
) {
    val profile by viewModel.currentUserProfile.collectAsState()
    val productDetails by billingManager.productDetails.collectAsState()
    val activity = LocalActivity.current ?: return

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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PlanCard(
                    title = "Free",
                    price = "$0",
                    description = "The complete messenger",
                    features = listOf("5 GB encrypted storage", "Compressed media", "100 MB max upload"),
                    isActive = profile?.planType == SubscriptionPlan.FREE,
                    onSelect = {}
                )
            }

            item {
                val plusMonthly = productDetails.find { it.productId == BillingManager.PLUS_MONTHLY }
                PlanCard(
                    title = "Plus",
                    price = plusMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$4.99/mo",
                    description = "For users who need more space",
                    features = listOf("100 GB encrypted storage", "Original quality media", "1 GB max upload"),
                    isActive = (profile?.planType == SubscriptionPlan.PLUS_MONTHLY || profile?.planType == SubscriptionPlan.PLUS_YEARLY),
                    onSelect = { plusMonthly?.let { billingManager.launchPurchaseFlow(activity, it) } }
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val proMonthly = productDetails.find { it.productId == BillingManager.PRO_MONTHLY }
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Pro",
                        price = proMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$14.99/mo",
                        description = "Power user tools",
                        features = listOf("500 GB storage", "AI Assistant", "5 GB max upload"),
                        isActive = profile?.planType == SubscriptionPlan.PRO_MONTHLY,
                        onSelect = { proMonthly?.let { billingManager.launchPurchaseFlow(activity, it) } }
                    )

                    val familyMonthly = productDetails.find { it.productId == BillingManager.FAMILY_MONTHLY }
                    PlanCard(
                        modifier = Modifier.weight(1f),
                        title = "Family",
                        price = familyMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$14.99/mo",
                        description = "Shared family pool",
                        features = listOf("2 TB shared pool", "Up to 6 users", "Plus features for all"),
                        isActive = profile?.planType == SubscriptionPlan.FAMILY_MONTHLY,
                        onSelect = { familyMonthly?.let { billingManager.launchPurchaseFlow(activity, it) } }
                    )
                }
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
    modifier: Modifier = Modifier
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
                enabled = !isActive,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isActive) "Current Plan" else "Select Plan")
            }
        }
    }
}
