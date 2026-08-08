# Implementation Plan - PART 4: Full Cloud Sync & Final Hardening

This final part implements the complete synchronization logic, advances group key management with epochs, and performs a final security hardening of the entire system.

## Proposed Changes

### [Component: Security - Group Epochs]

#### [MODIFY] [ConversationKeyManager.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/security/crypto/ConversationKeyManager.kt)
- Add `epoch` to `ConversationKeyEntity` and `createKey`.
- Implement `rotateKey(chatId: String)` to generate a new random PCK and increment the epoch.
- Update `getOrLoadKey` to support fetching the latest epoch from the cloud.

---

### [Component: Full Synchronization]

#### [MODIFY] [SecureBackupManager.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/security/crypto/SecureBackupManager.kt)
- Implement `syncAllMetadata()`:
    - 1. Recover AEK (Already done in Part 2).
    - 2. Sync Conversation Keys.
    - 3. Trigger `ChatRepository.syncChats()` and `syncFullChatHistory()`.
    - 4. Trigger `VaultRepository.sync()`.
    - 5. Trigger `NoteRepository.syncNotes()`.
    - 6. Trigger `CalendarRepository.syncCalendar()`.
- Ensure all repository `sync` methods correctly populate the local Room database with encrypted metadata.

#### [MODIFY] [VaultRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/VaultRepositoryImpl.kt)
- Implement the `sync()` method to fetch `vault_items` from Supabase and insert into local Room.

---

### [Component: Hardening & Cleanup]

#### [MODIFY] [AuthRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/AuthRepositoryImpl.kt)
- **Hardened Logout**:
    - Clear AEK from `KeyManager`.
    - Clear PCK cache from `ConversationKeyManager`.
    - Delete all files in `cacheDir` and `filesDir/media` to remove decrypted remnants.

#### [MODIFY] [CryptoManager.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/security/crypto/CryptoManager.kt)
- Remove any remaining traces of `deriveDeterministicKey` or chatId-based secrets.

---

### [Component: Verification & Audit]

#### [NEW] [FullRecoverySimulationTest.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/test/java/com/keeftalk/chat/security/crypto/FullRecoverySimulationTest.kt)
- Simulate a complete "New Device" flow:
    - User creates data on "Device A".
    - App data is cleared.
    - User logs in with password on "Device B".
    - Verify AEK is recovered, PCKs are recovered, and a Note/Vault item is successfully decrypted.

## Verification Plan

### Automated Tests
- Run `FullRecoverySimulationTest`.
- Run all existing `SecurityArchitectureTest` from Parts 2 & 3.

### Manual Verification
1.  **New Device Flow**:
    - Sign up.
    - Create a Secure Note and upload a Vault file.
    - Clear app data (Settings -> Apps -> Keeftalk -> Clear Data).
    - Log in with same email/password.
    - Verify Note content and Vault filename are decrypted correctly.
2.  **Group Rotation**:
    - Create a group.
    - Send a message.
    - (Simulation) Trigger key rotation.
    - Verify subsequent messages use the new epoch.
3.  **Database Audit**:
    - Inspect Supabase dashboard.
    - Verify `files` table contains NO plaintext `mediaKey`.
    - Verify `messages` table contains ONLY `[Encrypted]` placeholders or ciphertext.
