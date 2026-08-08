# Fix: Startup Regression & Glitchy Scrolling

Address the recent slowness in startup and the "jittery" (up/down jumping) scrolling in chats, especially those with media.

## User Review Required

> [!IMPORTANT]
> **Startup Strategy Reversion:** I am moving native library loading (SQLCipher) to a background thread and removing the `post` delay for the main list fragment. This will make the app feel "instant" again while keeping the main thread free for drawing.

> [!WARNING]
> **Scrolling Jumps:** The "up and down" jumping in chats is caused by dynamic item heights. I will implement **stable aspect ratios** for media placeholders. If the app doesn't know the image size yet, it will use a standard "WhatsApp-style" fixed height to prevent the list from jumping when the image finally appears.

## Proposed Changes

### 1. Startup Optimization (App & Activity)

#### [MODIFY] [KeeftalkApplication.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/KeeftalkApplication.kt)
- Move `SQLiteDatabase.loadLibs()` and `System.loadLibrary("sqlcipher")` to **Tier 2 (Background)**. This is a heavy operation that was blocking the app's very first millisecond of life.

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Remove the `nativeShell.post` wrapper. Instead, use a standard `commit()` (non-blocking) immediately in `onCreate`. This removes the forced 16ms+ delay.

#### [MODIFY] [ChatListFragment.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatListFragment.kt)
- **Fast Path Restoration:** Use the lightweight `ChatListAdapter` (non-paging) for the first frame to bind cached chats instantly.
- Swap to `ChatPagingAdapter` only when the first page of real database data arrives. This prevents the "Paging warm-up" delay during startup.

### 2. Chat Detail Smoothness (List & Media)

#### [MODIFY] [ChatMessageBubbles.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- **Stable Media Bounds:**
    - Use the stored `width` and `height` from the message entity to set a **fixed aspect ratio** for the bubble *before* the image loads.
    - If dimensions are missing, default to a stable 16:9 or 4:3 ratio instead of a generic shimmer. This prevents the "jumping" effect when images pop in.

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- **RecyclerView Tuning:**
    - Enable `setHasFixedSize(true)` on the message list.
    - Adjust `prefetchDistance` to 5 to reduce the CPU load during fast scrolling.

## Verification Plan

### Manual Verification
- **Startup:** Cold start the app and verify the splash screen disappears in under 500ms.
- **Scrolling:** Rapidly scroll through a chat with mixed text and large images. Verify that the list doesn't "jump" or "twitch" when images load.
- **Media Jitter:** Check that images appearing from the top (as you scroll up) don't push the current view down unexpectedly.
