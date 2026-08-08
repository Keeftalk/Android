# Walkthrough: Media Architecture Refactoring

The Keeftalk media and storage architecture has been successfully refactored into a centralized, deduplicated, and E2E encrypted system.

## Key Changes

### 1. Centralized File Management
- **Single Source of Truth**: All media across Chat, Notes, Agenda, and Vault now use the `files` table and a unified Supabase bucket.
- **Reference Counting**: Robust reference management ensures files are only deleted when no longer used by any feature.
- **Automated Cleanup**: A `FileCleanupWorker` runs daily to reconcile counts and purge orphan files.

### 2. Deduplication & Sync
- **SHA-256 Hashing**: Files are hashed before upload. If a file exists locally or on Supabase, it is reused instead of re-uploaded.
- **Remote Verification**: The upload pipeline now verifies both the database record and physical storage existence before deduplicating, preventing "ghost" file references.
- **Strict Sync**: Uploads now enforce remote database consistency, preventing Foreign Key constraint errors.

### 3. Enhanced Security & Privacy
- **Cloud-E2EE**: All physical files are encrypted with unique keys before upload. Only the client holds the keys (metadata-only on server).
- **Private Buckets**: The `files` bucket is now private. Downloads use authenticated requests.
- **Recipient Access**: RLS policies were updated to allow recipients of chats/notes to view the necessary file metadata.

### 4. Vault Redesign
- **Unified Views**: The Vault now features tabs based on the source of the media (My Uploads, Chats, Notes, etc.).
- **Smart Filtering**: Items are automatically categorized by their origin feature.

## Verification Results

- **Deduplication**: Verified that sending the same image in two different chats only results in one physical upload.
- **Lifecycle**: Verified that deleting a chat message decrements the `reference_count` and marks the file for deletion if it was the last reference.
- **Sync**: Resolved the "Bucket not found" and "Foreign Key constraint" errors through improved URI resolution and authenticated download paths.

> [!TIP]
> The daily cleanup worker will automatically purge any `PENDING_DELETE` files from local and remote storage to save space.
