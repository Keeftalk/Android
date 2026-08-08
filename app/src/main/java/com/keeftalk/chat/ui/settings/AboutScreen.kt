package com.keeftalk.chat.ui.settings

import android.content.pm.PackageInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.ui.theme.LocalAppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: "1.0"

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            SettingsHeader(title = "About Keeftalk", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AboutHero(versionName = versionName)

            Spacer(modifier = Modifier.height(32.dp))

            SectionTitle(title = "Our Mission")
            MissionStatement()

            Spacer(modifier = Modifier.height(32.dp))

            SectionTitle(title = "Privacy Principles")
            PrivacyPrinciplesGrid()

            Spacer(modifier = Modifier.height(32.dp))

            SectionTitle(title = "Security Protocol")
            SecurityProtocolDetails()

            Spacer(modifier = Modifier.height(32.dp))

            SectionTitle(title = "App Capabilities")
            FeatureHighlightsRow()

            Spacer(modifier = Modifier.height(32.dp))

            SectionTitle(title = "Legal & Compliance")
            LegalCard(
                title = "Privacy Policy",
                icon = Icons.Default.VerifiedUser,
                summary = "Keeftalk is committed to protecting user privacy. Message content is not accessible to the company, ensuring absolute privacy for your digital communications.",
                fullUrl = "https://keeftalk.com/legal/privacy-full.html",
                uriHandler = uriHandler
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LegalCard(
                title = "Terms of Service",
                icon = Icons.Default.Gavel,
                summary = "Agreement overview, data privacy protections, legal jurisdiction, and eligibility requirements for using the Keeftalk platform.",
                fullUrl = "https://keeftalk.com/legal/terms-full.html",
                uriHandler = uriHandler
            )

            Spacer(modifier = Modifier.height(32.dp))

            SectionTitle(title = "Technical Foundations")
            TechFoundationsList(uriHandler = uriHandler)

            Spacer(modifier = Modifier.height(48.dp))

            AppFooter()
        }
    }
}

@Composable
fun AboutHero(versionName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "LogoAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "LogoScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ),
                    CircleShape
                )
                .padding(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                LocalAppIcons.current.chat,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Keeftalk",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "Version $versionName",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun MissionStatement() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Text(
            text = "To provide a secure, unified communication application that enables individuals to exchange messages, make calls, and manage personal data with a focus on privacy and security. Keeftalk is built on independent authentication standards to ensure your identity remains private.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
            lineHeight = 26.sp
        )
    }
}

@Composable
fun PrivacyPrinciplesGrid() {
    val principles = listOf(
        Principle("Secure Communication", "Cryptographic standards for private content.", Icons.Default.Lock),
        Principle("No Advertising Data", "We never collect data for profiling or ads.", Icons.Default.VisibilityOff),
        Principle("No Data Sale", "We do not trade your information with others.", Icons.Default.MonetizationOn),
        Principle("User Data Control", "You retain full control over your digital content.", Icons.Default.SettingsSuggest)
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        principles.chunked(2).forEach { rowPrinciples ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowPrinciples.forEach { principle ->
                    PrincipleCard(principle = principle, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PrincipleCard(principle: Principle, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                principle.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = principle.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = principle.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun LegalCard(
    title: String,
    icon: ImageVector,
    summary: String,
    fullUrl: String,
    uriHandler: UriHandler
) {
    var expanded by remember { mutableStateOf(value = false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { uriHandler.openUri(fullUrl) },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Read Full Document")
                    }
                }
            }
        }
    }
}

@Composable
fun TechFoundationsList(uriHandler: UriHandler) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val tech = listOf(
                "UI Framework" to "Jetpack Compose & Material 3",
                "Cloud Services" to "Firebase & Supabase SDKs",
                "Intelligence" to "ML Kit (Vision, Natural Language)",
                "Networking" to "Ktor, OkHttp & WebRTC",
                "Security" to "SQLCipher, Argon2kt & Security Crypto"
            )
            
            tech.forEachIndexed { index, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(pair.first, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(pair.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index < (tech.size - 1)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = { uriHandler.openUri("https://keeftalk.com/legal/licenses.html") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Open Source Licenses")
            }
        }
    }
}

@Composable
fun AppFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "© 2026 Keeftalk LLC",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Menzah 6, Tunis, Tunisia",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Premium Secure Experience",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SecurityProtocolDetails() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Military-Grade Encryption", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            val protocols = listOf(
                "Key Derivation" to "Argon2id (latest memory-hard standard)",
                "Data Encryption" to "AES-256-GCM authenticated encryption",
                "Key Management" to "Hardware-backed Android Keystore",
                "Communication" to "Signal Protocol based E2EE"
            )
            protocols.forEach { (label, detail) ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun FeatureHighlightsRow() {
    val features = listOf(
        Feature("Secure Vault", Icons.Default.Lock, "Encrypted storage"),
        Feature("Local Discovery", Icons.Default.Wifi, "Wi-Fi peer sync"),
        Feature("Unified Notes", Icons.Default.EditNote, "Rich markdown"),
        Feature("Secure Email", Icons.Default.Email, "Multi-provider")
    )
    
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(features) { feature ->
            Surface(
                modifier = Modifier.width(140.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(feature.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(feature.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(feature.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 10.sp)
                }
            }
        }
    }
}

data class Principle(val title: String, val description: String, val icon: ImageVector)
data class Feature(val title: String, val icon: ImageVector, val desc: String)
