package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
enum class ExtensionType {
    THEME,
    EMOJI_PACK,
    ICON_PACK,
    SOUND_PACK,
    FONT_PACK,
    DICTIONARY,
    FEED_CARD,
    FILE_HANDLER,
    DATA_PROVIDER,
    BROWSER_ADDON,
    FULL_SCREEN_APP,
    SETTINGS_PAGE,
    TOOL_WIDGET;

    fun toDisplayName(): String = when (this) {
        THEME -> "App Theme & Palette"
        EMOJI_PACK -> "Emoji & Sticker Pack"
        ICON_PACK -> "Semantic Icon Pack"
        SOUND_PACK -> "Ringtones & Sound Pack"
        FONT_PACK -> "Typography & Font Pack"
        DICTIONARY -> "Offline Dictionary & Glossary"
        FEED_CARD -> "Declarative Feed Card"
        FILE_HANDLER -> "File & Attachment Viewer"
        DATA_PROVIDER -> "Data Provider Engine"
        BROWSER_ADDON -> "Browser Extension / Add-on"
        FULL_SCREEN_APP -> "Full-Screen Extension Application"
        SETTINGS_PAGE -> "Extension Settings Interface"
        TOOL_WIDGET -> "Interactive Tool & Widget"
    }
}
