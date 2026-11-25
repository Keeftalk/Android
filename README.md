<p align="center">
  <img src="https://keeftalk.com/keeftalk_logo.png" alt="Keeftalk Logo" width="180"/>
</p>

<h1 align="center">Keeftalk</h1>

<p align="center">
  <b>The Open-Source, Privacy-First Super App & Sustainable Cloud Ecosystem for Android</b><br/>
  100% Free Application Features • Offline-First Architecture • Zero Ads & Tracking • Pay-as-You-Go Cloud Business Model
</p>

<p align="center">
  <a href="https://github.com/Keeftalk/Android/releases/latest"><img src="https://img.shields.io/github/v/release/Keeftalk/Android?label=Version&style=for-the-badge&logo=android&logoColor=white&color=00C853" alt="Latest Version"></a>
  <a href="https://github.com/Keeftalk/Android/releases"><img src="https://img.shields.io/github/downloads/Keeftalk/Android/total?label=Downloads&style=for-the-badge&logo=android&logoColor=white&color=00C853" alt="Downloads"></a>
  <a href="#-licensing"><img src="https://img.shields.io/badge/License-GPLv3%20%7C%20AGPLv3-blue?style=for-the-badge&logo=gnu" alt="License"></a>
  <a href="#-pay-as-you-go-cloud-business-model"><img src="https://img.shields.io/badge/Business%20Model-Pay--As--You--Go-00E676?style=for-the-badge&logo=supabase" alt="Business Model"></a>
</p>

---

## 📖 Mission & Core Philosophy

Keeftalk is built on a non-negotiable principle:

> **Keeftalk is free to use, free to modify, free to fork, free to self-host, and free from advertisements and tracking.**

Users never pay subscriptions or purchase tiers to access application features. All 19+ Super App tools are 100% unlocked out-of-the-box for everyone. Payments apply exclusively to actual cloud infrastructure resources consumed (storage, bandwidth, operations, backups), allocated maintenance costs, and a transparent founder margin.

---

## 🌟 Key Features & Unlocked Modules

All features operate **offline-first** locally on your Android device without requiring a paid cloud account:

1. **My Chats:** End-to-end encrypted messaging (Argon2id + AES-256-GCM), voice notes, reaction pickers, and threaded replies.
2. **My Calls:** Low-latency WebRTC encrypted HD audio and video calls.
3. **My SMS/MMS:** Native Android telephony client for carrier dual-SIM SMS/MMS.
4. **My Notes:** WYSIWYG rich text editor with live Markdown preview, audio notes, and tag organization.
5. **My Vault:** Isolated SQLCipher hardware-backed secure storage protected by biometric authentication.
6. **My Secrets:** Built-in TOTP Two-Factor Authenticator (Google Authenticator / Aegis compatible) and credential manager.
7. **My Calendar:** Encrypted agenda scheduler, birthdates, meetings, and priority task alerts.
8. **My Wallet:** Payment log tracker and digital asset fund manager.
9. **My Finances:** Expense/income budgeting tools and recurring payment management.
10. **My E-Mail:** Secure email client with avatar previews and thread management.
11. **My Feed:** Distraction-free RSS/Atom XML feed reader with article extraction.
12. **My Books:** Full EPUB/PDF e-book reader with custom typography, dark mode, and gamified **Reading XP**.
13. **My Docs:** Multi-format document viewer, search, and page thumbnail navigation.
14. **My Market:** Real-time stock indexes, cryptocurrency tickers, and interactive candlestick charts.
15. **My Habits:** Custom recurring schedules, completion heatmaps, and streak counters.
16. **My Todo:** Priority task checklists with due date notifications.
17. **My Browser:** Privacy browser with built-in ad blocker and real-time environmental **Astronomical Sky Engine**.
18. **My Maps:** Interactive saved places and MapLibre GL navigation.
19. **My Din:** Spiritual schedule tracker, Qibla compass, and reminders.

---

## 💡 Pay-as-You-Go Cloud Business Model

Keeftalk replaces traditional monthly subscription tiers (`Plus`, `Pro`, `Family`) with a **genuine consumption-based billing system**.

### Dual Payment Support
- **Prepaid Balance:** Users top up a balance. Usage (storage, bandwidth, API ops) is continuously calculated using backend-authoritative rates and atomically deducted. Zero balance gracefully pauses cloud uploads, but **never locks local features or local data**.
- **Postpaid Invoicing:** Users receive periodic invoices based on usage throughout the billing cycle with configurable credit limits and spending alerts.

### Backend-Authoritative Pricing Formula
Pricing rates are managed entirely by the backend pricing engine (`billing_pricing`) and calculated using precise decimal arithmetic:

$$\text{Customer Unit Rate} = \text{Base Infrastructure Cost} \times (1 + 12\% \text{ Maintenance Allocation}) \times (1 + 5\% \text{ Founder Margin})$$

- **Base Infrastructure Costs:** Measured Cloudflare R2 object storage, Supabase database storage, Edge Function compute, and egress bandwidth.
- **12% Maintenance Allocation:** Proportional contribution covering server management, security monitoring, domain SSL, and database administration.
- **5% Founder Margin:** Small, transparent fee supporting ongoing open-source Keeftalk development.

---

## 🌐 Self-Hosting & Independence

Keeftalk is designed for complete digital sovereignty:
- **Self-Hosted Infrastructure:** Deploy your own Supabase instance and Cloudflare R2 bucket.
- **Custom Android Build:** Target your self-hosted instance by setting `SUPABASE_URL` and `SUPABASE_ANON_KEY` in `local.properties`.
- Instructions are documented in [`docs/SELF_HOSTING.md`](docs/SELF_HOSTING.md).

---

## ⚖️ Licensing

Keeftalk is licensed under explicit open-source terms:
- **Android Application (`app/`):** GNU General Public License v3.0 or later ([GPL-3.0-or-later](LICENSE)).
- **Backend Infrastructure (`supabase/`):** GNU Affero General Public License v3.0 or later ([AGPL-3.0-or-later](LICENSE)).
- **Documentation & Specs:** Creative Commons Attribution-ShareAlike 4.0 ([CC BY-SA 4.0](LICENSE)).

See [`LICENSE`](LICENSE) for full details and third-party attribution notices.

---

## 🤝 Contributing

We welcome community contributions! Please review [`CONTRIBUTING.md`](CONTRIBUTING.md) for developer setup, code style guidelines, Git branch requirements, and Pull Request procedures.
