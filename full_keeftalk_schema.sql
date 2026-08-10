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
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    encrypted_key TEXT NOT NULL,
    nonce TEXT NOT NULL,
    version INTEGER DEFAULT 2,
    epoch INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(conversation_id, user_id)
);

-- 3. UNIFIED FILES
CREATE TABLE IF NOT EXISTS public.files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id TEXT NOT NULL, -- Changed to TEXT to match existing profiles.id
    storage_path TEXT NOT NULL,
    file_hash TEXT,
    file_name TEXT,
    mime_type TEXT,
    file_size BIGINT,
    file_type TEXT,
    source_type TEXT,
    width INTEGER,
    height INTEGER,
    duration INTEGER,
    thumbnail_path TEXT,
    thumbnail_remote_path TEXT,
    thumbnail_local_path TEXT,
    thumbnail_size BIGINT,
    thumbnail_width INTEGER,
    thumbnail_height INTEGER,
    thumbnail_hmac TEXT,
    encryption_metadata JSONB,
    reference_count INTEGER DEFAULT 1,
    status TEXT DEFAULT 'ACTIVE',
    security_metadata TEXT,
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
    tags JSONB DEFAULT '[]'::jsonb,
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

-- Conversation Key Policies
DROP POLICY IF EXISTS "Chat members can read conversation keys" ON public.conversation_keys;
CREATE POLICY "Chat members can read conversation keys"
ON public.conversation_keys FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.chat_members
        WHERE chat_id::text = conversation_id::text
        AND user_id::text = auth.uid()::text
    )
);

DROP POLICY IF EXISTS "Users can insert keys for chats they are in" ON public.conversation_keys;
CREATE POLICY "Users can insert keys for chats they are in"
ON public.conversation_keys FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.chat_members
        WHERE chat_id::text = conversation_id::text
        AND user_id::text = auth.uid()::text
    )
);

-- File Policies
DROP POLICY IF EXISTS "Owners can manage their files" ON public.files;
CREATE POLICY "Owners can manage their files"
ON public.files FOR ALL
USING (auth.uid()::text = owner_id::text);

DROP POLICY IF EXISTS "Recipients can view referenced files" ON public.files;
CREATE POLICY "Recipients can view referenced files"
ON public.files FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.message_attachment ma
        JOIN public.messages m ON ma.message_id::text = m.id::text
        JOIN public.chat_members cm ON m.chat_id::text = cm.chat_id::text
        WHERE ma.file_id::text = files.id::text
        AND cm.user_id::text = auth.uid()::text
    )
);

DROP POLICY IF EXISTS "Recipients can update metadata for referenced files" ON public.files;
CREATE POLICY "Recipients can update metadata for referenced files"
ON public.files FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.message_attachment ma
        JOIN public.messages m ON ma.message_id::text = m.id::text
        JOIN public.chat_members cm ON m.chat_id::text = cm.chat_id::text
        WHERE ma.file_id::text = files.id::text
        AND cm.user_id::text = auth.uid()::text
    )
)
WITH CHECK (true);

-- Vault Policies
DROP POLICY IF EXISTS "Users can manage their vault folders" ON public.vault_folders;
CREATE POLICY "Users can manage their vault folders"
ON public.vault_folders FOR ALL
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

DROP POLICY IF EXISTS "Users can manage their vault items" ON public.vault_items;
CREATE POLICY "Users can manage their vault items"
ON public.vault_items FOR ALL
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

-- Attachment Policies
DROP POLICY IF EXISTS "Chat members can view attachments" ON public.message_attachment;
CREATE POLICY "Chat members can view attachments"
ON public.message_attachment FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.messages m
        JOIN public.chat_members cm ON m.chat_id::text = cm.chat_id::text
        WHERE m.id::text = message_id::text
        AND cm.user_id::text = auth.uid()::text
    )
);

DROP POLICY IF EXISTS "Users can insert attachments for their messages" ON public.message_attachment;
CREATE POLICY "Users can insert attachments for their messages"
ON public.message_attachment FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.messages m
        WHERE m.id::text = message_id::text
        AND m.sender_id::text = auth.uid()::text
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

-- =========================================
-- CUSTOM FUNCTIONS (RPC)
-- =========================================

-- RPC for atomic envelope addition to prevent lost updates and duplicates
-- Includes explicit authorization check for SECURITY DEFINER safety.
CREATE OR REPLACE FUNCTION public.add_file_envelope(target_file_id UUID, new_envelope JSONB)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Update the file metadata only if:
    -- 1. The caller is authorized (Owner OR has existing attachment access)
    -- 2. The envelope doesn't already exist (Duplicate prevention)
    UPDATE public.files
    SET encryption_metadata = jsonb_set(
        COALESCE(encryption_metadata, '{"envelopes": [], "fileIv": ""}'::jsonb),
        '{envelopes}',
        (COALESCE(encryption_metadata->'envelopes', '[]'::jsonb) || new_envelope)
    )
    WHERE id = target_file_id
    AND (
        owner_id::text = auth.uid()::text
        OR EXISTS (
            SELECT 1 FROM public.message_attachment ma
            JOIN public.messages m ON ma.message_id::text = m.id::text
            JOIN public.chat_members cm ON m.chat_id::text = cm.chat_id::text
            WHERE ma.file_id = target_file_id
            AND cm.user_id::text = auth.uid()::text
        )
    )
    AND NOT (COALESCE(encryption_metadata->'envelopes', '[]'::jsonb) @> new_envelope);
END;
$$;


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

-- =========================================
-- CUSTOM FUNCTIONS (RPC)
-- =========================================

-- RPC for atomic envelope addition to prevent lost updates and duplicates
-- Includes explicit authorization check for SECURITY DEFINER safety.
CREATE OR REPLACE FUNCTION public.add_file_envelope(target_file_id UUID, new_envelope JSONB)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Update the file metadata only if:
    -- 1. The caller is authorized (Owner OR has existing attachment access)
    -- 2. The envelope doesn't already exist (Duplicate prevention)
    UPDATE public.files
    SET encryption_metadata = jsonb_set(
        COALESCE(encryption_metadata, '{"envelopes": [], "fileIv": ""}'::jsonb),
        '{envelopes}',
        (COALESCE(encryption_metadata->'envelopes', '[]'::jsonb) || new_envelope)
    )
    WHERE id = target_file_id
    AND (
        owner_id::text = auth.uid()::text
        OR EXISTS (
            SELECT 1 FROM public.message_attachment ma
            JOIN public.messages m ON ma.message_id::text = m.id::text
            JOIN public.chat_members cm ON m.chat_id::text = cm.chat_id::text
            WHERE ma.file_id = target_file_id
            AND cm.user_id::text = auth.uid()::text
        )
    )
    AND NOT (COALESCE(encryption_metadata->'envelopes', '[]'::jsonb) @> new_envelope);
END;
$$;

