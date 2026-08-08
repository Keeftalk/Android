-- =========================
-- NOTES TABLE INDEXES
-- =========================
CREATE INDEX IF NOT EXISTS notes_updated_at_idx ON public.notes(updated_at);
CREATE INDEX IF NOT EXISTS notes_owner_id_idx ON public.notes(owner_id);
CREATE INDEX IF NOT EXISTS notes_owner_updated_idx ON public.notes(owner_id, updated_at);

-- =========================
-- OTHER OPTIMIZATIONS
-- =========================
CREATE INDEX IF NOT EXISTS chat_members_user_id_idx ON public.chat_members(user_id);
CREATE INDEX IF NOT EXISTS user_sessions_lookup_idx ON public.user_sessions(user_id, device_id);

-- =========================
-- NOTES MEDIA BLOCKS TABLE
-- =========================
CREATE TABLE IF NOT EXISTS public.note_media_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL REFERENCES public.notes(id) ON DELETE CASCADE,
    owner_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,

    local_cache_uri TEXT,
    remote_url TEXT,
    storage_path TEXT,

    media_type TEXT DEFAULT 'image/jpeg', -- Renamed from mime_type for consistency
    width INTEGER DEFAULT 0,
    height INTEGER DEFAULT 0,
    upload_status TEXT DEFAULT 'LOCAL_ONLY',

    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- RLS Enable
ALTER TABLE public.note_media_blocks ENABLE ROW LEVEL SECURITY;

-- Policies for note_media_blocks
DROP POLICY IF EXISTS "Users can view media blocks of notes they have access to" ON public.note_media_blocks;
CREATE POLICY "Users can view media blocks of notes they have access to"
ON public.note_media_blocks FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.notes
        WHERE id = note_id
        AND (owner_id = auth.uid() OR id IN (SELECT note_id FROM public.note_shares WHERE user_id = auth.uid()))
    )
);

DROP POLICY IF EXISTS "Users can insert their own media blocks" ON public.note_media_blocks;
CREATE POLICY "Users can insert their own media blocks"
ON public.note_media_blocks FOR INSERT
WITH CHECK (auth.uid() = owner_id);

DROP POLICY IF EXISTS "Users can update their own media blocks" ON public.note_media_blocks;
CREATE POLICY "Users can update their own media blocks"
ON public.note_media_blocks FOR UPDATE
USING (auth.uid() = owner_id);

-- =========================
-- STORAGE POLICIES (notes-media bucket)
-- =========================

-- 1. Allow authenticated users to upload to their own folder
DROP POLICY IF EXISTS "Allow authenticated uploads to own folder" ON storage.objects;
CREATE POLICY "Allow authenticated uploads to own folder"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'notes-media' AND
    (storage.foldername(name))[1] = 'notes' AND
    (storage.foldername(name))[2] = auth.uid()::text
);

-- 2. Allow authenticated users to read any file in notes-media (public access for members)
DROP POLICY IF EXISTS "Allow authenticated read access" ON storage.objects;
CREATE POLICY "Allow authenticated read access"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'notes-media');

-- 3. Allow owners to update/delete their own files
DROP POLICY IF EXISTS "Allow owners to update/delete own files" ON storage.objects;
CREATE POLICY "Allow owners to update/delete own files"
ON storage.objects FOR ALL
TO authenticated
USING (
    bucket_id = 'notes-media' AND
    (storage.foldername(name))[2] = auth.uid()::text
);

-- =========================
-- SYNC QUEUE TABLE
-- =========================
CREATE TABLE IF NOT EXISTS public.notes_sync_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    note_id UUID NOT NULL,
    operation TEXT NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE'
    payload JSONB,
    created_at TIMESTAMPTZ DEFAULT now(),
    retry_count INTEGER DEFAULT 0,
    status TEXT DEFAULT 'PENDING'
);
