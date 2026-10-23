package tn.keeftalk.chat.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.domain.model.entitlement.FeatureId

@Composable
fun FeatureCloudSyncCard(
    featureId: FeatureId,
    description: String,
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entitlementManager = remember { AppModule.provideEntitlementManager(context) }
    val hasSync by entitlementManager.hasFeatureSync(featureId).collectAsState(initial = featureId.isAlwaysFree)
    val isPremium by entitlementManager.isUniversalPremium.collectAsState()
    val activeKeys by entitlementManager.activeEntitlements.collectAsState()

    val isIndividuallyActive = activeKeys.contains(featureId.key)
    val isIncludedWithPremium = isPremium && !isIndividuallyActive && !featureId.isAlwaysFree

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (hasSync) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (hasSync) Color(0xFF2ECC71).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasSync) Icons.Rounded.CloudDone else Icons.Rounded.CloudQueue,
                        contentDescription = null,
                        tint = if (hasSync) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        featureId.isAlwaysFree -> MaterialTheme.colorScheme.secondaryContainer
                        hasSync -> Color(0xFF2ECC71).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when {
                            featureId.isAlwaysFree -> "100% Free"
                            isIncludedWithPremium -> "Included in Pro"
                            isIndividuallyActive -> "Active (Redeemed)"
                            hasSync -> "Active"
                            else -> "$0.99/mo"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            featureId.isAlwaysFree -> MaterialTheme.colorScheme.onSecondaryContainer
                            hasSync -> Color(0xFF2ECC71)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasSync && !featureId.isAlwaysFree) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF2ECC71).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF2ECC71)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isIncludedWithPremium) "Cloud Sync Active (Included with Keeftalk Pro)" else "Cloud Sync Active & Enabled for ${featureId.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2ECC71)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Local-first data safety notice
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your data is stored safely on this device offline and is never deleted if a subscription expires.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!featureId.isAlwaysFree) {
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onEnableClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (hasSync) ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)) else ButtonDefaults.buttonColors(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            isIncludedWithPremium -> "Active (Included in Pro)"
                            hasSync -> "Active — Cloud Sync Enabled ✓"
                            else -> "Redeem Code or Enable Cloud Sync"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
