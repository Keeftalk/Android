# Implementation Plan - Fixing Conversation Key Sync & Identity

Address the `PostgrestRestException` caused by a missing `user_id` in the `conversation_keys` upsert, and fix the unique constraint issue to support per-user encrypted keys for the same conversation.

## User Review Required

> [!IMPORTANT]
> This change involves a database migration for your local Room database and requires an update to the Supabase schema. Please run the updated [fix_security_rls.sql](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/fix_security_rls.sql) in your Supabase SQL Editor.

## Proposed Changes

### 1. Security & Crypto Core

#### [MODIFY] [ConversationKeyEntity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/security/crypto/ConversationKeyEntity.kt)
- Add `user_id` column to the entity.
- Update the unique index to be composite: `(conversationId, user_id)` to allow multiple users to store their own encrypted versions of the same Per-Conversation Key (PCK).

#### [MODIFY] [ConversationKeyDao.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/security/crypto/ConversationKeyDao.kt)
- Update `getKeyForConversation` and `deleteKeyForConversation` to include `userId` as a parameter to ensure user isolation.

#### [MODIFY] [ConversationKeyManager.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/security/crypto/ConversationKeyManager.kt)
- Update `getOrLoadKey` and `createKey` to fetch the current `userId` from `UserPreferencesRepository`.
- Include `user_id` in both Supabase `select` and `upsert` calls.

### 2. Database Migration

#### [MODIFY] [KeeftalkDatabase.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/KeeftalkDatabase.kt)
- Increment version from 82 to 83.
- Add `MIGRATION_82_83` to handle the `user_id` column addition and index update in `conversation_keys`.

### 3. SQL Schema

#### [MODIFY] [full_keeftalk_schema.sql](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/full_keeftalk_schema.sql)
- Update `conversation_keys` table definition to include `user_id` and a unique constraint on `(conversation_id, user_id)`.

#### [MODIFY] [fix_security_rls.sql](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/fix_security_rls.sql)
- Add idempotent logic to add `user_id` to `conversation_keys` if it doesn't exist, and add the unique constraint.

## Verification Plan

### Automated Tests
- Run `ConversationKeyManagerTest` (if exists) or verify via manual flow.

### Manual Verification
1. **Scenario: Key Sync**
   - Send a message in a new chat.
   - **Expected Result**: The PCK is successfully generated, encrypted with CPK, and synced to Supabase with the correct `user_id`. No `PostgrestRestException` should occur.
2. **Scenario: Device Sync**
   - Log in on a second device (or clear app data and re-login).
   - **Expected Result**: The app should fetch the PCK from Supabase using `conversation_id` and `user_id`, and successfully decrypt it with the restored CPK.
3. **Scenario: Migration**
   - Upgrade from version 82 to 83.
   - **Expected Result**: Room database migrates successfully without data loss.
