-- ====================================================================
-- User Active Sessions Schema & Realtime Revocation Migration
-- Table: public.user_sessions
-- Manages device sessions for Keeftalk Account Settings -> Activity Sessions
-- ====================================================================

CREATE TABLE IF NOT EXISTS public.user_sessions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NULL,
    device_id TEXT NOT NULL,
    device_name TEXT NULL,
    platform TEXT NULL,
    app_version TEXT NULL,
    ip_address TEXT NULL,
    country TEXT NULL,
    last_active TIMESTAMPTZ NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NULL DEFAULT NOW(),
    is_current BOOLEAN NULL DEFAULT FALSE,
    fcm_token TEXT NULL,
    manufacturer TEXT NULL,
    model TEXT NULL,
    city TEXT NULL,
    auth_method TEXT NULL,
    CONSTRAINT user_sessions_pkey PRIMARY KEY (id),
    CONSTRAINT user_sessions_user_id_device_id_key UNIQUE (user_id, device_id),
    CONSTRAINT user_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users (id) ON DELETE CASCADE
) TABLESPACE pg_default;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_active ON public.user_sessions USING btree (user_id, last_active DESC) TABLESPACE pg_default;
CREATE INDEX IF NOT EXISTS user_sessions_lookup_idx ON public.user_sessions USING btree (user_id, device_id) TABLESPACE pg_default;

-- Enable Row Level Security
ALTER TABLE public.user_sessions ENABLE ROW LEVEL SECURITY;

-- RLS Policies
DROP POLICY IF EXISTS "Users can view their own sessions" ON public.user_sessions;
CREATE POLICY "Users can view their own sessions"
    ON public.user_sessions
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can manage their own sessions" ON public.user_sessions;
CREATE POLICY "Users can manage their own sessions"
    ON public.user_sessions
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Realtime Revocation Trigger Function
CREATE OR REPLACE FUNCTION public.notify_session_revoked()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify(
        'session_revoked',
        json_build_object(
            'id', OLD.id,
            'user_id', OLD.user_id,
            'device_id', OLD.device_id
        )::text
    );
    RETURN OLD;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger on session deletion
DROP TRIGGER IF EXISTS tr_notify_session_revoked ON public.user_sessions;
CREATE TRIGGER tr_notify_session_revoked
    AFTER DELETE ON public.user_sessions
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_session_revoked();
