package tn.keeftalk.chat.extension.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.keeftalk.chat.extension.model.DeclarativeScreen

class DeclarativeScreenRegistry {
    private val screensMap = mutableMapOf<String, DeclarativeScreen>()
    private val _registeredScreens = MutableStateFlow<List<DeclarativeScreen>>(emptyList())
    val registeredScreens: StateFlow<List<DeclarativeScreen>> = _registeredScreens.asStateFlow()

    fun registerScreen(screen: DeclarativeScreen) {
        screensMap[screen.id] = screen
        _registeredScreens.value = screensMap.values.toList()
    }

    fun unregisterScreen(screenId: String) {
        screensMap.remove(screenId)
        _registeredScreens.value = screensMap.values.toList()
    }

    fun unregisterAllForExtension(extensionId: String) {
        val keysToRemove = screensMap.filter { it.value.extensionId == extensionId }.keys
        keysToRemove.forEach { screensMap.remove(it) }
        _registeredScreens.value = screensMap.values.toList()
    }

    fun getScreen(screenId: String): DeclarativeScreen? {
        return screensMap[screenId]
    }
}
