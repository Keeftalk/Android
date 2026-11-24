# Contributing to Keeftalk

First off, thank you for considering contributing to Keeftalk! We welcome contributions from developers worldwide to help make Keeftalk the leading open-source, privacy-first Super App for Android.

Please take a moment to review this document before submitting your contributions.

---

## 🛠️ Quick Start & Developer Setup

### 1. Fork & Clone the Repository

1. **Fork** the repository on GitHub by clicking the **Fork** button at [https://github.com/Keeftalk/Android](https://github.com/Keeftalk/Android).
2. **Clone** your fork to your local machine:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Android.git
   cd Android
   ```
3. **Add the upstream remote:**
   ```bash
   git remote add upstream https://github.com/Keeftalk/Android.git
   ```

### 2. Configure Android Studio

- **IDE:** Android Studio Ladybug (2024.2.1) or newer recommended.
- **JDK:** Java 17 or Java 21 configured as the Gradle JDK.
- **Android SDK:** API Level 37 (Android 16).

Open the project in Android Studio. Gradle sync will initialize automatically.

### 3. Local Configuration (Optional)

Keeftalk is built offline-first and can be compiled and executed immediately out of the box using standard Android debug keys and public client configuration defaults.

If you wish to configure custom backend endpoints or API keys:
- Copy `local.properties.example` to `local.properties` in the root directory.
- Copy `keystore.properties.example` to `keystore.properties` if you wish to sign release builds locally.
- Copy `.env.example` to `.env` if using environment variable configurations.

> ⚠️ **IMPORTANT:** Never commit private keys, secrets, or `keystore.properties` to version control.

---

## 🏗️ Build & Test Commands

Execute the following commands using the included Gradle wrapper:

```bash
# Verify project structure
./gradlew projects

# Run available unit tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug
```

All debug builds use the standard Android debug signing configuration automatically.

---

## 🌿 Creating a Feature Branch

Always create a dedicated topic branch for your changes:

```bash
# Keep main up to date with upstream
git checkout main
git pull upstream main

# Create a new feature branch
git checkout -b feature/my-amazing-feature
```

---

## 📝 Commit Message Guidelines

We follow clear, descriptive commit message conventions:

- Use the imperative mood in the subject line (e.g., `Add habit heatmap widget`, NOT `Added habit heatmap widget`).
- Limit the first line to 50–72 characters.
- Reference relevant issue numbers when applicable (e.g., `Fix #42: Resolve Compose recomposition in ChatScreen`).

### Format Example:
```text
feat(vault): add support for batch media export

Implement zero-knowledge ZIP archive export for selected Vault items.
Includes unit tests verifying encryption key preservation.

Closes #128
```

---

## 🔀 Submitting Pull Requests

Before submitting a Pull Request (PR):

1. **Verify your build:**
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```
2. **Check security hygiene:** Ensure no passwords, private credentials, or workstation-specific configuration paths were added.
3. **Push to your fork:**
   ```bash
   git push origin feature/my-amazing-feature
   ```
4. **Open a PR:** Go to [https://github.com/Keeftalk/Android](https://github.com/Keeftalk/Android) and open a Pull Request against `main`. Fill out the PR template completely.

---

## 🔍 Code Review Process

- All PRs are reviewed by core maintainers.
- Automated CI workflows will run `./gradlew test` and `./gradlew assembleDebug` on your PR.
- Feedback or request for changes will be communicated constructively through GitHub PR comments.
- Once approved and CI passes, your contribution will be merged!

Thank you for contributing to Keeftalk! 🚀
