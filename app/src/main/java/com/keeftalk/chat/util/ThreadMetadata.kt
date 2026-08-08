package com.keeftalk.chat.util

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class ThreadMetadata(
    val latestAuthorName: String? = null,
    val latestMentionIndices: List<Int>? = null,
    val reactionSummary: String? = null,
    val draftText: String? = null
) {
    fun toBlob(): ByteArray {
        return Json.encodeToString(this).toByteArray()
    }

    companion object {
        fun fromBlob(blob: ByteArray?): ThreadMetadata {
            if (blob == null) return ThreadMetadata()
            return try {
                Json.decodeFromString<ThreadMetadata>(String(blob))
            } catch (e: Exception) {
                ThreadMetadata()
            }
        }
    }
}
