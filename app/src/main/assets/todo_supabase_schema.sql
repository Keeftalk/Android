-- Supabase PostgreSQL Schema for Keeftalk My Todo Feature

-- 1. Create todo_lists table
CREATE TABLE IF NOT EXISTS public.todo_lists (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    color_hex TEXT NOT NULL DEFAULT '#7D5CFF',
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 2. Create todo_tasks table
CREATE TABLE IF NOT EXISTS public.todo_tasks (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    due_date TEXT NOT NULL DEFAULT '',
    priority INTEGER NOT NULL DEFAULT 1,
    list_id TEXT REFERENCES public.todo_lists(id) ON DELETE SET NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    completed_at BIGINT NOT NULL DEFAULT 0,
    reminder TEXT NOT NULL DEFAULT '',
    reminder_fired BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_todo_tasks_user_id ON public.todo_tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_todo_lists_user_id ON public.todo_lists(user_id);

-- Enable Row Level Security (RLS)
ALTER TABLE public.todo_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.todo_tasks ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can manage their own todo lists" ON public.todo_lists
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage their own todo tasks" ON public.todo_tasks
    FOR ALL USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Enable Realtime
ALTER PUBLICATION supabase_realtime ADD TABLE public.todo_lists;
ALTER PUBLICATION supabase_realtime ADD TABLE public.todo_tasks;
