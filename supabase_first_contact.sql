-- ==========================================
-- Keeftalk First Contact / Message Request System
-- ==========================================

-- 1. App Configuration Table (Remote Backend Control)
CREATE TABLE IF NOT EXISTS public.app_config (
    key TEXT PRIMARY KEY,
    value JSONB NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Insert default first-contact policy if not exists
INSERT INTO public.app_config (key, value)
VALUES (
    'first_contact_policy',
    '{
        "model": "ONE_MESSAGE",
        "max_messages_before_reply": 1,
        "max_first_contacts_per_day": 10,
        "cooldown_seconds": 30,
        "allow_text": true,
        "allow_images": false,
        "allow_videos": false,
        "allow_files": false,
        "allow_voice": false,
        "allow_links": false,
        "require_recipient_acceptance": false,
        "policy_version": 1
    }'::jsonb
)
ON CONFLICT (key) DO NOTHING;

-- 2. Message Requests Table
CREATE TABLE IF NOT EXISTS public.message_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    conversation_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending', -- pending, replied, accepted, declined, blocked, expired, cancelled, converted_to_chat
    policy_version INT DEFAULT 1,
    messages_sent INT DEFAULT 1,
    recipient_replied BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    expires_at TIMESTAMPTZ DEFAULT (now() + interval '7 days')
);

-- Enable RLS on message_requests and app_config
ALTER TABLE public.message_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;

-- RLS Policies for message_requests
DROP POLICY IF EXISTS "Users can view their own message requests" ON public.message_requests;
CREATE POLICY "Users can view their own message requests"
ON public.message_requests FOR SELECT
TO authenticated
USING (sender_id::uuid = auth.uid() OR recipient_id::uuid = auth.uid());

DROP POLICY IF EXISTS "Users can insert message requests as sender" ON public.message_requests;
CREATE POLICY "Users can insert message requests as sender"
ON public.message_requests FOR INSERT
TO authenticated
WITH CHECK (sender_id::uuid = auth.uid());

DROP POLICY IF EXISTS "Users can update their message requests" ON public.message_requests;
CREATE POLICY "Users can update their message requests"
ON public.message_requests FOR UPDATE
TO authenticated
USING (sender_id::uuid = auth.uid() OR recipient_id::uuid = auth.uid())
WITH CHECK (sender_id::uuid = auth.uid() OR recipient_id::uuid = auth.uid());

-- RLS Policy for app_config (Everyone can read)
DROP POLICY IF EXISTS "Anyone can read app_config" ON public.app_config;
CREATE POLICY "Anyone can read app_config"
ON public.app_config FOR SELECT
TO authenticated, anon
USING (true);

-- Enable Realtime for message_requests
ALTER PUBLICATION supabase_realtime ADD TABLE message_requests;

-- 3. RPC Functions for First Contact & Policy Enforcement

-- Get active first contact policy
CREATE OR REPLACE FUNCTION public.get_active_first_contact_policy()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    policy_val JSONB;
BEGIN
    SELECT value INTO policy_val FROM public.app_config WHERE key = 'first_contact_policy';
    RETURN COALESCE(policy_val, '{}'::jsonb);
END;
$$;

-- Check if user can message target user (Server-side enforcement)
CREATE OR REPLACE FUNCTION public.check_can_message(target_user_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    viewer_id UUID := auth.uid();
    target_id UUID := target_user_id::uuid;
    policy JSONB;
    model TEXT;
    is_connected BOOLEAN;
    is_blocked BOOLEAN;
    existing_req RECORD;
    active_chat_id TEXT;
BEGIN
    -- Self check
    IF viewer_id = target_id THEN
        RETURN jsonb_build_object('allowed', false, 'reason', 'Cannot message yourself');
    END IF;

    -- Check if blocked
    SELECT EXISTS (
        SELECT 1 FROM public.blocked_users
        WHERE (blocker_id::uuid = viewer_id AND blocked_id = target_id)
           OR (blocker_id::uuid = target_id AND blocked_id = viewer_id)
    ) INTO is_blocked;

    IF is_blocked THEN
        RETURN jsonb_build_object('allowed', false, 'reason', 'User is blocked');
    END IF;

    -- Check if socially connected (contacts or existing ONE_TO_ONE chat)
    SELECT public.is_social_connected(viewer_id, target_id) INTO is_connected;

    IF is_connected THEN
        RETURN jsonb_build_object('allowed', true, 'isNormalChat', true);
    END IF;

    -- Fetch policy
    SELECT public.get_active_first_contact_policy() INTO policy;
    model := COALESCE(policy->>'model', 'ONE_MESSAGE');

    IF model = 'DISABLED' THEN
        RETURN jsonb_build_object('allowed', false, 'reason', 'First-contact messaging is disabled by policy');
    END IF;

    -- Check existing message request
    SELECT * INTO existing_req
    FROM public.message_requests
    WHERE ((sender_id = viewer_id AND recipient_id = target_id) OR (sender_id = target_id AND recipient_id = viewer_id))
    AND status NOT IN ('declined', 'blocked', 'expired', 'cancelled');

    IF existing_req.id IS NOT NULL THEN
        IF existing_req.status = 'converted_to_chat' OR existing_req.recipient_replied THEN
            RETURN jsonb_build_object('allowed', true, 'isNormalChat', true, 'requestId', existing_req.id);
        ELSIF existing_req.sender_id = viewer_id THEN
            -- Check message limit before reply
            DECLARE
                max_msgs INT := COALESCE((policy->>'max_messages_before_reply')::int, 1);
            BEGIN
                IF existing_req.messages_sent >= max_msgs THEN
                    RETURN jsonb_build_object('allowed', false, 'reason', 'Waiting for recipient reply before sending more messages', 'requestId', existing_req.id);
                ELSE
                    RETURN jsonb_build_object('allowed', true, 'isNormalChat', false, 'requestId', existing_req.id, 'remainingMessages', max_msgs - existing_req.messages_sent);
                END IF;
            END;
        ELSE
            -- Recipient viewing sender request
            RETURN jsonb_build_object('allowed', true, 'isNormalChat', false, 'requestId', existing_req.id, 'incomingRequest', true);
        END IF;
    END IF;

    -- No existing request, allowed to initiate first contact
    RETURN jsonb_build_object('allowed', true, 'isNormalChat', false, 'canInitiateFirstContact', true, 'policy', policy);
END;
$$;

-- Reply to message request / Convert to normal chat
CREATE OR REPLACE FUNCTION public.reply_to_message_request(p_request_id TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    req RECORD;
    viewer_id UUID := auth.uid();
BEGIN
    SELECT * INTO req FROM public.message_requests WHERE id::text = p_request_id;
    IF req.id IS NULL THEN
        RAISE EXCEPTION 'Message request not found';
    END IF;

    IF req.recipient_id != viewer_id THEN
        RAISE EXCEPTION 'Only recipient can reply to message request';
    END IF;

    -- Update request status
    UPDATE public.message_requests
    SET recipient_replied = true,
        status = 'converted_to_chat',
        updated_at = now()
    WHERE id = req.id;

    -- Ensure chat is marked or registered as normal chat if needed
    -- (The conversation already exists in chats table)
END;
$$;
