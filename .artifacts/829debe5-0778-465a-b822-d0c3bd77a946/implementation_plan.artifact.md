# Implementation Plan - Fix Media Sending & Read Receipt Alignment

Resolve the "PGRST204" constraint error for media messages and fix the read receipt positioning on the right side.

## User Review Required

> [!IMPORTANT]
> 1. **Media Sending**: The `content` column in the database is `NOT NULL`, but the app was sending empty strings (`""`) which were being omitted by the serializer, causing Supabase to reject the message. I will ensure `content` is never blank for media messages (defaulting to "[Image]", "[Video]", etc.).
> 2. **Read Receipt Alignment**: I identified a coordinate calculation error where horizontal padding was being double-counted. I will fix the targeting logic to ensure the avatar sits perfectly in the 32dp gutter to the right of the bubble.

## Proposed Changes

### [Data Layer - Repositories]

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- In `sendMessage`, ensure the `content` field is never blank when creating the `MessageDto`.
- Example: `content = content.ifBlank { "[${type.name}]" }`.
- This satisfies the database `NOT NULL` constraint and provides a useful fallback for notifications.

### [UI Layer - Screens]

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- **Positioning Fix**:
    - Remove the redundant `horizontalPadding` from `targetX` calculation in `ReadReceiptOverlay`.
    - `targetX` will now be `offset.x + size.width + 4.dp`, which correctly places the avatar in the margin.
- **Visual Alignment**:
    - Adjust `targetY` to land the avatar exactly beside the bottom-right corner of the message bubble.
- **Safety**:
    - Ensure `content` fallback logic is also applied in `ChatDetailContent` if needed for UI previews.

## Verification Plan

### Automated Tests
- Run `:app:compileDebugKotlin` to verify syntax.

### Manual Verification
- **Media Test**: Send a picture. Verify it uploads successfully and shows up in the chat.
- **Alignment Test**: Send a message. Have the peer read it. Verify the "Tiny Seen Avatar" appears on the right side of the bubble, exactly where the 32dp gutter was created.
