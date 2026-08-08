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

    -- Update conversation_keys table
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='conversation_keys' AND column_name='user_id') THEN
        ALTER TABLE public.conversation_keys ADD COLUMN user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;

        -- Try to populate user_id from chat_members if possible (heuristic)
        -- Otherwise it will be populated on next app use.
    END IF;
END $$;

-- Ensure unique constraint on (conversation_id, user_id)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'conversation_keys_conversation_id_user_id_key') THEN
        ALTER TABLE public.conversation_keys ADD CONSTRAINT conversation_keys_conversation_id_user_id_key UNIQUE (conversation_id, user_id);
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

-- 4. Conversation Key Policies (Updated for user_id)
DROP POLICY IF EXISTS "Chat members can read conversation keys" ON public.conversation_keys;
CREATE POLICY "Chat members can read conversation keys"
ON public.conversation_keys FOR SELECT
TO authenticated
USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert keys for chats they are in" ON public.conversation_keys;
CREATE POLICY "Users can insert keys for chats they are in"
ON public.conversation_keys FOR ALL
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

ALTER TABLE public.conversation_keys ENABLE ROW LEVEL SECURITY;
