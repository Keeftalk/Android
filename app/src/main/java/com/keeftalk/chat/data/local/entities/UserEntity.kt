package com.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.keeftalk.chat.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val phone: String? = null,
    val avatarUrl: String?,
    val isActive: Boolean = false,
    val lastSeen: Long = 0L,
    val isOnline: Boolean = false,
    val isContact: Boolean = false,
    val isBlocked: Boolean = false,
    // New fields for rich contacts (Cloud ready)
    val secondaryPhone: String? = null,
    val secondaryPhoneLabel: String? = null,
    val tertiaryPhone: String? = null,
    val tertiaryPhoneLabel: String? = null,
    val email: String? = null,
    val birthday: Long? = null,
    val callingCard: String? = null,
    val cloudSyncStatus: Int = 0 // 0: Local only, 1: Synced, 2: Pending Update
)

fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    username = username,
    phone = phone,
    avatarUrl = avatarUrl,
    isActive = isActive,
    isOnline = isOnline,
    lastSeen = lastSeen,
    isContact = isContact,
    isBlocked = isBlocked,
    secondaryPhone = secondaryPhone,
    secondaryPhoneLabel = secondaryPhoneLabel,
    tertiaryPhone = tertiaryPhone,
    tertiaryPhoneLabel = tertiaryPhoneLabel,
    email = email,
    birthday = birthday,
    callingCard = callingCard
)

fun User.toEntity() = UserEntity(
    id = id,
    name = name,
    username = username,
    phone = phone,
    avatarUrl = avatarUrl,
    isActive = isActive,
    isOnline = isOnline,
    lastSeen = lastSeen,
    isContact = isContact,
    isBlocked = isBlocked,
    secondaryPhone = secondaryPhone,
    secondaryPhoneLabel = secondaryPhoneLabel,
    tertiaryPhone = tertiaryPhone,
    tertiaryPhoneLabel = tertiaryPhoneLabel,
    email = email,
    birthday = birthday,
    callingCard = callingCard
)
