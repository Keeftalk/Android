package tn.keeftalk.chat.extension.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.keeftalk.chat.extension.model.IconPack

class IconPackRegistry {
    private val iconPacksMap = mutableMapOf<String, IconPack>()
    private val _activeIconPack = MutableStateFlow<IconPack?>(null)
    val activeIconPack: StateFlow<IconPack?> = _activeIconPack.asStateFlow()

    fun registerIconPack(pack: IconPack) {
        iconPacksMap[pack.id] = pack
    }

    fun unregisterIconPack(packId: String) {
        iconPacksMap.remove(packId)
        if (_activeIconPack.value?.id == packId) {
            _activeIconPack.value = null
        }
    }

    fun setActiveIconPack(packId: String?): Boolean {
        if (packId == null) {
            _activeIconPack.value = null
            return true
        }
        val pack = iconPacksMap[packId] ?: return false
        _activeIconPack.value = pack
        return true
    }

    fun resolveIconPath(semanticKey: String): String? {
        val active = _activeIconPack.value ?: return null
        return active.iconOverrides.firstOrNull { it.semanticKey.equals(semanticKey, ignoreCase = true) }?.relativePath
    }
}
