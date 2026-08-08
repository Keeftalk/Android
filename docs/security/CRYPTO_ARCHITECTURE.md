# Unified Cryptographic Architecture - Keeftalk (v2)

## 1. Core Principles

- **Zero-Knowledge Content:** The server never sees plaintext content or media.
- **Cloud-Synchronized E2EE:** Modern encryption architecture supporting unlimited devices without complex session management.
- **AEK-Rooted trust:** Account Encryption Key (AEK) is the root of trust, synchronized across devices via a user password.
- **Authenticated Encryption:** AES-256-GCM for all messages and storage.
- **Secure Key Derivation:** Argon2id (Memory-hard) for deriving Key Encryption Keys (KEK).
- **Keystore Protection:** Local persistent keys are wrapped by the Android Keystore.

## 2. Key Hierarchy

```text
                    [USER PASSWORD]
                           │
                    [ARGON2ID KDF]
                           │
                    [KEY ENCRYPTION KEY (KEK)]
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   [ACCOUNT KEY (AEK)] [LOCAL DB KEY]     [VAULT KEYS]
   (Cloud-Synced)      (Keystore-Backed)  (AES-GCM 256)
          │                │                │
     ┌────┼────┐           │                │
     │    │    │           │                │
   PCK1  PCK2 ...      SQLCipher          Vault Items
   (Per-Conv Keys)
```

## 3. Messaging Protocol (Cloud-E2EE)

Keeftalk uses a **Per-Conversation Key (PCK)** model:
1. **Initial Setup:** On login/signup, a user-specific Account Encryption Key (AEK) is derived or recovered.
2. **Conversation Keys:** Every chat has a unique 256-bit PCK, encrypted with the user's AEK and stored on Supabase.
3. **Encryption:** Messages are encrypted using AES-256-GCM with the PCK.
4. **Synchronization:** New devices download and decrypt PCKs using the AEK, enabling immediate access to historical messages without re-establishing sessions.

## 4. Media & Attachment Encryption

1. **Generation:** Generate a random 256-bit AES key and a 96-bit IV per file.
2. **Encryption:** Encrypt file using AES-GCM.
3. **Upload:** Upload ciphertext to Supabase Storage.
4. **Key Delivery:** The media key and IV are encrypted using the **PCK** and sent as part of the message JSON payload.
5. **Thumbnails:** Encrypted using the same key as the original media.

## 5. Storage Encryption (SQLCipher)

- The entire Room Database is encrypted with SQLCipher using a 256-bit passphrase stored in `EncryptedSharedPreferences` (Keystore).
- Sensitive fields within encrypted services (Notes, Vault) may additionally use AEK-derived keys for multi-device sync.

## 6. Central Crypto Service

A dedicated `security.crypto` package exposes:
- `CryptoManager`: Main entry point for message encryption/decryption.
- `KeyManager`: Manages AEK lifecycle and derivation.
- `Argon2idManager`: Handles KEK derivation from user password.
- `ConversationKeyManager`: Handles PCK creation and caching.
- `StorageCryptoService`: Standard AES-GCM utility.
