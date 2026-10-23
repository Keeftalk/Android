# Task List - Backup & Restore Feature

- [ ] Create `BackupModels.kt` for manifest, categories, and settings
- [ ] Implement `BackupCryptoEngine.kt` for Argon2id and AES-256-GCM encryption/decryption
- [ ] Implement `BackupEngine.kt` for Room data export, permission checks, attachment bundling, ZIP creation, atomic replacement, and intelligent restoration
- [ ] Implement `BackupWorker.kt` for WorkManager scheduled automatic backups
- [ ] Implement `SecurityPrivacyCenterScreen.kt` and `BackupRestoreScreen.kt` UI with dashboard, password setup/change, selective category picker, and restore confirmation
- [ ] Update `HelpScreen.kt` and `MainActivity.kt` to wire up navigation
- [ ] Write unit tests in `BackupEngineTest.kt`
- [ ] Verify build and run tests
