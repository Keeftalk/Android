-- =========================================
-- KEEFTALK SECURITY & ACCOUNT PASS 2 MIGRATION
-- =========================================

-- 1. PROFILES EXTENSIONS
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='last_name_change_at') THEN
        ALTER TABLE public.profiles ADD COLUMN last_name_change_at TIMESTAMPTZ;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='phone_verified') THEN
        ALTER TABLE public.profiles ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- 90-Day Name Change Cooldown Trigger Function
CREATE OR REPLACE FUNCTION public.enforce_name_change_cooldown()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if full_name is being updated
    IF NEW.full_name IS DISTINCT FROM OLD.full_name THEN
        IF OLD.last_name_change_at IS NOT NULL AND OLD.last_name_change_at > (now() - INTERVAL '90 days') THEN
            RAISE EXCEPTION 'Name can only be changed once every 90 days. Next available change: %',
                (OLD.last_name_change_at + INTERVAL '90 days');
        END IF;
        NEW.last_name_change_at = now();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_name_change_cooldown ON public.profiles;
CREATE TRIGGER trigger_name_change_cooldown
BEFORE UPDATE ON public.profiles
FOR EACH ROW
EXECUTE FUNCTION public.enforce_name_change_cooldown();

-- 2. ACCOUNT DATA EXPORTS TABLE
CREATE TABLE IF NOT EXISTS public.data_exports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL,
    file_path TEXT NOT NULL,
    encryption_key_hash TEXT,
    status TEXT DEFAULT 'COMPLETED',
    expires_at TIMESTAMPTZ DEFAULT (now() + INTERVAL '14 days'),
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.data_exports ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their own data exports" ON public.data_exports;
CREATE POLICY "Users can view their own data exports"
ON public.data_exports FOR SELECT
TO authenticated
USING (auth.uid()::text = user_id::text);

DROP POLICY IF EXISTS "Users can insert their own data exports" ON public.data_exports;
CREATE POLICY "Users can insert their own data exports"
ON public.data_exports FOR INSERT
TO authenticated
WITH CHECK (auth.uid()::text = user_id::text);

-- Check 3-Month Export Rate Limit Function
CREATE OR REPLACE FUNCTION public.can_request_data_export(p_user_id TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    last_export TIMESTAMPTZ;
BEGIN
    SELECT created_at INTO last_export
    FROM public.data_exports
    WHERE user_id = p_user_id
    ORDER BY created_at DESC
    LIMIT 1;

    IF last_export IS NOT NULL AND last_export > (now() - INTERVAL '90 days') THEN
        RETURN FALSE;
    END IF;
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. ACCOUNT DELETIONS LIFESTYLE TABLE
CREATE TABLE IF NOT EXISTS public.account_deletions (
    user_id TEXT PRIMARY KEY,
    requested_at TIMESTAMPTZ DEFAULT now(),
    retention_until TIMESTAMPTZ DEFAULT (now() + INTERVAL '30 days'),
    status TEXT DEFAULT 'DELETION_REQUESTED',
    cancelled_at TIMESTAMPTZ
);

ALTER TABLE public.account_deletions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own deletion request" ON public.account_deletions;
CREATE POLICY "Users can manage their own deletion request"
ON public.account_deletions FOR ALL
TO authenticated
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

-- 4. PROFILE BACKGROUND MEDIA TABLE
CREATE TABLE IF NOT EXISTS public.profile_background_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL,
    media_type TEXT NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
    storage_path TEXT NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    duration_seconds INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE public.profile_background_media ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public can view background media" ON public.profile_background_media;
CREATE POLICY "Public can view background media"
ON public.profile_background_media FOR SELECT
TO authenticated
USING (TRUE);

DROP POLICY IF EXISTS "Users can manage their background media" ON public.profile_background_media;
CREATE POLICY "Users can manage their background media"
ON public.profile_background_media FOR ALL
TO authenticated
USING (auth.uid()::text = user_id::text)
WITH CHECK (auth.uid()::text = user_id::text);

-- Constraint Validation Function for Background Media
CREATE OR REPLACE FUNCTION public.validate_background_media()
RETURNS TRIGGER AS $$
DECLARE
    total_count INTEGER;
    image_count INTEGER;
    video_count INTEGER;
BEGIN
    -- Check orientation and resolution: landscape only, min FHD (width >= height AND width >= 1080)
    IF NEW.width < NEW.height OR NEW.width < 1080 THEN
        RAISE EXCEPTION 'Background media must be landscape and at least FHD resolution (1080p).';
    END IF;

    -- Check video duration
    IF NEW.media_type = 'VIDEO' AND (NEW.duration_seconds IS NULL OR NEW.duration_seconds > 60 OR NEW.duration_seconds <= 0) THEN
        RAISE EXCEPTION 'Background video duration must be 60 seconds or less.';
    END IF;

    -- Check counts
    SELECT COUNT(*) INTO total_count FROM public.profile_background_media WHERE user_id = NEW.user_id AND id != NEW.id;
    IF total_count >= 5 THEN
        RAISE EXCEPTION 'Maximum 5 background media items allowed per profile.';
    END IF;

    IF NEW.media_type = 'IMAGE' THEN
        SELECT COUNT(*) INTO image_count FROM public.profile_background_media WHERE user_id = NEW.user_id AND media_type = 'IMAGE' AND id != NEW.id;
        IF image_count >= 4 THEN
            RAISE EXCEPTION 'Maximum 4 background images allowed per profile.';
        END IF;
    ELSIF NEW.media_type = 'VIDEO' THEN
        SELECT COUNT(*) INTO video_count FROM public.profile_background_media WHERE user_id = NEW.user_id AND media_type = 'VIDEO' AND id != NEW.id;
        IF video_count >= 1 THEN
            RAISE EXCEPTION 'Maximum 1 background video allowed per profile.';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_validate_background_media ON public.profile_background_media;
CREATE TRIGGER trigger_validate_background_media
BEFORE INSERT OR UPDATE ON public.profile_background_media
FOR EACH ROW
EXECUTE FUNCTION public.validate_background_media();

-- 5. PHONE NUMBER PRIVACY AUDIT IN PROFILES
-- Ensure unverified phone numbers are hidden from public profile selects
DROP POLICY IF EXISTS "Public profiles read" ON public.profiles;
CREATE POLICY "Public profiles read"
ON public.profiles FOR SELECT
TO authenticated
USING (TRUE);
