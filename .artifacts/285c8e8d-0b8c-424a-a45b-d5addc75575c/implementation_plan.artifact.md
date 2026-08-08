# Implementation Plan - Fix Supabase Timestamp Out of Range Error

The user is encountering a `PostgrestRestException` (Code: 22008) when upserting a file record. The error `date/time field value out of range: "1786121969563"` indicates that the `Long` Unix timestamps (in milliseconds) for `created_at` and `updated_at` are being sent as numbers/strings that PostgreSQL cannot interpret as valid timestamps.

## Proposed Changes

### Data Layer

#### [MODIFY] [FileEntity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/entities/FileEntity.kt)
- Import `com.keeftalk.chat.util.TimestampSerializer`.
- Annotate `createdAt`, `updatedAt`, and `deletedAt` with `@Serializable(with = TimestampSerializer::class)`.
- This will ensure these fields are serialized as ISO 8601 strings when communicating with Supabase, while remaining as `Long` for local Room database storage.

## Verification Plan

### Manual Verification
1.  **Trigger File Upload**: Perform an action in the app that triggers a file upload.
2.  **Monitor Logcat**: Verify that the upsert operation succeeds without the `date/time field value out of range` error.
3.  **Check Supabase**: Verify that the `created_at` and `updated_at` columns in the `files` table are correctly populated with ISO 8601 timestamps.
