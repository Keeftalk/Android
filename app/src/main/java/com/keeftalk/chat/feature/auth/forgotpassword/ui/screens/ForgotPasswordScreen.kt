package com.keeftalk.chat.feature.auth.forgotpassword.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.Profile
import com.keeftalk.chat.feature.auth.forgotpassword.state.ForgotPasswordStep
import com.keeftalk.chat.feature.auth.forgotpassword.viewmodel.ForgotPasswordViewModel
import com.keeftalk.chat.ui.components.PremiumButton
import com.keeftalk.chat.ui.components.PremiumTextField
import com.keeftalk.chat.util.AuthUtils

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.forgotPasswordState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = {
                    when (state.step) {
                        ForgotPasswordStep.ENTER_IDENTITY -> onBack()
                        ForgotPasswordStep.SELECT_PROFILE -> viewModel.backToIdentity()
                        ForgotPasswordStep.CONFIRM_PROFILE -> {
                            if (state.foundProfiles.size > 1) {
                                viewModel.selectProfile(state.selectedProfile!!) // Should actually just go back to SELECT_PROFILE
                            } else {
                                viewModel.backToIdentity()
                            }
                        }
                        ForgotPasswordStep.VERIFY_OTP -> viewModel.selectProfile(state.selectedProfile!!)
                        ForgotPasswordStep.CREATE_NEW_PASSWORD -> {
                            // Maybe stay here or go back to OTP? Usually reset process is linear.
                        }
                        else -> onBack()
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Forgot Password",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ForgotPasswordStepTransition"
        ) { step ->
            when (step) {
                ForgotPasswordStep.ENTER_IDENTITY -> IdentityInput(state, viewModel, onBack)
                ForgotPasswordStep.SELECT_PROFILE -> ProfileSelection(state, viewModel)
                ForgotPasswordStep.CONFIRM_PROFILE -> ProfileConfirmation(state, viewModel)
                ForgotPasswordStep.VERIFY_OTP -> OtpVerification(state, viewModel, onBack)
                ForgotPasswordStep.CREATE_NEW_PASSWORD -> CreateNewPasswordScreen(
                    viewModel = viewModel,
                    onSuccess = { /* Already handled in ViewModel */ }
                )
                ForgotPasswordStep.CONFIRMATION -> PasswordResetSuccessScreen(
                    onSignInNow = onBack
                )
            }
        }
    }
}

@Composable
fun IdentityInput(
    state: com.keeftalk.chat.feature.auth.forgotpassword.state.ForgotPasswordState,
    viewModel: ForgotPasswordViewModel,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Find Your Account",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Enter your email, username, phone number, or full name to find your account.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        PremiumTextField(
            value = state.identifier,
            onValueChange = { viewModel.onIdentifierChange(it) },
            label = "Account Information",
            icon = Icons.Default.Search,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { 
                    focusManager.clearFocus()
                    viewModel.findAccount() 
                }
            ),
            error = state.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        PremiumButton(
            text = "Find Account",
            isLoading = state.isLoading,
            enabled = state.identifier.isNotBlank() && !state.isLoading,
            onClick = { 
                focusManager.clearFocus()
                viewModel.findAccount() 
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Back to Login", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ProfileSelection(
    state: com.keeftalk.chat.feature.auth.forgotpassword.state.ForgotPasswordState,
    viewModel: ForgotPasswordViewModel
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Select Your Account",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Multiple accounts found. Please select yours.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.foundProfiles) { profile ->
                ProfileItem(profile = profile) {
                    viewModel.selectProfile(profile)
                }
            }
        }
    }
}

@Composable
fun ProfileConfirmation(
    state: com.keeftalk.chat.feature.auth.forgotpassword.state.ForgotPasswordState,
    viewModel: ForgotPasswordViewModel
) {
    val profile = state.selectedProfile ?: return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Is this your account?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp
        ) {
            if (profile.avatarUrl != null) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = profile.fullName ?: profile.username,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (profile.email != null) {
            Text(
                text = AuthUtils.maskEmail(profile.email!!),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        PremiumButton(
            text = "Yes, Send OTP",
            isLoading = state.isLoading,
            enabled = !state.isLoading,
            onClick = { viewModel.sendOtp() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.backToIdentity() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Not my account", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OtpVerification(
    state: com.keeftalk.chat.feature.auth.forgotpassword.state.ForgotPasswordState,
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Enter Verification Code",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We've sent a 6-digit verification code to ${state.selectedProfile?.email?.let { AuthUtils.maskEmail(it) } ?: "your email"}.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        PremiumTextField(
            value = state.otp,
            onValueChange = { viewModel.onOtpChange(it) },
            label = "6-Digit Code",
            icon = Icons.Default.Dialpad,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.verifyOtp()
                }
            ),
            error = state.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        PremiumButton(
            text = "Verify Code",
            isLoading = state.isLoading,
            enabled = state.otp.length == 6 && !state.isLoading,
            onClick = {
                focusManager.clearFocus()
                viewModel.verifyOtp()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Didn't receive the code? ",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            TextButton(
                onClick = { viewModel.sendOtp() },
                enabled = state.countdown == 0 && !state.isLoading
            ) {
                Text(
                    text = if (state.countdown > 0) "Resend in ${state.countdown}s" else "Resend Code",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Back to Login", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ProfileItem(profile: Profile, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (profile.avatarUrl != null) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = profile.fullName ?: profile.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (profile.email != null) {
                    Text(
                        text = AuthUtils.maskEmail(profile.email!!),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
