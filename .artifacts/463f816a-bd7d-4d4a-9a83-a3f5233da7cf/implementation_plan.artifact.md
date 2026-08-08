# Fix Build Errors and Warnings

The project fails to build due to:
1. Missing library definition for `benchmark-macro` in `libs.versions.toml` (Already addressed in first step, but build verification failed due to other errors).
2. Duplicated Composable functions in `CalendarScreen.kt` causing conflicting overloads.
3. Missing parameters in `CalendarViewModel` instantiation in `MainActivity.kt`.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/gradle/libs.versions.toml)
- [x] Add `benchmark-macro` to the `[libraries]` section. (Completed)

### UI Components

#### [MODIFY] [CalendarScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/calendar/CalendarScreen.kt)
- Remove duplicate definitions of `FeatureTabs`, `CalendarWidget`, `Sidebar`, `SidebarCard`, and `UpcomingEventItem`.
- Clean up the file to maintain only one version of each Composable.

### Main Activity

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Update `CalendarViewModel` instantiation to provide required `AuthRepository` and `ChatRepository` using `AppModule`.

## Verification Plan

### Automated Tests
- Run `gradle assembleDebug` to verify the project builds successfully.
