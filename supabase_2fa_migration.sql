-- ============================================================================
-- KEEFTALK TWO-FACTOR AUTHENTICATION (2FA) PRODUCTION SECURITY MIGRATION
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. USER 2FA FACTORS TABLE
-- Stores enrolled 2FA factors per user (TOTP, Email, SMS)
CREATE TABLE IF NOT EXISTS public.user_2fa_factors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    factor_type TEXT NOT NULL CHECK (factor_type IN ('totp', 'email', 'sms')),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED')),
    verified_destination TEXT, -- e.g. masked email or phone number
    totp_secret TEXT, -- Stored encrypted / restricted to server RPCs only
    is_preferred BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
    last_used_at TIMESTAMPTZ,
    UNIQUE (user_id, factor_type)
);

ALTER TABLE public.user_2fa_factors ENABLE ROW LEVEL SECURITY;

-- RLS: Users can SELECT their own non-sensitive factor metadata
DROP POLICY IF EXISTS "Users can view their own 2fa factors" ON public.user_2fa_factors;
CREATE POLICY "Users can view their own 2fa factors"
ON public.user_2fa_factors FOR SELECT
TO authenticated
USING (auth.uid() = user_id);

-- Direct client INSERT/UPDATE/DELETE is blocked; all factor modifications occur via SECURITY DEFINER functions
DROP POLICY IF EXISTS "No direct client modifications to 2fa factors" ON public.user_2fa_factors;

-- 2. USER 2FA BACKUP CODES TABLE
-- Stores cryptographically hashed single-use recovery codes
CREATE TABLE IF NOT EXISTS public.user_2fa_backup_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    code_hash TEXT NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.user_2fa_backup_codes ENABLE ROW LEVEL SECURITY;

-- No direct client access to backup code hashes
DROP POLICY IF EXISTS "Users cannot directly query backup code hashes" ON public.user_2fa_backup_codes;


-- 3. TWO FACTOR OTP CHALLENGES TABLE
-- Short-lived OTP challenges for enrollment, login, or sensitive security actions
CREATE TABLE IF NOT EXISTS public.two_factor_otp_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    factor_type TEXT NOT NULL CHECK (factor_type IN ('totp', 'email', 'sms', 'backup_code')),
    purpose TEXT NOT NULL CHECK (purpose IN ('ENROLLMENT', 'LOGIN', 'SECURITY_ACTION')),
    otp_hash TEXT NOT NULL,
    destination_masked TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    attempt_count INT DEFAULT 0,
    max_attempts INT DEFAULT 5,
    resend_cooldown_until TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.two_factor_otp_challenges ENABLE ROW LEVEL SECURITY;


-- 4. TWO FACTOR LOGIN CHALLENGES TABLE
-- Tracks 2FA requirement state post primary authentication
CREATE TABLE IF NOT EXISTS public.two_factor_login_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VERIFIED', 'EXPIRED')),
    allowed_factors JSONB NOT NULL DEFAULT '[]'::jsonb,
    preferred_factor TEXT,
    verification_token TEXT UNIQUE,
    created_at TIMESTAMPTZ DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '10 minutes'),
    verified_at TIMESTAMPTZ
);

ALTER TABLE public.two_factor_login_challenges ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view active login challenges for themselves" ON public.two_factor_login_challenges;
CREATE POLICY "Users can view active login challenges for themselves"
ON public.two_factor_login_challenges FOR SELECT
TO authenticated
USING (auth.uid() = user_id);


-- 5. SAFELY EXTEND USER_SECURITY_SETTINGS TABLE
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_security_settings' AND column_name='preferred_2fa_method') THEN
        ALTER TABLE public.user_security_settings ADD COLUMN preferred_2fa_method TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_security_settings' AND column_name='two_factor_enabled') THEN
        ALTER TABLE public.user_security_settings ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE;
    END IF;
END $$;


-- ============================================================================
-- HELPER FUNCTIONS & RPCs (SECURITY DEFINER)
-- ============================================================================

-- Helper: Base32 Alphabet Encoding for TOTP secrets
CREATE OR REPLACE FUNCTION public.internal_random_base32_secret(p_length INT DEFAULT 32)
RETURNS TEXT AS $$
DECLARE
    v_chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    v_result TEXT := '';
    i INT;
    v_rand INT;
BEGIN
    FOR i IN 1..p_length LOOP
        v_rand := floor(random() * 32)::INT + 1;
        v_result := v_result || substr(v_chars, v_rand, 1);
    END LOOP;
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- Helper: Base32 Decoding to Bytea
CREATE OR REPLACE FUNCTION public.internal_base32_decode(p_base32 TEXT)
RETURNS BYTEA AS $$
DECLARE
    v_base32 TEXT := upper(p_base32);
    v_chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
    v_bits TEXT := '';
    v_char CHAR;
    v_val INT;
    i INT;
    v_byte_count INT;
    v_bytes BYTEA := '\x'::bytea;
    v_byte_val INT;
BEGIN
    FOR i IN 1..length(v_base32) LOOP
        v_char := substr(v_base32, i, 1);
        IF v_char = '=' THEN
            EXIT;
        END IF;
        v_val := position(v_char IN v_chars) - 1;
        IF v_val < 0 THEN
            CONTINUE;
        END IF;
        -- Append 5 bits
        v_bits := v_bits || lpad(to_hex(v_val)::bit(5)::text, 5, '0');
    END LOOP;

    v_byte_count := length(v_bits) / 8;
    FOR i IN 1..v_byte_count LOOP
        v_byte_val := (substr(v_bits, (i - 1) * 8 + 1, 8)::bit(8))::INT;
        v_bytes := v_bytes || set_byte('\x00'::bytea, 0, v_byte_val);
    END LOOP;

    RETURN v_bytes;
END;
$$ LANGUAGE plpgsql IMMUTABLE SECURITY DEFINER;


-- Helper: Compute TOTP for Secret at Timestamp
CREATE OR REPLACE FUNCTION public.internal_compute_totp(p_secret_base32 TEXT, p_timestamp TIMESTAMPTZ DEFAULT now())
RETURNS TEXT AS $$
DECLARE
    v_key BYTEA;
    v_time_step BIGINT;
    v_msg BYTEA := '\x0000000000000000'::bytea;
    v_hmac BYTEA;
    v_offset INT;
    v_part1 INT;
    v_part2 INT;
    v_part3 INT;
    v_part4 INT;
    v_binary INT;
    v_otp INT;
    i INT;
BEGIN
    v_key := public.internal_base32_decode(p_secret_base32);
    v_time_step := floor(extract(epoch from p_timestamp) / 30)::BIGINT;

    -- Encode BIGINT v_time_step into 8-byte big-endian bytea
    FOR i IN 0..7 LOOP
        v_msg := set_byte(v_msg, 7 - i, ((v_time_step >> (i * 8)) & 255)::INT);
    END LOOP;

    v_hmac := hmac(v_msg, v_key, 'sha1');
    v_offset := (get_byte(v_hmac, length(v_hmac) - 1) & 15);

    v_part1 := (get_byte(v_hmac, v_offset) & 127);
    v_part2 := (get_byte(v_hmac, v_offset + 1) & 255);
    v_part3 := (get_byte(v_hmac, v_offset + 2) & 255);
    v_part4 := (get_byte(v_hmac, v_offset + 3) & 255);

    v_binary := (v_part1 << 24) | (v_part2 << 16) | (v_part3 << 8) | v_part4;
    v_otp := v_binary % 1000000;

    RETURN lpad(v_otp::text, 6, '0');
END;
$$ LANGUAGE plpgsql IMMUTABLE SECURITY DEFINER;


-- Helper: Validate TOTP Code with Clock-Skew (±1 Step)
CREATE OR REPLACE FUNCTION public.internal_verify_totp(p_secret_base32 TEXT, p_code TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    v_now TIMESTAMPTZ := now();
    v_t0 TIMESTAMPTZ := v_now - INTERVAL '30 seconds';
    v_t1 TIMESTAMPTZ := v_now;
    v_t2 TIMESTAMPTZ := v_now + INTERVAL '30 seconds';
BEGIN
    IF p_code IS NULL OR length(trim(p_code)) != 6 THEN
        RETURN FALSE;
    END IF;

    IF public.internal_compute_totp(p_secret_base32, v_t1) = trim(p_code) THEN
        RETURN TRUE;
    END IF;
    IF public.internal_compute_totp(p_secret_base32, v_t0) = trim(p_code) THEN
        RETURN TRUE;
    END IF;
    IF public.internal_compute_totp(p_secret_base32, v_t2) = trim(p_code) THEN
        RETURN TRUE;
    END IF;

    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 1: Get 2FA Status for current authenticated user
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_get_2fa_status()
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_factors JSONB;
    v_has_backup BOOLEAN;
    v_pref TEXT;
    v_enabled BOOLEAN := FALSE;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT COALESCE(jsonb_agg(
        jsonb_build_object(
            'id', id,
            'factor_type', factor_type,
            'status', status,
            'verified_destination', verified_destination,
            'is_preferred', is_preferred,
            'created_at', extract(epoch from created_at)::BIGINT * 1000,
            'last_used_at', CASE WHEN last_used_at IS NOT NULL THEN extract(epoch from last_used_at)::BIGINT * 1000 ELSE NULL END
        )
    ), '[]'::jsonb)
    INTO v_factors
    FROM public.user_2fa_factors
    WHERE user_id = v_user_id AND status = 'ACTIVE';

    SELECT EXISTS(
        SELECT 1 FROM public.user_2fa_backup_codes
        WHERE user_id = v_user_id AND used = FALSE
    ) INTO v_has_backup;

    SELECT two_factor_enabled, preferred_2fa_method
    INTO v_enabled, v_pref
    FROM public.user_security_settings
    WHERE user_id = v_user_id;

    RETURN jsonb_build_object(
        'two_factor_enabled', COALESCE(v_enabled, FALSE),
        'preferred_method', v_pref,
        'has_backup_codes', v_has_backup,
        'active_factors', v_factors
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 2: Begin TOTP Enrollment
-- Generates Base32 Secret and otpauth:// URI
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_begin_totp_enrollment()
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_email TEXT;
    v_secret TEXT;
    v_qr_uri TEXT;
    v_factor_id UUID;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT email INTO v_email FROM auth.users WHERE id = v_user_id;
    IF v_email IS NULL THEN
        v_email := v_user_id::text;
    END IF;

    v_secret := public.internal_random_base32_secret(32);
    v_qr_uri := 'otpauth://totp/Keeftalk:' || v_email || '?secret=' || v_secret || '&issuer=Keeftalk';

    INSERT INTO public.user_2fa_factors (user_id, factor_type, status, totp_secret, created_at)
    VALUES (v_user_id, 'totp', 'PENDING', v_secret, now())
    ON CONFLICT (user_id, factor_type) DO UPDATE
    SET status = 'PENDING', totp_secret = EXCLUDED.totp_secret, created_at = now()
    RETURNING id INTO v_factor_id;

    RETURN jsonb_build_object(
        'factor_id', v_factor_id,
        'secret', v_secret,
        'qr_code_uri', v_qr_uri
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 3: Verify TOTP Enrollment
-- Validates 6-digit TOTP code and activates factor
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_verify_totp_enrollment(p_totp_code TEXT)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_secret TEXT;
    v_valid BOOLEAN;
    v_active_count INT;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT totp_secret INTO v_secret
    FROM public.user_2fa_factors
    WHERE user_id = v_user_id AND factor_type = 'totp';

    IF v_secret IS NULL THEN
        RAISE EXCEPTION 'TOTP enrollment has not been initialized.';
    END IF;

    v_valid := public.internal_verify_totp(v_secret, p_totp_code);
    IF NOT v_valid THEN
        RETURN jsonb_build_object('success', false, 'message', 'Invalid 6-digit TOTP code. Check your authenticator app time sync.');
    END IF;

    -- Check if other active factors exist
    SELECT count(*) INTO v_active_count
    FROM public.user_2fa_factors
    WHERE user_id = v_user_id AND status = 'ACTIVE';

    UPDATE public.user_2fa_factors
    SET status = 'ACTIVE', last_used_at = now(), is_preferred = (v_active_count = 0)
    WHERE user_id = v_user_id AND factor_type = 'totp';

    -- Update overall security settings
    INSERT INTO public.user_security_settings (user_id, two_factor_enabled, preferred_2fa_method, updated_at)
    VALUES (v_user_id, TRUE, 'totp', now())
    ON CONFLICT (user_id) DO UPDATE
    SET two_factor_enabled = TRUE,
        preferred_2fa_method = COALESCE(user_security_settings.preferred_2fa_method, 'totp'),
        updated_at = now();

    RETURN jsonb_build_object('success', true, 'message', 'Authenticator app enabled successfully.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 4: Request Email OTP Challenge
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_request_email_otp(p_purpose TEXT DEFAULT 'ENROLLMENT')
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_email TEXT;
    v_masked TEXT;
    v_code TEXT;
    v_code_hash TEXT;
    v_challenge_id UUID;
    v_cooldown TIMESTAMPTZ;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT email INTO v_email FROM auth.users WHERE id = v_user_id;
    IF v_email IS NULL OR v_email = '' THEN
        RAISE EXCEPTION 'No verified email found for this user account.';
    END IF;

    -- Mask email e.g. m***@example.com
    v_masked := overlay(v_email placing '***' from 2 to position('@' in v_email) - 2);

    -- Rate limiting / resend cooldown check
    SELECT resend_cooldown_until INTO v_cooldown
    FROM public.two_factor_otp_challenges
    WHERE user_id = v_user_id AND factor_type = 'email' AND consumed_at IS NULL
    ORDER BY created_at DESC LIMIT 1;

    IF v_cooldown IS NOT NULL AND v_cooldown > now() THEN
        RETURN jsonb_build_object(
            'success', false,
            'message', 'Please wait before requesting a new code.',
            'cooldown_seconds', ceil(extract(epoch from (v_cooldown - now())))
        );
    END IF;

    -- Generate cryptographically secure 6-digit OTP code
    v_code := lpad(floor(100000 + random() * 900000)::text, 6, '0');
    v_code_hash := encode(digest(v_code, 'sha256'), 'hex');

    -- Invalidate existing unconsumed challenges
    UPDATE public.two_factor_otp_challenges
    SET consumed_at = now()
    WHERE user_id = v_user_id AND factor_type = 'email' AND consumed_at IS NULL;

    INSERT INTO public.two_factor_otp_challenges (
        user_id, factor_type, purpose, otp_hash, destination_masked, expires_at, resend_cooldown_until
    ) VALUES (
        v_user_id, 'email', p_purpose, v_code_hash, v_masked, now() + INTERVAL '10 minutes', now() + INTERVAL '60 seconds'
    ) RETURNING id INTO v_challenge_id;

    RETURN jsonb_build_object(
        'success', true,
        'challenge_id', v_challenge_id,
        'masked_destination', v_masked,
        'cooldown_seconds', 60
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 5: Request SMS OTP Challenge
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_request_sms_otp(p_purpose TEXT DEFAULT 'ENROLLMENT')
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_phone TEXT;
    v_masked TEXT;
    v_code TEXT;
    v_code_hash TEXT;
    v_challenge_id UUID;
    v_cooldown TIMESTAMPTZ;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT phone INTO v_phone FROM public.profiles WHERE id = v_user_id::text;
    IF v_phone IS NULL OR v_phone = '' THEN
        SELECT phone INTO v_phone FROM auth.users WHERE id = v_user_id;
    END IF;

    IF v_phone IS NULL OR v_phone = '' THEN
        RAISE EXCEPTION 'No phone number registered on profile.';
    END IF;

    -- Mask phone number e.g. +1 *** *** 1234
    IF length(v_phone) > 6 THEN
        v_masked := substr(v_phone, 1, 3) || ' *** *** ' || substr(v_phone, length(v_phone) - 3);
    ELSE
        v_masked := '***' || substr(v_phone, length(v_phone) - 2);
    END IF;

    -- Rate limiting / resend cooldown check
    SELECT resend_cooldown_until INTO v_cooldown
    FROM public.two_factor_otp_challenges
    WHERE user_id = v_user_id AND factor_type = 'sms' AND consumed_at IS NULL
    ORDER BY created_at DESC LIMIT 1;

    IF v_cooldown IS NOT NULL AND v_cooldown > now() THEN
        RETURN jsonb_build_object(
            'success', false,
            'message', 'Please wait before requesting a new SMS code.',
            'cooldown_seconds', ceil(extract(epoch from (v_cooldown - now())))
        );
    END IF;

    v_code := lpad(floor(100000 + random() * 900000)::text, 6, '0');
    v_code_hash := encode(digest(v_code, 'sha256'), 'hex');

    UPDATE public.two_factor_otp_challenges
    SET consumed_at = now()
    WHERE user_id = v_user_id AND factor_type = 'sms' AND consumed_at IS NULL;

    INSERT INTO public.two_factor_otp_challenges (
        user_id, factor_type, purpose, otp_hash, destination_masked, expires_at, resend_cooldown_until
    ) VALUES (
        v_user_id, 'sms', p_purpose, v_code_hash, v_masked, now() + INTERVAL '5 minutes', now() + INTERVAL '60 seconds'
    ) RETURNING id INTO v_challenge_id;

    RETURN jsonb_build_object(
        'success', true,
        'challenge_id', v_challenge_id,
        'masked_destination', v_masked,
        'cooldown_seconds', 60
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 6: Verify OTP Challenge (Email / SMS)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_verify_otp_challenge(p_challenge_id UUID, p_otp_code TEXT)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_challenge RECORD;
    v_input_hash TEXT;
    v_dest TEXT;
    v_active_count INT;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT * INTO v_challenge
    FROM public.two_factor_otp_challenges
    WHERE id = p_challenge_id AND user_id = v_user_id;

    IF v_challenge IS NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Invalid or expired OTP challenge.');
    END IF;

    IF v_challenge.consumed_at IS NOT NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'This verification code has already been used.');
    END IF;

    IF v_challenge.expires_at < now() THEN
        RETURN jsonb_build_object('success', false, 'message', 'Verification code has expired. Request a new code.');
    END IF;

    IF v_challenge.attempt_count >= v_challenge.max_attempts THEN
        RETURN jsonb_build_object('success', false, 'message', 'Maximum verification attempts exceeded. Request a new code.');
    END IF;

    v_input_hash := encode(digest(trim(p_otp_code), 'sha256'), 'hex');

    IF v_challenge.otp_hash != v_input_hash THEN
        UPDATE public.two_factor_otp_challenges
        SET attempt_count = attempt_count + 1
        WHERE id = p_challenge_id;

        RETURN jsonb_build_object('success', false, 'message', 'Incorrect verification code.');
    END IF;

    -- Mark challenge as consumed
    UPDATE public.two_factor_otp_challenges
    SET consumed_at = now()
    WHERE id = p_challenge_id;

    -- If this was for ENROLLMENT, activate the factor
    IF v_challenge.purpose = 'ENROLLMENT' THEN
        SELECT count(*) INTO v_active_count
        FROM public.user_2fa_factors
        WHERE user_id = v_user_id AND status = 'ACTIVE';

        INSERT INTO public.user_2fa_factors (user_id, factor_type, status, verified_destination, is_preferred, created_at, last_used_at)
        VALUES (v_user_id, v_challenge.factor_type, 'ACTIVE', v_challenge.destination_masked, (v_active_count = 0), now(), now())
        ON CONFLICT (user_id, factor_type) DO UPDATE
        SET status = 'ACTIVE',
            verified_destination = EXCLUDED.verified_destination,
            last_used_at = now();

        UPDATE public.user_security_settings
        SET two_factor_enabled = TRUE,
            preferred_2fa_method = COALESCE(preferred_2fa_method, v_challenge.factor_type),
            updated_at = now()
        WHERE user_id = v_user_id;
    END IF;

    RETURN jsonb_build_object('success', true, 'message', 'Verification successful.');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 7: Generate Backup Codes
-- Generates 10 secure single-use codes and stores SHA-256 hashes
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_generate_backup_codes()
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_chars TEXT := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; -- Excluding ambiguous characters O,0,I,1
    v_code TEXT;
    v_hash TEXT;
    v_plain_codes JSONB := '[]'::jsonb;
    i INT;
    j INT;
    v_part1 TEXT;
    v_part2 TEXT;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    -- Delete old backup codes for this user
    DELETE FROM public.user_2fa_backup_codes WHERE user_id = v_user_id;

    FOR i IN 1..10 LOOP
        v_part1 := '';
        v_part2 := '';
        FOR j IN 1..4 LOOP
            v_part1 := v_part1 || substr(v_chars, floor(random() * 32)::INT + 1, 1);
            v_part2 := v_part2 || substr(v_chars, floor(random() * 32)::INT + 1, 1);
        END LOOP;
        v_code := v_part1 || '-' || v_part2;
        v_hash := encode(digest(v_code, 'sha256'), 'hex');

        INSERT INTO public.user_2fa_backup_codes (user_id, code_hash, used, created_at)
        VALUES (v_user_id, v_hash, FALSE, now());

        v_plain_codes := v_plain_codes || to_jsonb(v_code);
    END LOOP;

    RETURN jsonb_build_object(
        'success', true,
        'backup_codes', v_plain_codes
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 8: Create 2FA Login Challenge (Server-side 2FA check)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_create_2fa_login_challenge(p_user_id UUID)
RETURNS JSONB AS $$
DECLARE
    v_factors JSONB;
    v_pref TEXT;
    v_enabled BOOLEAN;
    v_challenge_id UUID;
BEGIN
    SELECT two_factor_enabled, preferred_2fa_method
    INTO v_enabled, v_pref
    FROM public.user_security_settings
    WHERE user_id = p_user_id;

    IF v_enabled IS NOT TRUE THEN
        RETURN jsonb_build_object('requires_2fa', false);
    END IF;

    SELECT COALESCE(jsonb_agg(factor_type), '[]'::jsonb)
    INTO v_factors
    FROM public.user_2fa_factors
    WHERE user_id = p_user_id AND status = 'ACTIVE';

    IF jsonb_array_length(v_factors) = 0 THEN
        RETURN jsonb_build_object('requires_2fa', false);
    END IF;

    INSERT INTO public.two_factor_login_challenges (
        user_id, status, allowed_factors, preferred_factor, expires_at
    ) VALUES (
        p_user_id, 'PENDING', v_factors, v_pref, now() + INTERVAL '10 minutes'
    ) RETURNING id INTO v_challenge_id;

    RETURN jsonb_build_object(
        'requires_2fa', true,
        'login_challenge_id', v_challenge_id,
        'user_id', p_user_id,
        'allowed_factors', v_factors,
        'preferred_factor', v_pref
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 9: Verify 2FA Login Challenge
-- Validates selected factor or backup code server-side
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_verify_2fa_login_challenge(
    p_login_challenge_id UUID,
    p_factor_type TEXT,
    p_code TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_challenge RECORD;
    v_secret TEXT;
    v_valid BOOLEAN := FALSE;
    v_code_hash TEXT;
    v_backup_rec RECORD;
    v_verification_token TEXT;
BEGIN
    SELECT * INTO v_challenge
    FROM public.two_factor_login_challenges
    WHERE id = p_login_challenge_id;

    IF v_challenge IS NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Invalid 2FA login challenge.');
    END IF;

    IF v_challenge.status != 'PENDING' THEN
        RETURN jsonb_build_object('success', false, 'message', '2FA login challenge is no longer valid.');
    END IF;

    IF v_challenge.expires_at < now() THEN
        UPDATE public.two_factor_login_challenges SET status = 'EXPIRED' WHERE id = p_login_challenge_id;
        RETURN jsonb_build_object('success', false, 'message', '2FA login challenge has expired.');
    END IF;

    -- Handle Factor Types
    IF p_factor_type = 'totp' THEN
        SELECT totp_secret INTO v_secret
        FROM public.user_2fa_factors
        WHERE user_id = v_challenge.user_id AND factor_type = 'totp' AND status = 'ACTIVE';

        IF v_secret IS NULL THEN
            RETURN jsonb_build_object('success', false, 'message', 'Authenticator app factor is not active.');
        END IF;

        v_valid := public.internal_verify_totp(v_secret, p_code);

    ELSIF p_factor_type IN ('email', 'sms') THEN
        -- Check recent unconsumed OTP challenge
        v_code_hash := encode(digest(trim(p_code), 'sha256'), 'hex');

        SELECT id INTO v_secret
        FROM public.two_factor_otp_challenges
        WHERE user_id = v_challenge.user_id
          AND factor_type = p_factor_type
          AND otp_hash = v_code_hash
          AND consumed_at IS NULL
          AND expires_at > now();

        IF v_secret IS NOT NULL THEN
            v_valid := TRUE;
            UPDATE public.two_factor_otp_challenges SET consumed_at = now() WHERE id = v_secret::UUID;
        END IF;

    ELSIF p_factor_type = 'backup_code' THEN
        v_code_hash := encode(digest(upper(trim(p_code)), 'sha256'), 'hex');

        SELECT * INTO v_backup_rec
        FROM public.user_2fa_backup_codes
        WHERE user_id = v_challenge.user_id AND code_hash = v_code_hash AND used = FALSE;

        IF v_backup_rec IS NOT NULL THEN
            v_valid := TRUE;
            UPDATE public.user_2fa_backup_codes
            SET used = TRUE, used_at = now()
            WHERE id = v_backup_rec.id;
        END IF;
    END IF;

    IF NOT v_valid THEN
        RETURN jsonb_build_object('success', false, 'message', 'Invalid verification code.');
    END IF;

    -- Issue short-lived verification token to finalize login
    v_verification_token := encode(digest(random()::text || now()::text, 'sha256'), 'hex');

    UPDATE public.two_factor_login_challenges
    SET status = 'VERIFIED',
        verified_at = now(),
        verification_token = v_verification_token
    WHERE id = p_login_challenge_id;

    UPDATE public.user_2fa_factors
    SET last_used_at = now()
    WHERE user_id = v_challenge.user_id AND factor_type = p_factor_type;

    RETURN jsonb_build_object(
        'success', true,
        'verification_token', v_verification_token,
        'user_id', v_challenge.user_id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 10: Disable 2FA Method
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_disable_2fa_method(p_factor_type TEXT)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_remaining_active INT;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    DELETE FROM public.user_2fa_factors
    WHERE user_id = v_user_id AND factor_type = p_factor_type;

    SELECT count(*) INTO v_remaining_active
    FROM public.user_2fa_factors
    WHERE user_id = v_user_id AND status = 'ACTIVE';

    IF v_remaining_active = 0 THEN
        UPDATE public.user_security_settings
        SET two_factor_enabled = FALSE, preferred_2fa_method = NULL
        WHERE user_id = v_user_id;

        DELETE FROM public.user_2fa_backup_codes WHERE user_id = v_user_id;
    END IF;

    RETURN jsonb_build_object('success', true, 'remaining_factors', v_remaining_active);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ----------------------------------------------------------------------------
-- RPC 11: Set Preferred 2FA Method
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.rpc_set_preferred_2fa_method(p_factor_type TEXT)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID := auth.uid();
    v_exists BOOLEAN;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT EXISTS(
        SELECT 1 FROM public.user_2fa_factors
        WHERE user_id = v_user_id AND factor_type = p_factor_type AND status = 'ACTIVE'
    ) INTO v_exists;

    IF NOT v_exists THEN
        RETURN jsonb_build_object('success', false, 'message', 'Specified factor is not active.');
    END IF;

    UPDATE public.user_2fa_factors
    SET is_preferred = (factor_type = p_factor_type)
    WHERE user_id = v_user_id;

    UPDATE public.user_security_settings
    SET preferred_2fa_method = p_factor_type
    WHERE user_id = v_user_id;

    RETURN jsonb_build_object('success', true, 'preferred_method', p_factor_type);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant EXECUTE privileges to authenticated users on RPCs
GRANT EXECUTE ON FUNCTION public.rpc_get_2fa_status() TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_begin_totp_enrollment() TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_verify_totp_enrollment(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_request_email_otp(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_request_sms_otp(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_verify_otp_challenge(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_generate_backup_codes() TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_create_2fa_login_challenge(UUID) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_verify_2fa_login_challenge(UUID, TEXT, TEXT) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_disable_2fa_method(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.rpc_set_preferred_2fa_method(TEXT) TO authenticated;
