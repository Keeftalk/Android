package tn.keeftalk.chat.extension.lifecycle

import tn.keeftalk.chat.extension.registry.*

class ExtensionRegistry {
    val themeRegistry = ThemeRegistry()
    val emojiPackRegistry = EmojiPackRegistry()
    val iconPackRegistry = IconPackRegistry()
    val soundPackRegistry = SoundPackRegistry()
    val dictionaryRegistry = DictionaryRegistry()
    val feedCardRegistry = FeedCardRegistry()
    val fileHandlerRegistry = FileHandlerRegistry()
    val declarativeScreenRegistry = DeclarativeScreenRegistry()
}
