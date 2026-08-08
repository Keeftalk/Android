# Fix Supabase Crash, FCM Token Failure, and Logging Noise

I have analyzed the logs and identified the following issues to be fixed:

1.  **Supabase Realtime Crash**: `java.lang.IllegalStateException: You cannot call postgresChangeFlow after joining the channel` in `ChatRepositoryImpl.getIncomingCalls()`. This happens because a channel with the same name might already be subscribed.
2.  **FCM Token Failure**: `LeftCompositionCancellationException` in `MainActivity.kt` when fetching the FCM token. This is due to the coroutine scope being cancelled before the 10-second delay finishes.
3.  **Logging Noise**: `EmailRepository.getAccounts()` emits too frequently, cluttering the logs.

## Proposed Changes

### Chat Component

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- Update `getIncomingCalls()` to use a unique channel name (appending a timestamp or UUID) to ensure we always start with an unsubscribed channel.
- Alternatively, ensure the channel is removed from Supabase's internal cache before creation.

### Email Component

#### [MODIFY] [EmailRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/feature/email/repository/EmailRepositoryImpl.kt)
- Add `distinctUntilChanged()` to the `getAccounts()` flow to reduce unnecessary emissions and logging.

### App Startup

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Handle `CancellationException` in the FCM token `LaunchedEffect` to avoid logging it as an error when the Composable is disposed.
- Reduce the delay if appropriate or move it to a more stable lifecycle scope if it needs to complete reliably.

## Verification Plan

### Manual Verification
- Deploy the app and check Logcat.
- Verify that the Supabase crash no longer occurs during startup or navigation.
- Verify that "Failed to get FCM token" errors are not logged upon normal UI transitions.
- Verify that `getAccounts` emissions are reduced in the logs.
