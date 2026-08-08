# E2E Security Audit Report - Keeftalk (v2)

## 1. Current Architecture Overview

Keeftalk uses a Cloud-Synchronized E2EE architecture:

```text
Android (Client)
   ↓
Repositories (AEK/PCK Decryption)
   ↓
SQLCipher (Encrypted Local Storage)
   ↓
Supabase (Encrypted Remote Storage)
```

### Data Flow
1. **Outgoing Data:** UI → Repository → Encrypt with PCK (AES-GCM) → Supabase.
2. **Incoming Data:** Supabase → Realtime/Sync → Decrypt with PCK (AES-GCM) → SQLCipher → UI.

## 2. Data Classification & Security Status

| Data | Sensitivity | Current Storage | Current Encryption | Target Encryption |
| :--- | :--- | :--- | :--- | :--- |
| **Text Messages** | Critical | Room / Postgres | **AES-256-GCM (PCK)** | COMPLETED |
| **Images / Videos** | Critical | Storage / Cache | **AES-256-GCM (Media Key)** | COMPLETED |
| **Voice / Audio** | Critical | Storage / Cache | **AES-256-GCM (Media Key)** | COMPLETED |
| **Files / Attachments** | Critical | Storage / Cache | **AES-256-GCM (Media Key)** | COMPLETED |
| **Notes** | Critical | Room / Postgres | **AES-256-GCM (AEK)** | COMPLETED |
| **Agenda / Calendar** | High | Room / Postgres | **AES-256-GCM (AEK)** | COMPLETED |
| **Vault Entries** | Critical | Room / Postgres | **AES-256-GCM (AEK + Keystore)** | COMPLETED |
| **FCM Payloads** | High | Google FCM | **Opaque (Placeholder)** | COMPLETED |
| **Profiles (Bio, Phone)**| High | Postgres | TLS Only | TBD |

## 3. Threat Model

### 3.1. Protected Against
- **Compromised Supabase:** All content is ciphertext. Keys are only on client devices.
- **Malicious Admin:** Cannot access user data without user password.
- **Unlimited Device Sync:** New devices recover keys securely via AEK.

### 3.2. Technical Implementation
- **KDF:** Argon2id (iterations: 3, memory: 64MB, parallelism: 4).
- **Encryption:** AES-256-GCM.
- **Local Protection:** SQLCipher + Android Keystore.

---
*Verified by Keeftalk Security Team.*
