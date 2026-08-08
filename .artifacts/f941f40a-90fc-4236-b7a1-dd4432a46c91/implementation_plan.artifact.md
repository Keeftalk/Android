# Chat Fixes: Status Icons, Timestamps, Date Separators & Real-Time Sync

This plan addresses several regressions in the chat screen, focusing on visual clutter, interaction consistency, and reliable real-time updates.

## User Review Required

> [!IMPORTANT]
> The fix for real-time updates involves adding logging and ensuring that the Supabase Realtime subscription is correctly filtered or globally observed. If RLS (Row Level Security) is not perfectly configured on the Supabase side, the client might not receive all updates.

## Proposed Changes

### [UI Fixes]

#### [MODIFY] [ChatMessageBubbles.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- **Remove Duplicate Status Icons**: Consolidate status rendering logic so that `MessageStatusIcon` is only rendered once per outgoing message, preferably within the `AnimatedVisibility` block if expanded, or as a single compact indicator if not.
- **Fix Timestamp Toggle**: Ensure `pointerInput` correctly captures the latest `onToggleTimestamp` lambda using `rememberUpdatedState` or by including it in the `pointerInput` key. Verify that child components are not consuming the tap events for text messages.

#### [MODIFY] [ChatDetailViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailViewModel.kt)
- **Fix Date Separator Placement**: Update `insertSeparators` logic to insert separators *above* (higher index in reverse layout) the first message of each day. Specifically, insert when `after` (older) is a different day than `before` (newer), or when `after` is null (oldest message).

### [Real-Time Sync Fixes]

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- **Improve Real-Time Observation**:
    - Add logging to `observeRealtimeChanges` to track subscription status and incoming events.
    - Ensure `postgresChangeFlow` is correctly observing both `Insert` and `Update` for the `messages` table.
    - Verify that `handleIncomingMessageInternal` correctly invalidates the Room database to trigger UI updates.
- **Status Sync**: Ensure that `Update` events for message statuses are processed with priority checks to avoid stale status overwrites.

### [Adapter & Screen Fixes]

#### [MODIFY] [MessagePagingAdapter.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/MessagePagingAdapter.kt)
- Ensure `DiffUtil` correctly identifies status changes to trigger partial rebinds.

## Verification Plan

### Automated Tests
- Manual verification is required for real-time sync across two devices/instances.

### Manual Verification
1. **Status Icons**: Send a message and verify only one icon (Sending -> Sent -> Delivered -> Seen) appears.
2. **Timestamp Toggle**: Single-tap a text message and verify the timestamp appears/disappears.
3. **Date Separators**: Verify "Today" appears at the top of today's messages, not at the bottom of the screen. Scroll up to verify other separators.
4. **Real-Time Sync**: Open chat on two devices. Send a message from A to B. Verify B sees it instantly. Verify A sees "Delivered" and "Seen" instantly when B receives/reads it.
5. **Scroll & Performance**: Verify smooth scrolling and that scroll position is preserved during updates.
