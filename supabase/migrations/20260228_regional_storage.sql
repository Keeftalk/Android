-- ====================================================================
-- Regional Storage Assignment Migration for Keeftalk
-- Table: user_storage_regions
-- Stores immutable per-user Cloudflare R2 bucket assignment
-- ====================================================================

CREATE TABLE IF NOT EXISTS public.user_storage_regions (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    country_code VARCHAR(2) NOT NULL,
    storage_region VARCHAR(10) NOT NULL,
    private_bucket TEXT NOT NULL,
    public_bucket TEXT NOT NULL,
    market_bucket TEXT NOT NULL,
    detention_bucket TEXT NOT NULL,
    backups_bucket TEXT NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assignment_version INT NOT NULL DEFAULT 1
);

-- Index for fast user lookup
CREATE INDEX IF NOT EXISTS idx_user_storage_regions_user_id ON public.user_storage_regions(user_id);

-- Enable RLS
ALTER TABLE public.user_storage_regions ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users to view their own storage assignment
DROP POLICY IF EXISTS "Users can read own storage region" ON public.user_storage_regions;
CREATE POLICY "Users can read own storage region"
    ON public.user_storage_regions
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

-- Atomic assignment function
CREATE OR REPLACE FUNCTION public.get_or_assign_user_storage_region(
    p_user_id UUID,
    p_country_code TEXT,
    p_storage_region TEXT,
    p_private_bucket TEXT,
    p_public_bucket TEXT,
    p_market_bucket TEXT,
    p_detention_bucket TEXT,
    p_backups_bucket TEXT
)
RETURNS TABLE (
    user_id UUID,
    country_code VARCHAR,
    storage_region VARCHAR,
    private_bucket TEXT,
    public_bucket TEXT,
    market_bucket TEXT,
    detention_bucket TEXT,
    backups_bucket TEXT,
    assigned_at TIMESTAMPTZ,
    assignment_version INT
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Atomically insert assignment if not present
    INSERT INTO public.user_storage_regions (
        user_id,
        country_code,
        storage_region,
        private_bucket,
        public_bucket,
        market_bucket,
        detention_bucket,
        backups_bucket
    ) VALUES (
        p_user_id,
        UPPER(p_country_code),
        UPPER(p_storage_region),
        p_private_bucket,
        p_public_bucket,
        p_market_bucket,
        p_detention_bucket,
        p_backups_bucket
    )
    ON CONFLICT (user_id) DO NOTHING;

    -- Return the existing or newly created assignment
    RETURN QUERY
    SELECT
        usr.user_id,
        usr.country_code,
        usr.storage_region,
        usr.private_bucket,
        usr.public_bucket,
        usr.market_bucket,
        usr.detention_bucket,
        usr.backups_bucket,
        usr.assigned_at,
        usr.assignment_version
    FROM public.user_storage_regions usr
    WHERE usr.user_id = p_user_id;
END;
$$;
