# Accurate Cloud Storage & Premium Analysis Plan

This plan implements a "very accurate" cloud storage calculation and a production-ready storage analysis system with real, actionable tips for the Keeftalk Vault.

## User Review Required

> [!IMPORTANT]
> **Storage Source of Truth**: Cloud usage will be calculated by summing the encrypted bytes of all objects (files + thumbnails) stored in Supabase for the current user. This is global for the account, but displayed in the Vault.

> [!NOTE]
> **Actionable Tips**: "Review" buttons will navigate the user to the specific tabs (e.g., Trash) or trigger system actions (e.g., Clearing Cache).

## Proposed Changes

### Domain Layer

#### [MODIFY] [Vault.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/model/Vault.kt)
*   Define `VAULT_STORAGE_LIMIT = 5L * 1024 * 1024 * 1024` (5GB).
*   Add `VaultStorageTip` data class and `StorageTipAction` enum.
*   Update `VaultStorageInfo` to include `cloudBytesUsed`, `cloudBytesLimit`, `trashBytesUsed`, and `localCacheBytesUsed`.

### Data Layer

#### [MODIFY] [FileDao.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/dao/FileDao.kt)
*   Add `getAccountCloudBytesFlow(userId: String): Flow<Long?>` to sum `file_size` and `thumbnail_size` for all `ACTIVE` files.

#### [MODIFY] [VaultDao.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/dao/VaultDao.kt)
*   Add `getTrashSizeFlow(): Flow<Long?>` to sum sizes of items in trash.

#### [MODIFY] [VaultRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/VaultRepositoryImpl.kt)
*   Implement `getStorageInfo()` using `combine` of cloud bytes, trash bytes, and a new `getLocalCacheSize()` helper.
*   Ensure missing metadata triggers a log warning and a one-time repair attempt during sync.

#### [MODIFY] [FileUploadManager.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/util/FileUploadManager.kt)
*   Ensure `fileSize` and `thumbnailSize` in `FileEntity` represent the **encrypted bytes** uploaded to the cloud, not plaintext.

### Presentation Layer

#### [MODIFY] [VaultViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultViewModel.kt)
*   Implement real-time tip generation in `observeData`.
*   Handle `StorageTipAction` (navigating to Trash, filtering large files, clearing cache).

#### [MODIFY] [VaultComponents.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultComponents.kt)
*   Update `VaultStorageCard` and `VaultStorageAnalyzer` to use precise byte values and the new limit constant.
*   Improve `formatVaultSize` to provide two decimal places for GB/MB for high precision.

## Verification Plan

### Automated Tests
*   **Precision Test**: Verify `SUM(bytes)` query against known file sizes.
*   **Trash Lifecycle**: Verify that moving a file to Trash does not change `cloudBytesUsed`, but permanent deletion does.
*   **Tip Logic**: Unit test `calculateTips` function with mock storage states.

### Manual Verification
1.  **Cloud Accuracy**: Check storage card after a large upload; verify it matches the file's encrypted size.
2.  **Trash Review**: Click "Review" on Trash tip -> verify navigation to Trash tab.
3.  **Large Files**: Check if files over 50MB trigger the recommendation.
4.  **No Double Counting**: Upload same file to Chat and Vault (deduplication) -> Verify cloud usage only increases once.
