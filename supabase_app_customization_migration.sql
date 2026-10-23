-- =========================================
-- USER APP CUSTOMIZATION TABLE & SYNC SETUP
-- =========================================

CREATE TABLE IF NOT EXISTS public.user_app_customization (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    is_enabled BOOLEAN DEFAULT true,
    sync_enabled BOOLEAN DEFAULT true,
    is_minimal_mode BOOLEAN DEFAULT true,
    enabled_modules JSONB DEFAULT '[]'::jsonb,
    bottom_nav_tabs JSONB DEFAULT '[]'::jsonb,
    enabled_fab_actions JSONB DEFAULT '[]'::jsonb,
    fab_size_multiplier REAL DEFAULT 0.8,
    secondary_fab_size_multiplier REAL DEFAULT 0.8,
    chat_spacing_multiplier REAL DEFAULT 0.5,
    updated_at TIMESTamPTZ DEFAULT now()
);

-- Enable RLS
ALTER TABLE public.user_app_customization ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Users can manage their own app customization
DROP POLICY IF EXISTS "Users can manage their own app customization" ON public.user_app_customization;
CREATE POLICY "Users can manage their own app customization"
ON public.user_app_customization FOR ALL
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION public.handle_app_customization_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_user_app_customization_update ON public.user_app_customization;
CREATE TRIGGER on_user_app_customization_update
BEFORE UPDATE ON public.user_app_customization
FOR EACH ROW EXECUTE FUNCTION public.handle_app_customization_updated_at();

-- Enable Realtime for user_app_customization (safely check if already added)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public'
          AND tablename = 'user_app_customization'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.user_app_customization;
    END IF;
END
$$;
