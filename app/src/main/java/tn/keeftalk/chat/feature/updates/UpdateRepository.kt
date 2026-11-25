package tn.keeftalk.chat.feature.updates

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateRepository(private val client: HttpClient) {

    private val releasesUrl = "https://api.github.com/repos/Keeftalk/Android/releases"

    suspend fun fetchReleases(): Result<List<GitHubRelease>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get(releasesUrl) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }
            if (response.status.isSuccess()) {
                val releases: List<GitHubRelease> = response.body()
                Result.success(releases)
            } else {
                Result.failure(Exception("GitHub API returned status: ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
