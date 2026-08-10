package com.keeftalk.chat.util

import android.util.Log
import net.dankito.readability4j.Readability4J
import org.jsoup.Jsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedArticle(
    val title: String,
    val contentHtml: String,
    val author: String?,
    val heroImageUrl: String?,
    val excerpt: String?
)

object ArticleExtractor {
    private const val TAG = "ArticleExtractor"

    suspend fun extract(url: String, httpClient: OkHttpClient): ExtractedArticle? = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
            if (!response.isSuccessful) return@withContext null
            
            val html = response.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html, url)
            
            val readability4J = Readability4J(url, html)
            val article = readability4J.parse()
            
            if (article.content == null) return@withContext null

            // Clean up content further if needed with Jsoup
            val contentDoc = Jsoup.parse(article.content!!)
            
            // Remove some problematic elements that Readability might miss
            contentDoc.select("script, style, iframe, ads, .ads, .advertisement, .social-share").remove()

            // Extract hero image from OpenGraph or other meta tags
            val heroImage = doc.select("meta[property=og:image]").attr("content").ifEmpty {
                doc.select("meta[name=twitter:image]").attr("content").ifEmpty {
                    doc.select("link[rel=image_src]").attr("href")
                }
            }

            return@withContext ExtractedArticle(
                title = article.title ?: doc.title(),
                contentHtml = contentDoc.body().html(),
                author = article.byline,
                heroImageUrl = heroImage.takeIf { it.isNotEmpty() },
                excerpt = article.excerpt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract article from $url", e)
            null
        }
    }
}
