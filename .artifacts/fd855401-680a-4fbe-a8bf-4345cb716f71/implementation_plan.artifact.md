# Implementation Plan - Premium Vault Bottom Navigation

Upgrade the Vault bottom tabs to feel premium and professional through custom animations, glassmorphism, and haptic feedback.

## User Review Required

> [!IMPORTANT]
> - **Visual Shift**: The default Material 3 pill indicator will be replaced with a sleeker, custom-animated "glow" or "dot" system.
> - **Haptic Feedback**: Subtle vibration will be added to each tab tap to improve tactile professional feel.

## Proposed Changes

### [Premium Navigation UI]

#### [MODIFY] [VaultComponents.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultComponents.kt)
- **[MODIFY] VaultBottomNavigation**:
    - Replace `NavigationBar` with a custom `Surface` to achieve a more modern, semi-transparent "glass" look.
    - Implement a custom **Indicator**: A slim, glowing horizontal line or a subtle dot that slides between selected tabs using `animateDpAsState`.
    - **Haptic Pulse**: Integrate `LocalHapticFeedback` to trigger a light pulse on selection.
    - **Icon Scaling**: Add a subtle spring scale animation to icons when they are selected.

#### [MODIFY] [VaultScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/vault/VaultScreen.kt)
- Ensure the `Scaffold` background remains clean to complement the semi-transparent bottom bar.
- Polish tab transition animations in the `HorizontalPager` for ultra-smooth shifts.

## Verification Plan

### Automated Tests
- Build verification (`app:assembleDebug`).

### Manual Verification
- **Tactile Feedback**: Verify that tapping a tab provides a subtle, professional haptic response.
- **Visual Smoothness**: Observe the indicator sliding smoothly and icons scaling with a high-end spring effect.
- **Transparency Check**: Confirm the glassmorphism effect looks premium against different vault content.
- **Theme Consistency**: Ensure the new premium bar respects Light and Dark mode color palettes.
