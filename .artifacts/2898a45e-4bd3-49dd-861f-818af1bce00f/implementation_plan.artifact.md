# Two-Way Contacts Sync with Supabase

This plan implements the logic to synchronize personal contacts between the local Android database and the Supabase `contacts` table.

## User Review Required

> [!IMPORTANT]
> - **Authentication**: Sync requires the user to be logged in. If not logged in, the sync will be skipped.
> - **Two-Way Strategy**:
>   1. **Pull**: All contacts from the cloud are downloaded and merged into the local database.
>   2. **Push**: Any local contact marked as "Pending Sync" is uploaded to the cloud.
> - **Conflict Resolution**: The cloud version takes precedence during the initial pull.

## Proposed Changes

### Data Layer

#### [MODIFY] [UserDao.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/dao/UserDao.kt)
- Add `@Query("SELECT * FROM users WHERE isContact = 1 AND cloudSyncStatus != 1")` to find contacts needing sync.
- Add `@Query("UPDATE users SET cloudSyncStatus = :status WHERE id = :userId")` to update sync state.

#### [NEW] [ContactDto.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/remote/ContactDto.kt)
- Serializable data class for Supabase Postgrest interaction.

### Repository Layer

#### [MODIFY] [ChatRepository.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/repository/ChatRepository.kt)
- Add `suspend fun syncContacts()` to the interface.

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- Implement `syncContacts()`:
    - Get `currentUserId`.
    - Fetch from `supabase.postgrest["contacts"]`.
    - Update local DB.
    - Upload local "unsynced" contacts to Supabase.

### ViewModel Layer

#### [MODIFY] [CallListViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/CallListViewModel.kt)
- Update `syncContactsWithSupabase()` to call `repository.syncContacts()`.

## Verification Plan

### Manual Verification
- **Sync Trigger**: Click "Sync" in the Contacts menu and observe the logs/UI message.
- **Data Persistence**: Add a contact, sync, then clear app data and sync again to verify recovery.
- **Supabase Console**: Verify entries appear in the `contacts` table with the correct `owner_id`.
