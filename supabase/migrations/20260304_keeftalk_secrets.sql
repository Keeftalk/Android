-- ====================================================================
-- Keeftalk Secrets Zero-Knowledge Ciphertext Migration
-- Encrypted password credentials, password history, TOTP 2FA accounts,
-- recovery codes, secret vault items, and security settings.
-- ====================================================================

-- 1. Secret Credentials Table
CREATE TABLE IF NOT EXISTS public.secret_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    service_name TEXT NOT NULL,
    website TEXT NULL DEFAULT '',
    username TEXT NULL DEFAULT '',
    encrypted_password TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Personal',
    password_strength INT NOT NULL DEFAULT 0,
    has_two_factor BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_secret_credentials_user ON public.secret_credentials(user_id);
CREATE INDEX IF NOT EXISTS idx_secret_credentials_cat ON public.secret_credentials(category);

ALTER TABLE public.secret_credentials ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own secret credentials" ON public.secret_credentials;
CREATE POLICY "Users can manage their own secret credentials"
    ON public.secret_credentials
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 2. Secret Password History Table
CREATE TABLE IF NOT EXISTS public.secret_password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credential_id UUID NOT NULL REFERENCES public.secret_credentials(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    encrypted_password TEXT NOT NULL,
    set_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    replaced_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_secret_pw_hist_cred ON public.secret_password_history(credential_id);
CREATE INDEX IF NOT EXISTS idx_secret_pw_hist_user ON public.secret_password_history(user_id);

ALTER TABLE public.secret_password_history ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own password history" ON public.secret_password_history;
CREATE POLICY "Users can manage their own password history"
    ON public.secret_password_history
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. Secret TOTP Accounts Table
CREATE TABLE IF NOT EXISTS public.secret_totp_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    service_name TEXT NOT NULL,
    username TEXT NULL DEFAULT '',
    encrypted_secret_key TEXT NOT NULL,
    issuer TEXT NULL DEFAULT '',
    type TEXT NOT NULL DEFAULT 'TOTP',
    algorithm TEXT NOT NULL DEFAULT 'SHA1',
    digits INT NOT NULL DEFAULT 6,
    period INT NOT NULL DEFAULT 30,
    counter BIGINT NOT NULL DEFAULT 0,
    pin TEXT NULL DEFAULT '',
    color_hex TEXT NOT NULL DEFAULT '#7D5CFF',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_secret_totp_user ON public.secret_totp_accounts(user_id);

ALTER TABLE public.secret_totp_accounts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own TOTP accounts" ON public.secret_totp_accounts;
CREATE POLICY "Users can manage their own TOTP accounts"
    ON public.secret_totp_accounts
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 4. Secret Backup Codes Table
CREATE TABLE IF NOT EXISTS public.secret_backup_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    encrypted_code TEXT NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_secret_backup_codes_user ON public.secret_backup_codes(user_id);

ALTER TABLE public.secret_backup_codes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own backup codes" ON public.secret_backup_codes;
CREATE POLICY "Users can manage their own backup codes"
    ON public.secret_backup_codes
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 5. Secret Vault Items Table
CREATE TABLE IF NOT EXISTS public.secret_vault_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    item_type TEXT NOT NULL,
    title TEXT NOT NULL,
    encrypted_content TEXT NOT NULL,
    subtitle TEXT NULL DEFAULT '',
    is_locked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_secret_vault_items_user ON public.secret_vault_items(user_id);
CREATE INDEX IF NOT EXISTS idx_secret_vault_items_type ON public.secret_vault_items(item_type);

ALTER TABLE public.secret_vault_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own vault items" ON public.secret_vault_items;
CREATE POLICY "Users can manage their own vault items"
    ON public.secret_vault_items
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 6. Secret Settings Table
CREATE TABLE IF NOT EXISTS public.secret_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    auto_lock_minutes INT NOT NULL DEFAULT 5,
    block_screenshots BOOLEAN NOT NULL DEFAULT TRUE,
    clear_clipboard_seconds INT NOT NULL DEFAULT 30,
    wipe_after_failed_tries BOOLEAN NOT NULL DEFAULT TRUE,
    encrypted_backup_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    biometric_required BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.secret_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own secret settings" ON public.secret_settings;
CREATE POLICY "Users can manage their own secret settings"
    ON public.secret_settings
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
