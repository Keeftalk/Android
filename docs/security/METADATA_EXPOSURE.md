# Metadata Exposure - Keeftalk

This document identifies all metadata that remains visible to the server (Supabase) after the E2E migration.

## 1. Plaintext Metadata (Required for Routing/Sync)

| Metadata | Reason for Exposure |
| :--- | :--- |
| **Sender ID** | Required to identify who sent a message for routing. |
| **Chat ID** | Required to deliver messages to the correct conversation. |
| **Message ID** | Required for unique identification and status tracking. |
| **Timestamps** | Required for chronological ordering and synchronization. |
| **Message Type** | (Partial) Required for UI handling before decryption (e.g., placeholder). |
| **Status** | (SENT/DELIVERED/SEEN) Required for delivery tracking. |
| **FCM Tokens** | Required for push notification delivery. |

## 2. Encrypted Metadata (Moving from Plaintext to Ciphertext)

| Metadata | Migration Strategy |
| :--- | :--- |
| **File Names** | Encrypt and store inside the encrypted message envelope. |
| **File Sizes** | Encrypt or obfuscate to prevent traffic analysis. |
| **MIME Types** | Encrypt and store in metadata envelope. |
| **Image/Video Dimensions** | Encrypt and store in metadata envelope. |
| **Note Titles** | Encrypt and store in the `notes` table ciphertext. |
| **Vault Metadata** | Encrypt all Map entries and store as ciphertext. |

## 3. Unavoidable Metadata (Traffic Analysis Risks)

- **Message Frequency:** The server knows when and how often users communicate.
- **Message Size:** Ciphertext size correlates with plaintext size (unless padded).
- **Online Status:** Realtime presence reveals when a user is active.

## 4. Optional Metadata (To be hidden later)

- **Read Receipts:** Can be disabled per-user to hide read status from the server.
- **Typing Indicators:** Can be disabled to hide activity.
- **Member Lists:** In Group chats, member IDs are currently visible to Supabase. This could be partially obscured in a later phase.
