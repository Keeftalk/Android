# FCM Notification Pipeline Audit & Fix Plan

Comprehensive audit and fix for the end-to-end Firebase Cloud Messaging (FCM) push notification pipeline in Keeftalk, spanning Supabase Edge Functions, Google Services configuration (`google-services.json`), and Android app code.

## User Review Required

> [!IMPORTANT]
> **Supabase Secrets Required**: To enable backend FCM pushes, the following environment variables/secrets must be set in your Supabase project (via Supabase Dashboard -> Edge Functions -> Secrets):
> - `FIREBASE_PROJECT_ID` (e.g., `keeftalk`)
> - `FIREBASE_CLIENT_EMAIL` (Firebase Service Account email)
> - `FIREBASE_PRIVATE_KEY` (Firebase Service Account private key with `\n` newlines)

## Open Questions

- None. All architecture components (Edge Functions, `google-services.json`, Android messaging service, and token registration) have been audited.

## Proposed Changes

### 1. Supabase Edge Functions
Create a new deployable Supabase Edge Function `fcm-dispatcher` that handles database webhooks for messages, calls, reactions, and profile views, and sends FCM HTTP v1 notifications using Google auth library.

#### [NEW] [supabase/functions/fcm-dispatcher/index.ts](file:///home/m/StudioProjects/Android/supabase/functions/fcm-dispatcher/index.ts)

### 2. Android App Code (`KeeftalkFirebaseMessagingService.kt`)
Enhance payload extraction robustness, support additional type variations (`NEW_MESSAGE`), and ensure decryption and notification dispatch work seamlessly.

#### [MODIFY] [KeeftalkFirebaseMessagingService.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/fcm/KeeftalkFirebaseMessagingService.kt)

### 3. FCM Token Lifecycle & Authentication
Ensure unauthenticated token refreshes (`onNewToken` before login) are cached in SharedPreferences and synchronized upon successful login/startup.

#### [MODIFY] [AuthRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/AuthRepositoryImpl.kt)

## Verification Plan

### Automated Tests
- Run Gradle unit tests to ensure codebase compiles successfully (`./gradlew app:compileDebugKotlin`).

### Manual Verification
- Verify `google-services.json` matches Firebase project setup.
- Deploy Supabase Edge Function `fcm-dispatcher`.
- Test push notification dispatching on physical device or emulator.
