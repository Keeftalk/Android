package com.keeftalk.chat.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

data class AppIcons(
    val chat: ImageVector,
    val search: ImageVector,
    val bell: ImageVector,
    val more: ImageVector,
    val phone: ImageVector,
    val note: ImageVector,
    val back: ImageVector,
    val send: ImageVector,
    val mic: ImageVector,
    val emoji: ImageVector,
    val attach: ImageVector,
    val check: ImageVector,
    val user: ImageVector,
    val lock: ImageVector,
    val shield: ImageVector,
    val image: ImageVector,
    val font: ImageVector,
    val storage: ImageVector,
    val palette: ImageVector,
    val info: ImageVector,
    val help: ImageVector,
    val logout: ImageVector,
    val badge: ImageVector,
    val email: ImageVector,
    val calendar: ImageVector,
    val qrCode: ImageVector,
    val brain: ImageVector,
    val archive: ImageVector,
    val block: ImageVector,
    val messageCircle: ImageVector,
    val messageSquare: ImageVector,
    val export: ImageVector,
    val edit: ImageVector,
    val delete: ImageVector,
    val place: ImageVector,
    val call: ImageVector,
    val camera: ImageVector,
    val chevronRight: ImageVector,
    val checkCircle: ImageVector,
    val moon: ImageVector,
    val sun: ImageVector,
    val heart: ImageVector,
    val add: ImageVector,
    val pin: ImageVector,
    val mute: ImageVector,
    val unmute: ImageVector,
    val menu: ImageVector,
    val sparkles: ImageVector,
    val notes: ImageVector,
    val clock: ImageVector,
    val dialpad: ImageVector,
    val wallet: ImageVector,
    val close: ImageVector,
    val backspace: ImageVector,
    val callMissed: ImageVector,
    val callReceived: ImageVector,
    val callMade: ImageVector
)

fun Modifier.fabGradientIcon(): Modifier = this
    .graphicsLayer(alpha = 0.99f)
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(FabGradient, blendMode = BlendMode.SrcAtop)
        }
    }

val LocalAppIcons = staticCompositionLocalOf<AppIcons> {
    error("No AppIcons provided")
}

object IconProvider {
    fun getIconsForTheme(theme: AppTheme): AppIcons {
        return AppIcons(
            chat = Icons.AutoMirrored.Filled.Chat,
            search = Icons.Default.Search,
            bell = Icons.Default.Notifications,
            more = Icons.Default.MoreVert,
            phone = LucideIcons.Phone,
            note = Icons.AutoMirrored.Filled.Note,
            back = Icons.AutoMirrored.Filled.ArrowBack,
            send = Icons.AutoMirrored.Filled.Send,
            mic = Icons.Default.Mic,
            emoji = Icons.Default.EmojiEmotions,
            attach = Icons.Default.AttachFile,
            check = Icons.Default.Check,
            user = Icons.Default.Person,
            lock = Icons.Default.Lock,
            shield = Icons.Default.Shield,
            image = Icons.Default.Image,
            font = Icons.Default.FontDownload,
            storage = Icons.Default.Storage,
            palette = Icons.Default.Palette,
            info = Icons.Default.Info,
            help = Icons.AutoMirrored.Filled.Help,
            logout = Icons.AutoMirrored.Filled.ExitToApp,
            badge = Icons.Default.Badge,
            email = Icons.Default.Email,
            calendar = Icons.Default.CalendarToday,
            qrCode = Icons.Default.QrCode,
            brain = Icons.Default.Psychology,
            archive = Icons.Default.Archive,
            block = Icons.Default.Block,
            messageCircle = LucideIcons.MessageCircle,
            messageSquare = LucideIcons.MessageSquare,
            export = Icons.Default.FileDownload,
            edit = Icons.Default.Edit,
            delete = Icons.Default.Delete,
            place = Icons.Default.Place,
            call = Icons.Default.Call,
            camera = LucideIcons.Video,
            chevronRight = Icons.Default.ChevronRight,
            checkCircle = Icons.Default.CheckCircle,
            moon = Icons.Default.Brightness2,
            sun = Icons.Default.WbSunny,
            heart = Icons.Default.Favorite,
            add = Icons.Default.Add,
            pin = Icons.Default.PushPin,
            mute = Icons.Default.NotificationsOff,
            unmute = Icons.Default.Notifications,
            menu = Icons.Default.Menu,
            sparkles = Icons.Default.AutoAwesome,
            notes = Icons.AutoMirrored.Filled.Notes,
            clock = Icons.Default.AccessTime,
            dialpad = Icons.Default.Dialpad,
            wallet = Icons.Default.AccountBalanceWallet,
            close = Icons.Default.Close,
            backspace = Icons.AutoMirrored.Filled.Backspace,
            callMissed = Icons.AutoMirrored.Filled.CallMissed,
            callReceived = Icons.AutoMirrored.Filled.CallReceived,
            callMade = Icons.AutoMirrored.Filled.CallMade
        )
    }
}
