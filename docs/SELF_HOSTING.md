# Self-Hosting Keeftalk Infrastructure

This guide details how community members and organizations can deploy their own independent Keeftalk backend infrastructure.

---

## 📋 Prerequisites

1. **Supabase Instance:** Self-hosted Supabase stack or Supabase Cloud project.
2. **S3-Compatible Object Storage:** Cloudflare R2, MinIO, or AWS S3.
3. **Deno CLI & Supabase CLI:** Installed locally for Edge Function deployment.
4. **Android Studio:** For building a custom APK targeting your backend.

---

## 🛠️ Step 1: Database Setup

1. Connect to your PostgreSQL instance:
   ```bash
   supabase db push
   ```
2. Ensure all migrations up to `20260310_pay_as_you_go_billing.sql` are applied.

---

## 🛠️ Step 2: Deploy Edge Functions

Deploy the serverless Edge Functions located in `supabase/functions/`:

```bash
supabase functions deploy billing-pricing
supabase functions deploy billing-usage
supabase functions deploy billing-invoices
supabase functions deploy billing-admin
supabase functions deploy storage
```

Set environment variables in your Deno runtime:
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_ENDPOINT`

---

## 🛠️ Step 3: Configure & Build Android App

In your Android project root, add your self-hosted backend credentials to `local.properties`:

```properties
SUPABASE_URL=https://your-custom-supabase-domain.com
SUPABASE_ANON_KEY=your-custom-anon-key
```

Build your custom APK:

```bash
./gradlew assembleRelease
```

Your compiled app will now sync exclusively with your independent self-hosted cloud infrastructure!
