-- =========================================
-- KEEFTALK PRODUCTION REDEEM CODE SYSTEM
-- =========================================

-- 1. REDEEM CODES TABLE
CREATE TABLE IF NOT EXISTS public.redeem_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT UNIQUE NOT NULL,
    type TEXT NOT NULL DEFAULT 'FEATURE', -- 'PRO', 'FAMILY', 'BUNDLE', 'FEATURE'
    plan_type TEXT, -- 'PRO_MONTHLY', 'FAMILY_MONTHLY' if type is PRO/FAMILY
    storage_limit BIGINT, -- Storage limit bytes (e.g. 536870912000 for 500GB)
    feature_keys TEXT[], -- Array of feature keys like ARRAY['notes_sync', 'books_sync']
    max_uses INT DEFAULT 1,
    used_count INT DEFAULT 0,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 2. REDEEM HISTORY TABLE
CREATE TABLE IF NOT EXISTS public.redeem_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    code_id UUID REFERENCES public.redeem_codes(id) ON DELETE SET NULL,
    code TEXT NOT NULL,
    redeemed_at TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.redeem_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.redeem_history ENABLE ROW LEVEL SECURITY;

-- RLS: Authenticated users can view their own redemption history
DROP POLICY IF EXISTS "Users can view own redemption history" ON public.redeem_history;
CREATE POLICY "Users can view own redemption history"
ON public.redeem_history FOR SELECT
USING (auth.uid() = user_id);

-- 3. STORED PROCEDURE FOR REDEEMING CODES (RPC)
CREATE OR REPLACE FUNCTION public.redeem_code(p_code TEXT)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_code_rec RECORD;
    v_fkey TEXT;
    v_already_redeemed BOOLEAN;
BEGIN
    -- 1. Verify user authentication
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Authentication required. Please log in.');
    END IF;

    -- 2. Lookup code (case-insensitive)
    SELECT * INTO v_code_rec
    FROM public.redeem_codes
    WHERE LOWER(code) = LOWER(TRIM(p_code));

    IF v_code_rec.id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Invalid redemption code.');
    END IF;

    IF NOT v_code_rec.is_active THEN
        RETURN jsonb_build_object('success', false, 'error', 'This redemption code has been deactivated.');
    END IF;

    IF v_code_rec.expires_at IS NOT NULL AND v_code_rec.expires_at < now() THEN
        RETURN jsonb_build_object('success', false, 'error', 'This redemption code has expired.');
    END IF;

    IF v_code_rec.max_uses > 0 AND v_code_rec.used_count >= v_code_rec.max_uses THEN
        RETURN jsonb_build_object('success', false, 'error', 'This redemption code has reached its maximum usage limit.');
    END IF;

    -- 3. Check if user already redeemed this code
    SELECT EXISTS (
        SELECT 1 FROM public.redeem_history
        WHERE user_id = v_user_id AND code_id = v_code_rec.id
    ) INTO v_already_redeemed;

    IF v_already_redeemed THEN
        RETURN jsonb_build_object('success', false, 'error', 'You have already redeemed this code.');
    END IF;

    -- 4. Apply Benefits (Subscription / Storage / Entitlements)
    IF v_code_rec.type = 'PRO' OR v_code_rec.type = 'FAMILY' OR v_code_rec.plan_type IS NOT NULL THEN
        UPDATE public.profiles
        SET plan_type = COALESCE(v_code_rec.plan_type, 'PRO_MONTHLY')::public.subscription_plan,
            storage_limit = COALESCE(v_code_rec.storage_limit, 536870912000)
        WHERE id = v_user_id;

        INSERT INTO public.subscriptions (user_id, plan, status, order_id, purchase_token)
        VALUES (
            v_user_id,
            COALESCE(v_code_rec.plan_type, 'PRO_MONTHLY')::public.subscription_plan,
            'active',
            'REDEEM_' || v_code_rec.code,
            'REDEEM_TOKEN_' || v_code_rec.code || '_' || v_user_id
        )
        ON CONFLICT (purchase_token) DO UPDATE SET status = 'active', updated_at = now();
    END IF;

    -- Insert Feature Entitlements
    IF v_code_rec.feature_keys IS NOT NULL AND array_length(v_code_rec.feature_keys, 1) > 0 THEN
        FOREACH v_fkey IN ARRAY v_code_rec.feature_keys LOOP
            INSERT INTO public.user_feature_entitlements (user_id, feature_id, enabled, status)
            VALUES (v_user_id, v_fkey, TRUE, 'ACTIVE')
            ON CONFLICT DO NOTHING;
        END LOOP;
    END IF;

    -- 5. Record History & Update Usage Count
    INSERT INTO public.redeem_history (user_id, code_id, code)
    VALUES (v_user_id, v_code_rec.id, v_code_rec.code);

    UPDATE public.redeem_codes
    SET used_count = used_count + 1
    WHERE id = v_code_rec.id;

    -- 6. Return Success Response
    RETURN jsonb_build_object(
        'success', true,
        'type', v_code_rec.type,
        'title', 'Redemption Successful!',
        'message', 'Code redeemed successfully. Your account capabilities and feature cloud syncs have been updated.',
        'plan_type', v_code_rec.plan_type
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. HELPER FUNCTION TO GENERATE BULK RANDOM REDEEM CODES
CREATE OR REPLACE FUNCTION public.generate_bulk_redeem_codes(
    p_count INT,
    p_prefix TEXT,
    p_type TEXT DEFAULT 'PRO',
    p_plan_type TEXT DEFAULT 'PRO_MONTHLY',
    p_storage_limit BIGINT DEFAULT 536870912000,
    p_feature_keys TEXT[] DEFAULT NULL,
    p_max_uses INT DEFAULT 1,
    p_expires_in_days INT DEFAULT NULL
)
RETURNS TABLE(generated_code TEXT) AS $$
DECLARE
    i INT;
    v_random_code TEXT;
    v_expiry TIMESTAMPTZ := NULL;
BEGIN
    IF p_expires_in_days IS NOT NULL THEN
        v_expiry := now() + (p_expires_in_days || ' days')::INTERVAL;
    END IF;

    FOR i IN 1..p_count LOOP
        v_random_code := p_prefix || '-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT) FROM 1 FOR 4)) || '-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT) FROM 1 FOR 4));

        INSERT INTO public.redeem_codes (
            code, type, plan_type, storage_limit, feature_keys, max_uses, expires_at
        ) VALUES (
            v_random_code, p_type, p_plan_type, p_storage_limit, p_feature_keys, p_max_uses, v_expiry
        ) ON CONFLICT (code) DO NOTHING;

        generated_code := v_random_code;
        RETURN NEXT;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
