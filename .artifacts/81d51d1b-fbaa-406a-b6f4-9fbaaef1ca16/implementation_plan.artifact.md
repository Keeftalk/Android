# Implementation Plan - "Scroll to Bottom" FAB with Unread Badge

Implement a "Scroll to Bottom" Floating Action Button in the chat screen with modern animations, unread message counts, and smooth scrolling behavior.

## User Review Required

> [!NOTE]
> The FAB will be positioned 16dp above the chat input area. When a reply preview is active, it will maintain its relative position above the input area.

## Proposed Changes

### Chat Screen Implementation

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- Add `unreadCount` state to track new messages while scrolled up.
- Update `showScrollToBottom` logic with a threshold (e.g., first visible item > 5).
- Refine `AdapterDataObserver` in `RecyclerView` to:
    - Auto-scroll to bottom if the user is already at the bottom when a new message arrives.
    - Increment `unreadCount` if the user is scrolled up when a new message arrives.
- Reset `unreadCount` to 0 when the user reaches the bottom or clicks the FAB.
- Replace the existing basic FAB with a new `ScrollToBottomFAB` component.
- Implement smooth scrolling to the bottom (position 0).

#### [NEW] [ScrollToBottomFAB.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ScrollToBottomFAB.kt)
- Create a dedicated Composable for the FAB.
- Style according to Material 3 with circular shape, elevation, and shadow.
- Add an unread count badge using a small red circle with text.
- Implement fade and scale animations for the badge itself when it appears/updates.
- Support haptic feedback on click.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.
- (Optional) Add a unit test if possible for the `unreadCount` logic, although it's mostly UI/State driven.

### Manual Verification
1. **Scrolling Visibility**:
    - Scroll up in a chat. Verify the button fades and scales in after passing the threshold.
    - Scroll back to the bottom. Verify the button disappears.
2. **Scroll Action**:
    - Click the button. Verify smooth scrolling to the latest message.
    - Verify the button disappears once the bottom is reached.
3. **New Messages**:
    - While at the bottom, send/receive a message. Verify the list auto-scrolls to show the new message and no FAB appears.
    - While scrolled up, receive a message. Verify the FAB appears (if not already visible) and the unread count badge increments.
    - Click the FAB with a badge. Verify it scrolls to bottom and the badge is cleared.
4. **UI Adaptability**:
    - Open/close keyboard. Verify FAB stays above the input area.
    - Rotate screen. Verify state (scroll position, FAB visibility, unread count) is preserved.
    - Test in light and dark themes.
