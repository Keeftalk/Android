-- ====================================================================
-- FIX: Infinite recursion in RLS policy for relation "family_members"
-- ====================================================================

-- 1. Drop problematic recursive policies on family_members
DROP POLICY IF EXISTS "Users can view their family links" ON public.family_members;
DROP POLICY IF EXISTS "Users can manage their family links" ON public.family_members;

-- 2. Re-create clean, non-recursive RLS policies for family_members
CREATE POLICY "Users can view their family links" ON public.family_members
    FOR SELECT
    USING (auth.uid() = user_id OR auth.uid() = member_id);

CREATE POLICY "Users can manage their family links" ON public.family_members
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. Ensure family_members table has RLS enabled
ALTER TABLE public.family_members ENABLE ROW LEVEL SECURITY;

-- 4. Ensure storage calculation functions are SECURITY DEFINER to bypass RLS recursion
CREATE OR REPLACE FUNCTION public.calculate_user_storage_usage(target_user_id UUID)
RETURNS BIGINT AS $$
DECLARE
    f_id UUID;
    total_usage BIGINT;
BEGIN
    SELECT family_id INTO f_id FROM public.profiles WHERE id = target_user_id;

    IF f_id IS NOT NULL THEN
        SELECT SUM(COALESCE(file_size, 0) + COALESCE(thumbnail_size, 0))
        INTO total_usage
        FROM public.files
        WHERE owner_id::uuid IN (SELECT id FROM public.profiles WHERE family_id = f_id)
        AND (status != 'DELETED' OR status IS NULL);
    ELSE
        SELECT SUM(COALESCE(file_size, 0) + COALESCE(thumbnail_size, 0))
        INTO total_usage
        FROM public.files
        WHERE owner_id::uuid = target_user_id
        AND (status != 'DELETED' OR status IS NULL);
    END IF;

    RETURN COALESCE(total_usage, 0);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
