package tn.keeftalk.chat.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tn.keeftalk.chat.domain.model.TwoFactorLoginChallenge
import tn.keeftalk.chat.domain.model.TwoFactorMethod
import tn.keeftalk.chat.ui.components.PremiumButton

@Composable
fun TwoFactorLoginScreen(
    challenge: TwoFactorLoginChallenge,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRequestOtp: (TwoFactorMethod) -> Unit,
    onVerify: (TwoFactorMethod, String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedMethod by remember {
        mutableStateOf(challenge.preferredFactor ?: challenge.allowedFactors.firstOrNull() ?: TwoFactorMethod.TOTP)
    }
    var code by remember { mutableStateOf("") }
    var cooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    // Auto-trigger OTP request if Email/SMS selected and not yet sent
    LaunchedEffect(selectedMethod) {
        code = ""
        if (selectedMethod == TwoFactorMethod.EMAIL || selectedMethod == TwoFactorMethod.SMS) {
            onRequestOtp(selectedMethod)
            cooldown = 60
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Verify It's You",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Two-factor authentication is required to complete your sign in.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Method Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            challenge.allowedFactors.forEach { method ->
                FilterChip(
                    selected = selectedMethod == method,
                    onClick = { selectedMethod = method },
                    label = {
                        Text(
                            when (method) {
                                TwoFactorMethod.TOTP -> "Authenticator"
                                TwoFactorMethod.EMAIL -> "Email"
                                TwoFactorMethod.SMS -> "SMS"
                                TwoFactorMethod.BACKUP_CODE -> "Backup Code"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (method) {
                                TwoFactorMethod.TOTP -> Icons.Rounded.QrCode2
                                TwoFactorMethod.EMAIL -> Icons.Rounded.Email
                                TwoFactorMethod.SMS -> Icons.Rounded.Sms
                                TwoFactorMethod.BACKUP_CODE -> Icons.Rounded.VpnKey
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            // Always offer backup code
            if (!challenge.allowedFactors.contains(TwoFactorMethod.BACKUP_CODE)) {
                FilterChip(
                    selected = selectedMethod == TwoFactorMethod.BACKUP_CODE,
                    onClick = { selectedMethod = TwoFactorMethod.BACKUP_CODE },
                    label = { Text("Backup Code") },
                    leadingIcon = {
                        Icon(Icons.Rounded.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Context Prompt
        Text(
            text = when (selectedMethod) {
                TwoFactorMethod.TOTP -> "Enter the 6-digit code from your authenticator app."
                TwoFactorMethod.EMAIL -> "Enter the 6-digit code sent to your email."
                TwoFactorMethod.SMS -> "Enter the 6-digit code sent to your mobile phone."
                TwoFactorMethod.BACKUP_CODE -> "Enter one of your 8-character single-use backup codes."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Input Field
        OutlinedTextField(
            value = code,
            onValueChange = { input ->
                if (selectedMethod == TwoFactorMethod.BACKUP_CODE) {
                    if (input.length <= 9) code = input.uppercase()
                } else {
                    if (input.length <= 6) code = input.filter { it.isDigit() }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = if (selectedMethod == TwoFactorMethod.BACKUP_CODE) "XXXX-XXXX" else "000 000",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (selectedMethod == TwoFactorMethod.BACKUP_CODE) KeyboardType.Text else KeyboardType.Number
            ),
            shape = RoundedCornerShape(16.dp),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )
        )

        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (selectedMethod == TwoFactorMethod.EMAIL || selectedMethod == TwoFactorMethod.SMS) {
            TextButton(
                onClick = {
                    if (cooldown == 0) {
                        onRequestOtp(selectedMethod)
                        cooldown = 60
                    }
                },
                enabled = cooldown == 0 && !isLoading
            ) {
                Text(
                    if (cooldown > 0) "Resend code in ${cooldown}s" else "Resend Code",
                    fontSize = 13.sp
                )
            }
        }

        // Primary Action
        PremiumButton(
            text = "Verify & Sign In",
            isLoading = isLoading,
            onClick = { onVerify(selectedMethod, code) },
            enabled = code.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = onCancel) {
            Text("Cancel Sign In", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
