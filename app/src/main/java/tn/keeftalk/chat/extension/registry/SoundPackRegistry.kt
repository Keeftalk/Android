package tn.keeftalk.chat.extension.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.keeftalk.chat.extension.model.SoundPack

class SoundPackRegistry {
    private val soundPacksMap = mutableMapOf<String, SoundPack>()
    private val _activeSoundPack = MutableStateFlow<SoundPack?>(null)
    val activeSoundPack: StateFlow<SoundPack?> = _activeSoundPack.asStateFlow()

    fun registerSoundPack(pack: SoundPack) {
        soundPacksMap[pack.id] = pack
    }

    fun unregisterSoundPack(packId: String) {
        soundPacksMap.remove(packId)
        if (_activeSoundPack.value?.id == packId) {
            _activeSoundPack.value = null
        }
    }

    fun setActiveSoundPack(packId: String?): Boolean {
        if (packId == null) {
            _activeSoundPack.value = null
            return true
        }
        val pack = soundPacksMap[packId] ?: return false
        _activeSoundPack.value = pack
        return true
    }

    fun resolveSoundPath(soundId: String): String? {
        val active = _activeSoundPack.value ?: return null
        return active.sounds.firstOrNull { it.soundId.equals(soundId, ignoreCase = true) }?.relativePath
    }
}
