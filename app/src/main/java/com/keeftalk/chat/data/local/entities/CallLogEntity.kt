package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.keeftalk.chat.data.local.CallLogEntry
import com.keeftalk.chat.data.local.CallLogType

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val peerId: String?,
    val number: String,
    val name: String?,
    val timestamp: Long,
    val duration: Int,
    val type: CallLogType,
    val isKeeftalk: Boolean,
    val callType: String?, // VOICE, VIDEO, GSM
    val avatarUrl: String? = null,
    val cloudSyncStatus: Int = 0 // 0: Local only, 1: Synced, 2: Pending Update
)

fun CallLogEntity.toDomain() = CallLogEntry(
    id = id,
    name = name,
    number = number,
    timestamp = timestamp,
    duration = duration,
    type = type,
    isKeeftalk = isKeeftalk,
    avatarUrl = avatarUrl,
    peerId = peerId
)

fun CallLogEntry.toEntity(callType: String? = null, cloudSyncStatus: Int = 0) = CallLogEntity(
    id = id,
    peerId = peerId,
    number = number,
    name = name,
    timestamp = timestamp,
    duration = duration,
    type = type,
    isKeeftalk = isKeeftalk,
    callType = callType ?: if (isKeeftalk) "VOICE" else "GSM",
    avatarUrl = avatarUrl,
    cloudSyncStatus = cloudSyncStatus
)
