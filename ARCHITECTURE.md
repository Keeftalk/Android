# Keeftalk System Architecture

Keeftalk is built as a zero-knowledge, privacy-first Super App for Android paired with a serverless pay-as-you-go backend infrastructure.

---

## 🏛️ High-Level System Architecture

```mermaid
graph TD
    subgraph Android["Android Client (GPLv3)"]
        UI["Jetpack Compose UI Layer"]
        VM["ViewModels & Repositories"]
        RoomDB[("SQLCipher Local Room DB<br/>Encrypted Offline Storage")]
        KeyStore["Android Keystore System"]
    end
    
    subgraph Backend["Backend Infrastructure (AGPLv3)"]
        SupaAuth["Supabase Auth & RLS"]
        DB[("PostgreSQL Database<br/>Pricing, Balances, Ledger")]
        Functions["Supabase Edge Functions<br/>(Deno / TypeScript)"]
        R2[("Cloudflare R2 Storage<br/>Encrypted Objects")]
    end
    
    UI --> VM
    VM --> RoomDB
    RoomDB <--> KeyStore
    VM <--> SupaAuth
    VM <--> Functions
    Functions <--> DB
    Functions <--> R2
```

---

## 🔒 Security & Data Sovereignty Architecture

1. **Zero-Knowledge Encryption:**
   - Account Encryption Keys (AEK) and Per-Conversation Keys (PCK) derived on-device using Argon2id.
   - Files and message payloads encrypted via `AES-256-GCM` before uploading to Cloudflare R2 or Supabase DB.
2. **Offline-First Storage:**
   - All 19+ Super App modules store data locally in Room encrypted with **SQLCipher**.
   - Cloud synchronization is completely optional and non-blocking.
3. **Multi-Tenant Row Level Security (RLS):**
   - User database tables (`billing_balances`, `billing_usage`, `billing_invoices`) enforce strict RLS policies (`auth.uid() = user_id`).

---

## 💳 Dual Pay-as-You-Go Billing Architecture

```mermaid
sequenceDiagram
    autonumber
    actor User as Android App User
    participant App as Android Client
    participant EF as billing-usage Edge Function
    participant DB as PostgreSQL (billing_*)
    participant R2 as Cloudflare R2 Bucket

    User->>App: Perform Cloud Upload / Sync
    App->>EF: Request Upload Presigned URL
    EF->>DB: Check Prepaid Balance / Credit Limit
    alt Balance <= 0 (Prepaid) or Credit Exceeded (Postpaid)
        EF-->>App: 402 Payment Required (Cloud Upload Paused)
        Note over App: Local features & data remain 100% accessible!
    else Balance OK
        EF-->>App: Return R2 Presigned Upload URL
        App->>R2: Upload Encrypted Ciphertext
        App->>EF: Finalize Upload (Verify HEAD size)
        EF->>DB: Deduct Usage / Record Ledger Transaction
    end
```

### Formula for Customer Rates
$$\text{Rate} = \text{Base Cost} \times (1 + 12\% \text{ Maintenance Allocation}) \times (1 + 5\% \text{ Founder Margin})$$

Rates are authoritatively managed by the backend (`billing_pricing` table) with decimal precision (`NUMERIC(18,6)`).
