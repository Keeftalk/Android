# Keeftalk Open-Source Ecosystem Roadmap

This document outlines the public development milestones for Keeftalk's open-source transformation and pay-as-you-go cloud architecture.

---

## 🎯 Phase 1: Open-Source Foundation & Licensing (Current)
- [x] Dual licensing mapping (GPL-3.0 for Android App, AGPL-3.0 for Backend, CC BY-SA 4.0 for Docs).
- [x] Community governance documentation (`CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, `ARCHITECTURE.md`, `ROADMAP.md`).
- [x] Self-hosting deployment instructions (`docs/SELF_HOSTING.md`).

---

## ⚡ Phase 2: Pay-as-You-Go Backend & Pricing Engine
- [ ] Database migration `20260310_pay_as_you_go_billing.sql` introducing `billing_pricing`, `billing_balances`, `billing_preferences`, `billing_usage`, `billing_ledger`, `billing_invoices`, and `billing_payments`.
- [ ] Concurrency-safe atomic balance deduction logic (`deduct_prepaid_usage`).
- [ ] Authoritative Edge Functions (`billing-pricing`, `billing-usage`, `billing-invoices`, `billing-admin`).
- [ ] Cloudflare R2 presigned storage metering integration.

---

## 📱 Phase 3: Android Application Integration
- [ ] Refactor `BillingManager.kt` to eliminate legacy fixed Google Play subscriptions.
- [ ] Implement `BillingRepository.kt` & data models for backend pricing and usage telemetry.
- [ ] Rebrand Subscription UI to **Cloud Usage & Billing Dashboard** (`CloudBillingScreen.kt`).
- [ ] Implement Prepaid balance top-up & Postpaid invoice management screens.
- [ ] Ensure 100% free offline functionality across all 19+ modules.

---

## 🛡️ Phase 4: Reliability, Security & Community Self-Hosting
- [ ] Row Level Security (RLS) policies verification for multi-tenant billing isolation.
- [ ] Automated end-to-end integration tests for balance deduction & invoice generation.
- [ ] Public PR submission and community feedback evaluation.
