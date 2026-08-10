package com.keeftalk.chat.security.crypto

import java.util.Base64
import android.util.Log
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Manages the lifecycle of Per-Conversation Keys (PCK).
 * PCKs are cryptographically random 256-bit keys (or derived as fallback), protected by CPK.
 * Supports Group Epochs for membership change rotation.
 */
class ConversationKeyManager(
    private val context: android.content.Context,
    private val dao: ConversationKeyDao
) {
    private val TAG = "ConversationKeyManager"
    
    private suspend fun getSupabase() = AppModule.provideSupabaseClientAsync(context)
    
    // In-memory cache for decrypted conversation keys. 
    // Keyed by (userId, conversationId) to prevent cache poisoning across accounts.
    private val keyCache = ConcurrentHashMap<Pair<String, String>, SecretKey>()
    
    // Global mutex to prevent concurrent creation/fetch races for the same chat.
    private val loadMutex = Mutex()

    /**
     * Retrieves the decrypted conversation key for a given chat.
     * If not in cache, attempts to load from DB or Supabase.
     * If still missing, derives a deterministic shared key.
     */
    suspend fun getOrLoadKey(conversationId: String): SecretKey? {
        val userId = AppModule.provideUserPreferencesRepository(context).getUserIdFast() ?: return null
        
        // 1. Check cache (Lock-free fast path)
        keyCache[userId to conversationId]?.let { return it }

        // 2. Synchronized load to prevent redundant network calls or DB inserts
        return loadMutex.withLock {
            // Re-check cache under lock
            keyCache[userId to conversationId]?.let { return@withLock it }

            // 3. Load from DB
            var entity = dao.getKeyForConversation(conversationId, userId)
            
            // 4. If missing locally, try fetching from Supabase
            if (entity == null) {
                Log.d(TAG, "Key missing in local DB for $conversationId (user $userId), attempting cloud fetch...")
                entity = fetchKeyFromCloud(conversationId, userId)
                if (entity != null) {
                    dao.insertKey(entity)
                }
            }

            // 5. If STILL missing (New Chat / No shared key), derive a deterministic one
            if (entity == null) {
                Log.i(TAG, "No PCK found on cloud for $conversationId. Deriving deterministic fallback.")
                return@withLock createDeterministicKey(conversationId, userId)
            }

            // 6. Decrypt and cache
            try {
                if (!KeyManager.isInitialized()) {
                    KeyManager.restoreAEK(context)
                }
                
                val protectionKey = if (entity.version >= 2) {
                    KeyManager.getConversationProtectionKey()
                } else {
                    KeyManager.getMasterKey()
                }

                val encryptedObj = EncryptedObject(
                    version = entity.version,
                    keyId = "cpk",
                    iv = entity.nonce,
                    ciphertext = entity.encryptedKey
                )
                val decryptedBytes = StorageCryptoService.decrypt(encryptedObj, protectionKey)
                val key = SecretKeySpec(decryptedBytes, "AES")
                
                keyCache[userId to conversationId] = key
                return@withLock key
            } catch (e: Exception) {
                Log.e(TAG, "CRITICAL: Failed to decrypt PCK for $conversationId. Deriving fresh key.", e)
                return@withLock createDeterministicKey(conversationId, userId)
            }
        }
    }

    private suspend fun fetchKeyFromCloud(conversationId: String, userId: String): ConversationKeyEntity? = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val queryResult = try {
                supabase.postgrest["conversation_keys"]
                    .select {
                        filter {
                            eq("conversation_id", conversationId)
                            eq("user_id", userId)
                        }
                        order("epoch", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(1)
                    }
                    .decodeSingleOrNull<JsonObject>()
            } catch (e: io.github.jan.supabase.postgrest.exception.PostgrestRestException) {
                if (e.code == "42703" || e.description?.contains("user_id") == true) {
                    Log.w(TAG, "Remote table missing columns. Falling back to legacy select.")
                    supabase.postgrest["conversation_keys"]
                        .select {
                            filter { eq("conversation_id", conversationId) }
                            limit(1)
                        }
                        .decodeSingleOrNull<JsonObject>()
                } else throw e
            }

            if (queryResult != null) {
                val encKey = queryResult["encrypted_key"]?.jsonPrimitive?.content
                val nonce = queryResult["nonce"]?.jsonPrimitive?.content
                val version = queryResult["version"]?.jsonPrimitive?.intOrNull ?: 1
                val epoch = queryResult["epoch"]?.jsonPrimitive?.intOrNull ?: 1
                
                if (encKey != null && nonce != null) {
                    Log.i(TAG, "Successfully fetched PCK (Epoch $epoch) from cloud for $conversationId")
                    return@withContext ConversationKeyEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        userId = userId,
                        encryptedKey = encKey,
                        nonce = nonce,
                        version = version,
                        epoch = epoch
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch conversation key from cloud for $conversationId", e)
        }
        null
    }

    /**
     * Derives a deterministic shared key based on the Chat ID.
     * This acts as the shared secret when no out-of-band key exchange has occurred.
     */
    private suspend fun createDeterministicKey(conversationId: String, userId: String): SecretKey {
        // Normalize ID to ensure cross-user identity
        val normalizedId = conversationId.lowercase(Locale.US).trim()
        
        val salt = "keeftalk-shared-pck-v1".toByteArray()
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(SecretKeySpec(salt, "HmacSHA256"))
        val derivedBytes = hmac.doFinal(normalizedId.toByteArray(Charsets.UTF_8))
        val key = SecretKeySpec(derivedBytes, "AES")
        
        // Persist and upload so it acts like a normal PCK from now on
        saveAndSyncKey(conversationId, userId, key)
        
        keyCache[userId to conversationId] = key
        return key
    }

    private suspend fun saveAndSyncKey(conversationId: String, userId: String, key: SecretKey, epoch: Int = 1) {
        withContext(Dispatchers.IO) {
            try {
                if (!KeyManager.isInitialized()) {
                    KeyManager.restoreAEK(context)
                }
                val cpk = KeyManager.getConversationProtectionKey()
                val encrypted = StorageCryptoService.encrypt(key.encoded, cpk)
                
                val entity = ConversationKeyEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    userId = userId,
                    encryptedKey = encrypted.ciphertext,
                    nonce = encrypted.iv,
                    version = 2,
                    epoch = epoch
                )
                dao.insertKey(entity)
                
                getSupabase().postgrest["conversation_keys"].upsert(buildJsonObject {
                    put("conversation_id", conversationId)
                    put("user_id", userId)
                    put("encrypted_key", encrypted.ciphertext)
                    put("nonce", encrypted.iv)
                    put("version", 2)
                    put("epoch", epoch)
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync PCK to cloud", e)
            }
        }
    }

    /**
     * Generates a new random conversation key, encrypts it with CPK, and saves to DB.
     */
    suspend fun createKey(conversationId: String, epoch: Int = 1): SecretKey {
        val userId = AppModule.provideUserPreferencesRepository(context).getUserIdFast() 
            ?: throw IllegalStateException("Cannot create conversation key: User not logged in")

        return loadMutex.withLock {
            // Ensure we don't overwrite if another thread just created it
            keyCache[userId to conversationId]?.let { return@withLock it }

            val randomKey = StorageCryptoService.generateRandomKey()
            saveAndSyncKey(conversationId, userId, randomKey, epoch)
            keyCache[userId to conversationId] = randomKey
            randomKey
        }
    }

    /**
     * Rotates the conversation key (increments epoch).
     * Used when group membership changes.
     */
    suspend fun rotateKey(conversationId: String): SecretKey {
        val userId = AppModule.provideUserPreferencesRepository(context).getUserIdFast() ?: "anonymous"
        val currentEntity = dao.getKeyForConversation(conversationId, userId)
        val nextEpoch = (currentEntity?.epoch ?: 0) + 1
        Log.i(TAG, "Rotating PCK for $conversationId to Epoch $nextEpoch")
        return createKey(conversationId, nextEpoch)
    }

    /**
     * Saves an existing (recovered) encrypted key to the local database.
     */
    suspend fun saveRestoredKey(conversationId: String, encryptedKey: String, nonce: String, version: Int, epoch: Int = 1) {
        val userId = AppModule.provideUserPreferencesRepository(context).getUserIdFast() ?: return
        val entity = ConversationKeyEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            userId = userId,
            encryptedKey = encryptedKey,
            nonce = nonce,
            version = version,
            epoch = epoch
        )
        dao.insertKey(entity)
        // Clear cache to force reload of the new key
        keyCache.remove(userId to conversationId)
    }

    /**
     * Clears a cached key, forcing a re-fetch or fallback on next access.
     */
    fun invalidateKey(conversationId: String) {
        val userId = AppModule.provideUserPreferencesRepository(context).getUserIdFast() ?: return
        keyCache.remove(userId to conversationId)
    }

    /**
     * Clears all cached keys.
     */
    fun clearCache() {
        keyCache.clear()
    }
}
