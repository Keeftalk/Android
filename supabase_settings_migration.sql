-- =========================================
-- KEEFTALK UNIFIED SETTINGS SYSTEM
-- =========================================

-- 1. UNIFIED SETTINGS TABLE
CREATE TABLE IF NOT EXISTS public.user_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    chat_settings JSONB DEFAULT '{}',
    call_settings JSONB DEFAULT '{}',
    note_settings JSONB DEFAULT '{}',
    vault_settings JSONB DEFAULT '{}',
    calendar_settings JSONB DEFAULT '{}',
    email_settings JSONB DEFAULT '{}',
    notification_settings JSONB DEFAULT '{}',
    privacy_settings JSONB DEFAULT '{}',
    parental_controls JSONB DEFAULT '{}',
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. ENABLE RLS
ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;

-- 3. RLS POLICIES

-- Users can manage their own settings (except parental_controls if they are restricted)
DROP POLICY IF EXISTS "Users can manage their own settings" ON public.user_settings;
CREATE POLICY "Users can manage their own settings"
ON public.user_settings FOR ALL
USING (auth.uid() = user_id)
WITH CHECK (
    auth.uid() = user_id
    AND (
        -- If parental_controls is being modified, ensure the user is NOT restricted
        -- or they are the supervisor.
        -- Simplest way: Users can't change parental_controls themselves if it was set by someone else.
        (OLD.parental_controls->>'supervisor_id' IS NULL OR OLD.parental_controls->>'supervisor_id' = auth.uid()::text)
        OR (NEW.parental_controls = OLD.parental_controls)
    )
);

-- Parents can view child settings
DROP POLICY IF EXISTS "Parents can view child settings" ON public.user_settings;
CREATE POLICY "Parents can view child settings"
ON public.user_settings FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.families f
        JOIN public.profiles p ON p.family_id = f.id
        WHERE f.owner_id::text = auth.uid()::text
        AND p.id::text = user_settings.user_id::text
    )
);

-- Parents can update child settings (Authorized Parental Control)
DROP POLICY IF EXISTS "Parents can update child settings" ON public.user_settings;
CREATE POLICY "Parents can update child settings"
ON public.user_settings FOR UPDATE
USING (
    EXISTS (
        SELECT 1 FROM public.families f
        JOIN public.profiles p ON p.family_id = f.id
        WHERE f.owner_id::text = auth.uid()::text
        AND p.id::text = user_settings.user_id::text
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.families f
        JOIN public.profiles p ON p.family_id = f.id
        WHERE f.owner_id::text = auth.uid()::text
        AND p.id::text = user_settings.user_id::text
    )
);

-- 4. TRIGGER FOR UPDATED_AT
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_user_settings_update ON public.user_settings;
CREATE TRIGGER on_user_settings_update
BEFORE UPDATE ON public.user_settings
FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();
