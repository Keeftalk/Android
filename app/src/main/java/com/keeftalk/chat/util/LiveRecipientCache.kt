package com.keeftalk.chat.util

import com.keeftalk.chat.domain.model.User
import java.util.concurrent.ConcurrentHashMap

/**
 * High-speed in-memory cache for resolved recipients.
 * Eliminates database hits during list scrolling.
 */
object LiveRecipientCache {
    private val cache = ConcurrentHashMap<String, User>()

    /**
     * Warms the cache by loading the top recipients.
     * Should be called in Tier 3 (Deferred Startup).
     */
    suspend fun warm(recipients: List<User>) {
        PerformanceProfiler.logEvent("LiveRecipientCache: Warming with ${recipients.size} recipients")
        recipients.forEach { user ->
            cache[user.id] = user
        }
    }

    fun get(userId: String): User? = cache[userId]

    fun put(user: User) {
        cache[user.id] = user
    }

    fun clear() {
        cache.clear()
    }
}
