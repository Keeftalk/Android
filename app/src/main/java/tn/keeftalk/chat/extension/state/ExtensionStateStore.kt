package tn.keeftalk.chat.extension.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExtensionStateStore {
    private val stateMaps = mutableMapOf<String, MutableStateFlow<Map<String, String>>>()

    fun getStateFlow(extensionId: String): StateFlow<Map<String, String>> {
        return stateMaps.getOrPut(extensionId) {
            MutableStateFlow(emptyMap())
        }.asStateFlow()
    }

    fun setState(extensionId: String, key: String, value: String) {
        val flow = stateMaps.getOrPut(extensionId) { MutableStateFlow(emptyMap()) }
        val current = flow.value.toMutableMap()
        current[key] = value
        flow.value = current.toMap()
    }

    fun getState(extensionId: String, key: String): String? {
        return stateMaps[extensionId]?.value?.get(key)
    }

    fun clearState(extensionId: String) {
        stateMaps[extensionId]?.value = emptyMap()
    }
}
