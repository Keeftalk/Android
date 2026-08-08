package com.keeftalk.chat.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Locale

object TimestampSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder ?: throw Exception("Only JSON is supported")
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonPrimitive && element.isString -> {
                parseTimestamp(element.content)
            }
            element is JsonPrimitive -> {
                element.longOrNull ?: 0L
            }
            else -> 0L
        }
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString(formatTimestamp(value))
    }

    fun formatTimestamp(value: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        return sdf.format(java.util.Date(value))
    }

    fun parseTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return 0L
        
        // Handle numeric timestamps (seconds or milliseconds)
        isoString.toLongOrNull()?.let {
            return if (it < 10000000000L) it * 1000 else it
        }

        return try {
            // Normalize space to T and Z to offset
            val normalized = isoString.replace(" ", "T").replace("Z", "+00:00")
            
            val pattern = if (normalized.contains(".")) {
                val fractionalPart = normalized.substringAfter(".").substringBefore("+").substringBefore("-").substringBefore("Z")
                "yyyy-MM-dd'T'HH:mm:ss.${"S".repeat(fractionalPart.length.coerceAtMost(3))}XXX"
            } else {
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            }
            
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.parse(normalized)?.time ?: 0L
        } catch (e: Exception) {
            try {
                // Fallback for formats without T or different offsets
                val fallbackSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                fallbackSdf.parse(isoString)?.time ?: 0L
            } catch (e2: Exception) {
                android.util.Log.e("TimestampSerializer", "Failed to parse timestamp: $isoString", e2)
                0L
            }
        }
    }
}
