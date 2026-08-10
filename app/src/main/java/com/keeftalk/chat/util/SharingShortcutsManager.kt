package com.keeftalk.chat.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.LocusIdCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.keeftalk.chat.domain.model.Chat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SharingShortcutsManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val CATEGORY_DIRECT_SHARE = "com.keeftalk.chat.category.DIRECT_SHARE"

    fun updateShortcuts(context: Context, chats: List<Chat>) {
        scope.launch {
            val topChats = chats
                .filter { it.peerId != null } // Only DMs for Direct Share row
                .sortedByDescending { it.lastTimestamp }
                .take(10)

            topChats.forEachIndexed { index, chat ->
                val shortcut = createShortcutInfo(context, chat, 10 - index)
                ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
                // Reporting usage helps the system rank the shortcut in the sharesheet
                ShortcutManagerCompat.reportShortcutUsed(context, chat.id)
            }
        }
    }

    private suspend fun createShortcutInfo(context: Context, chat: Chat, rank: Int): ShortcutInfoCompat {
        val label = chat.displayName
        val icon = loadIcon(context, chat.avatarUrl, label)

        val intent = Intent(context, com.keeftalk.chat.ui.share.ShareHandlerActivity::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra("chatId", chat.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val person = Person.Builder()
            .setName(label)
            .setKey(chat.id)
            .setIcon(icon)
            .setImportant(true)
            .build()

        return ShortcutInfoCompat.Builder(context, chat.id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setPerson(person)
            .setIcon(icon)
            .setIntent(intent)
            .setCategories(setOf(CATEGORY_DIRECT_SHARE))
            .setRank(rank)
            .setLongLived(true)
            .setLocusId(LocusIdCompat(chat.id))
            .build()
    }

    private suspend fun loadIcon(context: Context, avatarUrl: String?, name: String): IconCompat = withContext(Dispatchers.IO) {
        val avatarBitmap = NotificationAvatarHelper.getAvatarBitmap(context, avatarUrl, name)
        val styledBitmap = addKeeftalkBadge(context, avatarBitmap)
        IconCompat.createWithBitmap(styledBitmap)
    }

    private fun addKeeftalkBadge(context: Context, baseBitmap: Bitmap): Bitmap {
        val size = baseBitmap.width
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        
        // Draw base avatar
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)
        
        // Load Keeftalk Badge (ic_app_logo)
        val badgeSize = (size * 0.35f).toInt()
        val badgeMargin = (size * 0.02f).toInt()
        
        val badgeDrawable = ContextCompat.getDrawable(context, com.keeftalk.chat.R.drawable.ic_app_logo)
        if (badgeDrawable != null) {
            // Draw background circle for badge (white border/surface)
            val badgeBgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
            }
            val badgeX = size - badgeSize - badgeMargin
            val badgeY = size - badgeSize - badgeMargin
            
            canvas.drawCircle(
                badgeX + badgeSize / 2f,
                badgeY + badgeSize / 2f,
                badgeSize / 2f + 2f, // Slightly larger for border effect
                badgeBgPaint
            )

            // Draw primary color circle
            badgeBgPaint.color = android.graphics.Color.parseColor("#006684")
            canvas.drawCircle(
                badgeX + badgeSize / 2f,
                badgeY + badgeSize / 2f,
                badgeSize / 2f,
                badgeBgPaint
            )

            badgeDrawable.setBounds(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize)
            badgeDrawable.draw(canvas)
        }
        
        return output
    }
}
