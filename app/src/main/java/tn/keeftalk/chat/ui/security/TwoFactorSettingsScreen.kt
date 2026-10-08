package tn.keeftalk.chat.ui.security

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.*
import tn.keeftalk.chat.ui.security.dialogs.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorSettingsScreen(
    viewModel: TwoFactorViewModel,
    isEmbedded: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()

    var showTotpDialog by remember { mutableStateOf<TotpEnrollmentData?>(null) }
    var showOtpDialog by remember { mutableStateOf<Pair<OtpChallengeResponse, TwoFactorMethod>?>(null) }
    var showBackupCodesDialog by remember { mutableStateOf<List<String>?>(null) }
    var showDisableConfirm by remember { mutableStateOf<TwoFactorMethod?>(null) }

    val totpFactor = status?.activeFactors?.firstOrNull { it.type == TwoFactorMethod.TOTP }
    val emailFactor = status?.activeFactors?.firstOrNull { it.type == TwoFactorMethod.EMAIL }
    val smsFactor = status?.activeFactors?.firstOrNull { it.type == TwoFactorMethod.SMS }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is TwoFactorUiState.TotpEnrollmentStarted -> {
                showTotpDialog = state.data
            }
            is TwoFactorUiState.OtpChallengeSent -> {
                showOtpDialog = Pair(state.challengeResponse, state.method)
            }
            is TwoFactorUiState.BackupCodesGenerated -> {
                showBackupCodesDialog = state.codes
            }
            is TwoFactorUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                showTotpDialog = null
                showOtpDialog = null
                viewModel.resetUiState()
            }
            is TwoFactorUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    if (isEmbedded) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TwoFactorSettingsContent(
                status = status,
                totpFactor = totpFactor,
                emailFactor = emailFactor,
                smsFactor = smsFactor,
                viewModel = viewModel,
                onShowDisableConfirm = { showDisableConfirm = it }
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Two-Factor Authentication", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TwoFactorSettingsContent(
                    status = status,
                    totpFactor = totpFactor,
                    emailFactor = emailFactor,
                    smsFactor = smsFactor,
                    viewModel = viewModel,
                    onShowDisableConfirm = { showDisableConfirm = it }
                )
            }
        }
    }

    // Dialog Rendering
    showTotpDialog?.let { totpData ->
        TotpEnrollmentDialog(
            data = totpData,
            isLoading = uiState is TwoFactorUiState.Loading,
            errorMessage = (uiState as? TwoFactorUiState.Error)?.message,
            onDismiss = { showTotpDialog = null; viewModel.resetUiState() },
            onVerify = { code -> viewModel.verifyTotpEnrollment(code) }
        )
    }

    showOtpDialog?.let { (response, method) ->
        OtpEnrollmentDialog(
            response = response,
            method = method,
            isLoading = uiState is TwoFactorUiState.Loading,
            errorMessage = (uiState as? TwoFactorUiState.Error)?.message,
            onDismiss = { showOtpDialog = null; viewModel.resetUiState() },
            onResend = {
                if (method == TwoFactorMethod.EMAIL) viewModel.requestEmailOtp()
                else viewModel.requestSmsOtp()
            },
            onVerify = { code ->
                response.challengeId?.let { id ->
                    viewModel.verifyOtpChallenge(id, code)
                }
            }
        )
    }

    showBackupCodesDialog?.let { codes ->
        BackupCodesDialog(
            codes = codes,
            onDismiss = { showBackupCodesDialog = null }
        )
    }

    showDisableConfirm?.let { method ->
        AlertDialog(
            onDismissRequest = { showDisableConfirm = null },
            title = { Text("Disable ${method.name} 2FA?") },
            text = { Text("Are you sure you want to disable this verification method?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disable2FAMethod(method)
                        showDisableConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disable Method")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TwoFactorSettingsContent(
    status: TwoFactorStatus?,
    totpFactor: TwoFactorFactor?,
    emailFactor: TwoFactorFactor?,
    smsFactor: TwoFactorFactor?,
    viewModel: TwoFactorViewModel,
    onShowDisableConfirm: (TwoFactorMethod) -> Unit
) {
    // Status Header Banner
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (status?.twoFactorEnabled == true) Color(0xFF2ECC71).copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
        border = BorderStroke(
            1.dp,
            if (status?.twoFactorEnabled == true) Color(0xFF2ECC71).copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (status?.twoFactorEnabled == true) Color(0xFF2ECC71)
                        else MaterialTheme.colorScheme.error
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (status?.twoFactorEnabled == true) Icons.Rounded.Shield else Icons.Rounded.GppBad,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = if (status?.twoFactorEnabled == true) "2FA is Active" else "2FA is Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (status?.twoFactorEnabled == true) Color(0xFF2ECC71)
                            else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (status?.twoFactorEnabled == true) "Your account is protected with two-step verification."
                           else "Add an additional verification layer to secure your account.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Text(
        text = "VERIFICATION METHODS",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )

    // Method 1: Authenticator App
    MethodCard(
        title = "Authenticator App",
        subtitle = if (totpFactor != null) "Active • TOTP Codes" else "Google Authenticator, Aegis, 1Password",
        icon = Icons.Rounded.QrCode2,
        isEnabled = totpFactor != null,
        isPreferred = status?.preferredMethod == TwoFactorMethod.TOTP,
        onEnable = { viewModel.beginTotpEnrollment() },
        onDisable = { onShowDisableConfirm(TwoFactorMethod.TOTP) },
        onSetPreferred = { viewModel.setPreferredMethod(TwoFactorMethod.TOTP) }
    )

    // Method 2: Email OTP
    MethodCard(
        title = "Email Verification",
        subtitle = emailFactor?.verifiedDestination ?: "Verify codes delivered to your verified email",
        icon = Icons.Rounded.Email,
        isEnabled = emailFactor != null,
        isPreferred = status?.preferredMethod == TwoFactorMethod.EMAIL,
        onEnable = { viewModel.requestEmailOtp() },
        onDisable = { onShowDisableConfirm(TwoFactorMethod.EMAIL) },
        onSetPreferred = { viewModel.setPreferredMethod(TwoFactorMethod.EMAIL) }
    )

    // Method 3: Phone / SMS OTP
    MethodCard(
        title = "Phone / SMS Verification",
        subtitle = smsFactor?.verifiedDestination ?: "Verify codes sent to your mobile phone number",
        icon = Icons.Rounded.Sms,
        isEnabled = smsFactor != null,
        isPreferred = status?.preferredMethod == TwoFactorMethod.SMS,
        onEnable = { viewModel.requestSmsOtp() },
        onDisable = { onShowDisableConfirm(TwoFactorMethod.SMS) },
        onSetPreferred = { viewModel.setPreferredMethod(TwoFactorMethod.SMS) }
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Backup Codes Section
    Text(
        text = "ACCOUNT RECOVERY",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Backup Recovery Codes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (status?.hasBackupCodes == true) "Single-use codes generated" else "No backup codes generated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Use backup codes to log in if you lose your phone or authenticator app. Each code works only once.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.generateBackupCodes() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (status?.hasBackupCodes == true) "Regenerate Backup Codes" else "Generate Backup Codes")
            }
        }
    }
}

@Composable
private fun MethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    isPreferred: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onSetPreferred: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            if (isEnabled) Color(0xFF2ECC71).copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled) Color(0xFF2ECC71).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isEnabled) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (isPreferred) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PREFERRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (isEnabled) {
                    IconButton(onClick = onDisable) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Disable", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Button(
                        onClick = onEnable,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Set Up", fontSize = 12.sp)
                    }
                }
            }

            if (isEnabled && !isPreferred) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onSetPreferred,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Set as Preferred Method", fontSize = 12.sp)
                }
            }
        }
    }
}
