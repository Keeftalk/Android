# Implementation Plan - Refine Chat Containers and SMS Section

The goal is to ensure that chat containers (Unread, Archived, Favorites) and the SMS section strictly filter their content according to their names.

## Proposed Changes

### 1. Data Models and Repository [MODIFY]

- **[ChatEntity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/entities/ChatEntity.kt)**: Add `isFavorite: Boolean = false` field to support the "Favorites" container.
- **[Chat.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/model/Chat.kt)**: Add `isFavorite: Boolean = false` to the domain model and update `toUiModel()` to include it.
- **[ChatDao.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/dao/ChatDao.kt)**: Add a method to toggle the favorite status of a chat.
- **[ChatRepository.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/repository/ChatRepository.kt)** and **[ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)**: Implement `toggleFavorite(chatId, isFavorite)`.

### 2. ViewModel Logic [MODIFY]

- **[ChatListViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatListViewModel.kt)**:
    - Update the `activeChats` filter logic to handle the "favorites" container using the new `isFavorite` flag.
    - Ensure "unread" container strictly shows chats with `unreadCount > 0`.
    - Ensure "archived" container strictly shows chats with `isArchived = true`.

### 3. UI and Navigation [MODIFY]

- **[MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)**:
    - Update the `listPane` to switch between **Keeftalk Chats** (using `ChatListRecyclerView`) and **SMS Conversations** (using `SmsListScreen`) based on the `chatTab` (0 for Chats, 1 for SMS).
    - This ensures the "SMS" section only contains SMS chats and the "Chats" section only contains Keeftalk chats.
- **[SmsViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/SmsViewModel.kt)**: Ensure it's correctly instantiated and used in `MainActivity`.

## Verification Plan

### Automated Tests
- No new automated tests planned, but existing build and sync should be verified.

### Manual Verification
1.  **Chats Tab**:
    - Verify "All" shows all non-archived chats.
    - Verify "Unread" shows ONLY chats with unread messages.
    - Verify "Archived" shows ONLY archived chats.
    - Verify "Favorites" shows ONLY chats marked as favorite (once the feature is usable).
2.  **SMS Tab**:
    - Verify it shows ONLY SMS conversations and NO Keeftalk chats.
3.  **Cross-Check**:
    - Ensure archiving a chat moves it to the Archived container and removes it from "All"/"Unread".
    - Ensure reading all messages in a chat removes it from the "Unread" container.
