package com.keeftalk.chat.feature.auth.forgotpassword.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keeftalk.chat.feature.auth.forgotpassword.state.PasswordRequirements
import com.keeftalk.chat.feature.auth.forgotpassword.viewmodel.ForgotPasswordViewModel
import com.keeftalk.chat.ui.components.PremiumButton
import com.keeftalk.chat.ui.components.PremiumTextField

@Composable
fun CreateNewPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onSuccess: () -> Unit
) {
    val state by viewModel.resetPasswordState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Create New Password",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your new password must be different from previous used passwords.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        PremiumTextField(
            value = state.newPassword,
            onValueChange = { viewModel.onNewPasswordChange(it) },
            label = "New Password",
            icon = Icons.Default.Lock,
            isPassword = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordRequirementsList(requirements = state.passwordRequirements)

        Spacer(modifier = Modifier.height(24.dp))

        PremiumTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onConfirmPasswordChange(it) },
            label = "Confirm Password",
            icon = Icons.Default.CheckCircle,
            isPassword = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            error = if (state.confirmPassword.isNotEmpty() && !state.passwordRequirements.passwordsMatch) 
                "Passwords do not match" else null
        )

        Spacer(modifier = Modifier.height(32.dp))

        PremiumButton(
            text = "Reset Password",
            isLoading = state.isLoading,
            enabled = state.passwordRequirements.allMet && !state.isLoading,
            onClick = { viewModel.updatePassword() }
        )

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PasswordRequirementsList(requirements: PasswordRequirements) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RequirementItem("At least 8 characters", requirements.hasMinLength)
        RequirementItem("At least one uppercase letter", requirements.hasUppercase)
        RequirementItem("At least one lowercase letter", requirements.hasLowercase)
        RequirementItem("At least one number", requirements.hasDigit)
        RequirementItem("At least one special character", requirements.hasSpecialChar)
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.CheckCircle, // Use a different icon for unmet if desired
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isMet) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isMet) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
