-- ==============================================================================
-- KEEFTALK PAY-AS-YOU-GO BILLING & USAGE METERING MIGRATION
-- Timestamp: 20260310
-- Multi-Tenant Dual Payment Architecture (Prepaid & Postpaid)
-- ==============================================================================

-- 1. BILLING PRICING CONFIGURATION TABLE
CREATE TABLE IF NOT EXISTS public.billing_pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_name TEXT NOT NULL UNIQUE,
    currency TEXT NOT NULL DEFAULT 'USD',
    storage_rate_per_gb_month NUMERIC(18, 6) NOT NULL DEFAULT 0.015000,
    bandwidth_rate_per_gb NUMERIC(18, 6) NOT NULL DEFAULT 0.020000,
    read_op_rate_per_10k NUMERIC(18, 6) NOT NULL DEFAULT 0.003600,
    write_op_rate_per_10k NUMERIC(18, 6) NOT NULL DEFAULT 0.045000,
    backup_rate_per_gb_month NUMERIC(18, 6) NOT NULL DEFAULT 0.020000,
    maintenance_allocation_pct NUMERIC(6, 4) NOT NULL DEFAULT 0.1200, -- 12%
    founder_margin_pct NUMERIC(6, 4) NOT NULL DEFAULT 0.0500, -- 5%
    effective_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    effective_until TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Seed Active Backend Pricing Version (Authoritative Rates)
INSERT INTO public.billing_pricing (
    version_name,
    currency,
    storage_rate_per_gb_month,
    bandwidth_rate_per_gb,
    read_op_rate_per_10k,
    write_op_rate_per_10k,
    backup_rate_per_gb_month,
    maintenance_allocation_pct,
    founder_margin_pct,
    is_active
) VALUES (
    'v1.0-2026-authoritative',
    'USD',
    0.015000,
    0.020000,
    0.003600,
    0.045000,
    0.020000,
    0.1200,
    0.0500,
    TRUE
) ON CONFLICT (version_name) DO NOTHING;

-- 2. USER PREPAID BALANCES TABLE
CREATE TABLE IF NOT EXISTS public.billing_balances (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    prepaid_balance NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    currency TEXT NOT NULL DEFAULT 'USD',
    low_balance_threshold NUMERIC(18, 6) NOT NULL DEFAULT 2.000000,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 3. USER BILLING PREFERENCES TABLE
CREATE TABLE IF NOT EXISTS public.billing_preferences (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    payment_mode TEXT NOT NULL DEFAULT 'PREPAID', -- 'PREPAID' or 'POSTPAID'
    credit_limit NUMERIC(18, 6) NOT NULL DEFAULT 50.000000,
    spending_limit NUMERIC(18, 6) NOT NULL DEFAULT 100.000000,
    spending_alert_threshold NUMERIC(18, 6) NOT NULL DEFAULT 80.000000,
    auto_topup_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 4. MEASURED USAGE TELEMETRY TABLE
CREATE TABLE IF NOT EXISTS public.billing_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    resource_type TEXT NOT NULL, -- 'STORAGE', 'BANDWIDTH', 'READ_OPS', 'WRITE_OPS', 'BACKUPS'
    quantity NUMERIC(18, 6) NOT NULL,
    unit TEXT NOT NULL, -- 'GB_MONTH', 'GB', 'OPERATIONS'
    period_start TIMESTAMPTZ NOT NULL DEFAULT now(),
    period_end TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Index for usage range queries
CREATE INDEX IF NOT EXISTS idx_billing_usage_user_period ON public.billing_usage(user_id, period_start, period_end);

-- 5. IMMUTABLE FINANCIAL LEDGER TABLE
CREATE TABLE IF NOT EXISTS public.billing_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    transaction_type TEXT NOT NULL, -- 'USAGE_CHARGE', 'PREPAID_TOPUP', 'POSTPAID_PAYMENT', 'REFUND', 'ADJUSTMENT'
    base_amount NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    maintenance_amount NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    margin_amount NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    total_amount NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    currency TEXT NOT NULL DEFAULT 'USD',
    pricing_version_id UUID REFERENCES public.billing_pricing(id),
    description TEXT,
    idempotency_key TEXT UNIQUE,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_billing_ledger_user ON public.billing_ledger(user_id, created_at);

-- 6. PERIODIC INVOICES TABLE (POSTPAID / PERIODIC STATEMENTS)
CREATE TABLE IF NOT EXISTS public.billing_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number TEXT UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    billing_period_start TIMESTAMPTZ NOT NULL,
    billing_period_end TIMESTAMPTZ NOT NULL,
    subtotal NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    maintenance_amount NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    margin_amount NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    total NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    currency TEXT NOT NULL DEFAULT 'USD',
    status TEXT NOT NULL DEFAULT 'UNPAID', -- 'UNPAID', 'PAID', 'OVERDUE', 'CANCELLED'
    due_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_billing_invoices_user ON public.billing_invoices(user_id, status);

-- 7. PROVIDER-INDEPENDENT PAYMENT RECORDS TABLE
CREATE TABLE IF NOT EXISTS public.billing_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES public.billing_invoices(id),
    amount NUMERIC(18, 6) NOT NULL,
    currency TEXT NOT NULL DEFAULT 'USD',
    provider TEXT NOT NULL DEFAULT 'MANUAL', -- 'STRIPE', 'IN_APP_PURCHASE', 'MANUAL', 'MOCK'
    provider_transaction_id TEXT UNIQUE,
    status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'
    idempotency_key TEXT UNIQUE,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 8. PRICING AUDIT LOG TABLE
CREATE TABLE IF NOT EXISTS public.billing_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID,
    action TEXT NOT NULL,
    pricing_version_id UUID REFERENCES public.billing_pricing(id),
    changes JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 9. PROFILE BACKWARD COMPATIBILITY EXTENSIONS
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='plan_type') THEN
        ALTER TABLE public.profiles ALTER COLUMN plan_type SET DEFAULT 'FREE';
    END IF;
END $$;

-- 10. ATOMIC CONCURRENCY-SAFE PREPAID DEDUCTION RPC
CREATE OR REPLACE FUNCTION public.deduct_prepaid_usage(
    p_user_id UUID,
    p_amount NUMERIC(18, 6),
    p_idempotency_key TEXT,
    p_description TEXT
)
RETURNS BOOLEAN AS $$
DECLARE
    v_balance NUMERIC(18, 6);
    v_pricing_id UUID;
BEGIN
    -- Select current active pricing version
    SELECT id INTO v_pricing_id FROM public.billing_pricing WHERE is_active = TRUE LIMIT 1;

    -- Lock prepaid balance row for atomic update
    SELECT prepaid_balance INTO v_balance
    FROM public.billing_balances
    WHERE user_id = p_user_id
    FOR UPDATE;

    IF NOT FOUND THEN
        -- Auto-initialize balance record if missing
        INSERT INTO public.billing_balances (user_id, prepaid_balance)
        VALUES (p_user_id, 0.000000);
        v_balance := 0.000000;
    END IF;

    IF v_balance < p_amount THEN
        -- Insufficient prepaid balance
        RETURN FALSE;
    END IF;

    -- Deduct balance atomically
    UPDATE public.billing_balances
    SET prepaid_balance = prepaid_balance - p_amount,
        updated_at = now()
    WHERE user_id = p_user_id;

    -- Record immutable financial ledger entry
    INSERT INTO public.billing_ledger (
        user_id,
        transaction_type,
        base_amount,
        maintenance_amount,
        margin_amount,
        total_amount,
        pricing_version_id,
        description,
        idempotency_key
    ) VALUES (
        p_user_id,
        'USAGE_CHARGE',
        p_amount * 0.83, -- Base cost component
        p_amount * 0.12, -- 12% maintenance allocation
        p_amount * 0.05, -- 5% founder margin
        p_amount,
        v_pricing_id,
        p_description,
        p_idempotency_key
    ) ON CONFLICT (idempotency_key) DO NOTHING;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 11. ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE public.billing_pricing ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billing_balances ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billing_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billing_usage ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billing_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billing_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billing_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billing_audit_log ENABLE ROW LEVEL SECURITY;

-- Billing Pricing: Anyone authenticated can read active rates
CREATE POLICY "Authenticated users can read billing pricing"
    ON public.billing_pricing FOR SELECT
    TO authenticated
    USING (is_active = TRUE);

-- Billing Balances: Users can read their own balance
CREATE POLICY "Users can view own balance"
    ON public.billing_balances FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- Billing Preferences: Users can view & update their own preferences
CREATE POLICY "Users can view own preferences"
    ON public.billing_preferences FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update own preferences"
    ON public.billing_preferences FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id);

-- Billing Usage: Users can view their own usage records
CREATE POLICY "Users can view own usage"
    ON public.billing_usage FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- Billing Ledger: Users can view their own financial ledger entries
CREATE POLICY "Users can view own ledger"
    ON public.billing_ledger FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- Billing Invoices: Users can view their own invoices
CREATE POLICY "Users can view own invoices"
    ON public.billing_invoices FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- Billing Payments: Users can view their own payment transactions
CREATE POLICY "Users can view own payments"
    ON public.billing_payments FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- ==============================================================================
-- ROLLBACK PROCEDURE (COMMENTED FOR EMERGENCIES)
-- ==============================================================================
-- DROP FUNCTION IF EXISTS public.deduct_prepaid_usage(UUID, NUMERIC, TEXT, TEXT);
-- DROP TABLE IF EXISTS public.billing_audit_log CASCADE;
-- DROP TABLE IF EXISTS public.billing_payments CASCADE;
-- DROP TABLE IF EXISTS public.billing_invoices CASCADE;
-- DROP TABLE IF EXISTS public.billing_ledger CASCADE;
-- DROP TABLE IF EXISTS public.billing_usage CASCADE;
-- DROP TABLE IF EXISTS public.billing_preferences CASCADE;
-- DROP TABLE IF EXISTS public.billing_balances CASCADE;
-- DROP TABLE IF EXISTS public.billing_pricing CASCADE;
