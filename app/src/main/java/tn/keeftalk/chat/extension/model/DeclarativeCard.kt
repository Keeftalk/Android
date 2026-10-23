package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardComponentType {
    TITLE,
    LABEL,
    TEXT,
    IMAGE,
    NUMBER_STAT,
    PROGRESS_BAR,
    BADGE,
    BUTTON,
    DIVIDER
}

@Serializable
data class CardComponent(
    val type: CardComponentType,
    val text: String = "",
    val subtext: String = "",
    val value: Float = 0f,
    val maxValue: Float = 100f,
    val imageUrl: String? = null,
    val colorHex: String? = null,
    val badgeText: String? = null,
    val action: ExtensionAction? = null
)

@Serializable
data class DeclarativeCard(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: String = "GENERAL",
    val providerName: String = "Community Provider",
    val refreshIntervalMinutes: Int = 15,
    val components: List<CardComponent> = emptyList(),
    val actionUrl: String? = null,
    val primaryAction: ExtensionAction? = null
)
