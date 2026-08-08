# Implementation Plan - Update Chat Send/Mic Button Style

The user wants the send and microphone buttons in the chat input to match the style and colors of the primary FAB "+" button (from `AdaptiveFab.kt`).

## User Review Required

> [!NOTE]
> The new style uses a vibrant cyan-to-purple gradient and a matching shadow. I will apply this to the "default" theme and potentially others to ensure consistency with the FAB as requested. I will also add the pulse effect that the FAB uses when it's in the "+" state.

## Proposed Changes

### [Chat Component]

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- Update the `ChatInput` composable's send/microphone button.
- Apply `Brush.linearGradient(colors = listOf(Color(0xFF00CCCC), Color(0xFF7D5CFF)))` for the background.
- Add a shadow with `elevation = 28.dp` and `spotColor = Color(0x4D00CCCC)`.
- Implement the pulse ring effect matching the `KeeftalkFab` pulse (scale 1.0 to 1.25, alpha 0.5 to 0.0).
- Ensure the icon tint is consistently `Color.White` for this new background.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to a chat.
- Verify the send/mic button has the new gradient and shadow.
- Verify the pulse effect is visible when the microphone button is shown (not typing).
- Verify the button turns red during recording (preserving current functional behavior).
