package com.keeftalk.chat.feature.email.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeEmailScreen(
    viewModel: com.keeftalk.chat.feature.email.viewmodel.EmailViewModel,
    initialTo: String? = null,
    initialSubject: String? = null,
    initialBody: String? = null,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    
    LaunchedEffect(accounts) {
        if (selectedAccount == null) selectedAccount = accounts.firstOrNull()
    }

    var to by remember { mutableStateOf(initialTo ?: "") }
    var subject by remember { mutableStateOf(initialSubject ?: "") }
    val richTextState = remember { RichTextState() }
    
    LaunchedEffect(initialBody) {
        if (initialBody != null) richTextState.setHtml(initialBody)
    }
    val attachments = remember { mutableStateListOf<android.net.Uri>() }
    var showAccountSelector by remember { mutableStateOf(false) }
    
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        attachments.addAll(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Message", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { attachmentLauncher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                    }
                    Button(
                        onClick = { 
                            selectedAccount?.let { 
                                viewModel.sendMessage(it.id, subject, richTextState.toHtml(), to)
                                onBack()
                            }
                        },
                        enabled = to.isNotBlank() && subject.isNotBlank() && selectedAccount != null,
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // From Section
            if (accounts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccountSelector = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "From",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp)
                    )
                    Text(
                        selectedAccount?.emailAddress ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (showAccountSelector) {
                AlertDialog(
                    onDismissRequest = { showAccountSelector = false },
                    title = { Text("Choose Account") },
                    text = {
                        Column {
                            accounts.forEach { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            selectedAccount = acc
                                            showAccountSelector = false 
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedAccount?.id == acc.id, onClick = null)
                                    Spacer(Modifier.width(16.dp))
                                    Text(acc.emailAddress)
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showAccountSelector = false }) { Text("Cancel") } }
                )
            }

            // Recipient Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            "To",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(48.dp)
                        )
                        TextField(
                            value = to,
                            onValueChange = { to = it },
                            placeholder = { Text("Recipients", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        IconButton(onClick = { /* Add Cc/Bcc */ }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                    
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(
                            "Subject",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(64.dp)
                        )
                        TextField(
                            value = subject,
                            onValueChange = { subject = it },
                            placeholder = { Text("Email subject", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Editor Toolbar
            RichTextEditorControls(richTextState)

            // Rich Text Editor
            RichTextEditor(
                state = richTextState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Compose email...", style = MaterialTheme.typography.bodyLarge) },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            
            if (attachments.isNotEmpty()) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    attachments.forEach { uri ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.InsertDriveFile, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    uri.lastPathSegment ?: "Attachment",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { attachments.remove(uri) }, Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichTextEditorControls(state: RichTextState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ControlIcon(Icons.Default.FormatBold, "Bold", false) { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
        ControlIcon(Icons.Default.FormatItalic, "Italic", false) { state.toggleSpanStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) }
        ControlIcon(Icons.Default.FormatUnderlined, "Underline", false) { state.toggleSpanStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) }
        ControlIcon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet List", false) { /* state.toggleUnorderedList() */ }
    }
}

@Composable
fun ControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(20.dp))
    }
}
