package com.keeftalk.chat.domain.model

import com.keeftalk.chat.util.AvatarUtils
import kotlinx.serialization.Serializable
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Serializable
@Parcelize
data class User(
    val id: String,
    val name: String,
    val username: String,
    val phone: String? = null,
    val secondaryPhone: String? = null,
    val secondaryPhoneLabel: String? = null,
    val tertiaryPhone: String? = null,
    val tertiaryPhoneLabel: String? = null,
    val email: String? = null,
    val birthday: Long? = null,
    val callingCard: String? = null,
    val avatarUrl: String?,
    val isActive: Boolean,
    val isOnline: Boolean = false,
    val lastSeen: Long,
    val isContact: Boolean,
    val isBlocked: Boolean = false
) : Parcelable {
    @kotlinx.parcelize.IgnoredOnParcel
    val initials: String by lazy { AvatarUtils.getInitials(name) }
}
