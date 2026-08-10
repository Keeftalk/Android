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
    END IF;

    -- Update vault_items table
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vault_items' AND column_name='tags') THEN
        ALTER TABLE public.vault_items ADD COLUMN tags JSONB DEFAULT '[]'::jsonb;
    END IF;

    -- Ensure user_id exists in vault_folders
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vault_folders' AND column_name='user_id') THEN
        ALTER TABLE public.vault_folders ADD COLUMN user_id TEXT;
    END IF;

    -- Update files table (Ensure all metadata columns exist)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='file_name') THEN
        ALTER TABLE public.files ADD COLUMN file_name TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='mime_type') THEN
        ALTER TABLE public.files ADD COLUMN mime_type TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='source_type') THEN
        ALTER TABLE public.files ADD COLUMN source_type TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='width') THEN
        ALTER TABLE public.files ADD COLUMN width INTEGER;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='height') THEN
        ALTER TABLE public.files ADD COLUMN height INTEGER;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='duration') THEN
        ALTER TABLE public.files ADD COLUMN duration INTEGER;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='thumbnail_path') THEN
        ALTER TABLE public.files ADD COLUMN thumbnail_path TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='files' AND column_name='security_metadata') THEN
        ALTER TABLE public.files ADD COLUMN security_metadata TEXT;
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
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

-- 3. Verify Table exists and is accessible
ALTER TABLE public.user_security_settings ENABLE ROW LEVEL SECURITY;

-- 4. Conversation Key Policies (Updated for user_id)
DROP POLICY IF EXISTS "Chat members can read conversation keys" ON public.conversation_keys;
CREATE POLICY "Chat members can read conversation keys"
ON public.conversation_keys FOR SELECT
TO authenticated
USING (auth.uid()::text = user_id::text);

DROP POLICY IF EXISTS "Users can insert keys for chats they are in" ON public.conversation_keys;
CREATE POLICY "Users can insert keys for chats they are in"
ON public.conversation_keys FOR ALL
TO authenticated
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

ALTER TABLE public.conversation_keys ENABLE ROW LEVEL SECURITY;

-- 5. Vault Policies (Ensuring strict privacy)
DROP POLICY IF EXISTS "Users can manage their vault folders" ON public.vault_folders;
CREATE POLICY "Users can manage their vault folders"
ON public.vault_folders FOR ALL
TO authenticated
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

DROP POLICY IF EXISTS "Users can manage their vault items" ON public.vault_items;
CREATE POLICY "Users can manage their vault items"
ON public.vault_items FOR ALL
TO authenticated
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

ALTER TABLE public.vault_folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vault_items ENABLE ROW LEVEL SECURITY;
