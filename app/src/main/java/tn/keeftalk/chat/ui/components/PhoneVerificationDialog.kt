package tn.keeftalk.chat.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PhoneVerificationNotAvailableDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Phone verification isn't available yet", fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Phone number verification is not implemented yet. It will be available soon.\n\n" +
                "Until then, you can use your username or email to log in.\n\n" +
                "Your phone number cannot currently be used for phone-number discovery or caller identification."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    )
}
