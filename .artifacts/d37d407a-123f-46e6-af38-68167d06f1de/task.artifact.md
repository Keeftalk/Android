# Tasks - Refine Chat Containers and SMS Section

- [x] **Data Model & DAO Updates**
    - [x] Add `isFavorite` to `ChatEntity.kt`
    - [x] Add `isFavorite` to `Chat.kt` domain model
    - [x] Update `ChatDao.kt` with `updateFavorite` method
- [x] **Repository Implementation**
    - [x] Add `toggleFavorite` to `ChatRepository.kt` interface
    - [x] Implement `toggleFavorite` in `ChatRepositoryImpl.kt`
- [x] **ViewModel Filtering**
    - [x] Update `ChatListViewModel.kt` filtering logic for containers (Unread, Archived, Favorites)
- [x] **SMS Section Refinement**
    - [x] Update `MainActivity.kt` to display `SmsListScreen` in the SMS tab
- [x] **Verification**
    - [x] Build the project
    - [x] Verify container filtering in UI
    - [x] Verify SMS tab content
