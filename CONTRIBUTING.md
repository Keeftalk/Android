# Contributing to Keeftalk

First off, thank you for considering contributing to Keeftalk! We welcome contributions from developers worldwide to build an open-source, privacy-first Super App ecosystem.

---

## 📜 Principles & Rules

1. **100% Open Source:** All first-party Keeftalk code is licensed under GPLv3 (Android Application) or AGPLv3 (Backend Infrastructure).
2. **Zero Feature Paywalls:** Never submit code that locks local application features, restricts offline data access, or requires a subscription.
3. **Strict Git Workflow:**
   - **NEVER** commit directly to `main`.
   - **NEVER** push directly to `main`.
   - Always create dedicated topic branches (e.g., `feature/open-source-foundation`, `fix/chat-recomposition`).
   - Submit clean, well-tested Pull Requests for maintainer review.

---

## 🛠️ Quick Start & Developer Setup

### 1. Fork & Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/Android.git
cd Android
git remote add upstream https://github.com/Keeftalk/Android.git
```

### 2. Environment & Tooling
- **Android Studio:** Ladybug (2024.2.1) or newer.
- **JDK:** Java 17 or 21.
- **Android SDK:** API Level 37 (Android 16).

### 3. Local Configuration
Keeftalk is built offline-first and compiles immediately out of the box.
- To configure custom backend endpoints, create `local.properties`:
  ```properties
  SUPABASE_URL=https://your-self-hosted-instance.supabase.co
  SUPABASE_ANON_KEY=your-anon-key
  ```

---

## 🌿 Mandatory Branching & Commit Guidelines

### Branch Naming Conventions
- `feature/<feature-description>`: New application features or backend components.
- `fix/<bug-description>`: Bug fixes.
- `docs/<documentation-update>`: Documentation improvements.

### Commit Messages
Use imperative mood in subject lines (limit to 72 characters):
```text
feat(billing): implement prepaid balance deduction logic

Integrate atomic SQL function for concurrency-safe prepaid deductions.
Adds unit tests for zero balance cloud upload pause.
```

---

## 🔀 Submitting Pull Requests (PRs)

Before opening a PR:
1. Run local tests:
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```
2. Verify security hygiene:
   - **NEVER** commit service-role keys, private API keys, keystores, or credentials.
3. Submit your PR against `main`. Provide detailed information on:
   - What changed & why.
   - Database migration impacts (if any).
   - Security considerations.
   - Verification steps performed.

---

## 🛡️ Security Disclosures

To report security vulnerabilities, please refer to [`SECURITY.md`](SECURITY.md) and submit reports via GitHub Private Vulnerability Reporting.
