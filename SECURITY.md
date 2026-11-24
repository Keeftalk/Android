# Security Policy

At Keeftalk, security and user data sovereignty are fundamental design principles. We take security vulnerabilities seriously and appreciate the open-source community's efforts in identifying and reporting potential issues.

---

## 🛡️ Supported Versions

We actively provide security updates and patches for the following versions:

| Version | Supported          | Notes |
| :--- | :--- | :--- |
| `v1.x` (Latest) | :white_check_mark: | Maintained release branch |
| `< 1.0` | :x: | Legacy alpha versions (Unsupported) |

---

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability, please do **NOT** open a public issue on GitHub. Publicly disclosing a vulnerability can endanger users before a fix is available.

### Disclosure Process

1. **Submit via GitHub Private Vulnerability Reporting:**
   Use GitHub's private vulnerability reporting feature on the repository:
   Navigate to **Security** -> **Advisories** -> **Report a vulnerability**.

2. **What to include in your report:**
   - Detailed summary of the vulnerability.
   - Affected component (e.g., Vault SQLCipher key derivation, E2EE Signal protocol layer, WebRTC signaling, IPC Intent filters).
   - Step-by-step proof-of-concept (PoC) or reproduction steps.
   - Potential impact of the issue.
   - Suggested remediation or patch, if available.

3. **Response & Timeline:**
   - We will acknowledge receipt of your vulnerability report within 48 hours.
   - We will assess the report and provide status updates as we work on a resolution.
   - Once a fix is verified, a patch release will be published alongside an advisory.

---

## 🔑 Credential & Secret Management Policy

1. **Never Commit Private Credentials:**
   - No private API keys, signing keystore passwords, server credentials, or personal access tokens must ever be committed to git.
   - All release builds must use optional environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, etc.) or `keystore.properties` (which is excluded via `.gitignore`).
2. **Public Client Configuration vs Private Secrets:**
   - Client-side application keys (such as Supabase anon keys or Firebase public client configs) are intended for public distribution within Android APKs and carry scoped client-level permissions.
   - Private admin keys, database service-role secrets, and private signing credentials must remain strictly server-side or local.

---

## 🔒 Cryptographic Guidelines for Contributors

When contributing code to security-critical modules (e.g., `security/crypto/`, `data/local/`, `ui/vault/`):

- **Master Keys:** Never store master keys or unencrypted key material in memory longer than necessary. Clear byte arrays explicitly when done.
- **Randomness:** Always use `java.security.SecureRandom` for salt, IV, and nonce generation.
- **KDF:** Key derivation must use Argon2id with recommended parameters (`64MB`, `3 iterations`, `4 parallelism`).
- **Cipher:** Message and data payload encryption must use `AES-256-GCM` with non-repeating nonces.

Thank you for helping keep Keeftalk secure!
