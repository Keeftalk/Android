package com.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.keeftalk.chat.data.local.entities.CallLogEntity
import com.keeftalk.chat.data.local.CallLogType

import com.keeftalk.chat.util.TimestampSerializer

@Serializable
data class CallLogDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("peer_id") val peerId: String? = null,
    @SerialName("peer_number") val peerNumber: String,
    @SerialName("peer_name") val peerName: String? = null,
    @SerialName("type") val type: String,
    @SerialName("is_keeftalk") val isKeeftalk: Boolean,
    @SerialName("call_type") val callType: String? = null,
    @SerialName("duration") val duration: Int,
    @SerialName("timestamp") val timestamp: String? = null,
)

fun CallLogEntity.toDto(userId: String) = CallLogDto(
    id = if (id.startsWith("gsm_") || id.startsWith("kt_")) null else id,
    userId = userId,
    peerId = peerId,
    peerNumber = number,
    peerName = name,
    type = type.name,
    isKeeftalk = isKeeftalk,
    callType = callType,
    duration = duration,
    timestamp = TimestampSerializer.formatTimestamp(timestamp)
)

fun CallLogDto.toEntity(syncStatus: Int = 1) = CallLogEntity(
    id = id ?: "remote_${TimestampSerializer.parseTimestamp(timestamp)}",
    peerId = peerId,
    number = peerNumber,
    name = peerName,
    timestamp = TimestampSerializer.parseTimestamp(timestamp),
    duration = duration,
    type = try { CallLogType.valueOf(type) } catch(e: Exception) { CallLogType.INCOMING },
    isKeeftalk = isKeeftalk,
    callType = callType,
    cloudSyncStatus = syncStatus
)
