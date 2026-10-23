package tn.keeftalk.chat.ui.settings

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
import tn.keeftalk.chat.data.billing.BillingManager
import tn.keeftalk.chat.data.billing.PurchaseResult
import tn.keeftalk.chat.domain.model.SubscriptionPlan
import tn.keeftalk.chat.util.BandwidthPolicy
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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("100% Free Local Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Every Keeftalk feature (Notes, Books, Habits, Calendar, Secrets, Market, Chats, Maps) is 100% free and works fully offline on your device. Subscriptions only unlock optional cloud synchronization and cloud persistence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }

                item {
                    PlanCard(
                        title = SubscriptionPlan.FREE.displayName,
                        price = "$0",
                        description = "Free Forever Local & Offline Use",
                        features = listOf(
                            "Full offline feature access (Notes, Books, Habits, Secrets, etc.)",
                            "My Chats, My Vault, My Maps & My Market always free",
                            "${SubscriptionPlan.FREE.formatStorageLimit()} Vault storage for local files",
                            "Market posts active for 30 days (+ 1 extension = 60 days max)"
                        ),
                        isActive = profile?.planType == SubscriptionPlan.FREE,
                        isLoading = false,
                        onSelect = {}
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Vault Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cloud Sync and Vault Storage are separate. Syncing a feature does not give you additional file storage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    val proMonthly = productDetails.find { it.productId == BillingManager.PRO_MONTHLY }
                    PlanCard(
                        title = "Universal Premium",
                        price = proMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$9.99/mo",
                        description = "All feature sync + 500 GB Vault storage",
                        features = listOf(
                            "All paid feature cloud sync (Notes, Books, Habits, Secrets, etc.)",
                            "500 GB encrypted Vault storage pool",
                            "Premium themes & Market post extensions (+7d per buyer message)",
                            "Multi-device & identity verification eligibility"
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
                }

                item {
                    val familyMonthly = productDetails.find { it.productId == BillingManager.FAMILY_MONTHLY }
                    PlanCard(
                        title = "Family Premium",
                        price = familyMonthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$19.99/mo",
                        description = "Shared 2 TB Vault pool for up to 6 people",
                        features = listOf(
                            "Shared 2 TB Vault storage pool across 6 accounts (1 Owner + 5 Members)",
                            "Universal Premium capabilities for every active family member",
                            "Private & end-to-end encrypted (members cannot see each other's data)",
                            "1-year member removal protection enforced server-side"
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
