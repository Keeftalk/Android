# WhatsApp-like Location Sharing Implementation

Implement a comprehensive location sharing feature including static location sharing, live location sharing with duration, and map previews in chat.

## User Review Required

> [!IMPORTANT]
> Live Location sharing requires background location access and a mechanism to sync real-time updates. This implementation will focus on the UI and sending the initial live location message. Real-time updates sync might require further backend/repository implementation if not already supported.

> [!WARNING]
> Google Maps requires an API Key. Please ensure `com.google.android.geo.API_KEY` is configured in your `AndroidManifest.xml` or `secrets.properties`.

## Proposed Changes

### Dependencies

#### [MODIFY] [libs.versions.toml](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/gradle/libs.versions.toml)
- Add `play-services-maps` and `maps-compose` versions and libraries.

#### [MODIFY] [app/build.gradle.kts](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/build.gradle.kts)
- Add maps dependencies.

### Navigation and UI Structure

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Add `LocationPicker` and `LocationDetail` to `AppScreen`.
- Update `handleAttachmentAction` to navigate to `LocationPicker`.
- Add navigation handling for these new screens.

### Features

#### [NEW] [LocationPickerScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/LocationPickerScreen.kt)
- Implement a screen with a Google Map.
- "Share current location" button.
- "Share live location" button with a slider for duration (10 mins to 8 hours).
- Handle location permissions.

#### [NEW] [LocationDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/LocationDetailScreen.kt)
- Full-screen map view for a shared location.
- Support for live location tracking if applicable.

#### [MODIFY] [ChatMessageBubbles.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- Add `LocationMessageBubble` to display a map preview for location messages.
- Handle clicks to open `LocationDetailScreen`.

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- Update `onMessageClick` or `onMediaClick` to handle location messages.

## Verification Plan

### Automated Tests
- Build the project to ensure dependencies are correctly added.
- Unit tests for location formatting (if any).

### Manual Verification
- Deploy to a device.
- Open a chat and tap the "+" or attachment icon.
- Select "Location".
- Verify the map loads and shows the current location.
- Test "Share Current Location" - verify message appears in chat with map preview.
- Test "Share Live Location" - verify slider appears, allows selecting duration, and sends a message.
- Click a location message in chat and verify it opens the full map preview.
