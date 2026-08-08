package com.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserChatSettings(
    val userId: String,
    
    // Appearance
    val theme: String = "SYSTEM", // SYSTEM, LIGHT, DARK, AMOLED
    val wallpaperType: String = "COLOR", // COLOR, GRADIENT, BUILT_IN, GALLERY
    val wallpaperReference: String = "#F0F2F5",
    val bubbleStyle: String = "MODERN", // ROUNDED, MODERN, COMPACT
    val bubbleRadius: Int = 16,
    val chatDensity: String = "COMFORTABLE", // COMPACT, COMFORTABLE, SPACIOUS
    val animationsEnabled: Boolean = true,
    val emojiSize: Int = 24,
    
    // Text & Accessibility
    val fontSize: Int = 16,
    val fontScale: Float = 1.0f,
    val boldText: Boolean = false,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val linkPreviewSize: String = "NORMAL", // COMPACT, NORMAL
    
    // Media & Downloads
    val autoDownloadMobile: List<String> = listOf("PHOTO"), // PHOTO, VIDEO, AUDIO, DOCUMENT
    val autoDownloadWifi: List<String> = listOf("PHOTO", "VIDEO", "AUDIO", "DOCUMENT"),
    val autoDownloadRoaming: List<String> = emptyList(),
    val uploadPhotoQuality: String = "HIGH", // ORIGINAL, HIGH, MEDIUM, COMPRESSED
    val uploadVideoQuality: String = "STANDARD", // ORIGINAL, HD, STANDARD, COMPRESSED
    val autoplayGifs: Boolean = true,
    val autoplayVideos: Boolean = true,
    val autoplayVoiceNotes: Boolean = true,
    val saveToGallery: Boolean = false,
    
    // Chat Behavior
    val enterKeyBehavior: String = "NEW_LINE", // SEND, NEW_LINE
    val swipeLeftAction: String = "ARCHIVE", // ARCHIVE, DELETE, MUTE
    val swipeRightAction: String = "REPLY", // REPLY, MARK_UNREAD, NONE
    val doubleTapAction: String = "REACT", // REACT, REPLY, COPY, NONE
    val defaultReaction: String = "❤️",
    val linkPreviewsEnabled: Boolean = true,
    val autoTranslateMode: String = "DISABLED", // DISABLED, INCOMING, OUTGOING, BOTH
    val autoTranslateLanguage: String = "en"
)
