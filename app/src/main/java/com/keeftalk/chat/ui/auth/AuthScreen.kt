package com.keeftalk.chat.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.components.PremiumButton
import com.keeftalk.chat.ui.components.PremiumTextField
import com.keeftalk.chat.ui.theme.fabGradientIcon
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onForgotPassword: () -> Unit = {},
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formType by viewModel.formType.collectAsStateWithLifecycle()
    var showLegalDocumentType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onAuthSuccess()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                AuthHeader()
                
                Spacer(modifier = Modifier.height(32.dp))
                
                AuthTabs(
                    selectedType = formType,
                    onTypeSelected = { viewModel.setFormType(it) }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                AnimatedContent(
                    targetState = formType,
                    transitionSpec = {
                        if (targetState == AuthFormType.SIGNUP) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else if (targetState == AuthFormType.VERIFY_EMAIL) {
                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "AuthFormTransition"
                ) { targetType ->
                    when (targetType) {
                        AuthFormType.LOGIN -> LoginForm(viewModel, onForgotPassword)
                        AuthFormType.SIGNUP -> SignUpForm(
                            viewModel = viewModel, 
                            onShowLegal = { showLegalDocumentType = it }
                        )
                        AuthFormType.VERIFY_EMAIL -> EmailVerificationScreen(viewModel)
                    }
                }
                
                AnimatedVisibility(
                    visible = uiState is AuthUiState.Error,
                    enter = shake() + fadeIn(),
                    exit = fadeOut()
                ) {
                    if (uiState is AuthUiState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = (uiState as AuthUiState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                AuthFooter(formType) {
                    viewModel.setFormType(if (formType == AuthFormType.LOGIN) AuthFormType.SIGNUP else AuthFormType.LOGIN)
                }
            }
        }
    }

    showLegalDocumentType?.let { typeOrUrl ->
        com.keeftalk.chat.ui.components.LegalViewerModal(
            typeOrUrl = typeOrUrl,
            onDismiss = { showLegalDocumentType = null }
        )
    }

    if (uiState is AuthUiState.EncryptionError) {
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Security Context Mismatch") },
            text = { Text((uiState as AuthUiState.EncryptionError).message) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.performSecurityReset() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Security Reset (Wipe Keys)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("Try Again")
                }
            }
        )
    }
}

@Composable
fun AuthHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = rememberAsyncImagePainter(model = com.keeftalk.chat.R.mipmap.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to Keeftalk",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Connect instantly with everyone.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AuthTabs(
    selectedType: AuthFormType,
    onTypeSelected: (AuthFormType) -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedType == AuthFormType.LOGIN) 0.dp else maxWidth / 2,
            label = "IndicatorOffset",
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
        
        // Background indicator
        Box(
            modifier = Modifier
                .width(maxWidth / 2 - 4.dp)
                .fillMaxHeight()
                .offset(x = indicatorOffset)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TabItem(
                text = "Log In",
                isSelected = selectedType == AuthFormType.LOGIN,
                modifier = Modifier.weight(1f),
                onClick = { onTypeSelected(AuthFormType.LOGIN) }
            )
            TabItem(
                text = "Sign Up",
                isSelected = selectedType == AuthFormType.SIGNUP,
                modifier = Modifier.weight(1f),
                onClick = { onTypeSelected(AuthFormType.SIGNUP) }
            )
        }
    }
}

@Composable
fun TabItem(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TabTextColor"
    )
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            fontSize = 15.sp
        )
    }
}

@Composable
fun LoginForm(viewModel: AuthViewModel, onForgotPassword: () -> Unit) {
    val identifier by viewModel.loginIdentifier.collectAsStateWithLifecycle()
    val password by viewModel.loginPassword.collectAsStateWithLifecycle()
    val rememberMe by viewModel.rememberMe.collectAsStateWithLifecycle()
    val isLoginValid by viewModel.isLoginValid.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumTextField(
            value = identifier,
            onValueChange = viewModel::onLoginIdentifierChange,
            label = "Email, Username or Phone",
            icon = Icons.Default.Person,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = password,
            onValueChange = viewModel::onLoginPasswordChange,
            label = "Password",
            icon = Icons.Default.Lock,
            isPassword = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = viewModel::onRememberMeChange,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "Remember me",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            TextButton(onClick = onForgotPassword) {
                Text(text = "Forgot password?", fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        PremiumButton(
            text = "Log In",
            isLoading = uiState is AuthUiState.Loading,
            enabled = isLoginValid && uiState !is AuthUiState.Loading,
            onClick = { viewModel.login() }
        )
    }
}

@Composable
fun SignUpForm(viewModel: AuthViewModel, onShowLegal: (String) -> Unit) {
    val fullName by viewModel.signupFullName.collectAsStateWithLifecycle()
    val email by viewModel.signupEmail.collectAsStateWithLifecycle()
    val phone by viewModel.signupPhone.collectAsStateWithLifecycle()
    val currentCountry by viewModel.currentCountry.collectAsStateWithLifecycle()
    val password by viewModel.signupPassword.collectAsStateWithLifecycle()
    val agreeToTerms by viewModel.agreeToTerms.collectAsStateWithLifecycle()
    val passwordStrength by viewModel.signupPasswordStrength.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSignupValid by viewModel.isSignupValid.collectAsStateWithLifecycle()
    
    val signupErrors by viewModel.signupErrors.collectAsStateWithLifecycle()
    
    val focusManager = LocalFocusManager.current
    var showCountryPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumTextField(
            value = fullName,
            onValueChange = viewModel::onSignupFullNameChange,
            label = "Full Name",
            icon = Icons.Default.Badge,
            error = signupErrors["fullName"],
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Words)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = email,
            onValueChange = viewModel::onSignupEmailChange,
            label = "Email",
            icon = Icons.Default.Email,
            error = signupErrors["email"],
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        com.keeftalk.chat.ui.components.KeeftalkPhoneField(
            value = phone,
            onValueChange = viewModel::onSignupPhoneChange,
            selectedCountry = currentCountry,
            onCountryClick = { showCountryPicker = true },
            error = signupErrors["phone"]
        )
        
        if (showCountryPicker) {
            com.keeftalk.chat.ui.components.CountryPickerDialog(
                onCountrySelected = viewModel::onCountryChange,
                onDismissRequest = { showCountryPicker = false }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PremiumTextField(
            value = password,
            onValueChange = viewModel::onSignupPasswordChange,
            label = "Password",
            icon = Icons.Default.Lock,
            isPassword = true,
            initialPasswordVisible = true,
            error = signupErrors["password"],
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        
        if (password.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { passwordStrength },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(4.dp)
                    .clip(CircleShape),
                color = when {
                    passwordStrength < 0.4f -> Color.Red
                    passwordStrength < 0.7f -> Color.Yellow
                    else -> Color.Green
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = agreeToTerms,
                onCheckedChange = viewModel::onAgreeToTermsChange,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            
            val annotatedText = buildAnnotatedString {
                append("I agree to the ")
                pushStringAnnotation(tag = "terms", annotation = "https://keeftalk.com/legal/terms.html")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Terms")
                }
                pop()
                append(" & ")
                pushStringAnnotation(tag = "privacy", annotation = "https://keeftalk.com/legal/privacy.html")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Privacy Policy")
                }
                pop()
            }

            androidx.compose.foundation.text.ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                ),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                        .firstOrNull()?.let { onShowLegal(it.item) }
                    annotatedText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                        .firstOrNull()?.let { onShowLegal(it.item) }
                }
            )
        }
        
        if (signupErrors.containsKey("terms")) {
            Text(
                text = signupErrors["terms"]!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        PremiumButton(
            text = "Create Account",
            isLoading = uiState is AuthUiState.Loading,
            enabled = isSignupValid && uiState !is AuthUiState.Loading,
            onClick = { viewModel.signup() }
        )
    }
}

@Composable
fun AuthFooter(formType: AuthFormType, onSwitch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (formType == AuthFormType.LOGIN) "Don't have an account?" else "Already have an account?",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        TextButton(onClick = onSwitch) {
            Text(
                text = if (formType == AuthFormType.LOGIN) "Sign Up" else "Log In",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

fun shake(): EnterTransition {
    return slideInHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)) { 20 }
}
