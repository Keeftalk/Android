package com.keeftalk.chat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.domain.model.AppCustomization
import com.keeftalk.chat.domain.model.FabAction
import com.keeftalk.chat.domain.model.KeeftalkModule
import com.keeftalk.chat.ui.components.KeeftalkFeatureTopBar
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCustomizationScreen(
    viewModel: AppCustomizationViewModel,
    onBack: () -> Unit
) {
    val customization by viewModel.customization.collectAsState()
    val icons = LocalAppIcons.current
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KeeftalkFeatureTopBar(
                title = "App Customization",
                onBack = onBack,
                onSettings = { showMenu = true },
                actions = {
                    Box {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Enable Customization") },
                                leadingIcon = { 
                                    Checkbox(checked = customization.isEnabled, onCheckedChange = null) 
                                },
                                onClick = { viewModel.toggleIsEnabled() }
                            )
                            DropdownMenuItem(
                                text = { Text("Cloud Sync") },
                                leadingIcon = { 
                                    Checkbox(checked = customization.syncEnabled, onCheckedChange = null) 
                                },
                                onClick = { viewModel.toggleSyncEnabled() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Sync Now") },
                                leadingIcon = { Icon(Icons.Default.Sync, null) },
                                enabled = customization.syncEnabled,
                                onClick = { 
                                    viewModel.manualSync()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Text(
                    text = "FEATURES VISIBILITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            items(KeeftalkModule.entries) { module ->
                SettingsToggleItem(
                    icon = getModuleIcon(module, icons),
                    title = getModuleName(module),
                    subtitle = if (AppCustomization.MANDATORY_MODULES.contains(module)) "Mandatory Feature" else null,
                    checked = customization.enabledModules.contains(module),
                    onCheckedChange = { viewModel.toggleModule(module) }
                )
            }

            item {
                Text(
                    text = "FAB MENU ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            items(FabAction.entries) { action ->
                SettingsToggleItem(
                    icon = getFabActionIcon(action, icons),
                    title = getFabActionName(action),
                    subtitle = if (AppCustomization.MANDATORY_FAB_ACTIONS.contains(action)) "Mandatory Action" else null,
                    checked = customization.enabledFabActions.contains(action),
                    onCheckedChange = { viewModel.toggleFabAction(action) }
                )
            }

            item {
                Text(
                    text = "BOTTOM NAVIGATION MANAGER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                Text(
                    "Manage your bottom bar tabs (Max 5). Use arrows to reorder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(customization.bottomNavTabs) { index, module ->
                BottomNavItem(
                    module = module,
                    index = index,
                    totalCount = customization.bottomNavTabs.size,
                    onMoveUp = { viewModel.reorderBottomNav(index, index - 1) },
                    onMoveDown = { viewModel.reorderBottomNav(index, index + 1) },
                    onRemove = { viewModel.removeFromBottomNav(module) },
                    icon = getModuleIcon(module, icons)
                )
            }

            if (customization.bottomNavTabs.size < 5) {
                val availableModules = KeeftalkModule.entries.filter { 
                    customization.enabledModules.contains(it) && !customization.bottomNavTabs.contains(it) 
                }
                
                if (availableModules.isNotEmpty()) {
                    item {
                        Text(
                            "Add Tab",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(availableModules) { module ->
                        Surface(
                            onClick = { viewModel.addToBottomNav(module) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(getModuleIcon(module, icons), null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(getModuleName(module))
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ADVANCED UI SCALING",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            item {
                ScalingSliderItem(
                    label = "Primary FAB Size",
                    value = customization.fabSizeMultiplier,
                    onValueChange = { viewModel.updateFabSize(it) },
                    valueRange = 0.5f..1.5f
                )
            }

            item {
                ScalingSliderItem(
                    label = "Secondary FAB Size",
                    value = customization.secondaryFabSizeMultiplier,
                    onValueChange = { viewModel.updateSecondaryFabSize(it) },
                    valueRange = 0.5f..1.5f
                )
            }

            item {
                ScalingSliderItem(
                    label = "Chat List Spacing",
                    value = customization.chatSpacingMultiplier,
                    onValueChange = { viewModel.updateChatSpacing(it) },
                    valueRange = 0.2f..1.5f
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset to Defaults")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ScalingSliderItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 20
        )
    }
}

@Composable
fun BottomNavItem(
    module: KeeftalkModule,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(getModuleName(module), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onMoveDown, enabled = index < totalCount - 1) {
                Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(20.dp))
            }
            if (!AppCustomization.MANDATORY_MODULES.contains(module)) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun getModuleName(module: KeeftalkModule) = when(module) {
    KeeftalkModule.CHATS -> "Chats"
    KeeftalkModule.CALLS -> "Calls (VoIP)"
    KeeftalkModule.SMS -> "SMS/MMS"
    KeeftalkModule.AGENDA -> "My Agenda"
    KeeftalkModule.NOTES -> "My Notes"
    KeeftalkModule.EMAIL -> "My Email"
    KeeftalkModule.VAULT -> "My Vault"
    KeeftalkModule.WALLET -> "My Wallet"
    KeeftalkModule.FEED -> "My Feed"
}

private fun getFabActionName(action: FabAction) = when(action) {
    FabAction.NEW_CHAT -> "New Chat"
    FabAction.NEW_GROUP -> "New Group"
    FabAction.QR -> "Scan / Show QR"
    FabAction.AI -> "Chat with AI"
    FabAction.ACCESSIBILITY -> "Accessibility Options"
}

private fun getModuleIcon(module: KeeftalkModule, icons: com.keeftalk.chat.ui.theme.AppIcons) = when(module) {
    KeeftalkModule.CHATS -> icons.messageCircle
    KeeftalkModule.CALLS -> icons.phone
    KeeftalkModule.SMS -> icons.messageSquare
    KeeftalkModule.AGENDA -> icons.calendar
    KeeftalkModule.NOTES -> icons.notes
    KeeftalkModule.EMAIL -> icons.email
    KeeftalkModule.VAULT -> icons.lock
    KeeftalkModule.WALLET -> icons.wallet
    KeeftalkModule.FEED -> icons.feed
}

private fun getFabActionIcon(action: FabAction, icons: com.keeftalk.chat.ui.theme.AppIcons) = when(action) {
    FabAction.NEW_CHAT -> icons.messageCircle
    FabAction.NEW_GROUP -> Icons.Default.Groups
    FabAction.QR -> icons.qrCode
    FabAction.AI -> icons.brain
    FabAction.ACCESSIBILITY -> icons.sparkles
}
