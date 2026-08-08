-- =========================================
-- KEEFTALK UNIFIED E2EE & FILE SCHEMA
-- =========================================

-- 1. USER SECURITY SETTINGS
CREATE TABLE IF NOT EXISTS public.user_security_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    encrypted_account_key TEXT,
    key_salt TEXT,
    key_nonce TEXT,
    verification_tag TEXT,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    pin_hash TEXT,
    recovery_email TEXT,
    recovery_email_verified BOOLEAN DEFAULT FALSE,
    app_lock_enabled BOOLEAN DEFAULT FALSE,
    app_lock_timeout_seconds INTEGER DEFAULT 0,
    biometric_unlock_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. CONVERSATION KEYS
CREATE TABLE IF NOT EXISTS public.conversation_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id TEXT NOT NULL, -- Changed to TEXT to match existing chats.id
    encrypted_key TEXT NOT NULL,
    nonce TEXT NOT NULL,
    version INTEGER DEFAULT 2,
    epoch INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 3. UNIFIED FILES
CREATE TABLE IF NOT EXISTS public.files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id TEXT NOT NULL, -- Changed to TEXT to match existing profiles.id
    storage_path TEXT NOT NULL,
    file_hash TEXT,
    file_size BIGINT,
    file_type TEXT,
    encryption_metadata JSONB,
    reference_count INTEGER DEFAULT 1,
    status TEXT DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

-- 4. VAULT SYSTEM
CREATE TABLE IF NOT EXISTS public.vault_folders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL, -- Changed to TEXT to match existing profiles.id
    name TEXT NOT NULL,
    color INTEGER,
    icon TEXT,
    parent_id UUID REFERENCES public.vault_folders(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.vault_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL, -- Changed to TEXT to match existing profiles.id
    file_id UUID NOT NULL REFERENCES public.files(id) ON DELETE CASCADE,
    folder_id UUID REFERENCES public.vault_folders(id) ON DELETE SET NULL,
    title TEXT DEFAULT '[Encrypted]',
    favorite BOOLEAN DEFAULT FALSE,
    locked BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}',
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 5. ATTACHMENT LINKS
CREATE TABLE IF NOT EXISTS public.message_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id TEXT NOT NULL, -- Changed to TEXT to match existing messages.id
    file_id UUID NOT NULL REFERENCES public.files(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- 6. MESSAGES (CRYPTO EXTENSION)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='messages' AND column_name='ciphertext') THEN
        ALTER TABLE public.messages ADD COLUMN ciphertext TEXT;
        ALTER TABLE public.messages ADD COLUMN nonce TEXT;
        ALTER TABLE public.messages ADD COLUMN envelope_type INTEGER DEFAULT 100;
        ALTER TABLE public.messages ADD COLUMN crypto_version INTEGER DEFAULT 1;
    END IF;
END $$;

-- =========================================
-- RLS ENABLE
-- =========================================

ALTER TABLE public.user_security_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.files ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vault_folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vault_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_attachment ENABLE ROW LEVEL SECURITY;

-- =========================================
-- RLS POLICIES (Idempotent with Type Casting)
-- =========================================

-- User Security Policies
DROP POLICY IF EXISTS "Users can manage their own security settings" ON public.user_security_settings;
CREATE POLICY "Users can manage their own security settings"
ON public.user_security_settings FOR ALL
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Conversation Key Policies
DROP POLICY IF EXISTS "Chat members can read conversation keys" ON public.conversation_keys;
CREATE POLICY "Chat members can read conversation keys"
ON public.conversation_keys FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.chat_members
        WHERE chat_id = conversation_id AND user_id = auth.uid()::text
    )
);

DROP POLICY IF EXISTS "Users can insert keys for chats they are in" ON public.conversation_keys;
CREATE POLICY "Users can insert keys for chats they are in"
ON public.conversation_keys FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.chat_members
        WHERE chat_id = conversation_id AND user_id = auth.uid()::text
    )
);

-- File Policies
DROP POLICY IF EXISTS "Owners can manage their files" ON public.files;
CREATE POLICY "Owners can manage their files"
ON public.files FOR ALL
USING (auth.uid()::text = owner_id);

DROP POLICY IF EXISTS "Recipients can view referenced files" ON public.files;
CREATE POLICY "Recipients can view referenced files"
ON public.files FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.message_attachment ma
        JOIN public.messages m ON ma.message_id = m.id
        JOIN public.chat_members cm ON m.chat_id = cm.chat_id
        WHERE ma.file_id = files.id AND cm.user_id = auth.uid()::text
    )
);

-- Vault Policies
DROP POLICY IF EXISTS "Users can manage their vault folders" ON public.vault_folders;
CREATE POLICY "Users can manage their vault folders"
ON public.vault_folders FOR ALL
USING (auth.uid()::text = user_id)
WITH CHECK (auth.uid()::text = user_id);

DROP POLICY IF EXISTS "Users can manage their vault items" ON public.vault_items;
CREATE POLICY "Users can manage their vault items"
ON public.vault_items FOR ALL
USING (auth.uid()::text = user_id)
WITH CHECK (auth.uid()::text = user_id);

-- Attachment Policies
DROP POLICY IF EXISTS "Chat members can view attachments" ON public.message_attachment;
CREATE POLICY "Chat members can view attachments"
ON public.message_attachment FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.messages m
        JOIN public.chat_members cm ON m.chat_id = cm.chat_id
        WHERE m.id = message_id AND cm.user_id = auth.uid()::text
    )
);

DROP POLICY IF EXISTS "Users can insert attachments for their messages" ON public.message_attachment;
CREATE POLICY "Users can insert attachments for their messages"
ON public.message_attachment FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.messages m
        WHERE m.id = message_id AND m.sender_id = auth.uid()::text
    )
);

-- =========================================
-- STORAGE POLICIES (Idempotent)
-- =========================================

-- Ensure 'files' bucket is private
INSERT INTO storage.buckets (id, name, public)
VALUES ('files', 'files', false)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Allow authenticated uploads to own folder" ON storage.objects;
CREATE POLICY "Allow authenticated uploads to own folder"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'files' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

DROP POLICY IF EXISTS "Allow authenticated downloads" ON storage.objects;
CREATE POLICY "Allow authenticated downloads"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'files');

DROP POLICY IF EXISTS "Allow owners to delete" ON storage.objects;
CREATE POLICY "Allow owners to delete"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'files' AND
    (storage.foldername(name))[1] = auth.uid()::text
);
