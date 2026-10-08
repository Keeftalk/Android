-- ====================================================================
-- User Reading XP & Global Leaderboard Schema Migration
-- Table: public.user_reading_xp
-- Stores per-user total reading XP and computes global standing
-- ====================================================================

CREATE TABLE IF NOT EXISTS public.user_reading_xp (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT NULL,
    total_xp BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Btree Index for fast rank queries and sorted leaderboard fetching
CREATE INDEX IF NOT EXISTS idx_user_reading_xp_total_xp ON public.user_reading_xp USING btree (total_xp DESC);

-- Enable Row Level Security
ALTER TABLE public.user_reading_xp ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to view global leaderboard entries
DROP POLICY IF EXISTS "Authenticated users can read global XP leaderboard" ON public.user_reading_xp;
CREATE POLICY "Authenticated users can read global XP leaderboard"
    ON public.user_reading_xp
    FOR SELECT
    TO authenticated
    USING (true);

-- Allow authenticated users to insert/update their own XP record
DROP POLICY IF EXISTS "Users can manage their own XP" ON public.user_reading_xp;
CREATE POLICY "Users can manage their own XP"
    ON public.user_reading_xp
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Atomic RPC function to upsert user total_xp and retrieve global standing rank
CREATE OR REPLACE FUNCTION public.upsert_user_xp_and_get_rank(
    p_total_xp BIGINT,
    p_username TEXT DEFAULT NULL
)
RETURNS TABLE (
    global_rank BIGINT,
    total_readers BIGINT,
    percentile INT
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_id UUID;
    v_rank BIGINT;
    v_total BIGINT;
    v_pct INT;
BEGIN
    v_user_id := auth.uid();

    -- Upsert for logged-in user
    IF v_user_id IS NOT NULL THEN
        INSERT INTO public.user_reading_xp (user_id, username, total_xp, updated_at)
        VALUES (v_user_id, COALESCE(p_username, 'Reader'), p_total_xp, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET total_xp = EXCLUDED.total_xp,
            username = COALESCE(EXCLUDED.username, public.user_reading_xp.username),
            updated_at = NOW();
    END IF;

    -- Calculate rank: count of users with strictly higher total_xp + 1
    SELECT COUNT(*) + 1 INTO v_rank
    FROM public.user_reading_xp
    WHERE total_xp > p_total_xp;

    -- Calculate total registered readers count (at least 1)
    SELECT GREATEST(COUNT(*), 1) INTO v_total
    FROM public.user_reading_xp;

    -- Calculate top percentile (1% to 100%)
    v_pct := GREATEST(1, LEAST(100, ROUND((v_rank::NUMERIC / v_total::NUMERIC) * 100)::INT));

    RETURN QUERY SELECT v_rank, v_total, v_pct;
END;
$$;
