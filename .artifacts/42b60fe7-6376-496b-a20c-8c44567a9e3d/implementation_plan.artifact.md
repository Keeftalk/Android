# Implementation Plan - "My Calendar" Feature Update

Update the "My Calendar" feature to 100% match the provided HTML/CSS preview, using Jetpack Compose, Kotlin, and Supabase. The implementation will be offline-first with autosync.

## User Review Required

> [!IMPORTANT]
> The UI will strictly follow the dark theme and layout of the provided HTML, including custom gradients and chart visualizations.

> [!WARNING]
> This update involves changes to the database schema (both Room and Supabase) to support family sharing and permissions.

## Proposed Changes

### Supabase Setup
I will provide the SQL schema to be added to Supabase to support the new features.

#### [NEW] [supabase_setup.sql](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/.artifacts/42b60fe7-6376-496b-a20c-8c44567a9e3d/scratch/supabase_setup.sql)
- SQL script for `calendar_items`, `family_members`, and `family_permissions` tables.

---

### Data Layer

#### [MODIFY] [CalendarEntities.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/entities/CalendarEntities.kt)
- Add `userId` to `CalendarItemEntity`.
- Add `FamilyMemberEntity` and `FamilyPermissionEntity`.

#### [MODIFY] [CalendarDao.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/dao/CalendarDao.kt)
- Add methods to manage family members and permissions.
- Update queries to support filtering by user and shared status.

#### [MODIFY] [CalendarRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/CalendarRepositoryImpl.kt)
- Implement autosync for new tables.
- Update fetching logic to include shared items based on permissions.

---

### UI Layer

#### [MODIFY] [CalendarDesign.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/calendar/CalendarDesign.kt)
- Ensure all colors and gradients from the HTML are correctly defined.

#### [MODIFY] [CalendarViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/calendar/CalendarViewModel.kt)
- Add state for family members, stats, and analytics.
- Implement logic for grouping upcoming items and calculating analytics data.

#### [MODIFY] [CalendarScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/calendar/CalendarScreen.kt)
- Completely rebuild the UI to match the HTML:
    - **Header**: Back button, title, search, notifications, New button.
    - **Tabs**: Type filters with badges.
    - **Calendar Grid**: Styled as in HTML with dot indicators.
    - **Sidebar**:
        - **Upcoming**: Grouped list (Today, Tomorrow, Future, Past).
        - **Overview**: Stats row.
        - **Family & Shared**: List with toggles.
        - **Analytics**: Pie and Bar charts using Compose `Canvas`.
    - **FAB**: Custom menu with animated items.
    - **Modals**: Dynamic fields for each item type.

## Verification Plan

### Automated Tests
- Unit tests for analytics data calculation.
- Unit tests for repository sync logic.

### Manual Verification
- Deploy to device and verify UI pixel-perfection against the HTML preview.
- Test offline additions and verify they sync to Supabase once online.
- Test family sharing toggles and verify items appear/disappear based on selection.
- Verify the FAB menu animations and modal functionality.
