# Implementation Plan - Backup & Restore Feature for Keeftalk Android

Implement a comprehensive, secure, and encrypted **Backup & Restore** feature for Keeftalk Android, accessible via **Settings → Help & Support → Quick Actions → Security & Privacy Center → Backup & Restore**.

## User Review Required

> [!IMPORTANT]
> - **Single Encrypted `.ktb` Format**: Backups are packaged as a ZIP container (`.ktb` extension) encrypted with AES-256-GCM using a master key derived via Argon2id from a user-established backup password.
> - **Account-Bound Restoration**: Each backup embeds a stable account identifier in the signed/authenticated manifest to prevent cross-account restoration.
> - **Selective Categories**: Users can choose among 12 data categories (Books, Habits, Todo, Browser, Feed, Finances, Calendar, Notes, Docs, Secrets, Calls, SMS).
> - **Scheduling**: WorkManager provides automatic backups (Every 12 hours, 24 hours, 3 days, or Disabled).

## Proposed Changes

### Domain & Data Models
#### [NEW] [BackupModels.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/domain/model/BackupModels.kt)
- Define `BackupManifest`, `BackupCategory`, `BackupConfig`, `BackupStats`, `BackupRestoreResult`.

### Cryptography & Engine
#### [NEW] [BackupCryptoEngine.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/security/crypto/BackupCryptoEngine.kt)
- Implement Argon2id key derivation, AES-256-GCM encryption/decryption of `.ktb` archives, and integrity verification.

#### [NEW] [BackupEngine.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/service/BackupEngine.kt)
- Implement collection of data across all 12 categories from Room DAOs, permission handling for Calls and SMS, attachment bundling, manifest generation, ZIP packaging, atomic file replacement, temporary file cleanup, and intelligent restoration with duplicate detection and transaction rollback.

#### [NEW] [BackupWorker.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/service/BackupWorker.kt)
- WorkManager periodic worker for automatic backups respecting schedule settings.

### UI & Navigation
#### [NEW] [SecurityPrivacyCenterScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/settings/SecurityPrivacyCenterScreen.kt)
- Screen for Security & Privacy Center accessible from Help & Support Quick Actions, linking to Backup & Restore, E2EE keys, and session security.

#### [NEW] [BackupRestoreScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/settings/BackupRestoreScreen.kt)
- Backup Dashboard UI supporting Light/Dark themes, displaying backup date, size, status, selected categories, next scheduled backup, and actions (**Back Up Now**, **Restore Backup**, **Backup Settings**).
- Password setup / change dialogs, category selection sheet, restore confirmation and conflict resolution dialogs.

#### [MODIFY] [HelpScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/settings/HelpScreen.kt)
- Update "Security & Privacy Center" card click handler to navigate to `SecurityPrivacyCenterScreen`.

#### [MODIFY] [MainActivity.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/MainActivity.kt)
- Add `SecurityPrivacyCenter` and `BackupRestore` to `AppScreen` and wire up their navigation composables.

### Testing
#### [NEW] [BackupEngineTest.kt](file:///home/m/StudioProjects/Android/app/src/test/java/tn/keeftalk/chat/data/service/BackupEngineTest.kt)
- Unit tests verifying encryption, archive validation, duplicate detection, and restoration integrity.

## Verification Plan

### Automated Tests
- Run unit tests with gradle: `gradle_build("app:testDebugUnitTest")`.
- Verify build success with `gradle_build("app:assembleDebug")`.

### Manual Verification
- Navigate via **Settings → Help & Support → Quick Actions → Security & Privacy Center → Backup & Restore**.
- Test password creation on first backup, selective backup categories, manual backup, simulated restore with duplicate checking, and automatic backup scheduling.
