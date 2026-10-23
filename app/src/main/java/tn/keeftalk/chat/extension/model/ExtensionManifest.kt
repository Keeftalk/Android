package tn.keeftalk.chat.extension.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublisherInfo(
    val name: String = "Unknown Publisher",
    val id: String = "",
    val website: String? = null,
    val isVerifiedPublisher: Boolean = false
)

@Serializable
data class AssetMeta(
    val path: String,
    val mimeType: String = "*/*",
    val sizeBytes: Long = 0L,
    val sha256Hex: String = ""
)

@Serializable
data class ExtensionListingMeta(
    val schemaVersion: Int = 1,
    val extensionId: String = "",
    val name: String = "",
    val type: String = "TOOL",
    val publisher: PublisherInfo = PublisherInfo(),
    val description: String = "",
    val version: String = "1.0.0",
    val releaseDate: String = "",
    val releaseNotes: String = "",
    val minimumAppVersion: String = "3.0.0",
    val ageRating: String = "4+",
    val languages: List<String> = emptyList(),
    val compatibleFeatures: List<String> = emptyList(),
    val cover: String = "assets/cover.webp",
    val screenshots: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val packageSizeBytes: Long = 0L
)

@Serializable
data class ExtensionManifest(
    @SerialName("schemaVersion") val schemaVersion: Int = 1,
    val id: String,
    val type: ExtensionType,
    val name: String,
    val version: String,
    val description: String = "",
    val publisher: PublisherInfo = PublisherInfo(),
    val license: String = "Unspecified",
    @SerialName("minAppVersion") val minAppVersion: Int = 1,
    @SerialName("maxAppVersion") val maxAppVersion: Int? = null,
    val capabilities: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val entry: String,
    val assets: List<AssetMeta> = emptyList(),
    @SerialName("previewImages") val previewImages: List<String> = emptyList(),
    val homepage: String? = null,
    val supportedFileExtensions: List<String> = emptyList(),
    val dictionaryLanguages: List<String> = emptyList(),
    val listingMeta: ExtensionListingMeta? = null
) {
    fun isValidPackageId(): Boolean {
        return id.matches(Regex("^[a-z0-9_]+(\\.[a-z0-9_]+)+$"))
    }
}
