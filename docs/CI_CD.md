# Keeftalk Android CI/CD Architecture & Pre-Release Guide

This document provides a comprehensive overview of the Keeftalk Android CI/CD pipeline, automated pre-release system, build optimizations, security practices, and repository settings.

---

## 🛠️ Pipeline Overview

The Keeftalk Android repository uses two distinct, optimized GitHub Actions workflows:

1. **PR Validation Workflow (`.github/workflows/android-ci.yml`)**
   - **Trigger:** Pull Requests targeting `main` or `master`.
   - **Purpose:** Validates proposed code changes quickly and safely without exposing production signing credentials or building unnecessary modules.
   - **Tasks Executed:** `./gradlew :app:testDebugUnitTest :app:assembleDebug`
   - **Artifacts:** Debug APKs and Unit Test reports (if failures occur).

2. **Main Branch Release & Pre-Release Publisher (`.github/workflows/android-release.yml`)**
   - **Trigger:** Pushes/Merges to `main`, daily scheduled cron (`0 0 * * *`), or manual `workflow_dispatch`.
   - **Purpose:** Compiles, verifies, and publishes signed, architecture-specific release APKs to GitHub Pre-Releases when publication criteria are met.
   - **Tasks Executed:** `./gradlew :app:testDebugUnitTest :app:assembleRelease`

---

## ⏱️ Build Performance Optimizations

Previous CI builds suffered from severe slowdowns due to unoptimized Gradle configuration and task duplication. The following optimizations are active:

- **Parallel Execution (`org.gradle.parallel=true`):** Enables parallel compilation of decoupled modules in `gradle.properties`.
- **Gradle Local Build Caching (`org.gradle.caching=true`):** Enables task-output caching across CI runs in combination with `gradle/actions/setup-gradle@v4`.
- **Configuration Caching (`org.gradle.configuration-cache=true`):** Skips script evaluation phases when build configurations remain unchanged.
- **Targeted Gradle Tasks:** Directs execution strictly to `:app:testDebugUnitTest :app:assembleDebug` (or `:app:assembleRelease`), skipping `:macrobenchmark` and unnecessary root-level tasks during normal CI.
- **Single Gradle Invocations:** Consolidates testing and assembling into a single Gradle daemon process per job.

---

## 🚀 5-Merge / 5-Day Pre-Release Publication Policy

A new GitHub Pre-Release is automatically generated and published when **either** of the following conditions is satisfied:

1. **5 Merges:** At least **5 successful merges/commits** have landed on `main` since the last published pre-release.
2. **5 Days:** At least **5 days (120 hours)** have elapsed since the last published pre-release.

### How State Persistence Works
The publication policy uses persistent Git history and the GitHub Releases API rather than ephemeral state files or extra commits:
- On every `main` build or daily scheduled run, `.github/scripts/release_helper.py` inspects `git tag -l` to identify the most recent release/pre-release tag.
- It calculates `git rev-list --count <last_tag>..HEAD` and the timestamp delta between `<last_tag>` and current time.
- If thresholds are met, the workflow builds release APKs, creates a new Git tag (e.g., `v1.2-pre.1`), and creates a GitHub Pre-Release marked as `--prerelease`.
- Because the new pre-release creates a tag on `main`, the merge counter automatically resets to 0 for subsequent commits.

### Changing Thresholds
To adjust the thresholds (e.g., to 3 merges or 7 days), modify `merge_count >= 5` or `days_elapsed >= 5` in `.github/scripts/release_helper.py`.

---

## 🔢 Release Versioning Strategy

- **Format:** `v<versionName>-pre.<N>` (Example: `v1.2-pre.1`, `v1.2-pre.2`, `v1.2-pre.3`).
- **Version Name & Code:** Extracted dynamically from `app/build.gradle.kts` (`versionName` and `versionCode`).
- **Monotonic Progression:** `<N>` increments sequentially for each pre-release under the current `versionName`. Existing tags are never overwritten or reused.
- **Pre-Release Flag:** All automated pre-releases are explicitly flagged with GitHub's `prerelease: true` setting and will never be marked as stable releases automatically.

---

## 📦 Architecture-Specific Release APKs

When `:app:assembleRelease` executes with ABI splits enabled, Gradle produces four separate, optimized APKs:

1. `app-arm64-v8a-release.apk` (ARM 64-bit devices)
2. `app-armeabi-v7a-release.apk` (ARM 32-bit legacy devices)
3. `app-x86_64-release.apk` (x86 64-bit emulators and devices)
4. `app-x86-release.apk` (x86 32-bit emulators)

Each pre-release download section lists all four architecture APKs under:
`https://github.com/Keeftalk/Android/releases/download/<TAG>/<APK_NAME>`

Before publishing, the workflow verifies that all 4 APK files exist, are non-empty, match the `tn.keeftalk.chat` package ID, and pass `apksigner verify`.

---

## 🔐 Required GitHub Secrets & Repository Settings

### Required Secrets
Navigate to **Settings > Secrets and variables > Actions** in the GitHub repository and configure:

| Secret Name | Purpose / Description |
| :--- | :--- |
| `KEYSTORE_BASE64` | Base64-encoded string of the release `.jks` or `.keystore` file. |
| `KEYSTORE_PASSWORD` | Password for the release keystore store file. |
| `KEY_ALIAS` | Key alias name inside the keystore. |
| `KEY_PASSWORD` | Password for the specific key alias. |

### Converting Keystore to Base64
To prepare `KEYSTORE_BASE64`, convert your local release keystore file:
```bash
base64 -w 0 keeftalk-release.jks > keystore_base64.txt
```
Copy the contents of `keystore_base64.txt` into the `KEYSTORE_BASE64` repository secret.

### Permissions
Ensure the default `GITHUB_TOKEN` permissions under **Settings > Actions > General > Workflow permissions** are set to **Read and write permissions**, or rely on the explicitly declared `contents: write` permission in `android-release.yml`.

---

## 🕹️ Triggering a Manual Pre-Release

To publish a pre-release manually on demand:
1. Go to the **Actions** tab in GitHub.
2. Select **Keeftalk Android Release Publisher**.
3. Click **Run workflow**.
4. (Optional) Check **Force publication of pre-release regardless of thresholds**.
5. Click **Run workflow**.

---

## 🚢 Publishing a Stable Production Release

Automated workflows only generate **Pre-Releases**. To publish an official stable release:
1. Update `versionName` and `versionCode` in `app/build.gradle.kts`.
2. Tag the release commit manually: `git tag -a v1.2.0 -m "Release v1.2.0"`
3. Push the tag: `git push origin v1.2.0`
4. On GitHub, navigate to **Releases > Draft a new release**, select tag `v1.2.0`, attach the four release APKs from `app/build/outputs/apk/release/`, uncheck **Set as a pre-release**, and click **Publish release**.

---

## 🔍 Troubleshooting Failed Builds

- **Missing APK Error:** Verify `splits.abi` configuration in `app/build.gradle.kts`.
- **Signature Verification Warning/Failure:** Ensure `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` secrets match the release keystore exactly.
- **Configuration Cache Errors:** If a plugin change breaks configuration caching, test locally with `./gradlew :app:assembleRelease --configuration-cache`.
