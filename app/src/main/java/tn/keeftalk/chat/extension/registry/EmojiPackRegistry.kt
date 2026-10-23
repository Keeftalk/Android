package tn.keeftalk.chat.extension.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.keeftalk.chat.extension.model.EmojiPack

class EmojiPackRegistry {
    private val packsMap = mutableMapOf<String, EmojiPack>()
    private val _installedEmojiPacks = MutableStateFlow<List<EmojiPack>>(emptyList())
    val installedEmojiPacks: StateFlow<List<EmojiPack>> = _installedEmojiPacks.asStateFlow()

    fun registerEmojiPack(pack: EmojiPack) {
        packsMap[pack.id] = pack
        _installedEmojiPacks.value = packsMap.values.toList()
    }

    fun unregisterEmojiPack(packId: String) {
        packsMap.remove(packId)
        _installedEmojiPacks.value = packsMap.values.toList()
    }

    fun getAllCustomEmojis(): List<tn.keeftalk.chat.extension.model.CustomEmojiItem> {
        return packsMap.values.flatMap { it.items }
    }
}
