package com.keeftalk.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keeftalk.chat.ui.components.KeeftalkTopBar
import com.keeftalk.chat.ui.theme.LocalAppIcons
import com.keeftalk.chat.ui.theme.KeeftalkDimensions
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.keeftalk.chat.ui.components.KeeftalkAvatar
import com.keeftalk.chat.util.AvatarUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onSave: (
        name: String, 
        phone: String,
        secondaryPhone: String?,
        secondaryPhoneLabel: String?,
        tertiaryPhone: String?,
        tertiaryPhoneLabel: String?,
        email: String?,
        birthday: Long?,
        callingCard: String?,
        avatarUrl: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var secondaryPhone by remember { mutableStateOf("") }
    var secondaryPhoneLabel by remember { mutableStateOf("Work") }
    var tertiaryPhone by remember { mutableStateOf("") }
    var tertiaryPhoneLabel by remember { mutableStateOf("Home") }
    var email by remember { mutableStateOf("") }
    var callingCard by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<String?>(null) }
    
    var birthdayTimestamp by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val icons = LocalAppIcons.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        avatarUri = uri?.toString()
    }

    Scaffold(
        topBar = {
            KeeftalkTopBar(
                title = "Add Contact",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(icons.back, contentDescription = "Back", modifier = Modifier.size(KeeftalkDimensions.topBarIconSize))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Avatar Selection
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Photo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Primary Phone *") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    singleLine = true
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        label = { Text("Secondary Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = secondaryPhoneLabel,
                        onValueChange = { secondaryPhoneLabel = it },
                        label = { Text("Label") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tertiaryPhone,
                        onValueChange = { tertiaryPhone = it },
                        label = { Text("Tertiary Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tertiaryPhoneLabel,
                        onValueChange = { tertiaryPhoneLabel = it },
                        label = { Text("Label") },
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = birthdayTimestamp?.let { java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "",
                    onValueChange = { },
                    label = { Text("Birthday") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.Cake, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = callingCard,
                    onValueChange = { callingCard = it },
                    label = { Text("Calling Card / Notes") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    leadingIcon = { Icon(Icons.Default.Description, null) },
                    singleLine = false,
                    maxLines = 5
                )
            }

            item {
                Button(
                    onClick = { 
                        onSave(
                            name, 
                            phone, 
                            secondaryPhone.takeIf { it.isNotBlank() },
                            secondaryPhoneLabel,
                            tertiaryPhone.takeIf { it.isNotBlank() },
                            tertiaryPhoneLabel,
                            email.takeIf { it.isNotBlank() },
                            birthdayTimestamp,
                            callingCard.takeIf { it.isNotBlank() },
                            avatarUri
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 8.dp),
                    enabled = name.isNotBlank() && phone.isNotBlank()
                ) {
                    Text("Save Contact")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthdayTimestamp = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
