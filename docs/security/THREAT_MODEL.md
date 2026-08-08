# Threat Model - Keeftalk E2E

## 1. Assets
- **User Communications:** Private messages, images, videos, audio.
- **Personal Data:** Notes, Agenda/Calendar events, Vault secrets.
- **Identity:** FCM tokens, device IDs, profile information.

## 2. Adversaries & Capabilities

### 2.1. Server-Side Attacker (Supabase/Google)
- **Capability:** Access to full Postgres database dump, full Storage bucket access, Realtime traffic monitoring, FCM payload inspection.
- **Goal:** Read user messages, steal vault secrets, or track user activity.
- **Mitigation:** E2E encryption ensures that even with full server access, the attacker sees only ciphertext.

### 2.2. Network Attacker (Man-in-the-Middle)
- **Capability:** Intercept TLS traffic, observe request patterns.
- **Goal:** Decrypt traffic or perform traffic analysis.
- **Mitigation:** TLS provides transport security; E2E provides application-level security that persists even if TLS is compromised.

### 2.3. Local Attacker (Stolen Device)
- **Capability:** Physical access to the device.
- **Goal:** Extract app data from SQLite or SharedPrefs.
- **Mitigation:** Android Keystore protection for encryption keys. High-sensitivity data (Vault) requires biometric unlock.

### 2.4. Malicious App (On-device)
- **Capability:** Read public storage, intercept intents, access clipboard.
- **Goal:** Exfiltrate Keeftalk data.
- **Mitigation:** Use of app-private storage, Android Keystore `isInsideSecureHardware`, and clipboard protection for sensitive data.

## 3. Vulnerabilities & Mitigations

| Vulnerability | Mitigation |
| :--- | :--- |
| **Server Dump** | content is E2E encrypted; keys are never sent to the server. |
| **Weak Local Encryption** | Replace static keys with Keystore-backed per-user keys. |
| **Metadata Leakage** | Encrypt filenames, sizes, and previews. Opaque FCM notifications. |
| **Key Reuse** | Implement Ratchet for messaging; unique keys per file for media. |
| **Plaintext Logging** | Audit all `Log.*` calls; implement a `SecurityLogger` that redacts payloads. |

## 4. Residual Risks
- **Metadata Analysis:** Server still knows who talks to whom and when.
- **Compromised OS:** If the Android OS itself is compromised (e.g., kernel-level malware), Keystore might be bypassed.
- **Social Engineering:** User sharing recovery phrases or being coerced into unlocking the device.
