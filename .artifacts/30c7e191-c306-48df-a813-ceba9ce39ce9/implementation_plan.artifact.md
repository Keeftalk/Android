# Professional Landing Page Implementation Plan

The goal is to create a professional, engaging landing page for Keeftalk that introduces users to the app's core features before they sign up or log in.

## User Review Required

> [!IMPORTANT]
> The landing page will be shown to all unauthenticated users who haven't completed the onboarding. We need to ensure the messaging aligns with the brand.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [UserPreferences.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/prefs/UserPreferences.kt)
- Add `isOnboardingCompleted: Boolean = false` to the `UserPreferences` data class.

#### [MODIFY] [UserPreferencesRepository.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/prefs/UserPreferencesRepository.kt)
- Add `ONBOARDING_COMPLETED` to `PreferencesKeys`.
- Add `isOnboardingCompletedFast()` and `updateIsOnboardingCompleted(completed: Boolean)` methods.
- Update `userPreferencesFlow` to include the new field.

### [UI Layer]

#### [NEW] [LandingScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/auth/LandingScreen.kt)
- Create a new Composable `LandingScreen`.
- Implement a `HorizontalPager` to showcase features:
    - **Secure Communication**: End-to-end encrypted chats and calls.
    - **Private Vault**: Securely store your sensitive files and passwords.
    - **Integrated Tools**: Seamless access to Email, SMS, Notes, and Calendar.
- Add "Get Started" (Primary CTA) and "Log In" (Secondary CTA) buttons.
- Use smooth animations and high-quality icons/graphics.

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Add `Landing` to `AppScreen` sealed class.
- Update `FullAppContent` to handle the `Landing` state.
- Logic: If `!state.isLogged` and `!userPrefs.isOnboardingCompleted`, show `LandingScreen`.

#### [MODIFY] [AuthScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/auth/AuthScreen.kt)
- (Optional) Small tweaks to ensure consistency with the new landing page.

## Verification Plan

### Automated Tests
- N/A (UI focused change, manual verification preferred for "feel")

### Manual Verification
1. Clear app data.
2. Launch the app -> Should see the new Landing Page.
3. Swipe through features -> Observe animations.
4. Click "Get Started" -> Should navigate to Signup in AuthScreen.
5. Click "Log In" -> Should navigate to Login in AuthScreen.
6. Complete Auth -> Should land in the Main app.
7. Relaunch app while logged in -> Should NOT see landing page.
8. Log out -> Should see Landing Page again (since it's for unauthenticated users).
