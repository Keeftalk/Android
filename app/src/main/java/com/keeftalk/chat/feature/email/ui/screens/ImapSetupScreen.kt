package com.keeftalk.chat.feature.email.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImapSetupScreen(
    isSyncing: Boolean,
    onBack: () -> Unit,
    onTestConnection: (String, String, String, Int, String, Int) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var imapHost by remember { mutableStateOf("") }
    var imapPort by remember { mutableStateOf("993") }
    var smtpHost by remember { mutableStateOf("") }
    var smtpPort by remember { mutableStateOf("465") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password / App Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Incoming Server (IMAP)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = imapHost,
                onValueChange = { imapHost = it },
                label = { Text("IMAP Host") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = imapPort,
                onValueChange = { imapPort = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Outgoing Server (SMTP)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = smtpHost,
                onValueChange = { smtpHost = it },
                label = { Text("SMTP Host") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = smtpPort,
                onValueChange = { smtpPort = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    onTestConnection(
                        email, password, imapHost, imapPort.toIntOrNull() ?: 993, 
                        smtpHost, smtpPort.toIntOrNull() ?: 465
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSyncing && email.isNotBlank() && password.isNotBlank() && imapHost.isNotBlank() && smtpHost.isNotBlank()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Test Connection")
                }
            }
        }
    }
}
