package com.keeftalk.chat.data.repository

import android.content.Context
import com.keeftalk.chat.data.remote.LegalDocumentDto
import com.keeftalk.chat.data.remote.toDomain
import com.keeftalk.chat.domain.model.LegalDocument
import com.keeftalk.chat.domain.repository.LegalRepository
import com.keeftalk.chat.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class LegalRepositoryImpl(
    private val context: Context
) : LegalRepository {

    private val prefs = context.getSharedPreferences("legal_cache", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val CACHE_EXPIRATION = 24 * 60 * 60 * 1000L // 24 hours for persistent cache

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    override suspend fun getLegalDocument(type: String): Result<LegalDocument> = mutex.withLock {
        val now = System.currentTimeMillis()
        val cachedJson = prefs.getString("doc_$type", null)
        val lastUpdate = prefs.getLong("time_$type", 0L)

        // Try to fetch from network if cache is expired or missing
        if (cachedJson == null || now - lastUpdate > CACHE_EXPIRATION) {
            try {
                val supabase = getSupabase()
                val documentDto = supabase.postgrest["legal_documents"]
                    .select {
                        filter {
                            eq("type", type)
                            eq("is_active", true)
                        }
                    }
                    .decodeSingle<LegalDocumentDto>()

                val domainDoc = documentDto.toDomain()
                
                // Save to persistent cache
                prefs.edit().apply {
                    putString("doc_$type", Json.encodeToString(documentDto))
                    putLong("time_$type", now)
                    apply()
                }
                
                return Result.success(domainDoc)
            } catch (e: Exception) {
                // If fetch fails but we have a cached version, return it with isFromCache = true
                if (cachedJson != null) {
                    return try {
                        val cachedDto = Json.decodeFromString<LegalDocumentDto>(cachedJson)
                        Result.success(cachedDto.toDomain().copy(isFromCache = true))
                    } catch (_: Exception) {
                        Result.failure(e)
                    }
                }
                return Result.failure(e)
            }
        } else {
            // Return from cache
            return try {
                val cachedDto = Json.decodeFromString<LegalDocumentDto>(cachedJson)
                Result.success(cachedDto.toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
