package com.keeftalk.chat.security.crypto

import android.content.Context
import android.util.Log
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * Handles full synchronization and restoration of the user's encrypted world.
 * Coordinates fetching keys and triggering repository-level metadata sync.
 */
class SecureBackupManager(
    private val context: Context,
    private val conversationKeyManager: ConversationKeyManager
) {
    private val TAG = "SecureBackupManager"

    private suspend fun getSupabase() = AppModule.provideSupabaseClientAsync(context)

    /**
     * Complete synchronization flow for a new device.
     */
    suspend fun syncAllMetadata() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting FULL metadata synchronization...")
        
        // 1. Sync Conversation Keys
        restoreConversationKeys()

        // 2. Sync Chat History
        val chatRepo = AppModule.provideChatRepository(context)
        chatRepo.syncFullChatHistory()
        Log.d(TAG, "Chat history synced.")

        // 3. Sync Vault
        val vaultRepo = AppModule.provideVaultRepository(context)
        vaultRepo.sync()
        Log.d(TAG, "Vault metadata synced.")

        // 4. Sync Notes
        val noteRepo = AppModule.provideNoteRepository(context)
        noteRepo.syncNotes()
        Log.d(TAG, "Notes synced.")

        // 5. Sync Agenda
        val calendarRepo = AppModule.provideCalendarRepository(context)
        calendarRepo.syncCalendar()
        Log.d(TAG, "Calendar items synced.")

        Log.i(TAG, "FULL metadata synchronization COMPLETE.")
    }

    /**
     * Downloads and restores all conversation keys from the cloud.
     */
    suspend fun restoreConversationKeys() = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val keys = supabase.postgrest["conversation_keys"]
                .select()
                .decodeList<JsonObject>()
            
            keys.forEach { obj ->
                val convId = obj["conversation_id"]?.jsonPrimitive?.content ?: return@forEach
                val encKey = obj["encrypted_key"]?.jsonPrimitive?.content ?: return@forEach
                val nonce = obj["nonce"]?.jsonPrimitive?.content ?: return@forEach
                val version = obj["version"]?.jsonPrimitive?.int ?: 1
                val epoch = obj["epoch"]?.jsonPrimitive?.int ?: 1
                
                conversationKeyManager.saveRestoredKey(convId, encKey, nonce, version, epoch)
            }
            Log.i(TAG, "Successfully restored ${keys.size} conversation keys from cloud.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore conversation keys from cloud", e)
        }
    }
}
