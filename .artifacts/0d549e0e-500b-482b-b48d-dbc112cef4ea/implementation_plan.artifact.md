# Implementation Plan: PDF Sending Fix & Enhanced Media Locking

Resolve the PDF upload failure (database constraint violation) and ensure media locking works reliably across all UI surfaces (previews and full-screen viewers).

## User Review Required

> [!IMPORTANT]
> - **PDF Data Mapping**: To satisfy the database constraints, PDFs will be stored as `type = 'FILE'` in Supabase, but our app will use the `.pdf` extension and a custom metadata flag to trigger the high-end visual UI.
> - **Locking Logic**: Locking is a "Sender-Side" control. The sender can always see their media, but the receiver's preview is completely blocked until the sender unlocks it.
> - **Viewer Interaction**: I will add a lock/unlock toggle to the top bar of the full-screen viewers for both Pictures/Videos and PDFs.

## Proposed Changes

### 1. Robust PDF Sending (Fixing Check Constraint)

#### [MODIFY] [ChatRepositoryImpl.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/repository/ChatRepositoryImpl.kt)
- In `sendMessage`, when creating the `MessageDto` for Supabase:
    - If `type == MessageType.PDF`, set the `type` field to `"FILE"`.
    - This bypasses the Supabase `messages_type_check` constraint while allowing the app to render it as a PDF based on the file extension.
- Ensure the `sanitizedFileName` logic is applied to all uploads.

### 2. Media Locking Integrity

#### [MODIFY] [ChatDetailScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/ChatDetailScreen.kt)
- Standardize the `isMe` and `mediaLocked` checks for all media types.
- Ensure the `Lock` overlay in the chat bubble covers 100% of the preview area and blocks all click interactions for the receiver.

#### [MODIFY] [PdfPreviewContent.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/components/PdfPreviewContent.kt)
- Add an explicit `isLocked` parameter to `PdfPreviewContent` to reliably stop the `PdfRenderer` from attempting to load pages when locked.

### 3. Viewer Interaction (Lock from Inside)

#### [MODIFY] [PdfViewerScreen.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/PdfViewerScreen.kt)
- Update to accept `isMe` and `isLocked` parameters.
- Add a `toggleLock` lambda.
- Integrate the Lock/Unlock button into the `TopAppBar`.

#### [MODIFY] [MainActivity.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/MainActivity.kt)
- Pass the necessary `toggleLock` callbacks to the `PdfViewer` screen.

## Verification Plan

### Manual Verification
1. **PDF Send**: Send a PDF. Verify it no longer fails with "violates check constraint".
2. **Lock from Outside**: Lock a picture from the chat bubble. Verify the receiver sees the "Locked Media" overlay immediately.
3. **Lock from Inside**: Open a PDF viewer as the sender. Tap the lock icon in the top bar. Verify the icon updates and the receiver's preview is hidden.
4. **Filename Test**: Send a file named "my doc @ 2024!.pdf". Verify it uploads and sends successfully.
