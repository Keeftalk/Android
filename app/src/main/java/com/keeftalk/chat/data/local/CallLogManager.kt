package com.keeftalk.chat.data.local

import android.content.Context
import android.provider.CallLog
import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.cancel
import android.util.Log

import com.keeftalk.chat.data.local.dao.CallLogDao
import com.keeftalk.chat.data.local.entities.toDomain
import com.keeftalk.chat.data.local.entities.toEntity

private const val TAG = "CallLogManager"

enum class CallLogType {
    INCOMING, OUTGOING, MISSED, REJECTED
}

data class CallLogEntry(
    val id: String,
    val name: String?,
    val number: String,
    val timestamp: Long,
    val duration: Int,
    val type: CallLogType,
    val isKeeftalk: Boolean,
    val avatarUrl: String? = null,
    val peerId: String? = null,
    val isOnline: Boolean = false,
    val isVerified: Boolean = false,
)

class CallLogManager(
    private val context: Context,
    private val repository: ChatRepository,
    private val callLogDao: CallLogDao
) {
    fun getUnifiedCallLog(): Flow<List<CallLogEntry>> {
        // 1. Trigger background ingestion of GSM calls into local DB
        repositoryScope.launch {
            ingestGsmCalls()
        }

        // 2. Observe local DB
        return callLogDao.getAllCallLogs().map { entities ->
            val entries = entities.map { it.toDomain() }
            resolveIdentities(entries)
        }.flowOn(Dispatchers.Default)
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun ingestGsmCalls() = withContext(Dispatchers.IO) {
        val gsm = fetchGsmCallLog()
        if (gsm.isNotEmpty()) {
            callLogDao.insertCallLogs(gsm.map { it.toEntity(callType = "GSM") })
        }
        ingestKeeftalkCalls()
    }

    private suspend fun ingestKeeftalkCalls() {
        val chats = repository.getChats().first()
        val ktCalls = chats.asSequence()
            .filter { it.lastMessage?.contains("call", ignoreCase = true) == true }
            .map { chat ->
                val isMissed = chat.lastMessage?.contains("Missed", ignoreCase = true) == true
                CallLogEntry(
                    id = "kt_${chat.id}_${chat.lastTimestamp}",
                    name = chat.name,
                    number = "", 
                    timestamp = chat.lastTimestamp,
                    duration = 0,
                    type = if (isMissed) CallLogType.MISSED else CallLogType.INCOMING,
                    isKeeftalk = true,
                    avatarUrl = chat.avatarUrl,
                    peerId = chat.peerId,
                )
            }.toList()
        
        if (ktCalls.isNotEmpty()) {
            callLogDao.insertCallLogs(ktCalls.map { it.toEntity(callType = "VOICE") })
        }
    }

    private suspend fun resolveIdentities(entries: List<CallLogEntry>): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val searchableProfiles = repository.getSearchableProfiles().first()
        val localContacts = repository.getContacts().first()

        entries.map { entry ->
            if ((entry.isKeeftalk) && (entry.peerId != null)) {
                val profile = searchableProfiles.find { it.id == entry.peerId }
                entry.copy(
                    isOnline = (profile?.lastSeen ?: 0L) > System.currentTimeMillis() - 60_000,
                    isVerified = profile?.isVerified ?: false,
                    number = profile?.phone ?: entry.number
                )
            } else if (!entry.isKeeftalk && entry.number.isNotEmpty()) {
                // 1. Try matching with Keeftalk Profiles
                val profile = searchableProfiles.find { it.phone == entry.number }
                if (profile != null) {
                    entry.copy(
                        name = profile.fullName ?: profile.username,
                        avatarUrl = profile.avatarUrl,
                        peerId = profile.id,
                        isKeeftalk = true,
                        isOnline = profile.lastSeen > System.currentTimeMillis() - 60_000,
                        isVerified = profile.isVerified
                    )
                } else {
                    // 2. Try matching with Local Contacts
                    val localContact = localContacts.find { it.phone == entry.number }
                    if (localContact != null) {
                        entry.copy(
                            name = localContact.name,
                            avatarUrl = localContact.avatarUrl,
                            peerId = localContact.id,
                            isKeeftalk = localContact.isActive
                        )
                    } else {
                        entry
                    }
                }
            } else {
                entry
            }
        }
    }

    private suspend fun fetchGsmCallLog(): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallLogEntry>()
        try {
            if (context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return@withContext emptyList()
            }

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                var count = 0
                while (it.moveToNext() && count < 100) {
                    val number = it.getString(numberIndex)
                    val type = it.getInt(typeIndex)
                    val date = it.getLong(dateIndex)
                    val duration = it.getInt(durationIndex)
                    val name = it.getString(nameIndex)

                    val logType = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> CallLogType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallLogType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallLogType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallLogType.REJECTED
                        else -> CallLogType.INCOMING
                    }

                    entries.add(
                        CallLogEntry(
                            id = "gsm_$date",
                            name = name,
                            number = number ?: "",
                            timestamp = date,
                            duration = duration,
                            type = logType,
                            isKeeftalk = false
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching GSM call log", e)
        }
        entries
    }

    suspend fun deleteEntry(id: String) {
        callLogDao.deleteCallLog(id)
    }

    fun shutdown() {
        Log.i(TAG, "Shutting down CallLogManager")
        repositoryScope.cancel()
    }
}
