package tn.keeftalk.chat.feature.updates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long = 0L,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)
