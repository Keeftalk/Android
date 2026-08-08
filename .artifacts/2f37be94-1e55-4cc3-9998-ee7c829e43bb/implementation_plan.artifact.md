# Implementation Plan: Message Delivery State System Redesign

Audit and redesign the message delivery state system to ensure accuracy, real-time synchronization, and scalability for 1:1 and group chats.

## User Review Required

> [!IMPORTANT]
> This redesign introduces **Read Pointers** in the `chat_members` table. This changes how "Seen" status is calculated: instead of checking a `status` column on every message, the system will compare a message's timestamp/ID against the peer's `last_read_message_id`.

> [!WARNING]
> Database schema changes are required for both Supabase (PostgreSQL) and Room (Android). Existing message statuses will be migrated or implicitly handled by the new pointer logic.

## Proposed Changes

### Database Schema

#### Supabase (PostgreSQL)
- [MODIFY] `public.chat_members`:
    - Add `last_read_message_id` (UUID, nullable, references `messages`).
    - Add `last_delivered_message_id` (UUID, nullable, references `messages`).
    - Add `last_read_at` (TIMESTAMPTZ).
    - Add `last_delivered_at` (TIMESTAMPTZ).
- [NEW] Triggers to automatically update `chats.last_message_status` based on these pointers for 1:1 chats.

#### Room (Android)
- [MODIFY] `ChatMemberEntity`: Add `lastReadMessageId`, `lastDeliveredMessageId`.
- [NEW] `ReceiptSyncQueueEntity`:
    - `chatId` (String)
    - `messageId` (String)
    - `type` (READ or DELIVERED)
    - `timestamp` (Long)
- [MODIFY] `MessageDao`: Add queries to fetch messages by pointer comparison.

---

### Data & Synchronization Layer

#### [MODIFY] [ChatRepositoryImpl](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- **Read Receipt Logic**:
    - Implement `sendReadReceipt(chatId, messageId)` which only sends an update if `messageId` is newer than the current `last_read_message_id`.
    - Batch multiple read events into a single "Highest ID" update.
- **Delivery Receipt Logic**:
    - Automatically send delivery receipts upon receiving a message via Realtime or Sync, then mark locally as `DELIVERED`.
- **Realtime Listener**:
    - Update the listener to observe `chat_members` table changes.
    - When a peer's `last_read_message_id` updates, trigger a local Room update that refreshes the UI.
- **Offline Sync**:
    - Integrate with `BackgroundSyncManager` to flush the `ReceiptSyncQueue` when connectivity is restored.

---

### UI Layer

#### [MODIFY] [ChatDetailViewModel](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailViewModel.kt)
- Implement visibility tracking: as the user scrolls or new messages appear, calculate the highest visible `messageId` and trigger `sendReadReceipt`.
- Observe `chat_members` Flow to show peer "Seen" status.

#### [MODIFY] [MessagePagingAdapter](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/MessagePagingAdapter.kt) & [MessageBubble](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- Update status icon logic:
    - 1:1: Show `✓` (Sent), `✓✓` (Delivered), `✓✓` colored (Seen).
    - Group: Show aggregated status (e.g., "Seen by 5").
- Render peer avatars at the position of their `last_read_message_id` (Signal-style).

## Verification Plan

### Automated Tests
- **Unit Tests**:
    - `MessageStatusCalculatorTest`: Verify deterministic state transitions (no moving backwards).
    - `ReceiptBatcherTest`: Ensure only the highest message ID is sent in a batch.
- **Integration Tests**:
    - Mock Supabase Realtime to verify that updating `chat_members` reflects in the UI status icons.
    - Verify offline receipt queueing and flushing.

### Manual Verification
- Deploy to two devices:
    - Send message from A to B (Offline) -> A shows `Sent`.
    - Bring B Online (Background) -> A shows `Delivered`.
    - Open Chat on B -> A shows `Seen`.
    - Send 10 messages, scroll to top on B -> A shows `Seen` only up to visible messages.
- Test Group Chat with 3+ members:
    - Verify individual "Seen by" indicators.
