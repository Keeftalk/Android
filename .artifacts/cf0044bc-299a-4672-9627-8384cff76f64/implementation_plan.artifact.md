# Implementation Plan - Fix Avatar Border Issues & Tiny Avatar Styling

This plan addresses visual glitches with avatar borders on the Profile page and Chat list, and ensures the "Seen" tiny avatars follow the updated design language.

## User Review Required

> [!IMPORTANT]
> - **Profile Avatar Border**: Changing the external 4dp border from Black to White to match the "white environment" request.
> - **Chat List Trimming**: Increasing the avatar container size and adding internal padding to `ShapeableImageView` to prevent the white stroke from being clipped by the view bounds.

## Proposed Changes

### [Component: UI - Profile]

#### [MODIFY] [ProfileScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/profile/ProfileScreen.kt)
- Update the `KeeftalkAvatar` modifier in `ViewProfileContent` to use `Color.White` for its border instead of `Color.Black`.

### [Component: UI - Chat List (XML)]

#### [MODIFY] [layout_chat_item.xml](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/res/layout/layout_chat_item.xml)
- Increase `avatar_container` width/height from 56dp to 60dp.
- Center the `avatar` and `avatar_initials` within the container.
- Add `android:padding="2dp"` to the `avatar` `ShapeableImageView` to ensure the stroke is rendered fully without being trimmed by the view edges.
- Add `android:padding="1dp"` to the `status_indicator` for the same reason.

### [Component: UI - Chat Detail (Compose)]

#### [MODIFY] [ChatMessageBubbles.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- Ensure tiny seen avatars and the "seen count" badge use the same white border styling.
- Verify that `KeeftalkAvatar` handles small sizes (12dp-14dp) gracefully with the white border.

## Verification Plan

### Manual Verification
- **Profile Page**: Navigate to profile and verify the large avatar has a clean white border.
- **Chat List**: Verify that avatars in the list (both Compose and RecyclerView) have perfect circular white borders without any flat edges or trimming.
- **Seen Indicator**: Send a message and check the tiny "Seen" avatar at the bottom right. Verify it has a visible white border and matches the main avatar style.
- **Themes**: Switch between Light and Dark themes to ensure the "white environment" looks correct in both.
