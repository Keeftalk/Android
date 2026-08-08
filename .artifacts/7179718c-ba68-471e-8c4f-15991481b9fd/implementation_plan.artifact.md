# Implementation Plan - Advanced Chat UI & Interaction Refinement

This plan focuses on professionalizing the chat experience with real-time feedback, smarter scrolling, and robust error handling.

## User Review Required

> [!IMPORTANT]
> - **Upload/Download Progress**: I will implement progress tracking for media transfers. This involves modifying repository signatures and bubble UI.
> - **Failure Pop-up**: A new dialog will appear for failed messages, allowing for immediate retry or removal.
> - **Scroll Logic**: Strict enforcement of bottom-start and position-restoration will be implemented to prevent unexpected jumps to the top.

## Proposed Changes

### [Core Logic & Repository]

#### [MODIFY] [ChatRepository.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/repository/ChatRepository.kt)
- Update `sendMessage` to accept an optional `onProgress: (Float) -> Unit` callback.
- Update `downloadMedia` to accept an optional `onProgress: (Float) -> Unit` callback.

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- Implement chunked reading in `downloadMedia` to report progress.
- Implement progress reporting in `sendMessage` (simulated for now as Supabase standard upload is atomic, or using a wrapper if possible).

### [ViewModel]

#### [MODIFY] [ChatDetailViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailViewModel.kt)
- Add `transferProgress: StateFlow<Map<String, Float>>` to track upload/download percentages by message ID.
- Update `sendMessage` and `downloadMedia` calls to populate this map.
- Add `retryMessage(message: Message)` and `cancelMessage(message: Message)` functions.

### [UI Components - chatdetail package]

#### [MODIFY] [ChatMessageBubbles.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- **MessageBubble**:
    - Ensure timestamp/status are aligned to the **bottom right** of the bubble area for all messages.
    - Add HD badge visibility check for all media types.
- **MessageStatusIcon**:
    - Update `SENDING` state to show a circular progress indicator with the percentage text if available.
- **Video Message**:
    - Receiver view: Show download progress overlay when clicked.
    - Add "Download" icon that triggers `onDownloadClick` instead of full view.

#### [MODIFY] [ChatInput.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatInput.kt)
- No major changes expected here, but will ensure it doesn't trigger unwanted scrolls.

### [Main Screen]

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- **Scroll Logic**:
    - Ensure `initialScrollPosition = 0` (bottom) on first entry.
    - Strictly guard `scrollToPosition` calls.
- **Failure Handling**:
    - Add a `LaunchedEffect` or UI trigger to show the "Message Failed" dialog when a local message enters `FAILED` state.
- **UI**: Ensure the "Scroll to Bottom" FAB is prominently visible when scrolled up.

## Verification Plan

### Automated Tests
- `gradle assembleDebug` to verify compilation.

### Manual Verification
1. **Upload Progress**: Send a large video/file and check if the blue circle shows increasing percentage.
2. **Download Progress**: Tap the download icon on a received video and check progress.
3. **Scroll**:
    - Enter chat -> check if at bottom.
    - Open media -> exit -> check if position is preserved.
    - Manually scroll up -> check if it stays there (no auto-scroll to top).
4. **Failure Pop-up**: Simulate failure (e.g. airplane mode) -> check if "cool pop up" appears -> test Retry and Cancel (Remove).
5. **HD Badge**: Send/Receive HD media and check for badge.
6. **Alignment**: Check if status/timestamp are at the bottom right.
