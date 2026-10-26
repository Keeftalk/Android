# Implementation Plan - Keeftalk Time, My Calls, and My Habits Fixes

Fix issues in Keeftalk Time, My Calls, and My Habits while reusing existing architecture, database models, and UI components.

## User Review Required

> [!IMPORTANT]
> - **Keeftalk Time**: Background ticks and usage accumulation are capped to prevent 25-hour spikes, and zero-usage features are hidden from the time breakdown.
> - **My Calls**: Outgoing calls default to GSM; Keeftalk audio/video calls use internet calling with graceful fallback/error messages if unavailable. Unknown contacts show the generic user icon instead of a question mark.
> - **My Habits**: Completed habits suppress reminder notifications. Pending alarms are cancelled upon completion/deletion. Custom targets and units can be added via a "+" action and persist across sessions.

## Proposed Changes

### 1. Keeftalk Time
#### [MODIFY] [TimeTrackingManager.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/TimeTrackingManager.kt)
- Cap `elapsedMillis` per tick to a maximum safe interval (e.g. 15,000ms) to eliminate background sleep/resume spikes (25 hours on Friday bug).

#### [MODIFY] [TimeOnKeeftalkViewModel.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/settings/TimeOnKeeftalkViewModel.kt)
- Filter `featureDistribution` to hide features with zero recorded usage time (`minutes <= 0f`). Ensure My Din (`MY_DIN`) is properly tracked and displayed.

### 2. My Calls & Unknown Contacts
#### [MODIFY] [AvatarUtils.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/util/AvatarUtils.kt) & [KeeftalkAvatar.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/components/KeeftalkAvatar.kt)
- Replace question mark (`?`) initials for unknown contacts with generic user icon (`icons.user`).

#### [MODIFY] [CallListScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/CallListScreen.kt) & [CallListViewModel.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/CallListViewModel.kt)
- Use GSM calling by default for dialer calls.
- Show message-circle icon (`icons.messageCircle`) for Keeftalk accounts to open chats.
- Slightly reduce GSM call icon size and add separate Keeftalk Audio-Call and Video-Call buttons using internet calling infrastructure. Display appropriate error messages if internet calling is unavailable.

### 3. My Habits
#### [MODIFY] [HabitReminderReceiver.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/util/reminders/HabitReminderReceiver.kt) & [HabitsRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/HabitsRepositoryImpl.kt)
- Check completion status before firing reminders; do not notify for completed habits.
- Cancel pending alarms via `HabitReminderManager.cancelReminder` when a habit is completed, rescheduled, or deleted.

#### [MODIFY] [AddEditHabitSheet.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/habits/AddEditHabitSheet.kt) & [HabitsViewModel.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/habits/HabitsViewModel.kt)
- Expand available target types and units. Add a "+" action to create custom targets/units and persist them in `KeeftalkStore`.

## Verification Plan

### Automated Tests
- Run `./gradlew test` (unit tests for habits, time tracking, etc.).
- Build the app via `./gradlew assembleDebug`.

### Manual Verification
- Verify time breakdown hides zero-usage features and time calculation is correct.
- Verify dialer default to GSM, message-circle icon for Keeftalk users, and separate voice/video call buttons.
- Verify unknown contacts show user icon instead of question mark.
- Verify completed habits do not receive repeat notifications.
- Verify custom habit targets/units persistence.
