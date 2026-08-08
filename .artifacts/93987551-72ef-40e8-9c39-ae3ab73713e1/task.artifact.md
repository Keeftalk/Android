# Task: Full Synchronization & Final Hardening (PART 4)

- [ ] **Group Epochs & Key Rotation**
    - [ ] Update `ConversationKeyEntity` in `KeeftalkDatabase` (already has version, let's check)
    - [ ] Implement `rotateKey` in `ConversationKeyManager.kt`
    - [ ] Support latest epoch loading in `getOrLoadKey`
- [ ] **Full Metadata Synchronization**
    - [ ] Implement `syncAllMetadata` in `SecureBackupManager.kt`
    - [ ] Implement `sync()` in `VaultRepositoryImpl.kt`
    - [ ] Ensure `NoteRepository.syncNotes()` handles encrypted metadata correctly
- [ ] **Hardening & Logout**
    - [ ] Refine `logout()` in `AuthRepositoryImpl.kt` to clear all keys/media
    - [ ] Remove insecure code patterns (search and replace)
- [ ] **Verification & Audit**
    - [ ] Create `FullRecoverySimulationTest.kt`
    - [ ] Perform final server-side compromise audit simulation
    - [ ] Run full build
