package com.keeftalk.chat.data.local

import android.util.LruCache
import com.keeftalk.chat.domain.model.User

/**
 * Memory-resident cache for the top frequent contacts to speed up UI binding
 * in the conversation list and message views.
 */
object RecipientCache {
    private const val MAX_SIZE = 100
    private val cache = LruCache<String, User>(MAX_SIZE)

    fun get(userId: String): User? {
        return cache.get(userId)
    }

    fun put(user: User) {
        cache.put(user.id, user)
    }

    fun putAll(users: List<User>) {
        users.forEach { put(it) }
    }

    fun clear() {
        cache.evictAll()
    }
}
