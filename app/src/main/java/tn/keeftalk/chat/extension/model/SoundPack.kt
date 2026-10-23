package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
data class SoundItem(
    val soundId: String, // e.g. "incoming_call", "message_received"
    val title: String,
    val relativePath: String, // e.g. "assets/sounds/chime.mp3"
    val durationMs: Long = 0L
)

@Serializable
data class SoundPack(
    val id: String,
    val name: String,
    val sounds: List<SoundItem> = emptyList()
)
