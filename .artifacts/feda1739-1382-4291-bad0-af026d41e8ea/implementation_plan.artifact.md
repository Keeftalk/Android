# Implementation Plan - Fix Vault Issues and Add Refresh

Address issues with Vault thumbnails and sharing, and add manual/auto refresh capabilities.

## User Review Required

> [!IMPORTANT]
> The "Bucket not found" error suggests a configuration issue in Supabase or an incorrect download strategy. I will switch to using `downloadAuthenticated` by default for all Vault-related files to ensure maximum compatibility with private buckets.

## Proposed Changes

### [Component] Vault UI & Logic

#### [MODIFY] [VaultScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultScreen.kt)
- Wrap the main content in `PullToRefreshBox`.
- Trigger `viewModel.refresh()` on swipe.

#### [MODIFY] [VaultViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultViewModel.kt)
- Add `refresh()` method to trigger `repository.sync()`.
- Implement a periodic sync (auto fetch) every 5 minutes while the screen is active.

---

### [Component] Storage & Data

#### [MODIFY] [FileRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/FileRepositoryImpl.kt)
- Refactor `ensureMediaLocal` to use `downloadAuthenticated` first, especially for files where the URL indicates they are in a user-specific folder.
- Improve error logging to diagnose "Object not found" issues.

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- Ensure `shareVaultFileToChat` handles failures gracefully and provides feedback if decryption fails.

## Verification Plan

### Manual Verification
- **Vault Refresh**:
    - Swipe down on the Vault screen and verify the refresh indicator appears and data is re-synced.
    - Wait 5 minutes and verify "isSyncing" status or logs show auto-fetch activity.
- **Thumbnails & Decryption**:
    - Verify images and videos downloaded from the cloud now correctly decrypt and show thumbnails.
- **Sharing**:
    - Share a cloud-only Vault item to a chat and verify it appears as a standard media message.
