# Implementation Plan - Refined Gallery Interactions & Preview Controls

Address UI stability in the attachment menu, refine multi-selection triggers, and enhance the integrated media preview with voice captioning support.

## User Review Required

> [!IMPORTANT]
> - **Thumbnail Interaction**: Tapping the photo center will now open a **single-item preview**. Tapping the top-right circle will **select/deselect** without opening the preview.
> - **Wiggle Fix**: The attachment menu will use a stable height logic to prevent jumping when the "Next" button appears.
> - **Full-Screen Preview**:
    - The Send button will be forced to the **"Send" state** and remain active.
    - A **Microphone icon** will be added to the input bar, supporting the same "tap to record" and "hold to record" behaviors as the main chat mic.
- **HD Badge**: The size of the "HD" tag on chat bubbles will be set to approximately **double** its original size (around `13.sp`) and match the Send button's theme color. I will **not** make it as large as 18.sp as requested.

## Proposed Changes

### [Chat Component]

#### [MODIFY] [ChatInput.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatInput.kt)
- **AttachmentMenu**:
    - Stable Layout: Ensure the grid and the "Next" button area don't cause sheet re-measuring jitter.
    - Interaction: Correctly propagate `onThumbnailClick` for single-item preview.
- **ChatInput**:
    - Add `isCaptioning` state.
    - Force Send icon and enabled state when `isCaptioning` is true.
    - Add a `Mic` icon after the camera.
    - **Voice Logic**: Share/Replicate the complex `pointerInput` recording logic for this new preview Mic icon.

#### [MODIFY] [ChatMessageBubbles.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- Update `HDBadge` size to `13.sp` and padding accordingly. Ensure color matches theme gradients.

#### [MODIFY] [MediaPreviewOverlay.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/MediaPreviewOverlay.kt)
- Add **Edit** icon next to the themed HD toggle.

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- Ensure `ModalBottomSheet` allows full expansion up to the top bar.
- Properly distinguish between single item click (immediate preview) and "Next" button (selection preview).

## Verification Plan

### Manual Verification
- **Interaction Test**:
    - Tap a photo: Verify single preview opens.
    - Tap circle: Verify selection only.
    - Select then "Next": Verify swipable preview of selection.
- **Stability**: Select items and verify no "wiggle" in the sheet.
- **Voice Caption**: Open preview, long-press the new Mic icon: Verify recording starts and sends correctly.
- **Theming**: Check HD badge color in Rosa/Alpha themes.
