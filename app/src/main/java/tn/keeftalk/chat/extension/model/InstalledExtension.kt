package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
data class InstalledExtension(
    val id: String,
    val type: ExtensionType,
    val name: String,
    val version: String,
    val description: String = "",
    val publisherName: String = "",
    val isPublisherTrusted: Boolean = false,
    val installedPath: String,
    val installedTimeMillis: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true,
    val sizeBytes: Long = 0L,
    val entry: String,
    val manifestJson: String
)
