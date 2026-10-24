<p align="center">
  <img src="app/src/main/res/drawable/ic_app_logo.png" width="128" height="128" alt="Keeftalk Logo" />
</p>

<h1 align="center">Keeftalk</h1>

<p align="center">
  <b>The Privacy-First Zero-Knowledge Super App for Android</b><br/>
  Unified E2EE Communication • Native SMS/MMS • Secure Vault • Productivity Workspace • Astronomy Privacy Browser • 3D Social Graph
</p>

<p align="center">
  <a href="#-key-features"><img src="https://img.shields.io/badge/Version-0.9--Alpha-blue?style=for-the-badge&logo=android" alt="Version"></a>
  <a href="#-tech-stack--architecture"><img src="https://img.shields.io/badge/Target%20SDK-37%20%28Android%2016%20Preview%29-brightgreen?style=for-the-badge&logo=android" alt="Target SDK"></a>
  <a href="#-tech-stack--architecture"><img src="https://img.shields.io/badge/Min%20SDK-26%20%28Android%208.0%29-orange?style=for-the-badge&logo=android" alt="Min SDK"></a>
  <a href="#-cryptographic-architecture"><img src="https://img.shields.io/badge/Security-AEK%20%2B%20AES--256--GCM-red?style=for-the-badge&logo=1password" alt="Security"></a>
  <a href="#-tech-stack--architecture"><img src="https://img.shields.io/badge/Kotlin-2.0-purple?style=for-the-badge&logo=kotlin" alt="Kotlin"></a>
  <a href="#-tech-stack--architecture"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose"></a>
</p>

---

## 📖 Overview

**Keeftalk** is an all-in-one, privacy-first **Super App** designed to unify your digital life within a single end-to-end encrypted ecosystem. Developed with zero-knowledge architectural principles, Keeftalk ensures complete data sovereignty: no advertising tracking, no data monetization, and military-grade cryptography backed by hardware security.

Whether communicating via E2EE chat, bridging standard carrier SMS/MMS, hosting low-latency P2P WebRTC calls, securing confidential media in a hardware-locked vault, tracking habits, browsing the web behind a real-time astronomical sky engine, or mapping your social network in interactive 3D—Keeftalk delivers an uncompromised experience.

---

## ✨ Key Features ("The Cool Stuff")

### 💬 Unified Communications Engine
- **Cloud-Synchronized E2EE Messaging:** Zero-knowledge Signal-inspired encryption architecture utilizing Account Encryption Keys (AEK) and Per-Conversation Keys (PCK) with AES-256-GCM.
- **Native SMS/MMS Fallback:** Complete Android Telephony integration. Seamlessly read and send carrier SMS/MMS threads with dual-SIM support when offline or communicating with non-Keeftalk users.
- **WebRTC Encrypted HD Calls:** Peer-to-peer low-latency encrypted voice and video calls.
- **Rich Interactive Chat:** Voice notes, reaction pickers, thread replies, message expiration timers, media compression pipelines, and live typing indicators.

### 🔒 Hardware-Backed Zero-Knowledge Vault
- **SQLCipher + Android Keystore:** Isolated zero-knowledge storage vault protected by hardware-backed SQLCipher encryption.
- **Biometric & PIN Lock:** Multi-layer access control via Fingerprint, Face Unlock, or Master PIN.
- **Direct Cloud Import:** Connect and import documents and photos directly from Google Drive, Dropbox, and Google Photos into encrypted local storage.
- **Encrypted Portable Vault Backups:** Export and restore zero-knowledge `.vault.zip` archives with full key preservation.

### 📝 Productivity & Workspace Suite
- **Encrypted Rich Notes & Live Markdown:** WYSIWYG rich text formatting, full live Markdown previews, audio attachment notes, embedded images, and searchable tag systems.
- **PDF & Document Engine:** Built-in PDF reader featuring page thumbnails, text searching, annotation tools, and multi-format sheet viewers.
- **Habit Tracker & Analytics:** Custom schedules, streak tracking, interactive statistical charts, and progress completion heatmaps.
- **E-Book Reader & Gamified Reading XP:** EPUB and PDF book reader with custom reader themes, font scaling, and a gamified **Reading XP System** that rewards reading milestones and page turns!
- **Calendar & Task Manager:** Encrypted agenda scheduler with prioritized To-Do lists, due dates, and customizable reminders.

### 🌐 Privacy Browser & Astronomical Sky Engine
- **Privacy Web Browsing:** Integrated ad-blocker, tracker mitigation, custom user-agent toggles, tab manager, and glassmorphic address bar UI.
- **Live Real-time Astronomy Sky Engine:** The browser background renders real-time physics-calculated celestial simulation reflecting the exact position of the sun, moon phases, star fields, and atmospheric weather physics!

### 📰 Personal RSS Feed Reader
- Distraction-free article reader mode with topic filtering (Tech, Science, Finance, Culture) and custom RSS/Atom XML feed support.

### 📈 Market & Financial Tracker
- Live stock tickers, cryptocurrency prices, interactive candlestick and line charts, market capitalization rankings, and custom asset watchlists.

### 🕸️ Interactive 3D Social Connection Graph
- Real-time 3D network visualizer built on custom canvas engines, showcasing mutual connections, relationship degrees, and profile interaction paths in 3D space.

### 🛡️ Security, 2FA & Privacy Center
- **Two-Factor Authentication (2FA):** Built-in TOTP authenticator (Google Authenticator / Aegis compatible) and Email OTP verification.
- **Offline Emergency Recovery Keys:** Generate offline single-use recovery phrases for account recovery without server intervention.
- **Session Manager:** Inspect active device sessions and remotely revoke untrusted clients.
- **Sovereign Regional Endpoints:** User-selectable cloud data residency (EU, US, Asia).

### 🎨 Personalization & Digital Wellbeing
- **Material You Dynamic Theming:** Custom accent color palette pickers, dark/light modes, and customizable bottom navigation layouts.
- **Time on Keeftalk:** Native digital wellbeing screen-time dashboard with usage statistics.

---

## 🛡️ Cryptographic Architecture

Keeftalk operates on a **Zero-Knowledge Architecture**. The remote server infrastructure (Supabase) stores only opaque ciphertexts and never possesses master keys or unencrypted media.

```text
                    [USER MASTER PASSWORD]
                              │
                      [ARGON2ID KDF]
                              │
                    [KEY ENCRYPTION KEY (KEK)]
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
   [ACCOUNT KEY (AEK)]  [LOCAL DB KEY]     [VAULT KEYS]
   (Cloud-Synchronized) (Keystore-Backed)  (AES-256-GCM)
          │                   │                   │
     ┌────┴────┐              │                   │
   PCK 1     PCK 2        SQLCipher          Vault Items
 (Conv 1)   (Conv 2)    (Local Room DB)
```

- **Key Derivation Function:** Argon2id (Memory-hard parameters: 64MB memory, 3 iterations, 4 parallelism threads).
- **Symmetric Cipher:** AES-256-GCM for payload, attachment, and database field encryption.
- **Database Encryption:** SQLCipher with keys generated and secured inside the hardware-backed Android Keystore (`isInsideSecureHardware`).

---

## 🏗 Tech Stack & Architecture

Keeftalk is constructed adhering to modern Android engineering standards, using **Clean Architecture** and **MVVM / MVI** patterns alongside a **Multi-Tiered Startup Orchestration** engine for fast initial cold boots.

```text
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                         │
│   Jetpack Compose • Material 3 • Adaptive Layouts • Navigation 3 │
└────────────────────────────────┬────────────────────────────────┘
                                 │
┌────────────────────────────────┴────────────────────────────────┐
│                        DOMAIN / BUSINESS                        │
│         ViewModels • Coroutines & Flow • Security Crypto        │
└────────────────────────────────┬────────────────────────────────┘
                                 │
┌────────────────────────────────┴────────────────────────────────┐
│                           DATA LAYER                            │
│  Room + SQLCipher • Supabase Realtime/Auth • Telephony • Ktor   │
└─────────────────────────────────────────────────────────────────┘
```

| Layer | Technology |
| :--- | :--- |
| **Language & Platform** | 100% Kotlin, Target SDK 37 (Android 16), Min SDK 26 (Android 8.0) |
| **UI Toolkit** | Jetpack Compose, Material Design 3, Navigation 3, Compose Adaptive |
| **Database & Cache** | Room, SQLCipher (Hardware Encrypted), DataStore Preferences |
| **Networking & Backend** | Supabase (Auth, Realtime, Postgrest, Storage, Functions), Ktor Client, Retrofit |
| **Media & Audio/Video** | WebRTC (HD Voice/Video), Media3 ExoPlayer, CameraX |
| **AI & Machine Learning** | Google ML Kit (Text Recognition OCR, Barcode Scanning, Face Detection, Language ID, On-device Translation) |
| **Maps & Rendering** | MapLibre GL Android SDK, Custom Canvas 3D Graph Engine |
| **Image Loading** | Coil 3 & Glide with custom encrypted media fetchers |

---

## 📦 Project Structure

```text
tn.keeftalk.chat
├── account/          # Android Account Authenticator & Sync Manager
├── data/
│   ├── auth/         # Cloud & Session Auth Handlers
│   ├── billing/      # Play Billing & Subscription Handlers
│   ├── cloud/        # Google Drive, Google Photos, Dropbox Integrations
│   ├── local/        # Room Database, SQLCipher DAOs, Entities, Converters
│   ├── remote/       # DTOs, Supabase DataSources, Sync Services
│   ├── repository/   # Repository Implementations (E2EE Chat, Vault, Notes, Books, etc.)
│   └── service/      # Background Workers, Backup Engine, Media Pipelines
├── security/
│   └── crypto/       # CryptoManager, KeyManager, Argon2id, PCK Manager
└── ui/
    ├── auth/         # Login, Registration, 2FA, Email Verification
    ├── books/        # E-Book Reader & Gamified Reading XP System
    ├── browser/      # Privacy Browser & Astronomy Sky Background Engine
    ├── call/         # WebRTC Voice & Video Call Activities
    ├── docs/         # Multi-Sheet Document & PDF Viewer
    ├── feed/         # Personal RSS Reader
    ├── habits/       # Habit Tracker & Stats
    ├── market/       # Financial Tickers & Charts
    ├── profile/      # 3D Social Connection Graph
    ├── screens/      # E2EE Chat List, Chat Detail, Notes, Vault, SMS
    └── theme/        # Material 3 Color Schemes & Dynamics
```

---

## 🛠️ Building & Installation

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1+) or newer
- **JDK:** Java 17
- **Android SDK:** API Level 37 (Android 16 Preview)
- **Build System:** Gradle 8.x with Kotlin 2.0

### Steps to Build

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/keeftalk-android.git
   cd keeftalk-android
   ```

2. **Configure Local Environment:**
   Ensure `keystore.properties` exists in the root project directory if building signed release binaries:
   ```properties
   keyAlias=your_alias
   keyPassword=your_key_password
   storeFile=your_keystore.jks
   storePassword=your_store_password
   ```

3. **Build via Command Line:**
   ```bash
   # Assemble Debug APK
   ./gradlew :app:assembleDebug

   # Assemble Release APKS
   ./gradlew :app:assembleRelease

   # Run Unit Tests
   ./gradlew :app:testDebugUnitTest
   ```

---

## 🤝 Security & Vulnerability Reporting

Security is central to Keeftalk. If you discover a potential vulnerability or flaw in our cryptographic protocols, please do not open a public issue. Email security disclosures directly to `security@keeftalk.tn`.

---

## 📜 License

Copyright © 2025 **Keeftalk LLC**. All rights reserved.  
Licensed under the proprietary / open source licensing terms specified in the project repository.
