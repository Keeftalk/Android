package com.keeftalk.chat.security.crypto

import java.util.Base64
import android.util.Log
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Manages the lifecycle of Per-Conversation Keys (PCK).
 * PCKs are cryptographically random 256-bit keys, protected by the AEK-derived CPK.
 * Supports Group Epochs for membership change rotation.
 */
class ConversationKeyManager(
    private val context: android.content.Context,
    private val dao: ConversationKeyDao
) {
    private val TAG = "ConversationKeyManager"
    
    private suspend fun getSupabase() = AppModule.provideSupabaseClientAsync(context)
    
    // In-memory cache for decrypted conversation keys
    private val keyCache = ConcurrentHashMap<String, SecretKey>()

    /**
     * Retrieves the decrypted conversation key for a given chat.
     * If not in cache, attempts to load from DB and decrypt using CPK.
     */
    suspend fun getOrLoadKey(conversationId: String): SecretKey? {
        val userId = AppModule.provideUserPreferencesRepository(context).getUserIdFast() ?: return null
        
        // 1. Check cache
        keyCache[conversationId]?.let { return it }

        // 2. Load from DB
        var entity = dao.getKeyForConversation(conversationId, userId)
        
        // 3. If missing locally, try fetching from Supabase
        if (entity == null) {
            Log.d(TAG, "Key missing in local DB for $conversationId (user $userId), attempting cloud fetch...")
            entity = withContext(Dispatchers.IO) {
                try {
                    val supabase = getSupabase()
                    // Attempt to fetch with epoch ordering (Standard)
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
                        if (e.code == "42703" || e.description?.contains("user_id") == true) { // Undefined Column
                            Log.w(TAG, "Remote table missing columns. Falling back to legacy select.")
                            supabase.postgrest["conversation_keys"]
                                .select {
                                    filter {
                                        eq("conversation_id", conversationId)
                                    }
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
                            val newEntity = ConversationKeyEntity(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                userId = userId,
                                encryptedKey = encKey,
                                nonce = nonce,
                                version = version,
                                epoch = epoch
                            )
                            dao.insertKey(newEntity)
                            newEntity
                        } else null
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch conversation key from cloud for $conversationId", e)
                    null
                }
            }
        }

        // 4. Decrypt
        if (entity != null) {
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
                
                keyCache[conversationId] = key
                return key
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt PCK for $conversationId. Epoch=${entity.epoch}", e)
            }
        }

        return null
    }

    /**
     * Generates a new random conversation key, encrypts it with CPK, and saves to DB.
     */
    suspend fun createKey(conversationId: String, epoch: Int = 1): SecretKey {
        val userId = AppModule.provideUserPreferencesRepository(context).getUserIdFast() 
            ?: throw IllegalStateException("Cannot create conversation key: User not logged in")

        val randomKey = StorageCryptoService.generateRandomKey()
        keyCache[conversationId] = randomKey
        
        withContext(Dispatchers.IO) {
            try {
                if (!KeyManager.isInitialized()) {
                    KeyManager.restoreAEK(context)
                }
                val cpk = KeyManager.getConversationProtectionKey()
                val encrypted = StorageCryptoService.encrypt(randomKey.encoded, cpk)
                
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
        
        return randomKey
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
    }

    /**
     * Clears a cached key, forcing a re-fetch or fallback on next access.
     */
    fun invalidateKey(conversationId: String) {
        keyCache.remove(conversationId)
    }

    /**
     * Clears all cached keys.
     */
    fun clearCache() {
        keyCache.clear()
    }
}
