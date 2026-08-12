package com.keeftalk.chat.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.theme.LocalAppIcons

enum class AccountSubSetting {
    // Identity
    ID_USER, ID_NAME, ID_BIO, ID_QR,
    // Security
    SEC_PASS, ID_2FA, SEC_AEK, SEC_SESSIONS, SEC_LOG, SEC_RECOVERY,
    // Modules
    MOD_EMAIL, MOD_SMS, MOD_VOIP, MOD_WALLET, MOD_CALENDAR,
    // Cloud
    CLOUD_VAULT, CLOUD_NOTES, CLOUD_BACKUP, CLOUD_CACHE,
    // Privacy
    PRIV_PERMS, PRIV_BLOCKED,
    // Subscription
    SUB_STORAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onSubscriptionPlansClick: () -> Unit
) {
    var selectedSubSetting by remember { mutableStateOf<AccountSubSetting?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            SettingsHeader(
                title = if (selectedSubSetting == null) "Account Center" else selectedSubSetting!!.toDisplayName(),
                onBack = {
                    if (selectedSubSetting != null) {
                        selectedSubSetting = null
                    } else {
                        onBack()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AnimatedContent(
            targetState = selectedSubSetting,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "AccountContentTransition"
        ) { subSetting ->
            if (subSetting == null) {
                // Compact Main Dashboard
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp)
                ) {
                    CompactAccountDashboardHeader()

                    AccountControlSection(title = "Billing & Storage") {
                        ControlTileGrid {
                            CompactAccountTile("Subscription & Storage", "Membership & Quota", Icons.Rounded.Storage, Color(0xFF3498DB)) { selectedSubSetting = AccountSubSetting.SUB_STORAGE }
                        }
                    }

                    AccountControlSection(title = "Identity") {
                        ControlTileGrid {
                            CompactAccountTile("Username Handle", "Manage @username", Icons.Rounded.AlternateEmail, Color(0xFF6C63FF)) { selectedSubSetting = AccountSubSetting.ID_USER }
                            CompactAccountTile("Display Name", "Public identity", Icons.Rounded.Badge, Color(0xFF00CCCC)) { selectedSubSetting = AccountSubSetting.ID_NAME }
                            CompactAccountTile("Profile Bio", "Personal info", Icons.Rounded.Info, Color(0xFF7D5CFF)) { selectedSubSetting = AccountSubSetting.ID_BIO }
                            CompactAccountTile("My QR Code", "Share identity", Icons.Rounded.QrCode2, Color(0xFF607D8B)) { selectedSubSetting = AccountSubSetting.ID_QR }
                        }
                    }

                    AccountControlSection(title = "Security & Trust") {
                        ControlTileGrid {
                            CompactAccountTile("Password", "Auth credentials", Icons.Rounded.Password, Color(0xFFE91E63)) { selectedSubSetting = AccountSubSetting.SEC_PASS }
                            CompactAccountTile("2-Step Verification", "MFA setup", Icons.Rounded.VpnKey, Color(0xFF2196F3)) { selectedSubSetting = AccountSubSetting.ID_2FA }
                            CompactAccountTile("Encryption Key", "AEK status", Icons.Rounded.Security, Color(0xFF455A64)) { selectedSubSetting = AccountSubSetting.SEC_AEK }
                            CompactAccountTile("Active Sessions", "Logged devices", Icons.Rounded.Devices, Color(0xFF4CAF50)) { selectedSubSetting = AccountSubSetting.SEC_SESSIONS }
                            CompactAccountTile("Security Activity", "Action logs", Icons.Rounded.HistoryEdu, Color(0xFF795548)) { selectedSubSetting = AccountSubSetting.SEC_LOG }
                            CompactAccountTile("Recovery Methods", "Restore access", Icons.Rounded.HealthAndSafety, Color(0xFFFF9800)) { selectedSubSetting = AccountSubSetting.SEC_RECOVERY }
                        }
                    }

                    AccountControlSection(title = "Module Integrations") {
                        ControlTileGrid {
                            CompactAccountTile("Unified Email", "Connect accounts", Icons.Rounded.Email, Color(0xFF3F51B5)) { selectedSubSetting = AccountSubSetting.MOD_EMAIL }
                            CompactAccountTile("SMS Bridge", "Carrier sync", Icons.Rounded.Sms, Color(0xFF009688)) { selectedSubSetting = AccountSubSetting.MOD_SMS }
                            CompactAccountTile("VoIP Calling", "Call protocols", Icons.Rounded.Call, Color(0xFFFF5722)) { selectedSubSetting = AccountSubSetting.MOD_VOIP }
                            CompactAccountTile("Digital Wallet", "Keeftalk Pay", Icons.Rounded.AccountBalanceWallet, Color(0xFF9C27B0)) { selectedSubSetting = AccountSubSetting.MOD_WALLET }
                            CompactAccountTile("Smart Calendar", "Agenda sync", Icons.Rounded.CalendarMonth, Color(0xFF00BCD4)) { selectedSubSetting = AccountSubSetting.MOD_CALENDAR }
                        }
                    }

                    AccountControlSection(title = "Cloud & Storage") {
                        ControlTileGrid {
                            CompactAccountTile("Secure Vault", "Encrypted files", Icons.Rounded.Lock, Color(0xFF3B82F6)) { selectedSubSetting = AccountSubSetting.CLOUD_VAULT }
                            CompactAccountTile("Notes Sync", "Data health", Icons.Rounded.EditNote, Color(0xFFF43F5E)) { selectedSubSetting = AccountSubSetting.CLOUD_NOTES }
                            CompactAccountTile("System Backup", "Cloud snapshots", Icons.Rounded.CloudSync, Color(0xFF03A9F4)) { selectedSubSetting = AccountSubSetting.CLOUD_BACKUP }
                            CompactAccountTile("Cache Manager", "Storage cleanup", Icons.Rounded.Storage, Color(0xFFF44336)) { selectedSubSetting = AccountSubSetting.CLOUD_CACHE }
                        }
                    }

                    AccountControlSection(title = "Privacy & Safety") {
                        ControlTileGrid {
                            CompactAccountTile("Permissions", "System access", Icons.Rounded.Rule, Color(0xFF607D8B)) { selectedSubSetting = AccountSubSetting.PRIV_PERMS }
                            CompactAccountTile("Blocked List", "Restricted users", Icons.Rounded.Block, Color(0xFFFFC107)) { selectedSubSetting = AccountSubSetting.PRIV_BLOCKED }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    CriticalAccountActions(
                        onDelete = { viewModel.deleteAccount() },
                        onLogout = { viewModel.logout() }
                    )
                }
            } else {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    AccountSubSettingDetail(subSetting, viewModel, onSubscriptionPlansClick)
                }
            }
        }
    }
}

@Composable
fun AccountSubSettingDetail(
    setting: AccountSubSetting,
    viewModel: SettingsViewModel,
    onSubscriptionPlansClick: () -> Unit
) {
    val profile by viewModel.currentUserProfile.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        CompactSubSettingHeader(setting)
        Spacer(modifier = Modifier.height(24.dp))

        when (setting) {
            AccountSubSetting.ID_USER -> UserHandleView()
            AccountSubSetting.ID_NAME -> NameView()
            AccountSubSetting.ID_BIO -> BioView()
            AccountSubSetting.ID_QR -> QrCodeView()
            AccountSubSetting.SEC_PASS -> PasswordView()
            AccountSubSetting.ID_2FA -> TwoFactorView()
            AccountSubSetting.SEC_AEK -> MasterKeyView()
            AccountSubSetting.SEC_SESSIONS -> SessionsView()
            AccountSubSetting.SEC_LOG -> ActivityLogView()
            AccountSubSetting.SEC_RECOVERY -> RecoveryView()
            AccountSubSetting.MOD_EMAIL -> EmailView()
            AccountSubSetting.MOD_SMS -> SmsView()
            AccountSubSetting.MOD_VOIP -> VoipView()
            AccountSubSetting.MOD_WALLET -> WalletView()
            AccountSubSetting.MOD_CALENDAR -> CalendarView()
            AccountSubSetting.CLOUD_VAULT -> VaultView()
            AccountSubSetting.CLOUD_NOTES -> NotesView()
            AccountSubSetting.CLOUD_BACKUP -> BackupView()
            AccountSubSetting.CLOUD_CACHE -> CacheView()
            AccountSubSetting.PRIV_PERMS -> PermsView()
            AccountSubSetting.PRIV_BLOCKED -> BlockedView()
            AccountSubSetting.SUB_STORAGE -> SubscriptionView(profile, onSubscriptionPlansClick)
        }
    }
}

@Composable
fun CompactSubSettingHeader(setting: AccountSubSetting) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(getIconForSubSetting(setting), null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(setting.toDisplayName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Professional Settings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- COMPACT SUB-VIEWS ---

@Composable fun UserHandleView() { 
    Column {
        PremiumTextField("@m_abidi", "Username", Icons.Rounded.AlternateEmail)
        StatusIndicator("Username available", true)
        Spacer(modifier = Modifier.height(24.dp))
        InfoCard("Handles are unique and used for cross-network identification.")
    }
}
@Composable fun NameView() { PremiumTextField("Mohamed Abidi", "Display Name", Icons.Rounded.Badge) }
@Composable fun BioView() { PremiumTextField("Android Dev | Security Expert", "Bio", Icons.Rounded.Info, false, 3) }
@Composable fun QrCodeView() { 
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(modifier = Modifier.size(200.dp), shape = RoundedCornerShape(24.dp), color = Color.White, tonalElevation = 8.dp) {
            Box(contentAlignment = Alignment.Center) { Text("QR CODE MOCKUP", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Your personal QR code for quick peer sharing.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}
@Composable fun PasswordView() { 
    Column {
        SecureField("Current Password")
        Spacer(modifier = Modifier.height(12.dp))
        SecureField("New Password")
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(progress = { 0.8f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Color(0xFF2ECC71))
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable fun TwoFactorView() { 
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard("Secure your account with multi-factor authentication methods.")
        
        MfaMethodItem(
            title = "Email Link",
            subtitle = "Verify via secure login link sent to your mail",
            icon = Icons.Rounded.Email,
            active = true
        )
        
        MfaMethodItem(
            title = "SMS OTP",
            subtitle = "6-digit verification code",
            icon = Icons.Rounded.Sms,
            active = false
        )
        
        MfaMethodItem(
            title = "Passkeys",
            subtitle = "Biometric or hardware keys",
            icon = Icons.Rounded.Key,
            active = true,
            onRevoke = {}
        )
    }
}

@Composable
fun MfaMethodItem(title: String, subtitle: String, icon: ImageVector, active: Boolean, onRevoke: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, if (active) Color(0xFF2ECC71).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (active) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRevoke != null && active) {
                IconButton(onClick = onRevoke) {
                    Icon(Icons.Rounded.Cancel, "Revoke", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            } else {
                Switch(checked = active, onCheckedChange = {}, modifier = Modifier.scale(0.7f))
            }
        }
    }
}

@Composable fun MasterKeyView() {
    Column {
        PremiumStatusBox("Key Hardware Backed", "Android StrongBox Secure", true)
        Spacer(modifier = Modifier.height(16.dp))
        PremiumActionCard("Rotate Master Key", "Full re-encryption", Icons.Rounded.RotateRight) {
            // TODO: Implementation for key rotation
        }
        InfoCard("The Account Encryption Key (AEK) never leaves your device.")
    }
}
@Composable fun SessionsView() { 
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SessionItem("Pixel 8 Pro", "Tunis, TN", true)
        SessionItem("Desktop App", "London, UK", false)
        Button({}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Logout All Other") }
    }
}
@Composable fun ActivityLogView() { 
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LogItem("Login", "Tunis, TN", "2 mins ago")
        LogItem("Vault Access", "Pixel 8 Pro", "1 hour ago")
        LogItem("Key Rotated", "System", "3 days ago")
    }
}
@Composable fun RecoveryView() { 
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard("Manage your backup recovery methods. At least one verified email is required.")
        
        RecoveryManageItem("m.abidi@keeftalk.com", "Primary Email", verified = true, canDelete = false)
        RecoveryManageItem("backup@work.com", "Secondary Email", verified = false, canDelete = true)
        RecoveryManageItem("+216 22 123 456", "Recovery Phone", verified = true, canDelete = true)
        
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Recovery Method")
        }
    }
}

@Composable
fun RecoveryManageItem(value: String, type: String, verified: Boolean, canDelete: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!verified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unconfirmed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (verified) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(18.dp))
            } else {
                TextButton(onClick = {}) { Text("Confirm", style = MaterialTheme.typography.labelSmall) }
            }
            if (canDelete) {
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable fun EmailView() { 
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IntegrationCard("Gmail", "m.abidi@gmail.com", "Synced")
        Button({}, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Add Account") }
    }
}
@Composable fun SmsView() { PremiumToggleItem("SMS Gateway", "Relay carrier messages", true) }
@Composable fun VoipView() { PremiumToggleItem("Ultra-Secure VoIP", "Force WebRTC E2EE", true) }
@Composable fun WalletView() { StatusCard("Digital Wallet: SOON", Color.Gray, Icons.Rounded.AccountBalanceWallet) }
@Composable fun CalendarView() { PremiumToggleItem("Calendar Sync", "Active", true) }
@Composable fun VaultView() { 
    Column {
        StorageVisualizer(4.2f, 10f)
        Spacer(modifier = Modifier.height(24.dp))
        PremiumToggleItem("Zero-Knowledge", "Always Active", true)
    }
}
@Composable fun NotesView() { PremiumToggleItem("Notes Cloud Sync", "Real-time", true) }
@Composable fun BackupView() { SyncStatusItem("Full Backup", "1.8 GB", "12h ago") }
@Composable fun CacheView() { 
    Column {
        StorageVisualizer(0.45f, 1f)
        Spacer(modifier = Modifier.height(24.dp))
        Button({}, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Clear Cache") }
    }
}
@Composable fun PermsView() { 
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionItem("Camera", true)
        PermissionItem("Microphone", true)
        PermissionItem("Location", false)
    }
}
@Composable fun BlockedView() { BlockedItem("Spam User", "@spambot_1") }

@Composable
fun SubscriptionView(profile: com.keeftalk.chat.domain.model.Profile?, onUpgradeClick: () -> Unit) {
    val plan = profile?.planType ?: com.keeftalk.chat.domain.model.SubscriptionPlan.FREE
    val used = profile?.storageUsed ?: 0L
    val limit = profile?.storageLimit ?: (5L * 1024 * 1024 * 1024)
    val usedGb = used.toFloat() / (1024 * 1024 * 1024)
    val limitGb = limit.toFloat() / (1024 * 1024 * 1024)
    val isOverQuota = used > limit

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = if (isOverQuota) "Your storage is full! Please upgrade or delete files to keep syncing." 
                   else "Keeftalk is free forever. Pay only when you need more space and power",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        StoragePulseCard(used = usedGb, total = limitGb, isOverQuota = isOverQuota)
        
        MembershipStatusCard(
            planName = "Keeftalk ${plan.name.split("_")[0].lowercase().replaceFirstChar { it.uppercase() }}", 
            active = plan != com.keeftalk.chat.domain.model.SubscriptionPlan.FREE,
            onClick = onUpgradeClick
        )
        
        AccountControlSection(title = "${plan.name.split("_")[0].lowercase().replaceFirstChar { it.uppercase() }} Advantages") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdvantageItem("End-to-End Encryption (E2EE)")
                AdvantageItem("Hardware-Backed Account Keys")
                AdvantageItem("${if (limitGb >= 1000) (limitGb/1024).toInt().toString() + "TB" else limitGb.toInt().toString() + "GB"} High-Speed Cloud Storage")
                AdvantageItem("Unlimited Module Sync (Vault, Notes)")
                AdvantageItem("Priority Community Support")
                if (plan != com.keeftalk.chat.domain.model.SubscriptionPlan.FREE) {
                    AdvantageItem("Original Quality Media")
                    AdvantageItem("Larger File Sharing")
                }
            }
        }
        
        if (plan != com.keeftalk.chat.domain.model.SubscriptionPlan.PRO_MONTHLY && plan != com.keeftalk.chat.domain.model.SubscriptionPlan.FAMILY_MONTHLY) {
            AccountControlSection(title = "Special Offers") {
                PremiumActionCard("Upgrade Plan", "Get more storage and features", Icons.Rounded.WorkspacePremium) {
                    onUpgradeClick()
                }
                Spacer(modifier = Modifier.height(8.dp))
                PremiumActionCard("Annual Billing Save 20%", "Locked-in pricing", Icons.Rounded.Savings) {
                    onUpgradeClick()
                }
            }
        }
    }
}

@Composable
fun StoragePulseCard(used: Float, total: Float, isOverQuota: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, if (isOverQuota) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Global Storage Pulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("Aggregate across all modules", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background((if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CloudQueue, null, tint = if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            StorageVisualizer(used = used, total = total, isError = isOverQuota)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(used / total * 100).toInt()}% consumed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isOverQuota) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text("${if (total >= 1000) (total).toInt().toString() + "GB" else total.toInt().toString() + "GB"} total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MembershipStatusCard(planName: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Verified, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(planName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(if (active) "Subscription Active" else "No active membership", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
        }
    }
}

@Composable
fun AdvantageItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(Icons.Rounded.Check, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- REUSABLE COMPONENTS ---

@Composable fun CompactAccountDashboardHeader() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ManageAccounts, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Account Center", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Security & Identity Dashboard", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
@Composable fun AccountControlSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp), letterSpacing = 1.sp)
        content()
    }
}
@Composable fun ControlTileGrid(content: @Composable () -> Unit) { Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } }
@Composable fun CompactAccountTile(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
        }
    }
}
@Composable fun InfoCard(text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
@Composable fun PremiumTextField(value: String, label: String, icon: ImageVector, singleLine: Boolean = true, minLines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }, singleLine = singleLine, minLines = minLines)
}
@Composable fun StatusIndicator(text: String, success: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Icon(if (success) Icons.Rounded.CheckCircle else Icons.Rounded.Error, null, tint = if (success) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = if (success) Color(0xFF2ECC71) else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
    }
}
@Composable fun SecureField(label: String) {
    OutlinedTextField(value = "••••••••", onValueChange = {}, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(20.dp)) })
}
@Composable fun StatusCard(title: String, color: Color, icon: ImageVector) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = color.copy(alpha = 0.1f), border = BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(12.dp)); Text(title, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}
@Composable fun PremiumToggleItem(title: String, subtitle: String, checked: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = checked, onCheckedChange = {}, modifier = Modifier.scale(0.8f))
        }
    }
}
@Composable fun PremiumActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
            Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}
@Composable fun PremiumStatusBox(title: String, subtitle: String, active: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = if (active) Color(0xFF2ECC71).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, if (active) Color(0xFF2ECC71).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold, color = if (active) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium); Text(subtitle, style = MaterialTheme.typography.labelSmall) }
    }
}
@Composable fun SessionItem(device: String, location: String, isCurrent: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (device.contains("Pro")) Icons.Rounded.LaptopMac else Icons.Rounded.Smartphone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(device, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (isCurrent) Text("Current", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            else IconButton({}) { Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
        }
    }
}
@Composable fun LogItem(action: String, detail: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text("$detail • $time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
    }
}
@Composable fun IntegrationCard(name: String, account: String, status: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text(name.take(1), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
            Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(account, style = MaterialTheme.typography.labelSmall) }
            Text(status, color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}
@Composable fun SyncStatusItem(title: String, size: String, lastSync: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text("$size • $lastSync", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Rounded.Sync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}
@Composable fun StorageVisualizer(used: Float, total: Float, isError: Boolean = false) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Storage: ${"%.2f".format(used)}GB / ${"%.0f".format(total)}GB", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall) }
        Spacer(modifier = Modifier.height(8.dp)); Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) { Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((used / total).coerceIn(0f, 1f)).background(if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) }
    }
}
@Composable fun PermissionItem(name: String, granted: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium); Switch(checked = granted, onCheckedChange = {}, modifier = Modifier.graphicsLayer { scaleX = 0.7f; scaleY = 0.7f }) }
}
@Composable fun BlockedItem(name: String, identifier: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) { Text(name.take(1), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
            Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); Text(identifier, style = MaterialTheme.typography.labelSmall) }
            TextButton({}) { Text("Unblock", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
@Composable fun CriticalAccountActions(onDelete: () -> Unit, onLogout: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Identity Deactivation") },
            text = {
                Column {
                    Text("This process requires email confirmation and involves a 30-day retention period.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Note: Logging back in during this 30-day period will immediately cancel the deactivation process and reset the timer.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Start Process", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(10.dp)); Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium) } }
        Surface(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.error) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.DeleteForever, null, tint = Color.White, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(12.dp)); Column { Text("Deactivate Identity", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium); Text("Permanent deletion", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall) } } }
    }
}

fun AccountSubSetting.toDisplayName(): String = name.split("_").drop(1).joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
fun getIconForSubSetting(setting: AccountSubSetting): ImageVector = when (setting) {
    AccountSubSetting.ID_USER -> Icons.Rounded.AlternateEmail
    AccountSubSetting.ID_NAME -> Icons.Rounded.Badge
    AccountSubSetting.ID_BIO -> Icons.Rounded.Info
    AccountSubSetting.ID_QR -> Icons.Rounded.QrCode2
    AccountSubSetting.SEC_PASS -> Icons.Rounded.Password
    AccountSubSetting.ID_2FA -> Icons.Rounded.VpnKey
    AccountSubSetting.SEC_AEK -> Icons.Rounded.Security
    AccountSubSetting.SEC_SESSIONS -> Icons.Rounded.Devices
    AccountSubSetting.SEC_LOG -> Icons.Rounded.HistoryEdu
    AccountSubSetting.SEC_RECOVERY -> Icons.Rounded.HealthAndSafety
    AccountSubSetting.MOD_EMAIL -> Icons.Rounded.Email
    AccountSubSetting.MOD_SMS -> Icons.Rounded.Sms
    AccountSubSetting.MOD_VOIP -> Icons.Rounded.Call
    AccountSubSetting.MOD_WALLET -> Icons.Rounded.AccountBalanceWallet
    AccountSubSetting.MOD_CALENDAR -> Icons.Rounded.CalendarMonth
    AccountSubSetting.CLOUD_VAULT -> Icons.Rounded.Lock
    AccountSubSetting.CLOUD_NOTES -> Icons.Rounded.EditNote
    AccountSubSetting.CLOUD_BACKUP -> Icons.Rounded.CloudSync
    AccountSubSetting.CLOUD_CACHE -> Icons.Rounded.Storage
    AccountSubSetting.PRIV_PERMS -> Icons.Rounded.Rule
    AccountSubSetting.PRIV_BLOCKED -> Icons.Rounded.Block
    AccountSubSetting.SUB_STORAGE -> Icons.Rounded.Storage
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)
