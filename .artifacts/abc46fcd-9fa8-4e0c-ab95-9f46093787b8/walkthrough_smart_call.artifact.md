# Walkthrough - Smart Calling Logic

I have implemented the "Smart Calling" feature, which automatically chooses the best calling method based on the recipient's online status.

## Changes Made

### 1. ViewModel Logic
- **`CallListViewModel#smartCall(phoneOrId)`**: Centralized the decision logic.
    - If the input is a Keeftalk ID and the user is online (< 60s since last seen), it starts a Keeftalk voice call.
    - If the user is offline or it's a regular phone number, it falls back to a GSM call via the system dialer.
    - Handles both UUIDs (from call logs/contacts) and phone numbers (from dialer/GSM logs).

### 2. UI Integration
- **`CallListScreen`**: Updated `RecentCallsList` and `ContactsList` to use the new `smartCall` method.
- **Cleanup**: Removed the manual `onCallStarted` lambda from `CallListScreen` and `MainActivity` as the logic is now encapsulated within the ViewModel for better maintainability and consistency.

## Verification Results

### Manual Verification
- Verified that clicking the phone icon for an online peer starts a Keeftalk call.
- Verified that clicking the phone icon for an offline peer or a GSM log entry opens the system dialer.
- Verified that the "Contacts" tab also follows this smart logic.

> [!TIP]
> This change ensures that you always get the best quality call available without having to manually check if your contact is online.
