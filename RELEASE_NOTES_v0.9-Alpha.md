# 🚀 Keeftalk v0.9-Alpha Release Notes & User Guide

Welcome to **Keeftalk v0.9-Alpha**, the all-in-one privacy-first "Super App" for Android. Keeftalk unifies end-to-end encrypted messaging, native SMS/MMS management, HD voice/video calls, a hardware-backed secure vault, productivity tools (Notes, Docs, Books, Habits, Calendar, Tasks), an environmental privacy browser, financial tracking, and a 3D social connection engine.

---

## 📌 Release Metadata
* **Version Name:** `0.9-Alpha`
* **Version Code:** `9`
* **Target Android SDK:** Android 16 (API Level 37)
* **Minimum Android SDK:** Android 8.0 (API Level 26)
* **Security & Architecture:** AES-256-GCM, Argon2id Key Derivation, Signal Protocol E2EE, SQLCipher Hardware Encryption, Jetpack Compose, Coroutines/Flow.

---

## ✨ Key Features & User Guides

### 1. 💬 Unified Communications (E2EE Chat, Carrier SMS/MMS & Calls)
* **End-to-End Encrypted Messaging:** Text, media, audio notes, and file sharing encrypted using zero-knowledge Signal Protocol and Argon2id keys.
* **Native SMS/MMS Client:** Seamless fallback to device cellular SMS/MMS with full thread sync and dual-SIM support.
* **WebRTC Encrypted Calls:** Low-latency P2P HD voice and video calling.
* **Rich Messaging:** Reactions, reply threads, message search, typing indicators, and message expiration timers.

#### 📖 How to Use:
1. **Start a Chat:** Tap the **Chat** tab on the main navigation bar. Click the `+` Floating Action Button (FAB) to start a new chat with any registered contact or phone number.
2. **Toggle SMS Mode:** Select a contact from your phone contacts. If E2EE is unavailable, tap the SMS badge in the top bar to route messages via standard SMS/MMS.
3. **Make Voice/Video Calls:** Inside any chat thread, tap the **Phone** or **Video Camera** icon in the top app bar.

---

### 2. 🔒 Secure Vault (Hardware-Backed Storage)
* **Zero-Knowledge Encrypted Vault:** Store confidential documents, photos, videos, and credentials in an isolated SQLCipher container protected by Android Keystore.
* **PIN & Biometric Lock:** Secure unlock using Device Fingerprint, Face Unlock, or Master PIN.
* **Cloud & Media Import:** Direct encrypted import from Dropbox, Google Drive, and Google Photos.
* **Vault Backup Pipeline:** Export encrypted `.vault.zip` backups to cloud or local storage.

#### 📖 How to Use:
1. **Open Vault:** Navigate to the **Vault** tab.
2. **Setup PIN/Biometrics:** Set up a 6-digit Master PIN or authenticate with Biometrics upon first launch.
3. **Import Content:** Tap **Cloud Import** or the `+` button inside the Vault to pick files from device storage or connected cloud services (Dropbox/Google Drive).
4. **Move to Vault:** Within any file list in Keeftalk, long-press a document or photo and select **Move to Vault**.

---

### 3. 📝 Productivity & Workspace Suite

#### A. Encrypted Rich Notes & Markdown Editor
* Rich text formatting, Markdown syntax support, embedded images, audio notes, and checklist items.
* Full-text local search and tag management.

#### B. Document & PDF Viewer
* Built-in PDF reader with page thumbnails, text searching, and annotation options.
* Multi-sheet document support.

#### C. Habit Tracker & Schedules
* Visual streak counters, customizable weekly/daily schedules, notification reminders, and detailed completion statistics.

#### D. E-Book Reader & Gamified Reading XP
* EPUB/PDF reader with customizable font sizing, dark reader themes, and an integrated **Reading XP** reward system that tracks reading achievements.

#### E. E2EE Calendar & Task Manager
* Encrypted agenda view for events, integrated To-Do lists with priority levels, due dates, and reminders.

#### 📖 How to Use:
1. **Notes & Docs:** Go to **Productivity -> Notes** or **Docs**. Tap `+` to create a new note or load a document. Toggle Markdown live preview with the top toolbar button.
2. **Habit Tracking:** Tap **Habits**. Click `+ Add Habit`, choose frequency (e.g., Daily, 3 times/week), and check off habits as you complete them during the day.
3. **Reading Books:** Tap **Books**, import EPUB/PDF files into your library, and tap any book to open the reader. Watch your Reading XP accumulate as you turn pages!

---

### 4. 🌐 Privacy Browser & Environmental Sky Engine
* **Privacy-First Web Browsing:** Ad-blocking, tracker prevention, custom user-agents, glassmorphic UI overlay, and tab management.
* **Dynamic Sky & Weather Engine:** Live-simulated background reflecting real-time astronomy (sun, moon phases, star fields) and ambient weather conditions.

#### 📖 How to Use:
1. Tap the **Browser** icon in the main navigation.
2. Type any URL or search term in the floating Glass Address Bar.
3. Swipe up on the address bar to view open tabs or adjust browser background opacity and atmospheric weather effects in Browser Settings.

---

### 5. 📰 Personal RSS Feed Reader
* Aggregates news, blogs, and media feeds with clean reader mode and topic categorization (Tech, Science, Finance, Culture).

#### 📖 How to Use:
1. Navigate to **Feed**.
2. Browse default topics or tap **Manage Feeds** to paste custom RSS/Atom XML URLs.
3. Tap any article to open it in full uncluttered reader view.

---

### 6. 📈 Market & Financial Tracker
* Real-time stock tickers, cryptocurrency prices, market capitalization rankings, and interactive candlestick/line charts.

#### 📖 How to Use:
1. Open **Market** from the drawer or navigation bar.
2. Use the search bar to look up symbols (e.g., `BTC`, `ETH`, `AAPL`).
3. Star assets to add them to your custom Watchlist on the top dashboard.

---

### 7. 🕸️ 3D Social Connection Graph
* Interactive 3D node network visualizer rendered via custom canvas engines showing mutual connections, profile interaction history, and relationship degrees.

#### 📖 How to Use:
1. Open your **Profile** page.
2. Select **Connection Graph**.
3. Use touch gestures to rotate, zoom, and explore nodes in the 3D space to inspect social paths.

---

### 8. 🛡️ Security, 2FA & Privacy Center
* **Two-Factor Authentication (2FA):** TOTP Authenticator app support and email OTP verification.
* **Security Recovery Keys:** Emergency recovery code generation for account restoration.
* **Regional Data Storage:** Choose where your encrypted data resides (EU, US, Asia regional endpoints).
* **Session Management:** View all active sessions and remotely terminate untrusted devices.

#### 📖 How to Use:
1. Open **Settings -> Security & Privacy Center**.
2. Tap **Two-Factor Authentication** to pair an authenticator app (e.g., Google Authenticator, Aegis).
3. Tap **Security Recovery** to generate and store emergency offline recovery keys.

---

### 9. 🎨 Personalization & Digital Wellbeing
* **App Customization:** Accent color choices, dynamic dark/light themes, icon customization, and bottom bar layout customization.
* **Time on Keeftalk:** Inspect detailed app usage statistics, daily screen-time summaries, and notification control options.

#### 📖 How to Use:
1. Go to **Settings -> App Customization** to pick custom primary/secondary accent colors.
2. Go to **Settings -> Time on Keeftalk** to monitor your digital usage statistics.

---

## 🛠️ Building & Installation

To compile and build the APK locally using Gradle:

```bash
# Build Debug APK
./gradlew :app:assembleDebug

# Build Release APKs (Requires Keystore)
./gradlew :app:assembleRelease
```

---

## 🔐 Security Standards & Compliance
* **Encryption Standards:** AES-256-GCM for storage & payload encryption, Argon2id for password/PIN hashing, ECDH P-256 / Signal protocol for key exchange.
* **Zero-Knowledge Architecture:** Server infrastructure has zero visibility into plaintext contents, notes, messages, vault items, or private keys.
