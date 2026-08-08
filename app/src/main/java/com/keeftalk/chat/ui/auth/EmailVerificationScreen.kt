package com.keeftalk.chat.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.components.PremiumButton
import com.keeftalk.chat.ui.components.PremiumTextField
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EmailVerificationScreen(
    viewModel: AuthViewModel
) {
    val email by viewModel.signupEmail.collectAsStateWithLifecycle()
    val countdown by viewModel.resendCountdown.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.size(100.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Verify your email",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (email.isNotBlank()) "We've sent a verification link to\n" else "Please check your inbox for a verification link.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        if (email.isNotBlank()) {
            Text(
                text = email,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = "\nPlease click the link in the email to continue.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PremiumButton(
            text = "I've Verified",
            isLoading = uiState is AuthUiState.Loading,
            enabled = uiState !is AuthUiState.Loading,
            onClick = { viewModel.checkVerificationStatus() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = { viewModel.resendVerification() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = countdown == 0 && uiState !is AuthUiState.Loading,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (countdown > 0) "Resend in ${countdown}s" else "Resend Verification Email",
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp).alpha(0.1f))
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = { showChangeEmailDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Change Email")
            }
            
            TextButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Out")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { showDeleteConfirmDialog = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete Account")
        }
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            currentEmail = email,
            onDismiss = { showChangeEmailDialog = false },
            onConfirm = { 
                viewModel.updateEmail(it)
                showChangeEmailDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently remove your account and all associated data. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.deleteAccount()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newEmail by remember { mutableStateOf(currentEmail) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Email Address") },
        text = {
            Column {
                Text("Enter your new email address. We will send a new verification link.")
                Spacer(modifier = Modifier.height(16.dp))
                PremiumTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = "New Email",
                    icon = Icons.Default.Email
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newEmail) },
                enabled = android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()
            ) {
                Text("Update & Resend")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
