-- ====================================================================
-- Keeftalk My Drawings Cloud Synchronization Schema Migration
-- Creates user-isolated tables for drawings metadata and content
-- protected by Row Level Security (RLS).
-- ====================================================================

-- 1. USER DRAWINGS TABLE
CREATE TABLE IF NOT EXISTS public.user_drawings (
    id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    canvas_width INT NOT NULL DEFAULT 1080,
    canvas_height INT NOT NULL DEFAULT 1920,
    background_color TEXT NOT NULL DEFAULT '#FFFFFF',
    format_version INT NOT NULL DEFAULT 1,
    content_json TEXT NULL,
    thumbnail_url TEXT NULL,
    content_hash TEXT NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

-- 2. INDEXES
CREATE INDEX IF NOT EXISTS idx_user_drawings_user_id ON public.user_drawings(user_id);
CREATE INDEX IF NOT EXISTS idx_user_drawings_updated_at ON public.user_drawings(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_drawings_is_deleted ON public.user_drawings(is_deleted);

-- 3. ROW LEVEL SECURITY (RLS)
ALTER TABLE public.user_drawings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own drawings" ON public.user_drawings;
CREATE POLICY "Users manage their own drawings"
    ON public.user_drawings FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
