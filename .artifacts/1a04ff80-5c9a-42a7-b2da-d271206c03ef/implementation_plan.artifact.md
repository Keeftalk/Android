# Feature: Integrated Content Sharing in Chat

Replace the "Video" attachment button with a multi-purpose "Share" button to allow sharing Notes, Emails, Vault files, and Agenda items directly in chat with granular permissions.

## User Review Required

> [!IMPORTANT]
> - **Video Button Replacement**: The "Video" button in the attachment menu will be replaced by a "Share" button. Users can still send videos via the "Gallery" or "Document" options if needed.
> - **Shared Sections**: New "Shared" categories will be added to the Vault and Email modules to track items shared with or by the user.
> - **Note Permissions**: Sharing a note will now prompt for access levels (Read or Read/Write) and the "Can invite others" permission.

## Proposed Changes

### Domain & Data Layer

#### [MODIFY] [Message.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/model/Message.kt)
- Add `SHARED_EMAIL`, `SHARED_VAULT_FILE`, and `SHARED_AGENDA` to `MessageType`.
- Add a `metadata: Map<String, String>?` field to `Message` to store sharing permissions and specific item details.

#### [MODIFY] [Vault.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/domain/model/Vault.kt) and [EmailModels.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/feature/email/model/EmailModels.kt)
- Add `SHARED` to `VaultTab` and `FolderType`.

#### [MODIFY] Repositories
- Update `NoteRepository`, `VaultRepository`, `EmailRepository`, and `CalendarRepository` to support querying "Shared" items.
- A "Shared" item is one that has been sent or received as a specific `MessageType` in a chat.

### UI Layer - Chat

#### [MODIFY] [ChatInput.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatInput.kt)
- Replace `AttachmentType.VIDEO` with `AttachmentType.SHARE`.
- Update `AttachmentMenu` to show "Share" icon (using `LucideIcons.Share2` or similar).
- Implement a `ShareSelectionBottomSheet` that appears when "Share" is clicked, providing options for:
    - **Notes**: Opens `NotePicker`.
    - **Emails**: Opens `EmailPicker`.
    - **Vault**: Opens `VaultPicker`.
    - **Agenda**: Opens `AgendaPicker`.

#### [NEW] Picker Components
- Create reusable picker modals for each content type.
- **NotePicker**: Includes a permission selector (Read/Write, Can Invite).

#### [MODIFY] [ChatMessageBubbles.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/ChatMessageBubbles.kt)
- Add rendering logic for `SHARED_EMAIL`, `SHARED_VAULT_FILE`, and `SHARED_AGENDA` message bubbles.

### UI Layer - Features

#### [MODIFY] Feature Screens
- Update `VaultScreen`, `EmailScreen`, and `CalendarScreen` to include a "Shared" view/tab that lists items discovered from chat history.

## Verification Plan

### Automated Tests
- Unit tests for repository filtering logic for "Shared" items.
- UI tests for the new `ShareSelectionBottomSheet`.

### Manual Verification
1. **Sharing Flow**:
    - Open a chat, tap "+", tap "Share".
    - Select "Note", choose a note, set "Read/Write" and "Invite others = true", then send.
    - Verify the message appears in chat with the correct badge.
2. **Recipient Experience**:
    - Log in as the recipient.
    - Open the chat and tap the shared note.
    - Verify it opens with edit permissions.
3. **Integrated Views**:
    - Go to "My Emails". Verify the shared email appears in the "Shared" folder.
    - Go to "My Vault". Verify the shared file appears in the "Shared" section.
