package tn.keeftalk.chat.extension.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.extension.action.ExtensionActionDispatcher
import tn.keeftalk.chat.extension.model.DeclarativeScreen
import tn.keeftalk.chat.extension.model.ScreenComponent
import tn.keeftalk.chat.extension.model.ScreenComponentType
import tn.keeftalk.chat.extension.persistence.ExtensionStorageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclarativeScreenRenderer(
    screen: DeclarativeScreen,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigate: ((String) -> Unit)? = null,
    onOpenUrl: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val storageManager = remember(context) { ExtensionStorageManager(context) }
    val actionDispatcher = remember(context, storageManager) { ExtensionActionDispatcher(context, storageManager) }

    val prefsState by storageManager.getPreferencesFlow(screen.extensionId).collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(screen.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (screen.subtitle.isNotBlank()) {
                            Text(screen.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            screen.components.forEach { comp ->
                RenderScreenComponent(
                    extensionId = screen.extensionId,
                    comp = comp,
                    prefs = prefsState,
                    storageManager = storageManager,
                    actionDispatcher = actionDispatcher,
                    clipboardManager = clipboardManager,
                    onNavigate = onNavigate,
                    onOpenUrl = onOpenUrl
                )
            }
        }
    }
}

@Composable
private fun RenderScreenComponent(
    extensionId: String,
    comp: ScreenComponent,
    prefs: Map<String, String>,
    storageManager: ExtensionStorageManager,
    actionDispatcher: ExtensionActionDispatcher,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    onNavigate: ((String) -> Unit)?,
    onOpenUrl: ((String) -> Unit)?
) {
    when (comp.type) {
        ScreenComponentType.HEADER -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(comp.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (comp.subtext.isNotBlank()) {
                        Text(comp.subtext, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
        ScreenComponentType.SECTION_TITLE -> {
            Text(
                text = comp.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        ScreenComponentType.TEXT_BODY -> {
            Text(
                text = comp.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ScreenComponentType.KEY_VALUE -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comp.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                val storedVal = comp.key?.let { prefs[it] } ?: comp.value
                Text(storedVal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        ScreenComponentType.STAT -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(comp.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (comp.subtext.isNotBlank()) {
                            Text(comp.subtext, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                    val storedVal = comp.key?.let { prefs[it] } ?: comp.value
                    Text(storedVal, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        ScreenComponentType.BUTTON -> {
            Button(
                onClick = {
                    comp.action?.let { act ->
                        actionDispatcher.dispatch(
                            extensionId = extensionId,
                            action = act,
                            onNavigate = onNavigate,
                            onOpenUrl = onOpenUrl,
                            clipboardManager = clipboardManager
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(comp.label)
            }
        }
        ScreenComponentType.TEXT_INPUT -> {
            val key = comp.key ?: comp.label
            val textVal = prefs[key] ?: comp.value
            OutlinedTextField(
                value = textVal,
                onValueChange = { newValue ->
                    storageManager.setPreference(extensionId, key, newValue)
                },
                label = { Text(comp.label) },
                placeholder = { if (comp.placeholder.isNotBlank()) Text(comp.placeholder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
        ScreenComponentType.PROGRESS -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (comp.label.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(comp.label, style = MaterialTheme.typography.bodySmall)
                        val pct = if (comp.maxProgressValue > 0) (comp.progressValue / comp.maxProgressValue * 100).toInt() else 0
                        Text("$pct%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
                LinearProgressIndicator(
                    progress = { if (comp.maxProgressValue > 0) (comp.progressValue / comp.maxProgressValue).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }
        ScreenComponentType.BADGE_ROW -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                comp.badges.forEach { badge ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(badge) }
                    )
                }
            }
        }
        ScreenComponentType.CARD_CONTAINER -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (comp.label.isNotBlank()) {
                        Text(comp.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    comp.children.forEach { child ->
                        RenderScreenComponent(
                            extensionId = extensionId,
                            comp = child,
                            prefs = prefs,
                            storageManager = storageManager,
                            actionDispatcher = actionDispatcher,
                            clipboardManager = clipboardManager,
                            onNavigate = onNavigate,
                            onOpenUrl = onOpenUrl
                        )
                    }
                }
            }
        }
        ScreenComponentType.DIVIDER -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
