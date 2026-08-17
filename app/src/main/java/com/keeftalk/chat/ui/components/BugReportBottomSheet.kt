package com.keeftalk.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.settings.BugReportUiState
import com.keeftalk.chat.ui.settings.BugReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportBottomSheet(
    viewModel: BugReportViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                Text(
                    text = "Send feedback to Keeftalk",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp)) // To center the title
            }

            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "BugReportContent"
            ) { state ->
                when (state) {
                    is BugReportUiState.Idle -> WelcomeStep(onGetStarted = { viewModel.startReporting() })
                    is BugReportUiState.CollectingLogs -> LoadingStep("Collecting system logs...")
                    is BugReportUiState.Inputting -> InputStep(onSubmit = { viewModel.submitReport(it) })
                    is BugReportUiState.Sending -> LoadingStep("Sending report...")
                    is BugReportUiState.Success -> SuccessStep(onDone = onDismiss)
                    is BugReportUiState.Error -> ErrorStep(message = state.message, onRetry = { viewModel.startReporting() })
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Welcome! \uD83D\uDC4B",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "We are constantly improving our product by listening to your feedback.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Answer few simple questions, highlight the area of concern on the screenshot & send it to us.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started")
        }
    }
}

@Composable
fun InputStep(onSubmit: (String) -> Unit) {
    var description by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Great! Let's get started \uD83D\uDC4D",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Could you describe your issue or suggestion?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Please don't include sensitive information",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text("Tell us how we can improve our product") },
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(
                    onClick = { onSubmit(description) },
                    enabled = description.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (description.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        )
    }
}

@Composable
fun LoadingStep(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SuccessStep(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF2ECC71)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Thank you for your feedback!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your report has been sent and will be reviewed by our team.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDone, shape = RoundedCornerShape(12.dp)) {
            Text("Done")
        }
    }
}

@Composable
fun ErrorStep(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Text("Retry")
        }
    }
}
