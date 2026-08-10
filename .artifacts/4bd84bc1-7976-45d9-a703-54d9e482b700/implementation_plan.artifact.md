# Implementation Plan - Vault UX Finalization & Cloud FAB

Refine the Vault experience with direct actions, bulk drag-and-drop moves, synced folder colors, and a new image-only animated Cloud FAB.

## User Review Required

> [!IMPORTANT]
> **Cloud FAB Visuals:** The Cloud FAB will no longer have a button background. It will be the PNG logos themselves, floating and cycling with a cross-fade animation.

> [!IMPORTANT]
> **Direct FAB Flow:** I will bypass all intermediate menus for the main "+" FAB in the Vault. Selecting an action will trigger the destination (picker or dialog) instantly.

## Proposed Changes

### 1. Image-Only Cloud FAB

#### [MODIFY] [CloudImportFab.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/CloudImportFab.kt)
- Remove `FloatingActionButton` container.
- Use `Box` with `Modifier.clickable` and `shadow` to make the PNG logos themselves act as the button.
- Maintain the 3-second cycling animation.

### 2. Vault Screen Logic & Layout

#### [MODIFY] [VaultScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultScreen.kt)
- **Direct Actions:** Update `LaunchedEffect` for `fabActionFlow` to trigger `filePickerLauncher` and `showNewFolderDialog` immediately.
- **Scope Fixes:** Ensure launchers are accessible to `VaultTabPage` for "Upload" actions within empty states.
- **Drag-and-Drop:**
    - Implement root-level drag state (ID, Offset) and hit-testing against a map of folder bounding boxes.
    - Add a "Wrapped" overlay that shows when dragging multiple items.
- **Grid Density:** Lock `VaultTabPage` grid to 5 columns for the Folders tab.
- **UI Cleanup:** Verify the old "Secure import" banner is removed.

### 3. Folder Color & Sync

#### [MODIFY] [VaultComponents.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultComponents.kt)
- Ensure `VaultItemSimpleGrid`, `VaultItemGrid`, and `VaultItemList` use the `color` metadata for icons and background tints.
- Support syncing via `SAVE_FOLDER` in the repository sync queue.

## Verification Plan

### Manual Verification
- **Cloud FAB:** Confirm PNGs cycle and are clickable without a button background.
- **Drag-and-Drop:** Select 5 items, drag them together, and drop them on a folder. Verify they move.
- **Direct Actions:** Click "+" -> "Upload File" and confirm no intermediate menu appears.
- **Grid:** Verify 5 items per row in the Folders tab grid view.
- **Sync:** Confirm folder color changes persist after app restart.
