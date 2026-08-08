# Implementation Plan: Chat Refinement and Precision

This plan addresses performance, navigation, and UX polish for media, status icons, and RTL support.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [Message.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/model/Message.kt)
- Add `width: Int?` and `height: Int?` to the `Message` data class.

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- Update `sendMessage`:
    - Use `BitmapFactory` to decode image dimensions without loading the full image into memory.
    - Store `width` and `height` in the local database and remote DTO.
- Update `handleIncomingMessageInternal` to decode and store dimensions for received messages.

### [UI Components]

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- **Scroll Restoration FAB**:
    - Move `FloatingActionButton` to the root-level `Box` of the screen.
    - Set `zIndex(100f)` to ensure it stays above all chat bubbles and indicators.
- **Vertical Video Aspect Ratio**:
    - Update `InlineVideoPlayer` to use a dynamic `Modifier.aspectRatio()` based on the video size detected from the player listener.
- **Message Status Icons**:
    - Ensure status icons (Sent, Delivered, Seen) are **only** displayed on messages where `isMe == true`.
    - Fix alignment to the bottom-right of the bubble.
- **HD Indicator**:
    - Update `HDBadge` logic: Show if `(width * height) >= (1920 * 1080)`.
- **Arabic (RTL) Support**:
    - Force `TextAlign.End` for Arabic text in both bubbles and the input field.
    - Ensure the cursor starts on the right for Arabic input.

## User Review Required

> [!IMPORTANT]
> To accurately detect HD photos by resolution, I will need to update the database schema. I will handle the Room migration automatically (destructive migration if necessary for this dev phase, or a safe addColumn).

> [!NOTE]
> The scroll position restoration is now tied to the exact pixel offset of the top visible item, providing "Resume" level precision.

## Verification Plan

### Manual Verification
1.  **HD Photo**: Send a 4K image. Verify the "HD" badge appears.
2.  **Vertical Video**: Send a portrait video. Verify the bubble height adjusts to match the video's aspect ratio perfectly.
3.  **Status Icons**: Verify that icons **do not** appear on messages received from a peer.
4.  **FAB Visibility**: Scroll up in a busy chat. Verify the "Scroll to Bottom" button is visible and clickable even when the typing indicator is active.
5.  **Arabic Typing**: Type "مرحبا". Verify the cursor is on the right and the text is right-aligned.
