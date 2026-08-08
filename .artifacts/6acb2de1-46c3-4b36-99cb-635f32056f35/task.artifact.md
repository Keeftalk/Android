# Tasks - Email Module Enhancements & Fixes

- [x] **Phase 1: Pro Data Layer & Models**
    - [x] Update `EmailModels.kt` (profilePicUrl, senderProfilePicUrl)
    - [x] Update `MailEntities.kt` (Add profilePic columns)
    - [x] Add Database Migration 71 -> 72 in `KeeftalkDatabase.kt`
- [x] **Phase 2: Robust Sync & Logic**
    - [x] Refine Gmail folder mapping (Priority: INBOX)
    - [x] Add granular sync logging for folder classification
- [x] **Phase 3: Pro UI & Multi-Selection**
    - [x] Redesign `EmailScreen.kt` (Flat list, dividers, dimming read items)
    - [x] Shrink sender email address by 60% and add transparency
    - [x] Implement long-press selection & Contextual Action Bar
    - [x] Remove duplicate FAB from `EmailScreen.kt`
- [x] **Phase 4: Advanced Navigation & Rendering**
    - [x] Update `AdaptiveFab.kt` to show stacked account avatars
    - [x] Enhance `EmailHtmlRenderer.kt` for mobile fitting
    - [x] Fully wire Reply/Forward with quoted content
