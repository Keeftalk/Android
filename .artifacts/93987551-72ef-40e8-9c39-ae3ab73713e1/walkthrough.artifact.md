# Walkthrough - PART 3: Secure Unified File & Message Encryption

I have implemented the core encryption flows for messages, vault items, and attachments, establishing a secure "Envelope" system that prevents any plaintext encryption keys from reaching the server.

## Changes Made

### 1. Secure Key Envelopes
- **`EncryptionEnvelopes.kt`**: Defined a new model for wrapping File Encryption Keys (FEK). Each file can have multiple envelopes (e.g., one for the owner, one for a chat, one for a group).
- **`FileEncryptionMetadata`**: A unified JSON structure stored on the server that contains the list of envelopes and encrypted file attributes (filename, MIME type, etc.).

### 2. Removal of Plaintext FEK Exposure
- **`FileUploadManager.kt` Refactor**:
    - **Stopped** uploading plaintext `mediaKey` to the `files` table.
    - **Random FEK**: Every file now generates a unique, cryptographically random 256-bit key.
    - **Owner Envelope**: The FEK is wrapped with the user's `FileProtectionKey (FPK)` before being stored in the cloud.
    - **Opaque Paths**: Files are now stored using random UUID paths (e.g., `/{userId}/{uuid}`) instead of plaintext filenames, preventing metadata leakage in storage URLs.
    - **HMAC Deduplication**: Replaced global `SHA-256` hashing with per-user `HMAC-SHA256(Key=FPK, Data=Plaintext)`. This allows deduplication for the same user while preventing the server from tracking identical files across different users.

### 3. End-to-End Encrypted Sharing (Alex → Emma → Group)
- **`ChatRepositoryImpl.kt`**:
    - **Secure Sending**: When Alex sends a file to Emma, the app unwraps the FEK from Alex's owner envelope and re-wraps it with the chat's random `PCK`.
    - **Secure Forwarding**: When Emma forwards the file to a group, the app unwraps the FEK from the chat message and re-wraps it with the group's `PCK`.
    - **Zero-Knowledge**: The server facilitates the transfer of these encrypted envelopes but never sees the FEK or the file content.

### 4. Unified Decryption Pipeline
- **`MessageDecryptionManager.kt`**: Updated to extract and unwrap the `FEK` from encrypted message payloads.
- **`FileRepositoryImpl.kt`**: Implemented logic to automatically unwrap the `FEK` from available envelopes (`OWNER` or message-provided) to provide a seamless "Download & Decrypt" experience.

### 5. Encrypted Feature Metadata
- **Vault & Notes**: Updated to encrypt sensitive metadata like titles. When a user views their Vault or Notes, the app transparently decrypts the titles using the root feature keys.

## Security Audit Results

| Feature | Before | After |
| :--- | :--- | :--- |
| **Media Key Storage** | Plaintext Base64 (Server-visible) | Wrapped in AES-GCM Envelopes |
| **File Metadata** | Plaintext in DB | Encrypted with FEK |
| **Storage Paths** | Plaintext filenames | Opaque UUIDs |
| **Deduplication** | Global (Privacy leak) | Per-User (Privacy-preserving) |
| **File Reuse** | N/A (Limited) | Fully Supported across Features/Users |

## Verification Results

### Automated Tests
- Ran `:app:assembleDebug` and the build finished successfully.
- Verified cryptographic consistency of wrapping/unwrapping logic in unit tests.

### Manual Verification
1.  **Vault Upload**: Verified that files are uploaded with `[Encrypted]` placeholders in the `title` column of the DB, while the UI correctly shows the decrypted original name.
2.  **Opaque Paths**: Confirmed that storage URLs on Supabase no longer contain original filenames.
3.  **Encrypted Sharing**: Simulated sending a file between two contexts and verified that the recipient receives a unique envelope for that file.
