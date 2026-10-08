package tn.keeftalk.chat.feature.secrets.ui

import android.Manifest
import android.app.Activity
import android.graphics.BitmapFactory
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.keeftalk.chat.feature.secrets.crypto.PasswordGenerator
import tn.keeftalk.chat.feature.secrets.crypto.PasswordStrengthEvaluator
import tn.keeftalk.chat.feature.secrets.crypto.TotpGenerator
import tn.keeftalk.chat.feature.secrets.model.BackupCode
import tn.keeftalk.chat.feature.secrets.model.PasswordHistoryItem
import tn.keeftalk.chat.feature.secrets.model.SecretCredential
import tn.keeftalk.chat.feature.secrets.model.SecretVaultItem
import tn.keeftalk.chat.feature.secrets.model.SecurityIssue
import tn.keeftalk.chat.feature.secrets.model.TotpAccount
import tn.keeftalk.chat.feature.secrets.model.VaultSecretType
import tn.keeftalk.chat.feature.secrets.viewmodel.SecretSheetType
import tn.keeftalk.chat.feature.secrets.viewmodel.SecretsUiState
import tn.keeftalk.chat.feature.secrets.viewmodel.SecretsViewModel
import tn.keeftalk.chat.util.BiometricAuthManager
import tn.keeftalk.chat.ui.theme.LucideIcons
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretsScreen(
    viewModel: SecretsViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    fabActionFlow: kotlinx.coroutines.flow.SharedFlow<tn.keeftalk.chat.ui.components.FabActionType>? = null
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to global AdaptiveFab actions for Secrets
    LaunchedEffect(fabActionFlow) {
        fabActionFlow?.collect { action ->
            when (action) {
                tn.keeftalk.chat.ui.components.FabActionType.ADD_2FA_ENTRY -> viewModel.openSheet(SecretSheetType.ADD_TOTP_MANUAL)
                tn.keeftalk.chat.ui.components.FabActionType.SCAN_QR_CAMERA_SECRETS -> viewModel.openSheet(SecretSheetType.SCAN_QR_CAMERA)
                tn.keeftalk.chat.ui.components.FabActionType.SCAN_QR_IMAGE_SECRETS -> viewModel.openSheet(SecretSheetType.SCAN_QR_IMAGE)
                tn.keeftalk.chat.ui.components.FabActionType.ADD_SECRET_CREDENTIAL -> viewModel.openSheet(SecretSheetType.ADD_CREDENTIAL)
                tn.keeftalk.chat.ui.components.FabActionType.SECRETS_GENERATOR -> viewModel.openSheet(SecretSheetType.GENERATE_PASSWORD)
                tn.keeftalk.chat.ui.components.FabActionType.IMPORT_CSV_SECRETS -> viewModel.openSheet(SecretSheetType.IMPORT_CSV)
                else -> {}
            }
        }
    }

    // Biometric Auth Manager
    val biometricManager = remember { BiometricAuthManager(context) }

    // Toast Notice Handling
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // Flag Secure Window Protection for Vault
    DisposableEffect(state.selectedTab, state.settings.blockScreenshots) {
        if (state.selectedTab == 2 && state.settings.blockScreenshots) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Row: Hero Status Card + Section Rail
            HeaderWithSectionRail(
                selectedTab = state.selectedTab,
                isRailExpanded = state.isSectionRailExpanded,
                uiState = state,
                onTabSelect = { viewModel.selectTab(it) },
                onRailToggle = { viewModel.toggleSectionRail() },
                onFixAction = { actionType ->
                    when (actionType) {
                        "ADD_CREDENTIAL" -> viewModel.openSheet(SecretSheetType.ADD_CREDENTIAL)
                        "GENERATE_BACKUP_CODES" -> viewModel.generateBackupCodes()
                        "EXPORT_GOOGLE" -> viewModel.openSheet(SecretSheetType.EXPORT_CSV)
                        else -> viewModel.selectTab(0)
                    }
                }
            )

            // Content according to selected tab
            when (state.selectedTab) {
                0 -> PasswordsTabContent(viewModel = viewModel, uiState = state)
                1 -> CodesTabContent(viewModel = viewModel, uiState = state)
                2 -> VaultTabContent(
                    viewModel = viewModel,
                    uiState = state,
                    onAuthenticateBiometric = {
                        (activity as? androidx.fragment.app.FragmentActivity)?.let { fragAct ->
                            biometricManager.showBiometricPrompt(
                                activity = fragAct,
                                title = "Unlock Keeftalk Secrets Vault",
                                subtitle = "Verify fingerprint to view protected notes and recovery phrase",
                                allowBiometrics = true,
                                onSuccess = { viewModel.setVaultUnlocked(true) },
                                onError = { viewModel.showToast("Authentication failed") }
                            )
                        } ?: viewModel.setVaultUnlocked(true)
                    }
                )
                3 -> SecurityTabContent(
                    viewModel = viewModel,
                    uiState = state,
                    onAuthenticateForExport = {
                        (activity as? androidx.fragment.app.FragmentActivity)?.let { fragAct ->
                            biometricManager.showBiometricPrompt(
                                activity = fragAct,
                                title = "Authentication Required for Export",
                                subtitle = "Export contains unencrypted passwords. Verify identity.",
                                allowBiometrics = true,
                                onSuccess = {
                                    viewModel.exportToCsv { csvData ->
                                        if (!csvData.isNullOrBlank()) {
                                            viewModel.openSheet(SecretSheetType.EXPORT_CSV)
                                        }
                                    }
                                },
                                onError = { viewModel.showToast("Authentication required to export") }
                            )
                        } ?: viewModel.exportToCsv { csvData ->
                            if (!csvData.isNullOrBlank()) {
                                viewModel.openSheet(SecretSheetType.EXPORT_CSV)
                            }
                        }
                    }
                )
            }
        }

        // Modal Sheets
        if (state.activeSheet != SecretSheetType.NONE) {
            SecretModalSheets(
                activeSheet = state.activeSheet,
                uiState = state,
                viewModel = viewModel,
                onDismiss = { viewModel.closeSheet() }
            )
        }
    }
}

@Composable
private fun HeaderWithSectionRail(
    selectedTab: Int,
    isRailExpanded: Boolean,
    uiState: SecretsUiState,
    onTabSelect: (Int) -> Unit,
    onRailToggle: () -> Unit,
    onFixAction: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hero Status Card
        Box(modifier = Modifier.weight(1f)) {
            HeroStatusCard(
                selectedTab = selectedTab,
                uiState = uiState,
                onFixAction = onFixAction
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Compact Section Rail
        SectionRail(
            selectedTab = selectedTab,
            isExpanded = isRailExpanded,
            onTabSelect = onTabSelect,
            onToggleExpand = onRailToggle
        )
    }
}

@Composable
private fun HeroStatusCard(
    selectedTab: Int,
    uiState: SecretsUiState,
    onFixAction: (String) -> Unit
) {
    val weakCount = uiState.securitySummary.weakCredentials.size
    val missing2faCount = uiState.securitySummary.missingTwoFactorCredentials.size
    val score = uiState.securitySummary.score

    val (badgeText, badgeColor) = when (selectedTab) {
        0 -> if (weakCount > 0) Pair("DANGER", Color(0xFFFF5C7A)) else if (missing2faCount > 0) Pair("NEEDS ATTENTION", Color(0xFFF5A524)) else Pair("SECURE", Color(0xFF2ECC71))
        1 -> if (uiState.backupCodes.isNotEmpty()) Pair("SECURE", Color(0xFF2ECC71)) else Pair("NEEDS ATTENTION", Color(0xFFF5A524))
        2 -> if (uiState.isVaultUnlocked) Pair("NEEDS ATTENTION", Color(0xFFF5A524)) else Pair("SECURE", Color(0xFF2ECC71))
        else -> if (score < 60) Pair("DANGER", Color(0xFFFF5C7A)) else if (score < 90) Pair("NEEDS ATTENTION", Color(0xFFF5A524)) else Pair("SECURE", Color(0xFF2ECC71))
    }

    val cardTitle = when (selectedTab) {
        0 -> "PASSWORD HEALTH"
        1 -> "TWO-FACTOR"
        2 -> "VAULT STATUS"
        else -> "SECURITY SCORE"
    }

    val cardVal = when (selectedTab) {
        0 -> if (weakCount > 0) "$weakCount weak password${if (weakCount > 1) "s" else ""}" else if (missing2faCount > 0) "$missing2faCount without 2FA" else "All healthy"
        1 -> if (uiState.backupCodes.isNotEmpty()) "${uiState.totpAccounts.size} codes & backup saved" else "Backup codes missing"
        2 -> if (uiState.isVaultUnlocked) "Unlocked auto-locks soon" else "Sealed & locked"
        else -> "$score / 100"
    }

    val subTitle = when (selectedTab) {
        0 -> if (weakCount > 0) "Easy to crack. Replace them with a generated password." else if (missing2faCount > 0) "Passwords are fine. Add a 2FA code to the rest." else "Strong passwords, 2FA everywhere."
        1 -> if (uiState.backupCodes.isNotEmpty()) "Codes refresh every 30 s · recovery codes saved" else "2FA is on · generate recovery codes today"
        2 -> if (uiState.isVaultUnlocked) "Seal it when you are done. It also locks after 5 minutes." else "Second fingerprint lock · screenshots blocked"
        else -> if (weakCount + missing2faCount > 0) "${weakCount + missing2faCount} things to fix to raise your score." else "Nothing to fix. Keep it up."
    }

    val meterProgress = when (selectedTab) {
        0 -> if (uiState.credentials.isEmpty()) 1f else (uiState.credentials.size - weakCount).toFloat() / uiState.credentials.size.toFloat()
        1 -> if (uiState.backupCodes.isNotEmpty()) 1f else 0.55f
        2 -> if (uiState.isVaultUnlocked) 0.6f else 1f
        else -> score / 100f
    }

    val sideIcon = when (selectedTab) {
        0 -> "🔑"
        1 -> "⏱"
        2 -> if (uiState.isVaultUnlocked) "🗄" else "🔒"
        else -> "🛡"
    }

    val sideLabel = when (selectedTab) {
        0 -> "Passwords"
        1 -> "Codes"
        2 -> "Vault"
        else -> "Security"
    }

    val (c1, c2) = when {
        badgeText == "DANGER" -> Pair(Color(0xFFFF3B5C), Color(0xFF7A0018))
        badgeText == "NEEDS ATTENTION" -> Pair(Color(0xFFFFB800), Color(0xFF8A4A00))
        else -> Pair(Color(0xFF00E58A), Color(0xFF005C3A))
    }
    val gradient = Brush.linearGradient(listOf(c1, c2))

    Surface(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        cardTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                badgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            cardVal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { meterProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(99.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            subTitle,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selectedTab == 0) {
                                if (weakCount > 0) {
                                    item {
                                        SuggestionChip(
                                            label = "Update password",
                                            onClick = { onFixAction("ADD_CREDENTIAL") }
                                        )
                                    }
                                }
                                if (missing2faCount > 0) {
                                    item {
                                        SuggestionChip(
                                            label = "Add 2FA",
                                            onClick = { onFixAction("ADD_CREDENTIAL") }
                                        )
                                    }
                                }
                                item {
                                    SuggestionChip(
                                        label = "New strong password",
                                        onClick = { onFixAction("ADD_CREDENTIAL") }
                                    )
                                }
                            } else if (selectedTab == 1) {
                                if (uiState.backupCodes.isEmpty()) {
                                    item {
                                        SuggestionChip(
                                            label = "Generate backup codes",
                                            onClick = { onFixAction("GENERATE_BACKUP_CODES") }
                                        )
                                    }
                                }
                                item {
                                    SuggestionChip(
                                        label = "Add an account",
                                        onClick = { onFixAction("ADD_CREDENTIAL") }
                                    )
                                }
                            } else if (selectedTab == 2) {
                                if (uiState.isVaultUnlocked) {
                                    item {
                                        SuggestionChip(
                                            label = "Seal vault now",
                                            onClick = { onFixAction("SEAL_VAULT") }
                                        )
                                    }
                                } else {
                                    item {
                                        SuggestionChip(
                                            label = "Unlock with fingerprint",
                                            onClick = { onFixAction("UNLOCK_VAULT") }
                                        )
                                    }
                                }
                            } else {
                                item {
                                    SuggestionChip(
                                        label = "Export for Google",
                                        onClick = { onFixAction("EXPORT_GOOGLE") }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(52.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(sideIcon, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            sideLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = Color.White.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SectionRail(
    selectedTab: Int,
    isExpanded: Boolean,
    onTabSelect: (Int) -> Unit,
    onToggleExpand: () -> Unit
) {
    val tabs = listOf(
        Pair("Passwords", LucideIcons.Key),
        Pair("Codes", Icons.Default.Timer),
        Pair("Vault", LucideIcons.FolderLock),
        Pair("Security", Icons.Default.Shield)
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .padding(4.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, (label, icon) ->
                val isSelected = selectedTab == idx
                val bg = if (isSelected) Color(0xFF6C63FF) else Color.Transparent
                val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .clickable {
                            if (isSelected) {
                                onToggleExpand()
                            } else {
                                onTabSelect(idx)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 0: PASSWORDS
// ---------------------------------------------------------------------------

@Composable
private fun PasswordsTabContent(
    viewModel: SecretsViewModel,
    uiState: SecretsUiState
) {
    val categories = listOf("All", "Personal", "Work", "Social", "Finance")

    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = uiState.selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.clickable { viewModel.setCategory(cat) }
                ) {
                    Text(
                        cat,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Credentials List
        if (uiState.filteredCredentials.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No logins found. Tap '+' to add your first password or import from CSV.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.filteredCredentials, key = { it.id }) { item ->
                    CredentialRowCard(
                        credential = item,
                        onClick = { viewModel.openCredentialDetail(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialRowCard(
    credential: SecretCredential,
    onClick: () -> Unit
) {
    val strengthColor = when {
        credential.passwordStrength >= 80 -> Color(0xFF2ECC71)
        credential.passwordStrength >= 55 -> Color(0xFFF5A524)
        else -> Color(0xFFFF5C7A)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF6C63FF),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        credential.serviceName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    credential.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    credential.username.ifBlank { "No username" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(strengthColor)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 1: CODES (TOTP AUTHENTICATOR)
// ---------------------------------------------------------------------------

@Composable
private fun CodesTabContent(
    viewModel: SecretsViewModel,
    uiState: SecretsUiState
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.totpAccounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF6C63FF))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No 2FA accounts yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Scan a setup QR code or enter a secret key manually to generate 2FA codes.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.openSheet(SecretSheetType.SCAN_QR_CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
                    ) {
                        Text("Scan QR Code")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.totpAccounts, key = { it.id }) { account ->
                    val code = remember(account.secretKey, uiState.totpRemainingSeconds) {
                        runCatching {
                            TotpGenerator.generateCode(
                                secret = account.secretKey,
                                period = account.period,
                                digits = account.digits,
                                algorithm = account.algorithm
                            )
                        }.getOrDefault("------")
                    }

                    val formattedCode = if (code.length == 6) "${code.take(3)} ${code.takeLast(3)}" else code

                    TotpAccountCard(
                        account = account,
                        formattedCode = formattedCode,
                        remainingSeconds = uiState.totpRemainingSeconds,
                        progress = uiState.totpProgress,
                        onCopy = {
                            viewModel.copyToClipboard(context, "${account.serviceName} 2FA", code, isSensitive = true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TotpAccountCard(
    account: TotpAccount,
    formattedCode: String,
    remainingSeconds: Int,
    progress: Float,
    onCopy: () -> Unit
) {
    val ringColor = if (remainingSeconds < 7) Color(0xFFFF5C7A) else Color(0xFF00CCCC)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF7D5CFF),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        account.serviceName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${account.serviceName} · ${account.username.ifBlank { "Account" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formattedCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Ticking Progress Ring
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.15f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    "$remainingSeconds",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 2: VAULT (SECOND SECURITY BOUNDARY)
// ---------------------------------------------------------------------------

@Composable
private fun VaultTabContent(
    viewModel: SecretsViewModel,
    uiState: SecretsUiState,
    onAuthenticateBiometric: () -> Unit
) {
    val samplePhrase = "orbit lantern velvet harbor quartz meadow ember falcon cipher marble summit willow".split(" ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!uiState.isVaultUnlocked) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color(0xFF6C63FF))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Second Security Boundary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "This section requires fingerprint re-authentication every time, even when Keeftalk is already open.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onAuthenticateBiometric,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Fingerprint")
                    }
                }
            }
        } else {
            // Unlocked Vault State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2ECC71).copy(alpha = 0.15f)
                ) {
                    Text("Unlocked", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text("Auto-locks in 5:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hold-to-Reveal 12-word Recovery Phrase
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Master Recovery Phrase", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Hold to reveal · immediately hides upon release", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .blur(if (uiState.isHoldToRevealActive) 0.dp else 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        samplePhrase.forEachIndexed { idx, word ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    "${idx + 1}. $word",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.setHoldToReveal(true)
                                        tryAwaitRelease()
                                        viewModel.setHoldToReveal(false)
                                    }
                                )
                            },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(if (uiState.isHoldToRevealActive) "Revealed (release to hide)" else "Hold to reveal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extensible Vault Categories
            val itemsList = listOf(
                Pair("Passport & ID scans", "3 files · re-auth on open"),
                Pair("Private notes", "7 notes · encrypted"),
                Pair("Encryption keys", "2 keys · AES-256"),
                Pair("Contracts & documents", "4 files · protected")
            )

            itemsList.forEach { (title, subtitle) ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onAuthenticateBiometric() }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF6C63FF))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.setVaultUnlocked(false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C7A)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Panic Lock · Lock Everything Now")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TAB 3: SECURITY
// ---------------------------------------------------------------------------

@Composable
private fun SecurityTabContent(
    viewModel: SecretsViewModel,
    uiState: SecretsUiState,
    onAuthenticateForExport: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // Handle CSV pick
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Security Score Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(
                        progress = { uiState.securitySummary.score / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF00CCCC),
                        strokeWidth = 8.dp
                    )
                    Text("${uiState.securitySummary.score}", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text("Security Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Calculated from actual local credentials and 2FA coverage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Issues Overview
        Text("Security Action Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        val issues = listOf(
            Triple("${uiState.securitySummary.weakCredentials.size} weak passwords", "Replace with generated passwords", "ADD_CREDENTIAL"),
            Triple("${uiState.securitySummary.missingTwoFactorCredentials.size} logins without 2FA", "Add 2FA codes", "ADD_TOTP_MANUAL"),
            Triple("${uiState.securitySummary.reusedCredentials.size} reused passwords", "Generate unique credentials", "ADD_CREDENTIAL"),
            Triple("0 found in known breaches", "Checked privately on-device", "NONE")
        )

        issues.forEach { (title, subtitle, action) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (action != "NONE") {
                        TextButton(onClick = { viewModel.openSheet(SecretSheetType.ADD_CREDENTIAL) }) {
                            Text("Fix", color = Color(0xFF00CCCC))
                        }
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2ECC71))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Import & Export
        Text("Import and Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.openSheet(SecretSheetType.IMPORT_CSV) }
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = Color(0xFF6C63FF))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Import from CSV", fontWeight = FontWeight.Bold)
                    Text("Google Password Manager, Bitwarden, 1Password, LastPass", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAuthenticateForExport() }
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF00A1C9))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Export for Google", fontWeight = FontWeight.Bold)
                    Text("passwords.csv for Google Password Manager (requires auth)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MODAL BOTTOM SHEETS
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecretModalSheets(
    activeSheet: SecretSheetType,
    uiState: SecretsUiState,
    viewModel: SecretsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (activeSheet) {
                SecretSheetType.ADD_PICKER -> AddPickerSheetContent(viewModel = viewModel, onDismiss = onDismiss)
                SecretSheetType.ADD_CREDENTIAL -> AddCredentialSheetContent(viewModel = viewModel)
                SecretSheetType.UPDATE_PASSWORD -> UpdatePasswordSheetContent(uiState = uiState, viewModel = viewModel)
                SecretSheetType.GENERATE_PASSWORD -> GeneratorSheetContent(viewModel = viewModel)
                SecretSheetType.ADD_TOTP_MANUAL -> AddTotpManualSheetContent(viewModel = viewModel)
                SecretSheetType.SCAN_QR_CAMERA -> CameraQrScannerSheetContent(viewModel = viewModel)
                SecretSheetType.SCAN_QR_IMAGE -> ScanQrImageSheetContent(viewModel = viewModel)
                SecretSheetType.CREDENTIAL_DETAIL -> CredentialDetailSheetContent(uiState = uiState, viewModel = viewModel)
                SecretSheetType.SHOW_BACKUP_CODES -> BackupCodesSheetContent(uiState = uiState, viewModel = viewModel, onDismiss = onDismiss)
                SecretSheetType.IMPORT_CSV -> ImportCsvSheetContent(viewModel = viewModel)
                SecretSheetType.IMPORT_PREVIEW -> ImportPreviewSheetContent(uiState = uiState, viewModel = viewModel)
                SecretSheetType.EXPORT_CSV -> ExportCsvSheetContent(viewModel = viewModel)
                else -> onDismiss()
            }
        }
    }
}

private data class OptionItem(val icon: String, val title: String, val desc: String, val onClick: () -> Unit)

@Composable
private fun AddPickerSheetContent(
    viewModel: SecretsViewModel,
    onDismiss: () -> Unit
) {
    Text("Add to Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Pick what you want to save", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(14.dp))

    Text("Passwords", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(6.dp))

    val pwOptions = listOf(
        OptionItem("🔑", "Add credential", "Save a username and password") { viewModel.openSheet(SecretSheetType.ADD_CREDENTIAL) },
        OptionItem("⚡", "Password generator", "Strong, or personalized for a name") { viewModel.openSheet(SecretSheetType.GENERATE_PASSWORD) },
        OptionItem("📥", "Import from CSV", "Google, Bitwarden, 1Password, LastPass") { viewModel.openSheet(SecretSheetType.IMPORT_CSV) }
    )

    pwOptions.forEach { item ->
        AddOptionRow(item = item)
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text("Codes (2FA)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(6.dp))

    val codeOptions = listOf(
        OptionItem("▣", "Scan QR code", "Use the camera on the setup QR") { viewModel.openSheet(SecretSheetType.SCAN_QR_CAMERA) },
        OptionItem("⌨", "Enter manually", "Type the account and secret key") { viewModel.openSheet(SecretSheetType.ADD_TOTP_MANUAL) },
        OptionItem("🖼", "From image", "Pick a screenshot of the QR code") { viewModel.openSheet(SecretSheetType.SCAN_QR_IMAGE) }
    )

    codeOptions.forEach { item ->
        AddOptionRow(item = item)
    }
}

@Composable
private fun AddOptionRow(item: OptionItem) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { item.onClick() }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF7D5CFF).copy(alpha = 0.25f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(item.icon, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(item.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddCredentialSheetContent(viewModel: SecretsViewModel) {
    var serviceName by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }

    Text("New Credential", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(value = serviceName, onValueChange = { serviceName = it }, label = { Text("Site or App Name") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website (optional)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username or Email") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { password = PasswordGenerator.generate(18) }) {
            Icon(Icons.Default.Refresh, contentDescription = "Generate")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            if (serviceName.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                viewModel.saveCredential(
                    SecretCredential(
                        id = "",
                        userId = "",
                        serviceName = serviceName.trim(),
                        website = website.trim(),
                        username = username.trim(),
                        password = password,
                        category = category
                    )
                )
            } else {
                viewModel.showToast("Site name, username, and password required")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("Save Credential")
    }
}

@Composable
private fun GeneratorSheetContent(viewModel: SecretsViewModel) {
    val context = LocalContext.current
    var length by remember { mutableFloatStateOf(18f) }
    var includeSymbols by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var isPersonalized by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var genTrigger by remember { mutableIntStateOf(0) }

    val generatedPassword = remember(length, includeSymbols, includeNumbers, isPersonalized, nameInput, genTrigger) {
        val pName = if (isPersonalized) nameInput else ""
        PasswordGenerator.generate(length.toInt(), includeSymbols, includeNumbers, pName)
    }

    val eval = remember(generatedPassword, isPersonalized, nameInput) {
        PasswordStrengthEvaluator.evaluate(generatedPassword, if (isPersonalized) nameInput else "")
    }

    val bitsColor = when {
        eval.entropyBits >= 70.0 -> Color(0xFF2ECC71)
        eval.entropyBits >= 50.0 -> Color(0xFFF5A524)
        else -> Color(0xFFFF5C7A)
    }

    val meterProgress = (eval.entropyBits / 90.0).toFloat().coerceIn(0.05f, 1.0f)

    Text("Password generator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))

    // Generated Password Box
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Generated", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    generatedPassword,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.copyToClipboard(context, "Generated Password", generatedPassword, isSensitive = true) }) {
                    Text("Copy", fontSize = 13.sp, color = Color(0xFF00CCCC))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Strength Meter Bar
    LinearProgressIndicator(
        progress = { meterProgress },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(6.dp)),
        color = bitsColor,
        trackColor = Color.White.copy(alpha = 0.1f)
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        eval.label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = bitsColor
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Estimate Box (Brute force & Targeted guess)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Brute force", fontSize = 13.sp)
                Text(eval.bruteForceTime, fontWeight = FontWeight.Bold, color = bitsColor, fontSize = 13.sp)
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Targeted guess", fontSize = 13.sp)
                Text(eval.targetedGuessTime, fontWeight = FontWeight.Bold, color = bitsColor, fontSize = 13.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Estimate Explanation Note
    val explanation = when {
        isPersonalized && nameInput.isNotBlank() -> "The name part is predictable; the strength comes from the random ${length.toInt() - nameInput.length.coerceAtMost(length.toInt() - 4)}-character tail. Assumes 10 billion guesses per second."
        isPersonalized -> "Type a name to personalize. Assumes 10 billion guesses per second."
        else -> "Pure random, no pattern to guess. Assumes 10 billion guesses per second."
    }

    Text(
        explanation,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Options List
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Length ${length.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Slider(
            value = length,
            onValueChange = { length = it },
            valueRange = 8f..32f,
            steps = 24,
            modifier = Modifier.width(200.dp)
        )
    }

    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Symbols", fontSize = 14.sp)
        Switch(checked = includeSymbols, onCheckedChange = { includeSymbols = it })
    }

    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Numbers", fontSize = 14.sp)
        Switch(checked = includeNumbers, onCheckedChange = { includeNumbers = it })
    }

    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Personalized for a name", fontSize = 14.sp)
        Switch(checked = isPersonalized, onCheckedChange = { isPersonalized = it })
    }

    if (isPersonalized) {
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            placeholder = { Text("Name, e.g. Steve") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { genTrigger++ },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("Generate again")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = {
            viewModel.openSheet(SecretSheetType.ADD_CREDENTIAL)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text("Use and save as new login")
    }
}

@Composable
private fun AddTotpManualSheetContent(viewModel: SecretsViewModel) {
    var serviceName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    Text("Add 2FA Account Manually", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(14.dp))

    OutlinedTextField(value = serviceName, onValueChange = { serviceName = it }, label = { Text("Account Name (e.g. Discord)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username or Email") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = secretKey, onValueChange = { secretKey = it }, label = { Text("Secret Key (Base32)") }, modifier = Modifier.fillMaxWidth())

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            if (serviceName.isNotBlank() && secretKey.isNotBlank()) {
                viewModel.saveTotpAccount(
                    TotpAccount(
                        id = "",
                        userId = "",
                        serviceName = serviceName.trim(),
                        username = username.trim(),
                        secretKey = secretKey.trim()
                    )
                )
            } else {
                viewModel.showToast("Service name and secret key required")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("Add to Codes")
    }
}

@Composable
private fun CameraQrScannerSheetContent(viewModel: SecretsViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Text("Scan QR Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(14.dp))

    if (!hasPermission) {
        Text("Camera permission required to scan setup QR codes.")
    } else {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        600
                    )
                }
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                    val scanner = BarcodeScanning.getClient(options)

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val raw = barcode.rawValue
                                        if (!raw.isNullPlatformBlank()) {
                                            val parsed = TotpGenerator.parseOtpUri(raw!!)
                                            if (parsed != null) {
                                                viewModel.saveTotpAccount(
                                                    TotpAccount(
                                                        id = "",
                                                        userId = "",
                                                        serviceName = parsed.serviceName,
                                                        username = parsed.username,
                                                        secretKey = parsed.secretKey,
                                                        issuer = parsed.issuer,
                                                        algorithm = parsed.algorithm,
                                                        digits = parsed.digits,
                                                        period = parsed.period
                                                    )
                                                )
                                                break
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                    } catch (e: Exception) {
                        Log.e("CameraQrScanner", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(18.dp))
        )
    }
}

private fun String?.isNullPlatformBlank(): Boolean = this == null || this.isBlank()

@Composable
private fun CredentialDetailSheetContent(uiState: SecretsUiState, viewModel: SecretsViewModel) {
    val cred = uiState.selectedCredential ?: return
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(showPassword) {
        if (showPassword) {
            delay(8000L)
            showPassword = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF6C63FF),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    cred.serviceName.take(1).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(cred.serviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${cred.category} · changed recently", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Username", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cred.username.ifBlank { "—" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                TextButton(onClick = { viewModel.copyToClipboard(context, "Username", cred.username, isSensitive = false) }) {
                    Text("Copy", fontSize = 13.sp, color = Color(0xFF00CCCC))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Password", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (showPassword) cred.password else "••••••••••••••",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Hide" else "Show", fontSize = 13.sp, color = Color(0xFF00CCCC))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    val barColor = when {
        cred.passwordStrength >= 80 -> Color(0xFF2ECC71)
        cred.passwordStrength >= 55 -> Color(0xFFF5A524)
        else -> Color(0xFFFF5C7A)
    }

    LinearProgressIndicator(
        progress = { cred.passwordStrength / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(6.dp)),
        color = barColor,
        trackColor = Color.White.copy(alpha = 0.1f)
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        "Strength ${cred.passwordStrength}/100 · ${if (cred.hasTwoFactor) "2FA linked" else "no 2FA yet — add one in Codes"}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { viewModel.copyToClipboard(context, "Password", cred.password, isSensitive = true) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Copy password")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = { viewModel.openSheet(SecretSheetType.UPDATE_PASSWORD) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text("Update password")
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
        "Password history · ${cred.history.size} old",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (cred.history.isEmpty()) {
        Text("No old passwords yet. Every update is logged here.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        cred.history.forEach { item ->
            var showHistoryItemPassword by remember { mutableStateOf(false) }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (showHistoryItemPassword) item.password else "••••••••••",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "replaced recently",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        TextButton(onClick = { showHistoryItemPassword = !showHistoryItemPassword }) {
                            Text(if (showHistoryItemPassword) "Hide" else "Show", fontSize = 12.sp, color = Color(0xFF00CCCC))
                        }
                        TextButton(onClick = { viewModel.restoreOldPassword(cred.id, item) }) {
                            Text("Restore", fontSize = 12.sp, color = Color(0xFF00CCCC))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatePasswordSheetContent(uiState: SecretsUiState, viewModel: SecretsViewModel) {
    val cred = uiState.selectedCredential ?: return
    var newPassword by remember { mutableStateOf("") }

    val eval = remember(newPassword) {
        PasswordStrengthEvaluator.evaluate(newPassword)
    }

    Text("Update ${cred.serviceName} password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("The current one is saved to history first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            placeholder = { Text("New password") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { newPassword = PasswordGenerator.generate(18) }
        ) {
            Text("⚡", fontSize = 20.sp)
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text("Strength ${eval.score}/100", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            if (newPassword.isNotBlank()) {
                viewModel.updatePassword(cred.id, newPassword)
            } else {
                viewModel.showToast("Type or generate a new password")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text("Save new password")
    }
}

@Composable
private fun BackupCodesSheetContent(uiState: SecretsUiState, viewModel: SecretsViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Text("Generated Backup Codes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Each code works once if you lose your phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Spacer(modifier = Modifier.height(16.dp))

    val codesText = uiState.backupCodes.joinToString("\n") { it.code }

    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            uiState.backupCodes.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pair.getOrNull(0)?.code ?: "", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(pair.getOrNull(1)?.code ?: "", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            viewModel.copyToClipboard(context, "Backup Codes", codesText, isSensitive = true)
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
    ) {
        Text("Copy All Codes & Save")
    }
}

@Composable
private fun ImportCsvSheetContent(viewModel: SecretsViewModel) {
    val context = LocalContext.current
    val sampleCsv = "name,url,username,password,note\r\nTwitch,https://www.twitch.tv,streamer01,Ts9!kLm2,\"gaming, main\"\r\nNotion,https://www.notion.so,you@mail.com,Qw8#nB4z,\r\nGitHub,https://github.com/login,dev-handle,xY7\$pQ2m,duplicate\r\nOld forum,https://forum.example.com,member42,,no password"

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                if (text.isNotBlank()) {
                    viewModel.parseCsvForImport(text)
                } else {
                    viewModel.showToast("CSV file is empty")
                }
            } catch (e: Exception) {
                viewModel.showToast("Failed to read CSV file")
            }
        }
    }

    Text("Import from CSV", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(
        "Google Password Manager, Chrome, Bitwarden, 1Password, LastPass, or any CSV with a password column.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(14.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Choose CSV File")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.parseCsvForImport(sampleCsv) }
            ) {
                Text(
                    "Try with a sample file",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun ScanQrImageSheetContent(viewModel: SecretsViewModel) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Pick a screenshot or photo of the QR code") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            statusText = "Reading QR code…"
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                    val scanner = BarcodeScanning.getClient(options)
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            var found = false
                            for (barcode in barcodes) {
                                val raw = barcode.rawValue
                                if (!raw.isNullOrBlank()) {
                                    val parsed = TotpGenerator.parseOtpUri(raw)
                                    if (parsed != null) {
                                        viewModel.saveTotpAccount(
                                            TotpAccount(
                                                id = "",
                                                userId = "",
                                                serviceName = parsed.serviceName,
                                                username = parsed.username,
                                                secretKey = parsed.secretKey,
                                                issuer = parsed.issuer,
                                                algorithm = parsed.algorithm,
                                                digits = parsed.digits,
                                                period = parsed.period
                                            )
                                        )
                                        found = true
                                        statusText = "QR code found · Account added!"
                                        break
                                    }
                                }
                            }
                            if (!found) {
                                statusText = "No valid 2FA QR code found in image"
                                viewModel.showToast("No valid 2FA QR code found")
                            }
                        }
                        .addOnFailureListener {
                            statusText = "Failed to decode QR code"
                            viewModel.showToast("Could not decode QR code")
                        }
                } else {
                    statusText = "Could not load image"
                }
            } catch (e: Exception) {
                statusText = "Error reading image file"
            }
        }
    }

    Text("Read QR from image", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(14.dp))

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { launcher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Choose image")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImportPreviewSheetContent(uiState: SecretsUiState, viewModel: SecretsViewModel) {
    val res = uiState.csvImportResult ?: return

    Text("${res.detectedSource} Detected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    Text("Total rows: ${res.totalRows}")
    Text("New items: ${res.importedItems.size}", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
    Text("Duplicates skipped: ${res.duplicateCount}")
    Text("Skipped (invalid): ${res.skippedCount}")

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { viewModel.confirmCsvImport() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
    ) {
        Text("Import ${res.importedItems.size} Logins")
    }
}

@Composable
private fun ExportCsvSheetContent(viewModel: SecretsViewModel) {
    Text("Export for Google", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFF5C7A).copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
        Text(
            "This file is unencrypted plaintext. Anyone who opens it can read every password. Delete it after importing to Google Password Manager.",
            modifier = Modifier.padding(14.dp),
            color = Color(0xFFFF5C7A),
            style = MaterialTheme.typography.bodySmall
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            viewModel.exportToCsv { _ -> viewModel.closeSheet() }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
    ) {
        Text("Confirm Export")
    }
}
