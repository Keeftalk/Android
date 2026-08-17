package com.keeftalk.chat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BugReport(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val description: String,
    @SerialName("system_info")
    val systemInfo: SystemInfo,
    @SerialName("log_url")
    val logUrl: String? = null,
    val status: String = "open",
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class SystemInfo(
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("device_model")
    val deviceModel: String,
    @SerialName("os_version")
    val osVersion: String,
    @SerialName("sdk_version")
    val sdkVersion: Int
)
