# Implementation Plan - End-to-End Encryption Notice

Add a subtle, informational end-to-end encryption notice to the top of every chat conversation in Keeftalk.

## User Review Required

> [!IMPORTANT]
> The notice will be inserted at the very beginning (oldest part) of the message timeline. In the current reversed message list implementation, this means it will appear at the top of the chat when the user scrolls all the way up.

## Proposed Changes

### 1. Data Model & ViewModel

#### [MODIFY] [ChatDetailViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailViewModel.kt)
- Add `EncryptionNoticeItem` to the `ChatItem` sealed class.
- Update `messagesPagingData` to insert `EncryptionNoticeItem` at the end of the paging data (representing the oldest part of the chat).

### 2. UI Components

#### [NEW] [EncryptionNotice.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/EncryptionNotice.kt)
- Create `EncryptionNotice` Composable:
    - Subtle styling (minimal, elegant).
    - Text: "Messages and calls are secured with end-to-end encryption. Only people in this chat can read, listen to, or share them. ?"
    - Make the "?" clickable.
- Create `EncryptionExplanationBottomSheet` Composable:
    - Lock/Shield icon.
    - Title: "Your messages are private".
    - Description explaining E2EE.
    - "See more →" button opening `https://keeftalk.com/security`.

### 3. List Adapter

#### [MODIFY] [MessagePagingAdapter.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/MessagePagingAdapter.kt)
- Add `TYPE_ENCRYPTION_NOTICE` view type.
- Handle `EncryptionNoticeItem` in `getItemViewType`, `onCreateViewHolder`, and `onBindViewHolder`.
- Add `onEncryptionNoticeClick` callback to the adapter.
- Update `ChatItemDiffCallback` to support the new item type.

### 4. Integration

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- Add state `showEncryptionExplanation` to manage the bottom sheet visibility.
- Pass `onEncryptionInfoClick` through `ChatMessageList` to `MessagePagingAdapter`.
- Display `EncryptionExplanationBottomSheet` when requested.

## Verification Plan

### Automated Tests
- N/A (UI-focused change, manual verification preferred).

### Manual Verification
1. Open a 1-to-1 chat: verify the notice appears at the top.
2. Open a group chat: verify the notice appears at the top.
3. Scroll down (to newest messages) and back up: verify the notice remains at the top.
4. Click the "?": verify the explanation bottom sheet opens.
5. Click "See more →": verify it opens the browser with the correct URL.
6. Toggle Dark/Light theme: verify the notice and bottom sheet colors adjust correctly.
7. Verify no duplicate notices appear after sending/receiving messages or refreshing the chat.
