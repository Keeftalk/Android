package com.keeftalk.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactDto(
    val id: String? = null,
    @SerialName("owner_id")
    val ownerId: String? = null,
    val name: String,
    val username: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("secondary_phone")
    val secondaryPhone: String? = null,
    @SerialName("secondary_phone_label")
    val secondaryPhoneLabel: String? = null,
    @SerialName("tertiary_phone")
    val tertiaryPhone: String? = null,
    @SerialName("tertiary_phone_label")
    val tertiaryPhoneLabel: String? = null,
    val birthday: Long? = null,
    @SerialName("calling_card")
    val callingCard: String? = null,
    @SerialName("is_keeftalk_user")
    val isKeeftalkUser: Boolean = false
)
