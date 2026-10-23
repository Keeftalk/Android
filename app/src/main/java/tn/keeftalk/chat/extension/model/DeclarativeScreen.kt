package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
enum class ScreenComponentType {
    HEADER,
    SECTION_TITLE,
    TEXT_BODY,
    KEY_VALUE,
    STAT,
    BUTTON,
    TEXT_INPUT,
    PROGRESS,
    BADGE_ROW,
    CARD_CONTAINER,
    DIVIDER
}

@Serializable
data class ScreenComponent(
    val type: ScreenComponentType,
    val label: String = "",
    val value: String = "",
    val subtext: String = "",
    val placeholder: String = "",
    val key: String? = null, // Storage key for inputs/values
    val progressValue: Float = 0f,
    val maxProgressValue: Float = 100f,
    val badges: List<String> = emptyList(),
    val action: ExtensionAction? = null,
    val children: List<ScreenComponent> = emptyList()
)

@Serializable
data class DeclarativeScreen(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val extensionId: String = "",
    val components: List<ScreenComponent> = emptyList()
)
