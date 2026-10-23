-- ====================================================================
-- AUDIT & FIX SCRIPT FOR 5XX / POSTGRES ERRORS IN KEEFTALK SUPABASE
-- ====================================================================

-- 1. FIX: Infinite recursion detected in policy for relation "family_members" (Error 42P17)
-- Cause: RLS policies on family_members referencing family_members recursively.
-- Solution: Drop recursive policies and recreate clean, non-recursive policies.

DROP POLICY IF EXISTS "Users can view their family links" ON public.family_members;
DROP POLICY IF EXISTS "Users can manage their family links" ON public.family_members;

CREATE POLICY "Users can view their family links" ON public.family_members
    FOR SELECT
    USING (auth.uid() = user_id OR auth.uid() = member_id OR auth.uid() IN (SELECT owner_id FROM public.families WHERE id = family_id));

CREATE POLICY "Users can manage their family links" ON public.family_members
    FOR ALL
    USING (auth.uid() = user_id OR auth.uid() IN (SELECT owner_id FROM public.families WHERE id = family_id))
    WITH CHECK (auth.uid() = user_id OR auth.uid() IN (SELECT owner_id FROM public.families WHERE id = family_id));

ALTER TABLE public.family_members ENABLE ROW LEVEL SECURITY;


-- 2. FIX: calendar_items & messages 500 errors caused by family_members recursion
-- Re-create / ensure calendar_items policies are robust and non-recursive (using owner_id)

DROP POLICY IF EXISTS "Users can view their own items" ON public.calendar_items;
DROP POLICY IF EXISTS "Users can view shared family items" ON public.calendar_items;
DROP POLICY IF EXISTS "Users can manage their own items" ON public.calendar_items;

CREATE POLICY "Users can view their own items" ON public.calendar_items
    FOR SELECT
    USING (auth.uid() = owner_id);

CREATE POLICY "Users can view shared family items" ON public.calendar_items
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.family_members fm
            WHERE fm.user_id = auth.uid()
            AND fm.family_id IN (SELECT family_id FROM public.family_members WHERE user_id = calendar_items.owner_id)
        )
    );

CREATE POLICY "Users can manage their own items" ON public.calendar_items
    FOR ALL
    USING (auth.uid() = owner_id)
    WITH CHECK (auth.uid() = owner_id);

ALTER TABLE public.calendar_items ENABLE ROW LEVEL SECURITY;


-- 3. FIX: Storage Buckets "new row violates row-level security policy for table buckets" (Error 42501)
-- Cause: Authenticated users attempting to insert/create buckets directly into storage.buckets without service role.
-- Solution: Ensure storage.buckets allows service role (which bypasses RLS) or grant insert policy if needed,
-- and ensure 'files' bucket exists properly.

INSERT INTO storage.buckets (id, name, public)
VALUES ('files', 'files', false)
ON CONFLICT (id) DO NOTHING;

-- Ensure storage.objects policies are correct for authenticated users uploading to 'files' bucket
DROP POLICY IF EXISTS "Allow authenticated uploads to own folder" ON storage.objects;
CREATE POLICY "Allow authenticated uploads to own folder"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'files' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

DROP POLICY IF EXISTS "Allow authenticated downloads" ON storage.objects;
CREATE POLICY "Allow authenticated downloads"
ON storage.objects FOR SELECT
TO authenticated
USING (bucket_id = 'files');

DROP POLICY IF EXISTS "Allow owners to delete" ON storage.objects;
CREATE POLICY "Allow owners to delete"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'files' AND (storage.foldername(name))[1] = auth.uid()::text);
