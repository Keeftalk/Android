package tn.keeftalk.chat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserBrowserSettings(
    // Weather & Time
    val unit: String = "C", // "C" or "F"
    val h24: Boolean = true, // 24h or 12h
    val fx: Boolean = true, // Weather effects
    val sound: Boolean = false, // Thunder sound
    val haptic: Boolean = true, // Haptics on lightning

    // Search & Homepage
    val searchEngine: String = "Google", // "Google", "DuckDuckGo", "Bing", "Ecosia", "Private", "Web"
    val homepage: String = "https://www.google.com",

    // Privacy & Security
    val trk: Boolean = true, // Block trackers
    val b3p: Boolean = true, // Block third-party cookies
    val javaScriptEnabled: Boolean = true,
    val blockPopups: Boolean = true,
    val doNotTrack: Boolean = true,
    val clearDataOnExit: Boolean = false,
    val saveHistory: Boolean = true,

    // Home Screen Sections
    val ql: Boolean = true, // Quick links
    val tabs: Boolean = true, // Open tabs
    val reads: Boolean = true, // Reading list

    // Display & Behavior
    val desktopMode: Boolean = false,
    val openLinksInApp: Boolean = true,

    // Storage & Downloads
    val downloadPath: String = "Downloads",
    val cacheEnabled: Boolean = true
)
