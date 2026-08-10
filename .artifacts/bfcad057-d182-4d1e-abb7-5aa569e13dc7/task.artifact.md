# Tasks - Accurate Cloud Storage & Premium Analysis

- [x] **Domain Layer**
    - [x] Update `Vault.kt`: Add constants, Tip models, and update `VaultStorageInfo`
- [x] **Data Layer**
    - [x] Update `FileDao.kt`: Add `getAccountCloudBytesFlow`
    - [x] Update `VaultDao.kt`: Add `getTrashSizeFlow`
    - [x] Update `VaultRepositoryImpl.kt`: Accurate `getStorageInfo` implementation
    - [x] Update `FileUploadManager.kt`: Ensure size fields use encrypted byte counts
- [x] **Presentation Layer**
    - [x] Update `VaultViewModel.kt`: Storage tip engine and action handling
    - [x] Update `VaultComponents.kt`: Precise UI rendering and Analysis sheet improvements
    - [x] Update `VaultScreen.kt`: Wiring actions to UI
- [x] **Verification**
    - [x] Verify byte-perfect storage calculation
    - [x] Verify actionable storage tips
