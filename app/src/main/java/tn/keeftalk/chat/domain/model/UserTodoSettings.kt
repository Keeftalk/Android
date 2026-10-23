package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserTodoSettings(
    val defaultPriority: String = "MEDIUM",
    val showCompletedTasks: Boolean = true,
    val dueRemindersEnabled: Boolean = true,
    val weekStartDay: String = "MONDAY",
    val syncTodosAcrossDevices: Boolean = true
)
