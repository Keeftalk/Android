-- Browser History Table
CREATE TABLE IF NOT EXISTS public.browser_history (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    timestamp BIGINT NOT NULL
);

-- Browser Bookmarks / Quick Links Table
CREATE TABLE IF NOT EXISTS public.browser_bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    icon_name TEXT,
    color TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.browser_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.browser_bookmarks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own browser history"
ON public.browser_history FOR ALL
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage their own browser bookmarks"
ON public.browser_bookmarks FOR ALL
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Index for performance
CREATE INDEX IF NOT EXISTS idx_browser_history_user_timestamp ON public.browser_history(user_id, timestamp DESC);
