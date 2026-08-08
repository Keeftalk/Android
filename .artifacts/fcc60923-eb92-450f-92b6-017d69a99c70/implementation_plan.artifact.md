# Keeftalk Startup Optimization: "Skeleton is Everything"

This plan focuses on eliminating the slow "UI Hydration" phase by making the initial skeleton UI the foundation of the entire app experience. We will move away from a "hard swap" between a skeleton and the full app, instead adopting a "progressive enhancement" model.

## Goal
Achieve a perceived instant launch where the initial UI is fully functional and simply gains more features (real-time updates, detailed settings) as background processes complete.

## User Review Required

> [!IMPORTANT]
> The `Crossfade` and the 2-second delay between the skeleton and the full app will be removed.
> The `MainSkeleton` and `FullAppContent` will be merged into a single, high-performance root UI.
> The high-performance native `ChatListFragment` will be the default list view for the main screen.

## Proposed Changes

### 1. ViewModel State Refinement
#### [MODIFY] [MainViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/MainViewModel.kt)
- Add `val isHydrated: StateFlow<Boolean>` to track when repositories and full states are ready.
- Ensure `onStartupComplete()` sets `isHydrated = true`.
- Seed `userPreferences` with Fast Path data immediately to prevent theme flickering.

### 2. Unified MainActivity Root
#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Remove the `if (!startupUiReady)` branching logic in `setContent`.
- Remove the root `Crossfade` component.
- Use `isHydrated` to control the display of "heavy" UI components (Search, Notification badges, FAB).

### 3. Progressive MainScaffold
#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Update `MainScaffold` to accept `isHydrated`.
- **TopBar**: Render simplified static version when not hydrated.
- **BottomBar**: Hide badges and disable non-chat tabs until hydrated.
- **Content Area**: If not hydrated OR data is still loading, show the `ChatListFragment` via `AndroidView`. This preserves the "cool, fast, clickable" skeleton.

### 4. Theme Consistency
#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Pass Fast Path values directly to `KeeftalkTheme` on first composition.

## Verification Plan

### Performance
- Use `StartupProfiler` to ensure "First UI Frame Visible" happens under 300ms.
- Verify that `isHydrated` transition happens in the background without causing a full recomposition of the scaffold.

### Manual Verification
- Verify that the app launches "instantly" into a clickable list.
- Confirm there is no "white flash" or shift in layout when the app fully hydrates.
- Verify that notification badges "pop in" once the background sync is complete.
