-- Supabase PostgreSQL Schema for Keeftalk My Habits Feature

CREATE TABLE IF NOT EXISTS habits (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    icon TEXT NOT NULL,
    category TEXT NOT NULL,
    base_days INTEGER NOT NULL,
    created_date_epoch_day BIGINT NOT NULL,
    reminder_time_minutes INTEGER,
    slot TEXT NOT NULL,
    habit_type TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS habit_schedules (
    id TEXT PRIMARY KEY,
    habit_id TEXT REFERENCES habits(id) ON DELETE CASCADE NOT NULL,
    effective_from_epoch_day BIGINT NOT NULL,
    days_mask_string TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS habit_checkins (
    id TEXT PRIMARY KEY,
    habit_id TEXT REFERENCES habits(id) ON DELETE CASCADE NOT NULL,
    epoch_day BIGINT NOT NULL,
    completed BOOLEAN NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS habit_grace_uses (
    id TEXT PRIMARY KEY,
    habit_id TEXT REFERENCES habits(id) ON DELETE CASCADE NOT NULL,
    forgiven_epoch_day BIGINT NOT NULL,
    used_epoch_day BIGINT NOT NULL,
    used_month_key TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS habit_milestones (
    key TEXT PRIMARY KEY,
    unlocked_at_epoch_day BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- Row Level Security (RLS) policies
ALTER TABLE habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE habit_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE habit_checkins ENABLE ROW LEVEL SECURITY;
ALTER TABLE habit_grace_uses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own habits" ON habits
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can manage their own schedules" ON habit_schedules
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM habits WHERE id = habit_id));

CREATE POLICY "Users can manage their own checkins" ON habit_checkins
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM habits WHERE id = habit_id));

CREATE POLICY "Users can manage their own grace uses" ON habit_grace_uses
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM habits WHERE id = habit_id));
