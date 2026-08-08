# Implementation Plan - Smart Calling Logic

This plan implements "Smart Calling" logic for the Calls screen. When the call icon is clicked, the app will automatically decide whether to use a Keeftalk call (if the user is online) or a GSM call (fallback).

## User Review Required

> [!NOTE]
> - **Smart Decision**: The logic will check the user's online status (last seen < 60s) before choosing Keeftalk.
> - **Fallback**: If the user is offline or not registered on Keeftalk, a standard GSM call will be initiated via the system dialer.

## Proposed Changes

### UI Layer

#### [MODIFY] [CallListViewModel.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/CallListViewModel.kt)
- Add `smartCall(phoneOrId: String)` method.
- This method will:
    1. Check if the input is a UUID (peerId) or a phone number.
    2. Lookup the profile to check `lastSeen` status.
    3. Emit `DialerEvent.NavigateToCall` for Keeftalk or `DialerEvent.LaunchGsmCall` for GSM.

#### [MODIFY] [CallListScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/CallListScreen.kt)
- Update `RecentCallsList` and `ContactsList` to call `viewModel.smartCall(input)` when the phone icon is clicked.
- Remove the external `onCallStarted` dependency for these specific icons to centralize the logic in the ViewModel.

---

### Verification Plan

### Manual Verification
1. Open the "Calls" screen.
2. Click the call icon for an **online** Keeftalk user.
    - Verify: Keeftalk call activity starts.
3. Click the call icon for an **offline** Keeftalk user.
    - Verify: System dialer opens for a GSM call.
4. Click the call icon for a **non-Keeftalk** contact.
    - Verify: System dialer opens for a GSM call.
