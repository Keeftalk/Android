package tn.keeftalk.chat.extension.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.extension.action.ExtensionActionDispatcher
import tn.keeftalk.chat.extension.model.CardComponentType
import tn.keeftalk.chat.extension.model.DeclarativeCard
import tn.keeftalk.chat.extension.persistence.ExtensionStorageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclarativeCardRenderer(
    card: DeclarativeCard,
    modifier: Modifier = Modifier,
    onCardClick: (String?) -> Unit = {},
    onNavigate: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val storageManager = remember(context) { ExtensionStorageManager(context) }
    val actionDispatcher = remember(context, storageManager) { ExtensionActionDispatcher(context, storageManager) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !card.actionUrl.isNullOrBlank()) { onCardClick(card.actionUrl) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (card.subtitle.isNotBlank()) {
                        Text(
                            text = card.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = card.providerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            // Render Components
            card.components.forEach { comp ->
                when (comp.type) {
                    CardComponentType.TITLE -> {
                        Text(
                            text = comp.text,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    CardComponentType.LABEL, CardComponentType.TEXT -> {
                        Text(
                            text = comp.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CardComponentType.NUMBER_STAT -> {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = comp.text,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (comp.subtext.isNotBlank()) {
                                Text(
                                    text = comp.subtext,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    CardComponentType.PROGRESS_BAR -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (comp.text.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(comp.text, style = MaterialTheme.typography.bodySmall)
                                    Text("${(comp.value / comp.maxValue * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { if (comp.maxValue > 0) (comp.value / comp.maxValue).coerceIn(0f, 1f) else 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                    CardComponentType.BADGE -> {
                        SuggestionChip(
                            onClick = {
                                if (!card.actionUrl.isNullOrBlank()) onCardClick(card.actionUrl)
                            },
                            label = { Text(comp.badgeText ?: comp.text) }
                        )
                    }
                    CardComponentType.BUTTON -> {
                        Button(
                            onClick = {
                                comp.action?.let { act ->
                                    actionDispatcher.dispatch(
                                        extensionId = card.id,
                                        action = act,
                                        onNavigate = onNavigate,
                                        onOpenUrl = onCardClick,
                                        clipboardManager = clipboardManager
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(comp.text)
                        }
                    }
                    CardComponentType.DIVIDER -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
