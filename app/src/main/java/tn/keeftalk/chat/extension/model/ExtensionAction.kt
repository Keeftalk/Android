package tn.keeftalk.chat.extension.model

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    NAVIGATE,
    OPEN_URL,
    SET_PREFERENCE,
    SHOW_TOAST,
    COPY_TO_CLIPBOARD,
    TRIGGER_REFRESH,
    CLEAR_PREFERENCES,
    SCHEDULE_NOTIFICATION,
    CANCEL_NOTIFICATION
}

@Serializable
data class ExtensionAction(
    val type: ActionType,
    val targetUrl: String? = null,
    val key: String? = null,
    val value: String? = null,
    val message: String? = null,
    val screenId: String? = null
)
