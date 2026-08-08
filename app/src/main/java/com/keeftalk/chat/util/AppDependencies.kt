package com.keeftalk.chat.util

import android.app.Application
import com.keeftalk.chat.data.local.KeeftalkDatabase
import com.keeftalk.chat.data.repository.ChatRepositoryImpl
import com.keeftalk.chat.domain.repository.ChatRepository
import com.keeftalk.chat.domain.model.ChatListItemUiModel
import com.keeftalk.chat.di.AppModule

/**
 * Global dependency graph for Keeftalk.
 * Holds critical data and services with lazy initialization.
 */
object AppDependencies {
    private lateinit var application: Application

    fun init(app: Application) {
        application = app
    }

    private var _database: KeeftalkDatabase? = null
    val database: KeeftalkDatabase
        get() = _database ?: AppModule.provideDatabase(application).also { _database = it }

    private var _chatRepository: ChatRepository? = null
    val chatRepository: ChatRepository
        get() = _chatRepository ?: AppModule.provideChatRepository(application).also { _chatRepository = it }

    private var _authRepository: com.keeftalk.chat.domain.repository.AuthRepository? = null
    val authRepository: com.keeftalk.chat.domain.repository.AuthRepository
        get() = _authRepository ?: AppModule.provideAuthRepository(application).also { _authRepository = it }

    private var _callLogManager: com.keeftalk.chat.data.local.CallLogManager? = null
    val callLogManager: com.keeftalk.chat.data.local.CallLogManager
        get() = _callLogManager ?: AppModule.provideCallLogManager(application).also { _callLogManager = it }

    fun reset() {
        _database = null
        _chatRepository = null
        _authRepository = null
        _callLogManager = null
    }
}
