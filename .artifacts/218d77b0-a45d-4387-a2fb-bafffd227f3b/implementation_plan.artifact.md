# Refactoring Final Fixes: Sync & Stability

This plan addresses remaining issues after the media architecture refactor:
1.  **Supabase Schema Mismatch:** Fix remaining plural/singular inconsistencies in Postgrest queries.
2.  **Reference Count Sync:** Ensure `referenceCount` changes are synchronized with the Supabase backend.
3.  **Paging Stability:** Fix the "Attempt to collect twice" crash in the Chat Detail screen.
4.  **Reference Ownership:** Add ownership validation to reference management.

## User Review Required

> [!IMPORTANT]
> I will be standardizing all Supabase table references to `message_attachment` (singular) across the entire project. This matches the previously executed SQL script.

## Proposed Changes

### 1. Supabase Sync Consistency

#### [MODIFY] [FileRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/FileRepositoryImpl.kt)
- Update `incrementReferenceCount` and `decrementReferenceCount` to perform a remote `upsert/update` on Supabase to keep the counts in sync.
- Update `updateFileStatus` to ensure status changes (like `PENDING_DELETE`) reach the server.

#### [MODIFY] [FileUploadManager.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/util/FileUploadManager.kt)
- In the deduplication block, when a local file is reused, verify it exists on Supabase. If not, re-upload metadata.

### 2. Paging Stability

#### [MODIFY] [ChatDetailViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailViewModel.kt)
- Move `.cachedIn(viewModelScope)` to be the absolute last operation on the `messagesPagingData` flow to ensure all operators (like `insertSeparators`) are part of the cached stream.

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- Ensure `messagesFlow` collection in `LaunchedEffect` correctly manages its lifecycle.

### 3. Schema Cleanup (Plural to Singular)

- Perform a global search and replace of `message_attachments` with `message_attachment` in all Supabase queries.

## Verification Plan

### Automated Tests
- Verify `reconcileReferenceCounts` correctly calculates totals from all attachment tables.

### Manual Verification
1. Send an image that was previously deleted but exists locally (verify deduplication and Supabase metadata re-sync).
2. Rapidly enter and exit a chat (verify Paging collection doesn't crash).
3. Delete a message and check Supabase `files` table for `reference_count` decrement.
