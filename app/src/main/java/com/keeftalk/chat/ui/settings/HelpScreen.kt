package com.keeftalk.chat.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val faqCategories = remember { getFaqCategories() }
    var searchQuery by remember { mutableStateOf(value = "") }
    var selectedTopic by remember { mutableStateOf<Topic?>(null) }

    val filteredFaqs = remember(searchQuery, faqCategories, selectedTopic) {
        val baseList = if (selectedTopic != null) {
            faqCategories.filter { it.title.equals(selectedTopic?.title, ignoreCase = true) }
        } else {
            faqCategories
        }

        if (searchQuery.isEmpty()) {
            baseList
        } else {
            baseList.map { category ->
                category.copy(
                    faqs = category.faqs.filter { 
                        it.question.contains(searchQuery, ignoreCase = true) || 
                        it.answer.contains(searchQuery, ignoreCase = true) 
                    }
                )
            }.filter { it.faqs.isNotEmpty() }
        }
    }

    Scaffold(
        topBar = {
            SettingsHeader(
                title = selectedTopic?.title ?: "Help & Support", 
                onBack = {
                    if (selectedTopic != null) {
                        selectedTopic = null
                    } else {
                        onBack()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AnimatedContent(
            targetState = selectedTopic,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "HelpContentTransition"
        ) { topic ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (topic == null) {
                    // Main Help View
                    item {
                        HelpSearchHeader(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it }
                        )
                    }

                    item {
                        HelpQuickStartSection()
                    }

                    item {
                        HelpSectionHeader(title = "Support Topics")
                        TopicTilesGrid(
                            onTopicClick = { selectedTopic = it }
                        )
                    }

                    item {
                        HelpSectionHeader(title = "Premium Services Status")
                        ServiceStatusPulse()
                    }

                    item {
                        HelpSectionHeader(title = "Quick Actions")
                        PremiumTaskCards(context, uriHandler)
                    }

                    item {
                        HelpSectionHeader(title = "Frequently Asked Questions")
                    }
                } else {
                    // Detailed Topic View
                    item {
                        TopicDetailHeader(topic)
                    }
                    
                    item {
                        HelpSectionHeader(title = "Relevant FAQs")
                    }
                }

                if (filteredFaqs.isEmpty()) {
                    item {
                        NoResultsView()
                    }
                } else {
                    filteredFaqs.forEach { category ->
                        if (topic == null) {
                            item {
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        }
                        items(category.faqs) { faq ->
                            FaqItem(faq = faq)
                        }
                    }
                }

                if (topic == null) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        WebSupportLink(uriHandler = uriHandler)
                    }
                } else {
                    item {
                        TopicActionFooter(context, topic)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpSearchHeader(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "How can we help you?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                placeholder = { Text(text = "Search FAQ, features, or topics...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
    }
}

@Composable
fun HelpSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
fun TopicTilesGrid(onTopicClick: (Topic) -> Unit) {
    val topics = listOf(
        Topic("Account", Icons.Default.AccountCircle, "Profile, Identity, Security", Color(0xFF6C63FF)),
        Topic("Messaging", Icons.AutoMirrored.Filled.Chat, "Chats, Media, Groups", Color(0xFF00CCCC)),
        Topic("Vault", Icons.Default.Lock, "Secure storage, Biometrics", Color(0xFF3B82F6)),
        Topic("Productivity", Icons.Default.EditNote, "Notes, Email, Agenda", Color(0xFFF43F5E)),
        Topic("Connectivity", Icons.Default.Wifi, "Local Discovery, Sync", Color(0xFF10B981)),
        Topic("Security", Icons.Default.Shield, "Keys, Sessions, Privacy", Color(0xFFFFCC00))
    )

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        topics.chunked(2).forEach { rowTopics ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowTopics.forEach { topic ->
                    TopicTile(topic = topic, modifier = Modifier.weight(1f), onClick = { onTopicClick(topic) })
                }
            }
        }
    }
}

@Composable
fun HelpQuickStartSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Quick Start Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            val tips = listOf(
                "Verify your identity via the security center for E2EE.",
                "Enable Biometrics in Vault settings for extra protection.",
                "Use 'Local Discovery' on same Wi-Fi for ultra-fast sync."
            )
            tips.forEach { tip ->
                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
                    Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun TopicTile(topic: Topic, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(topic.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(topic.icon, contentDescription = null, tint = topic.color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(topic.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(topic.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PremiumTaskCards(context: Context, uriHandler: UriHandler) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PremiumTaskCard(
            title = "Report a Technical Issue",
            description = "Encountered a bug? Send us a detailed report to help us fix it.",
            icon = Icons.Default.BugReport,
            color = MaterialTheme.colorScheme.error,
            onClick = { contactSupport(context, "Bug Report") }
        )
        PremiumTaskCard(
            title = "Security & Privacy Center",
            description = "Learn how we protect your data and manage your encryption keys.",
            icon = Icons.Default.Security,
            color = MaterialTheme.colorScheme.primary,
            onClick = { uriHandler.openUri("https://keeftalk.com/security.html") }
        )
    }
}

@Composable
fun PremiumTaskCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ServiceStatusPulse() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow(name = "E2EE Engine & Keys", status = "Active", active = true)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            StatusRow(name = "Messaging & Media", status = "Operational", active = true)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            StatusRow(name = "Cloud Sync & Vault", status = "Operational", active = true)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            StatusRow(name = "Local Discovery (NSD)", status = "Ready", active = true)
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Last checked: Just now • Pulse is normal",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2ECC71),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun StatusRow(name: String, status: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (active) Color(0xFF2ECC71) else Color(0xFFFFCC00))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium)
        }
        Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TopicDetailHeader(topic: Topic) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(topic.color.copy(alpha = 0.1f))
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(topic.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(topic.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(topic.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Support & Documentation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TopicActionFooter(context: Context, topic: Topic) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Couldn't find what you were looking for?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { contactSupport(context, "${topic.title} Detailed Inquiry") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Email, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Contact ${topic.title} Specialist")
        }
    }
}

@Composable
fun NoResultsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Try different keywords or contact support",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WebSupportLink(uriHandler: UriHandler) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = { uriHandler.openUri("https://keeftalk.com/contact.html") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Still need help? Visit our Help Center")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun FaqItem(faq: Faq) {
    var expanded by remember { mutableStateOf(value = false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

fun contactSupport(context: Context, subject: String = "Support Request") {
    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName
        val deviceModel = Build.MODEL
        val osVersion = Build.VERSION.RELEASE
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            val mailUri = Uri.parse("mailto:support@keeftalk.com")
            data = mailUri
            putExtra(Intent.EXTRA_SUBJECT, "Keeftalk - $subject")
            putExtra(Intent.EXTRA_TEXT, "\n\n---\nSystem Information:\nApp: Keeftalk $appVersion\nDevice: $deviceModel\nOS: Android $osVersion")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Contact Support"))
    } catch (e: Exception) {
        // Fallback or log error
    }
}

data class FaqCategory(val title: String, val faqs: List<Faq>)
data class Faq(val question: String, val answer: String)

fun getFaqCategories(): List<FaqCategory> = listOf(
    FaqCategory(
        "Account",
        listOf(
            Faq("How do I change my username?", "You can update your username in the profile settings section. Note that usernames must be unique across the Keeftalk network."),
            Faq("Can I use Keeftalk on multiple devices?", "Yes. You can log in on multiple devices. Each device will independently verify its own encryption keys to maintain E2EE."),
            Faq("How is my identity protected during signup?", "Keeftalk uses independent authentication standards and minimal data collection to ensure your identity remains private and disconnected from third-party trackers."),
            Faq("What happens if I delete my account?", "Deleting your account is permanent. All your messages, profile data, and Vault files will be immediately and securely erased from our servers."),
            Faq("How do I verify my email address?", "After signing up, a verification link is sent to your email. Clicking this link activates your account and enables secure cloud features."),
            Faq("Can I link my phone number?", "Yes, you can add a phone number to your profile to make it easier for contacts to find you, though it is not mandatory for using the service."),
            Faq("How do I manage active sessions?", "Navigate to Settings -> Security -> Active Sessions to view and remotely terminate any active logins on other devices."),
            Faq("Is there a 'Parental Control' mode?", "Yes, Keeftalk includes parental controls in the account settings to manage usage limits and feature access for younger users."),
            Faq("How do I change my profile picture?", "Go to your profile view and tap the avatar icon. You can upload a new photo or use our privacy-focused avatar generator."),
            Faq("What data is stored in my public profile?", "Only your username and display name are public. Other details like phone number and email are hidden based on your privacy settings.")
        )
    ),
    FaqCategory(
        "Messaging",
        listOf(
            Faq("Are my messages really E2EE?", "Absolutely. All one-to-one chats use end-to-end encryption based on the Signal Protocol. Not even Keeftalk can read your messages."),
            Faq("How do I start a secure group chat?", "Tap the '+' icon, select 'New Group', and choose your contacts. All group communications are fully encrypted."),
            Faq("Can I send self-destructing messages?", "Yes, you can enable 'Disappearing Messages' in any chat's settings to have them automatically deleted after a set duration."),
            Faq("How do I know if a message was read?", "A single check means sent, double check means delivered, and colored checks mean the recipient has read the message."),
            Faq("Is media also encrypted?", "Yes. All photos, videos, and voice notes are encrypted using the same military-grade standards as your text messages."),
            Faq("Can I edit or delete sent messages?", "You can delete messages for everyone within a specific timeframe. Editing support is currently being rolled out for text messages."),
            Faq("How do I archive a conversation?", "Long-press a chat in the main list and select 'Archive'. This hides the chat from the main view without deleting the history."),
            Faq("What is the 'Message Request' folder?", "Messages from people who aren't in your contacts are placed in the Requests folder to protect you from spam."),
            Faq("Can I block someone?", "Yes. You can block any user from their profile or within the chat settings to stop receiving messages or calls from them."),
            Faq("How do I pin important chats?", "Long-press any chat and select the 'Pin' icon to keep it at the top of your conversation list for quick access.")
        )
    ),
    FaqCategory(
        "Vault",
        listOf(
            Faq("What is the Secure Vault?", "The Vault is a zero-knowledge encrypted storage space for your sensitive files. Encryption happens locally on your device."),
            Faq("How do I enable Biometric Unlock?", "In Vault settings, enable 'Biometric Unlock'. This requires your fingerprint or face ID every time you open the Vault view."),
            Faq("Is Vault storage limited?", "Vault storage depends on your current plan. Free accounts include a generous baseline, while premium users get expanded capacity."),
            Faq("Can I share files from the Vault?", "Yes, you can share Vault files directly into a chat. The file will be re-encrypted for the recipient during the transfer."),
            Faq("What if I forget my Vault password?", "Because we use zero-knowledge encryption, we cannot reset your password. Please ensure you have your recovery key backed up."),
            Faq("Are file thumbnails encrypted?", "Yes. Even the thumbnails for your photos and videos in the Vault are encrypted to ensure no data leaks via the file system."),
            Faq("Can I create folders in the Vault?", "Absolutely. You can organize your encrypted files into custom folders with unique icons and color coding."),
            Faq("How do I move files into the Vault?", "You can use the 'Import' feature within the Vault or select 'Move to Vault' from the attachment menu in any chat."),
            Faq("Does Vault sync across devices?", "Yes. Your encrypted Vault is synced to our secure cloud so you can access your files from any of your authorized devices."),
            Faq("Can I store notes inside the Vault?", "Yes, the Vault supports 'Secure Notes' which are markdown documents stored alongside your other encrypted files.")
        )
    ),
    FaqCategory(
        "Productivity",
        listOf(
            Faq("How do I use Unified Notes?", "Keeftalk Notes allows you to create, edit, and sync rich text documents across all your devices with full E2EE protection."),
            Faq("Does the editor support Markdown?", "Yes. The note editor supports standard Markdown syntax for formatting, including bold, italics, lists, and code blocks."),
            Faq("How do I connect my Gmail account?", "Navigate to the Email module and tap 'Add Account'. Select Gmail and follow the secure OAuth process."),
            Faq("Can I manage my calendar?", "Yes. Keeftalk includes a built-in calendar for managing events, reminders, and professional appointments securely."),
            Faq("How do I set task reminders?", "You can create reminders directly within the Agenda or by adding a reminder date to any of your Secure Notes."),
            Faq("Is my email content encrypted?", "Keeftalk provides a secure viewer for your emails. For supported providers, we offer enhanced privacy layers during the sync process."),
            Faq("Can I share a note with others?", "Yes, notes can be shared with other Keeftalk users. The shared note remains encrypted and is only accessible by the invited participants."),
            Faq("How do I use the Smart Agenda?", "The Smart Agenda automatically pulls in reminders from your notes and events from your calendar into a single, unified view."),
            Faq("Can I export notes to PDF?", "Yes. You can export any Secure Note to a professional PDF document directly from the note's action menu."),
            Faq("How do I search across tools?", "The global search feature (found in the main menu) can index your chats, notes, emails, and calendar events simultaneously.")
        )
    ),
    FaqCategory(
        "Connectivity",
        listOf(
            Faq("What is Local Discovery (NSD)?", "Local Discovery (Network Service Discovery) allows you to find other Keeftalk users on the same Wi-Fi for offline data exchange."),
            Faq("Do I need internet for LAN sync?", "No. Once your devices are authenticated, they can sync data directly over a local Wi-Fi network without using any internet data."),
            Faq("How do I find local users?", "Open the 'Discovery' screen and ensure 'Local Discovery' is toggled on. Nearby users on the same network will appear automatically."),
            Faq("Is LAN synchronization secure?", "Yes. All local data transfers are protected by the same end-to-end encryption keys used for our cloud-based communications."),
            Faq("Why can't I see local peers?", "Ensure both devices are on the same Wi-Fi network and that local network permissions are granted in your Android system settings."),
            Faq("Does Discovery drain battery?", "No. Keeftalk uses energy-efficient NSD protocols that only active when the Discovery screen is open or during scheduled syncs."),
            Faq("Can I transfer large files via LAN?", "Yes. Local transfers are significantly faster than cloud sync and are ideal for sharing large videos or documents with nearby peers."),
            Faq("How do I hide from local networks?", "You can disable your local visibility at any time in Settings -> Privacy -> Local Discovery."),
            Faq("Does LAN sync work with desktop?", "Yes. Local Discovery is cross-platform and allows you to sync data between your mobile device and the Keeftalk desktop client."),
            Faq("What protocols are used for LAN?", "We utilize standard DNS-SD and mDNS protocols (NSD) for discovery, followed by secure peer-to-peer TCP streams for data transfer.")
        )
    ),
    FaqCategory(
        "Security",
        listOf(
            Faq("What is an Account Key (AEK)?", "The Account Encryption Key is a master key derived from your password. It is the root of all your device-specific encryption keys."),
            Faq("How does Keeftalk use Argon2id?", "We use Argon2id, the winner of the Password Hashing Competition, to derive your keys. This makes your account extremely resistant to brute-force attacks."),
            Faq("What is AES-256-GCM?", "This is the industry standard for authenticated encryption. It ensures that your data is both private and hasn't been tampered with."),
            Faq("Where are my keys stored?", "Keys are stored in the hardware-backed Android Keystore, which is isolated from the main operating system for maximum security."),
            Faq("How do I report a vulnerability?", "If you find a security issue, please contact our security team immediately at security@keeftalk.com for a coordinated disclosure."),
            Faq("Can staff access my keys?", "No. Keeftalk follows a zero-knowledge architecture. We do not have access to your password or your encryption keys at any time."),
            Faq("What is Zero-Knowledge?", "This means we have zero knowledge of your data. Your device encrypts everything before it ever reaches our servers."),
            Faq("How do I audit my sessions?", "Navigate to the Security Activity section to see a detailed log of every login, key change, and sensitive action taken on your account."),
            Faq("What if my account is compromised?", "If you suspect issues, immediately use the 'Terminate All Other Sessions' feature and reset your password to generate new encryption keys."),
            Faq("Are there hardware-backed keys?", "Yes. On supported devices, Keeftalk leverages the Secure Element (SE) or TrustZone to ensure your keys never leave the hardware module.")
        )
    )
)

data class Topic(val title: String, val icon: ImageVector, val subtitle: String, val color: Color)
