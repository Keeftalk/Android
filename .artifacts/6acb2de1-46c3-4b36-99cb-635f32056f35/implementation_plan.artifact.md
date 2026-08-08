# Implementation Plan - Email Module Pro Redesign

This plan covers advanced synchronization reliability, UI polish for flat lists and multi-selection, and a high-end navigation experience with account avatars.

## User Review Required

> [!IMPORTANT]
> - **Continuous List Design**: I am removing the individual card containers for emails. They will now appear as a unified list with thin dividers, providing a modern "Gmail" feel.
> - **FAB Navigation Overhaul**: The "My E-Mail" row in the main app FAB will now dynamically display the profile pictures of all logged-in accounts, stacked from right-to-left.
> - **Selection Mode**: Long-press on any email will trigger a "Selection Mode" with a contextual top bar for batch operations (Delete, Archive, Star, Mark Read).

## Proposed Changes

### 1. Data Layer & Models

#### [MODIFY] [EmailModels.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/feature/email/model/EmailModels.kt)
- Add `profilePicUrl` to `EmailAccount`.
- Add `senderProfilePicUrl` to `EmailMessage`.

#### [MODIFY] [MailEntities.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/entities/MailEntities.kt)
- Update `MailAccountEntity` and `MailMessageEntity` with the new fields.
- **Migration**: Increment DB version to 72 and add a migration to add these columns.

### 2. Robust Synchronization

#### [MODIFY] [GmailSyncer.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/feature/email/data/sync/providers/GmailSyncer.kt)
- **Folder Mapping**: Strictly prioritize `INBOX` if present in labels. If a message has both `SENT` and `INBOX`, it stays in `INBOX` (this happens if you send a message to yourself or are in a thread).
- **Logging**: Add detailed logs for every folder assignment to track why emails might be "vanishing" from certain views.

### 3. UI Redesign & Polish

#### [MODIFY] [EmailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/feature/email/ui/screens/EmailScreen.kt)
- **Continuous List**: Use a flat `Surface` per item with a thin `HorizontalDivider` between them.
- **Visual Differentiation**: Dim the font color and weight for "Read" messages significantly.
- **Email Item Tweak**: Shrink the sender's email address by 60% and make it 50% transparent.
- **Multi-Selection**: Implement `onLongClick` to trigger selection mode and show the `SelectionTopAppBar`.
- **FAB Cleanup**: Delete the squared FAB inside the screen; the app now relies on the main `AdaptiveFab`.

#### [MODIFY] [EmailHtmlRenderer.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/feature/email/ui/components/EmailHtmlRenderer.kt)
- Ensure the viewport meta tag forces a mobile fit without user zooming.
- Add CSS to handle overflow-wrap for long strings.

### 4. Advanced Navigation FAB

#### [MODIFY] [AdaptiveFab.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/components/AdaptiveFab.kt)
- **`FabMenuItem`**: Add `trailingAvatars: List<String?> = emptyList()`.
- **`MenuItem`**: Render a horizontal overlapping stack of avatars on the far right if `trailingAvatars` is provided.
- **`AdaptiveFab`**: Pass the list of logged-in email account profile pictures to the "My E-Mail" menu item.

### 5. Functional Wiring

#### [MODIFY] [EmailDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/feature/email/ui/screens/EmailDetailScreen.kt)
- Fully wire "Reply" and "Forward" to open the `ComposeEmailScreen` with quoted text and correct subjects.

## Verification Plan

### Manual Verification
1.  **Avatars**: Add two Gmail accounts and verify their profile pics appear in the main FAB's "My E-Mail" row.
2.  **Selection**: Long-press an email, select 3 more, and hit "Delete". Verify all 4 are gone.
3.  **UI Style**: Verify "Read" emails look dimmer than "Unread" ones.
4.  **Rendering**: Open a complex newsletter and verify it fits the screen width perfectly.
5.  **Sync**: Verify new incoming mail appears in the "Inbox" even if it also has custom labels.
