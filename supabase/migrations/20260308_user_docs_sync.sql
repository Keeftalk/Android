-- ====================================================================
-- Keeftalk My Docs Cloud Synchronization Schema Migration
-- Creates user-isolated tables for documents, bookmarks, highlights,
-- and reader settings protected by Row Level Security (RLS).
-- ====================================================================

-- 1. USER DOCS TABLE
CREATE TABLE IF NOT EXISTS public.user_docs (
    id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    author_or_source TEXT NOT NULL DEFAULT '',
    total_pages_or_sections INT NOT NULL DEFAULT 1,
    format TEXT NOT NULL DEFAULT 'PDF',
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type TEXT NOT NULL DEFAULT '',
    cover_url TEXT NULL,
    custom_cover_json TEXT NULL,
    percent_progress INT NOT NULL DEFAULT 0,
    last_read_page INT NOT NULL DEFAULT 1,
    last_read_position_json TEXT NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    content_hash TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

CREATE INDEX IF NOT EXISTS idx_user_docs_user_id ON public.user_docs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_docs_updated_at ON public.user_docs(updated_at DESC);

ALTER TABLE public.user_docs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own documents" ON public.user_docs;
CREATE POLICY "Users manage their own documents"
    ON public.user_docs FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 2. USER DOC BOOKMARKS TABLE
CREATE TABLE IF NOT EXISTS public.user_doc_bookmarks (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    doc_id TEXT NOT NULL,
    page_or_position INT NOT NULL,
    chapter_title TEXT NOT NULL DEFAULT '',
    excerpt TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, doc_id, page_or_position)
);

CREATE INDEX IF NOT EXISTS idx_user_doc_bookmarks_user_doc ON public.user_doc_bookmarks(user_id, doc_id);

ALTER TABLE public.user_doc_bookmarks ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own doc bookmarks" ON public.user_doc_bookmarks;
CREATE POLICY "Users manage their own doc bookmarks"
    ON public.user_doc_bookmarks FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. USER DOC HIGHLIGHTS TABLE
CREATE TABLE IF NOT EXISTS public.user_doc_highlights (
    id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    doc_id TEXT NOT NULL,
    page_or_position INT NOT NULL,
    start_offset INT NOT NULL DEFAULT 0,
    end_offset INT NOT NULL DEFAULT 0,
    selected_text TEXT NOT NULL DEFAULT '',
    color_hex TEXT NOT NULL DEFAULT '#2ECC71',
    note_text TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

CREATE INDEX IF NOT EXISTS idx_user_doc_highlights_user_doc ON public.user_doc_highlights(user_id, doc_id);

ALTER TABLE public.user_doc_highlights ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own doc highlights" ON public.user_doc_highlights;
CREATE POLICY "Users manage their own doc highlights"
    ON public.user_doc_highlights FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 4. USER DOC SETTINGS TABLE
CREATE TABLE IF NOT EXISTS public.user_doc_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    theme TEXT NOT NULL DEFAULT 'original',
    font_size_sp INT NOT NULL DEFAULT 15,
    keep_screen_on BOOLEAN NOT NULL DEFAULT TRUE,
    show_line_numbers BOOLEAN NOT NULL DEFAULT TRUE,
    word_wrap BOOLEAN NOT NULL DEFAULT TRUE,
    sync_across_devices BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.user_doc_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own doc settings" ON public.user_doc_settings;
CREATE POLICY "Users manage their own doc settings"
    ON public.user_doc_settings FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
