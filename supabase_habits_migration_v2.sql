-- ========================================================
-- KEEFTALK MY HABITS MIGRATION V2 (PRODUCTION SAFE)
-- ========================================================

-- 1. Add habit_settings JSONB column to unified user_settings table if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_settings' AND column_name='habit_settings') THEN
        ALTER TABLE public.user_settings ADD COLUMN habit_settings JSONB DEFAULT '{}';
    END IF;
END $$;

-- 2. Create Habits Tables with UUID Primary & Foreign Keys (matching Supabase auth.users & auth conventions)
CREATE TABLE IF NOT EXISTS public.habits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    icon TEXT NOT NULL,
    category TEXT NOT NULL,
    base_days INTEGER NOT NULL,
    created_date_epoch_day BIGINT NOT NULL,
    reminder_time_minutes INTEGER,
    slot TEXT NOT NULL,
    habit_type TEXT NOT NULL,
    amount INTEGER,
    unit TEXT,
    color_hex TEXT DEFAULT '#00CCCC' NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS public.habit_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    habit_id UUID REFERENCES public.habits(id) ON DELETE CASCADE NOT NULL,
    effective_from_epoch_day BIGINT NOT NULL,
    days_mask_string TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS public.habit_checkins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    habit_id UUID REFERENCES public.habits(id) ON DELETE CASCADE NOT NULL,
    epoch_day BIGINT NOT NULL,
    completed BOOLEAN NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS public.habit_grace_uses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    habit_id UUID REFERENCES public.habits(id) ON DELETE CASCADE NOT NULL,
    forgiven_epoch_day BIGINT NOT NULL,
    used_epoch_day BIGINT NOT NULL,
    used_month_key TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

-- 3. Per-User Milestones Table (Scoped to user_id to prevent global collisions)
CREATE TABLE IF NOT EXISTS public.habit_milestones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    key TEXT NOT NULL,
    unlocked_at_epoch_day BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE(user_id, key)
);

-- 4. Enable Row Level Security (RLS)
ALTER TABLE public.habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_checkins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_grace_uses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_milestones ENABLE ROW LEVEL SECURITY;

-- 5. Strict User-Owned RLS Policies
DROP POLICY IF EXISTS "Users can manage their own habits" ON public.habits;
CREATE POLICY "Users can manage their own habits" ON public.habits
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can manage their own schedules" ON public.habit_schedules;
CREATE POLICY "Users can manage their own schedules" ON public.habit_schedules
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id))
    WITH CHECK (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id));

DROP POLICY IF EXISTS "Users can manage their own checkins" ON public.habit_checkins;
CREATE POLICY "Users can manage their own checkins" ON public.habit_checkins
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id))
    WITH CHECK (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id));

DROP POLICY IF EXISTS "Users can manage their own grace uses" ON public.habit_grace_uses;
CREATE POLICY "Users can manage their own grace uses" ON public.habit_grace_uses
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id))
    WITH CHECK (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id));

DROP POLICY IF EXISTS "Users can manage their own milestones" ON public.habit_milestones;
CREATE POLICY "Users can manage their own milestones" ON public.habit_milestones
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- 6. Add Habit Tables to Supabase Realtime Publication
ALTER PUBLICATION supabase_realtime ADD TABLE public.habits;
ALTER PUBLICATION supabase_realtime ADD TABLE public.habit_schedules;
ALTER PUBLICATION supabase_realtime ADD TABLE public.habit_checkins;
ALTER PUBLICATION supabase_realtime ADD TABLE public.habit_grace_uses;
ALTER PUBLICATION supabase_realtime ADD TABLE public.habit_milestones;
