package com.keeftalk.chat.data.sync

import com.keeftalk.chat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class IncrementalSyncManager(
    private val repository: ChatRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val lastSyncTimestamps = mutableMapOf<String, Long>()
    val syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    fun startSync(peerId: String) {
        scope.launch {
            syncStatus.value = SyncStatus.Syncing(peerId)
            try {
                val lastSync = lastSyncTimestamps[peerId] ?: 0L
                val newSyncTimestamp = repository.syncMessages(peerId, lastSync)
                lastSyncTimestamps[peerId] = newSyncTimestamp
                syncStatus.value = SyncStatus.Success(peerId)
            } catch (e: Exception) {
                syncStatus.value = SyncStatus.Error(peerId, e.message ?: "Unknown error")
            } finally {
                delay(2000)
                syncStatus.value = SyncStatus.Idle
            }
        }
    }

    sealed class SyncStatus {
        data object Idle : SyncStatus()
        data class Syncing(val peerId: String) : SyncStatus()
        data class Success(val peerId: String) : SyncStatus()
        data class Error(val peerId: String, val message: String) : SyncStatus()
    }
}
