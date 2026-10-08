package tn.keeftalk.chat.domain.repository

import tn.keeftalk.chat.domain.model.CuratedFeed
import tn.keeftalk.chat.domain.model.FeedArticle
import tn.keeftalk.chat.domain.model.FeedSource
import tn.keeftalk.chat.domain.model.WeatherInfo
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getSources(): Flow<List<FeedSource>>
    fun getArticles(): Flow<List<FeedArticle>>
    fun getWeather(): Flow<WeatherInfo?>
    suspend fun addSource(url: String): Result<Unit>
    suspend fun addCuratedSources(feeds: List<CuratedFeed>)
    suspend fun removeSource(sourceId: String)
    suspend fun refreshAll()
    suspend fun refreshWeather()
    suspend fun getFullArticle(articleId: String): FeedArticle?
    suspend fun prefetchArticles(articleIds: List<String>)
    fun shutdown()
}
