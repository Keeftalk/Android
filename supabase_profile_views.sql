-- ==========================================
-- Profile View Counter Automation
-- ==========================================

-- 1. Add views_count column to profiles table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
        AND table_name = 'profiles'
        AND column_name = 'views_count'
    ) THEN
        ALTER TABLE public.profiles ADD COLUMN views_count BIGINT DEFAULT 0;
    END IF;
END $$;

-- 2. Create trigger function to increment views_count
CREATE OR REPLACE FUNCTION public.increment_profile_view_count()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    -- Increment the views_count for the user being viewed
    -- NEW.viewed_id refers to the ID in the profile_views table
    UPDATE public.profiles
    SET views_count = views_count + 1
    WHERE id::text = NEW.viewed_id::text;

    RETURN NEW;
END;
$$;

-- 3. Attach trigger to profile_views table
DROP TRIGGER IF EXISTS tr_on_profile_view_insert ON public.profile_views;
CREATE TRIGGER tr_on_profile_view_insert
    AFTER INSERT ON public.profile_views
    FOR EACH ROW
    EXECUTE FUNCTION public.increment_profile_view_count();
