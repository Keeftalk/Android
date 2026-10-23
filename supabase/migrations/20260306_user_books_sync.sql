-- ====================================================================
-- Keeftalk My Books Cloud Synchronization Schema Migration
-- Creates user-isolated tables for books, reading progress, bookmarks,
-- highlights, reading sessions, and settings protected by Row Level Security.
-- ====================================================================

-- 1. USER BOOKS TABLE
CREATE TABLE IF NOT EXISTS public.user_books (
    id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    author TEXT NOT NULL DEFAULT '',
    total_pages INT NOT NULL DEFAULT 1,
    format TEXT NOT NULL DEFAULT 'PDF',
    cover_url TEXT NULL,
    custom_cover_json TEXT NULL,
    percent_progress INT NOT NULL DEFAULT 0,
    last_read_page INT NOT NULL DEFAULT 1,
    hours_read REAL NOT NULL DEFAULT 0,
    pages_per_day INT NOT NULL DEFAULT 10,
    speed_pages_per_hour INT NOT NULL DEFAULT 30,
    insight_text TEXT NOT NULL DEFAULT '',
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    marked_done BOOLEAN NOT NULL DEFAULT FALSE,
    original_progress INT NOT NULL DEFAULT 0,
    original_insight TEXT NOT NULL DEFAULT '',
    finished_at TIMESTAMPTZ NULL,
    content_hash TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

CREATE INDEX IF NOT EXISTS idx_user_books_user_id ON public.user_books(user_id);
CREATE INDEX IF NOT EXISTS idx_user_books_updated_at ON public.user_books(updated_at DESC);

ALTER TABLE public.user_books ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own books" ON public.user_books;
CREATE POLICY "Users manage their own books"
    ON public.user_books FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 2. USER BOOK PROGRESS TABLE
CREATE TABLE IF NOT EXISTS public.user_book_progress (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    book_id TEXT NOT NULL,
    page_number INT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    phrase_dock_index INT NULL,
    free_x REAL NULL,
    free_y REAL NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, book_id, page_number)
);

CREATE INDEX IF NOT EXISTS idx_user_book_progress_user_book ON public.user_book_progress(user_id, book_id);

ALTER TABLE public.user_book_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own page progress" ON public.user_book_progress;
CREATE POLICY "Users manage their own page progress"
    ON public.user_book_progress FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. USER BOOK BOOKMARKS TABLE
CREATE TABLE IF NOT EXISTS public.user_book_bookmarks (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    book_id TEXT NOT NULL,
    page_number INT NOT NULL,
    chapter_title TEXT NOT NULL DEFAULT '',
    excerpt TEXT NOT NULL DEFAULT '',
    is_user BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, book_id, page_number)
);

CREATE INDEX IF NOT EXISTS idx_user_book_bookmarks_user_book ON public.user_book_bookmarks(user_id, book_id);

ALTER TABLE public.user_book_bookmarks ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own bookmarks" ON public.user_book_bookmarks;
CREATE POLICY "Users manage their own bookmarks"
    ON public.user_book_bookmarks FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 4. USER BOOK HIGHLIGHTS TABLE
CREATE TABLE IF NOT EXISTS public.user_book_highlights (
    id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    book_id TEXT NOT NULL,
    page_number INT NOT NULL,
    start_offset INT NOT NULL,
    end_offset INT NOT NULL,
    selected_text TEXT NOT NULL DEFAULT '',
    color_hex TEXT NOT NULL DEFAULT '#2ECC71',
    is_quote BOOLEAN NOT NULL DEFAULT FALSE,
    note_text TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

CREATE INDEX IF NOT EXISTS idx_user_book_highlights_user_book ON public.user_book_highlights(user_id, book_id);

ALTER TABLE public.user_book_highlights ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own highlights" ON public.user_book_highlights;
CREATE POLICY "Users manage their own highlights"
    ON public.user_book_highlights FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 5. USER BOOK SESSIONS TABLE
CREATE TABLE IF NOT EXISTS public.user_book_sessions (
    id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    book_id TEXT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    pages_read INT NOT NULL DEFAULT 0,
    duration_seconds BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

CREATE INDEX IF NOT EXISTS idx_user_book_sessions_user_book ON public.user_book_sessions(user_id, book_id);

ALTER TABLE public.user_book_sessions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own sessions" ON public.user_book_sessions;
CREATE POLICY "Users manage their own sessions"
    ON public.user_book_sessions FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 6. USER BOOK SETTINGS TABLE
CREATE TABLE IF NOT EXISTS public.user_book_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    show_read_marker BOOLEAN NOT NULL DEFAULT TRUE,
    button_size_px INT NOT NULL DEFAULT 38,
    green_opacity_percent INT NOT NULL DEFAULT 50,
    read_marker_color_hex TEXT NOT NULL DEFAULT '#2ECC71',
    theme TEXT NOT NULL DEFAULT 'original',
    page_margins_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    keep_screen_on BOOLEAN NOT NULL DEFAULT TRUE,
    auto_next_page_on_read BOOLEAN NOT NULL DEFAULT TRUE,
    volume_keys_turn_page BOOLEAN NOT NULL DEFAULT FALSE,
    sync_across_devices BOOLEAN NOT NULL DEFAULT TRUE,
    daily_goal_minutes INT NOT NULL DEFAULT 30,
    yearly_goal_books INT NOT NULL DEFAULT 24,
    pdf_dpi_option TEXT NOT NULL DEFAULT 'lowest',
    pdf_custom_dpi INT NOT NULL DEFAULT 200,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.user_book_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage their own book settings" ON public.user_book_settings;
CREATE POLICY "Users manage their own book settings"
    ON public.user_book_settings FOR ALL TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
