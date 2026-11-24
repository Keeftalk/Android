# Security Policy & Privacy Guidelines

At Keeftalk, security, privacy, and data sovereignty are fundamental design principles. We take security vulnerabilities seriously and appreciate the open-source community's help in identifying and reporting issues.

---

## 🛡️ Supported Versions

| Version | Supported | Notes |
| :--- | :--- | :--- |
| `v1.x` (Latest) | :white_check_mark: | Maintained release branch |
| `< 1.0` | :x: | Legacy alpha versions (Unsupported) |

---

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability, please **DO NOT** open a public issue on GitHub.

### Disclosure Process
1. Submit via **GitHub Private Vulnerability Reporting**:
   Navigate to **Security** -> **Advisories** -> **Report a vulnerability**.
2. Include:
   - Detailed summary & affected component (e.g., Vault SQLCipher key derivation, WebRTC signaling, Edge Functions).
   - Step-by-step Proof-of-Concept (PoC).
   - Suggested remediation if available.
3. We will acknowledge receipt within 48 hours and work on a resolution.

---

## 🔑 Credential & Secret Management Policy

1. **Never Commit Private Credentials:**
   - Service-role keys, private API keys, signing keystores, or server passwords MUST NEVER be committed to Git.
   - Use environment variables or `local.properties` / `keystore.properties` (excluded via `.gitignore`).
2. **Public Client Keys:**
   - Client-side public keys (e.g., Supabase anon key) carry scoped row-level permissions and are safe for APK distribution.

---

## 🔒 Cryptographic Guidelines

- **Master Keys:** Derived on-device via Argon2id (`64MB`, `3 iterations`, `4 parallelism`) and stored in memory only as long as necessary.
- **Local Storage:** Room databases encrypted via **SQLCipher** and bound to hardware Keystore (`AES-256-GCM`).
- **Zero Telemetry:** No ad networks, tracking SDKs, or behavioral telemetry are present in Keeftalk.
