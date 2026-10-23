package tn.keeftalk.chat.extension.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CustomFileHandler(
    val extensionId: String,
    val name: String,
    val supportedExtensions: List<String>, // e.g. ["kt", "java", "py", "js", "cpp", "json", "xml"]
    val viewerTitle: String = "Source Code Viewer"
)

class FileHandlerRegistry {
    private val handlersMap = mutableMapOf<String, CustomFileHandler>()
    private val _registeredHandlers = MutableStateFlow<List<CustomFileHandler>>(emptyList())
    val registeredHandlers: StateFlow<List<CustomFileHandler>> = _registeredHandlers.asStateFlow()

    fun registerHandler(handler: CustomFileHandler) {
        handlersMap[handler.extensionId] = handler
        _registeredHandlers.value = handlersMap.values.toList()
    }

    fun unregisterHandler(extensionId: String) {
        handlersMap.remove(extensionId)
        _registeredHandlers.value = handlersMap.values.toList()
    }

    fun canHandleExtension(fileExtension: String): Boolean {
        val ext = fileExtension.lowercase().removePrefix(".")
        return handlersMap.values.any { handler ->
            handler.supportedExtensions.any { it.equals(ext, ignoreCase = true) }
        }
    }
}
