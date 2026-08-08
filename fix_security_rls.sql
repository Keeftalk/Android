-- 1. Ensure columns exist (Idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_security_settings' AND column_name='encrypted_account_key') THEN
        ALTER TABLE public.user_security_settings ADD COLUMN encrypted_account_key TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_security_settings' AND column_name='key_salt') THEN
        ALTER TABLE public.user_security_settings ADD COLUMN key_salt TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_security_settings' AND column_name='key_nonce') THEN
        ALTER TABLE public.user_security_settings ADD COLUMN key_nonce TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_security_settings' AND column_name='verification_tag') THEN
        ALTER TABLE public.user_security_settings ADD COLUMN verification_tag TEXT;
    END IF;
END $$;

-- 2. Correct RLS Policy to allow INSERTs
-- The previous "FOR ALL" with only "USING" might block INSERTS because there's no row yet.
-- We need "WITH CHECK" for INSERTS and UPDATES.

DROP POLICY IF EXISTS "Users can manage their own security settings" ON public.user_security_settings;

CREATE POLICY "Users can manage their own security settings"
ON public.user_security_settings
FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- 3. Verify Table exists and is accessible
ALTER TABLE public.user_security_settings ENABLE ROW LEVEL SECURITY;
