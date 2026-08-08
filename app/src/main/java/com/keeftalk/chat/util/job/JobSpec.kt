package com.keeftalk.chat.util.job

data class JobSpec(
    val id: String,
    val type: String,
    val data: String,
    val nextRunAttempt: Long,
    val isRunning: Boolean = false
)
