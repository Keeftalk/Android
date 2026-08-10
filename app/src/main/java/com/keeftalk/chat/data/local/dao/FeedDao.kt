package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.FeedArticleEntity
import com.keeftalk.chat.data.local.entities.FeedSourceEntity
import com.keeftalk.chat.data.local.entities.WeatherCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_sources")
    fun getAllSources(): Flow<List<FeedSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: FeedSourceEntity): Long

    @Update
    suspend fun updateSource(source: FeedSourceEntity)

    @Transaction
    suspend fun upsertSource(source: FeedSourceEntity) {
        val id = insertSource(source)
        if (id == -1L) {
            updateSource(source)
        }
    }

    @Delete
    suspend fun deleteSource(source: FeedSourceEntity)

    @Query("SELECT COUNT(*) FROM feed_articles")
    suspend fun getArticleCount(): Int

    @Query("SELECT * FROM feed_articles ORDER BY pubDate DESC")
    fun getAllArticlesRaw(): Flow<List<FeedArticleEntity>>

    @Query("""
        SELECT feed_articles.*, COALESCE(feed_sources.title, 'Unknown') as sourceName 
        FROM feed_articles 
        LEFT JOIN feed_sources ON feed_articles.sourceId = feed_sources.id 
        ORDER BY feed_articles.pubDate DESC
    """)
    fun getAllArticles(): Flow<List<FeedArticleWithSource>>

    @Query("SELECT * FROM feed_articles WHERE sourceId = :sourceId ORDER BY pubDate DESC")
    fun getArticlesBySource(sourceId: String): Flow<List<FeedArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<FeedArticleEntity>)

    @Query("DELETE FROM feed_articles WHERE sourceId = :sourceId")
    suspend fun deleteArticlesBySource(sourceId: String)

    @Query("""
        SELECT feed_articles.*, COALESCE(feed_sources.title, 'Unknown') as sourceName 
        FROM feed_articles 
        LEFT JOIN feed_sources ON feed_articles.sourceId = feed_sources.id 
        WHERE feed_articles.id = :articleId
    """)
    fun getArticleById(articleId: String): Flow<FeedArticleWithSource?>

    @Query("SELECT * FROM weather_cache WHERE id = 'current'")
    fun getWeatherCache(): Flow<WeatherCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWeatherCache(weather: WeatherCacheEntity)
}

data class FeedArticleWithSource(
    @Embedded val article: FeedArticleEntity,
    val sourceName: String
)
