# Implementation Plan - Chat UI Refinements and Filtering

This plan addresses several UI improvements and bug fixes related to chat containers, unread badges, and light mode visibility.

## Proposed Changes

### 1. Chat Container Filtering & Badge Counts

- **Goal**: Ensure "Unread" shows only unread chats, "Groups" shows only group chats, and "Channels" is removed.
- **Fix**: Update the badge count logic to use unfiltered data so counts remain consistent across tabs.

#### [MODIFY] [ChatListViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatListViewModel.kt)
- Add StateFlows for `unreadChatsCount` and `groupChatsCount` (optionally others like `archivedChatsCount`) based on the full `allChats` list.
- Ensure `activeChats` filtering correctly handles the "unread" and "groups" logic (current implementation seems correct, but will double-check).

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Remove the "Channels" container from the `containers` list.
- Update the badge count calculation in the `containers` list to use the new flows from `ChatListViewModel` (or calculate from an unfiltered list).

#### [MODIFY] [ChatListScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatListScreen.kt)
- Mirror the "Channels" removal and badge count logic updates in `ChatListContent`.

### 2. Unread Counts Size (Chat Containers Only)

- **Goal**: Increase the size of unread count badges in the chat containers row by 50%.
- **Note**: This change applies ONLY to the top container row, not to individual chat items, bottom navigation, or other UI elements.

#### [MODIFY] [ChatListScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatListScreen.kt)
- In `ChatContainersRow`, increase the badge text size from `7.sp` to `10.5.sp` and adjust the container padding/min-size if necessary to accommodate the larger text.

### 3. Light Mode Visibility Fix

- **Goal**: Fix the hidden text issue in "Chats" and "SMS" tabs when in light mode.

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Change the hardcoded `Color.White.copy(alpha = 0.5f)` for unselected tab text to a theme-aware color like `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)`.
- Ensure the selected tab color (`MaterialTheme.colorScheme.primary`) is legible against the background.

## Verification Plan

### Manual Verification
1.  **Filtering**: Select "Unread" container and verify only chats with unread messages are shown. Select "Groups" and verify only group chats are shown. Verify "Channels" is gone.
2.  **Badge Sizes**: Visually confirm that unread badges in the chat list, bottom nav, and top bar are significantly larger.
3.  **Light Mode**: Switch system theme to Light Mode. Navigate to the "Chats" and "SMS" tabs and verify that the unselected tab labels are clearly visible.
4.  **Badge Consistency**: Switch between containers (All, Unread, Groups) and verify that the badge counts on the container row remain constant and accurate.
