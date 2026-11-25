-- ====================================================================
-- Profile Media System: History, Likes, and 30-Day Retention Storage
-- ====================================================================

CREATE TABLE IF NOT EXISTS public.profile_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('AVATAR', 'WALLPAPER')),
    original_object_key TEXT NOT NULL,
    compressed_object_key TEXT,
    original_cdn_url TEXT NOT NULL,
    compressed_cdn_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'DETENTION')),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    activated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    scheduled_purge_at TIMESTAMPTZ,
    detention_object_key TEXT,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS public.profile_media_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id UUID NOT NULL REFERENCES public.profile_media(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_profile_media_like UNIQUE (media_id, user_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_profile_media_user_type ON public.profile_media(user_id, media_type, status);
CREATE INDEX IF NOT EXISTS idx_profile_media_active ON public.profile_media(user_id, media_type, is_active);
CREATE INDEX IF NOT EXISTS idx_profile_media_likes_media ON public.profile_media_likes(media_id);
CREATE INDEX IF NOT EXISTS idx_profile_media_likes_user ON public.profile_media_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_profile_media_purge ON public.profile_media(status, scheduled_purge_at);

-- RLS Security
ALTER TABLE public.profile_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profile_media_likes ENABLE ROW LEVEL SECURITY;

-- Profile Media Policies
DROP POLICY IF EXISTS "Public/authenticated can view non-detention media" ON public.profile_media;
CREATE POLICY "Public/authenticated can view non-detention media"
    ON public.profile_media FOR SELECT
    TO authenticated
    USING (status != 'DETENTION' OR user_id = auth.uid());

DROP POLICY IF EXISTS "Users can insert own profile media" ON public.profile_media;
CREATE POLICY "Users can insert own profile media"
    ON public.profile_media FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own profile media" ON public.profile_media;
CREATE POLICY "Users can update own profile media"
    ON public.profile_media FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own profile media" ON public.profile_media;
CREATE POLICY "Users can delete own profile media"
    ON public.profile_media FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- Profile Media Likes Policies
DROP POLICY IF EXISTS "Authenticated users can view media likes" ON public.profile_media_likes;
CREATE POLICY "Authenticated users can view media likes"
    ON public.profile_media_likes FOR SELECT
    TO authenticated
    USING (true);

DROP POLICY IF EXISTS "Users can like media" ON public.profile_media_likes;
CREATE POLICY "Users can like media"
    ON public.profile_media_likes FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can unlike media" ON public.profile_media_likes;
CREATE POLICY "Users can unlike media"
    ON public.profile_media_likes FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);

-- Function to set active profile media atomically
CREATE OR REPLACE FUNCTION public.set_active_profile_media(
    p_user_id UUID,
    p_media_id UUID
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_media_type VARCHAR(20);
BEGIN
    SELECT media_type INTO v_media_type
    FROM public.profile_media
    WHERE id = p_media_id AND user_id = p_user_id AND status != 'DETENTION';

    IF v_media_type IS NULL THEN
        RAISE EXCEPTION 'Media item not found or unavailable';
    END IF;

    -- Deactivate current active media of same type
    UPDATE public.profile_media
    SET is_active = FALSE
    WHERE user_id = p_user_id AND media_type = v_media_type AND is_active = TRUE;

    -- Activate selected media
    UPDATE public.profile_media
    SET is_active = TRUE,
        status = 'ACTIVE',
        activated_at = NOW()
    WHERE id = p_media_id AND user_id = p_user_id;
END;
$$;
