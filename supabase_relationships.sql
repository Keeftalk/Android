-- ==========================================
-- Keeftalk Relationship Graph System (Refined Nudge Aggregation)
-- ==========================================

-- 1. Helper Function: Check if two users have an active connection
CREATE OR REPLACE FUNCTION public.is_social_connected(user_id_a UUID, user_id_b UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN EXISTS (
        -- Check Contacts
        SELECT 1 FROM public.contacts c
        JOIN public.profiles p ON c.phone = p.phone
        WHERE c.owner_id::uuid = user_id_a AND p.id = user_id_b
        UNION ALL
        -- Check Chat Members
        SELECT 1 FROM public.chat_members cm1
        JOIN public.chat_members cm2 ON cm1.chat_id = cm2.chat_id AND cm1.user_id != cm2.user_id
        JOIN public.chats c ON cm1.chat_id = c.id
        WHERE cm1.user_id::uuid = user_id_a AND cm2.user_id::uuid = user_id_b
        AND c.type = 'ONE_TO_ONE'
    );
END;
$$;

-- 2. Infrastructure Updates (Phase 5: Nudge Aggregation)
DO $$
BEGIN
    -- Connections visibility
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='user_privacy_settings' AND column_name='connections_visibility') THEN
        ALTER TABLE public.user_privacy_settings ADD COLUMN connections_visibility TEXT DEFAULT 'EVERYONE';
    END IF;

    -- Global nudge count on profile
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='nudges_received') THEN
        ALTER TABLE public.profiles ADD COLUMN nudges_received INT DEFAULT 0;
    END IF;

    -- Notification priority if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='priority') THEN
        ALTER TABLE public.notifications ADD COLUMN priority TEXT DEFAULT 'NORMAL';
    END IF;

    -- Notification source_id if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='source_id') THEN
        ALTER TABLE public.notifications ADD COLUMN source_id TEXT;
    END IF;

    -- NEW: Aggregate counter for specific notification records
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='notifications' AND column_name='nudge_count') THEN
        ALTER TABLE public.notifications ADD COLUMN nudge_count INT DEFAULT 1;
    END IF;
END $$;

-- Enable Realtime for notifications table (Critical for bell icon)
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;

-- 3. BFS Relationship Summary RPC
CREATE OR REPLACE FUNCTION public.get_relationship_summary(target_user_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    viewer_id UUID := auth.uid();
    target_id UUID := target_user_id::uuid;
    result_distance INT := NULL;
    mutual_count INT := 0;
    mutual_previews JSONB := '[]'::jsonb;
    shortest_path_count INT := 0;
    density_label TEXT := 'LOW';
    strength_label TEXT := 'Inactive';
    strength_val FLOAT := 0.0;
    nudge_count INT := 0;
BEGIN
    -- 0. Self check
    IF viewer_id = target_id THEN
        RETURN jsonb_build_object('distance', 0, 'mutualCount', 0, 'nudgesReceived', 0, 'strengthLabel', 'Very strong');
    END IF;

    -- 0.1 Fetch Nudge Count
    SELECT nudges_received INTO nudge_count FROM public.profiles WHERE id = target_id;

    -- 1. Distance 1 (Direct connection)
    IF public.is_social_connected(viewer_id, target_id) THEN
        result_distance := 1;
        shortest_path_count := 1;
    END IF;

    -- 2. BFS for Distance 2-4
    IF result_distance IS NULL THEN
        WITH RECURSIVE bfs(user_id, depth, first_hop) AS (
            SELECT p.id, 1, p.id
            FROM public.profiles p
            WHERE public.is_social_connected(viewer_id, p.id)
            AND NOT EXISTS (SELECT 1 FROM public.blocked_users WHERE blocker_id::uuid = viewer_id AND blocked_id = p.id)
          UNION ALL
            SELECT p_target.id, depth + 1, bfs.first_hop
            FROM public.profiles p_target
            JOIN bfs ON public.is_social_connected(bfs.user_id, p_target.id)
            JOIN public.user_privacy_settings privacy ON bfs.user_id::text = privacy.user_id::text
            WHERE depth < 4
            AND (
                privacy.connections_visibility = 'EVERYONE'
                OR (privacy.connections_visibility = 'CONTACTS' AND public.is_social_connected(bfs.user_id, viewer_id))
            )
            AND NOT EXISTS (SELECT 1 FROM public.blocked_users WHERE blocker_id::uuid = bfs.user_id AND blocked_id = p_target.id)
        )
        SELECT
            MIN(depth),
            COUNT(DISTINCT first_hop)
        INTO result_distance, shortest_path_count
        FROM bfs
        WHERE user_id = target_id;
    END IF;

    -- 3. Mutual Connections Count
    SELECT
        COUNT(*),
        COALESCE(jsonb_agg(user_id::text) FILTER (WHERE row_num <= 3), '[]'::jsonb)
    INTO mutual_count, mutual_previews
    FROM (
        SELECT p.id as user_id, ROW_NUMBER() OVER() as row_num
        FROM public.profiles p
        WHERE public.is_social_connected(viewer_id, p.id)
        AND public.is_social_connected(target_id, p.id)
        AND NOT EXISTS (SELECT 1 FROM public.blocked_users WHERE blocker_id::uuid = viewer_id AND blocked_id = p.id)
    ) t;

    -- 4. Density & Strength
    IF shortest_path_count >= 3 OR mutual_count >= 6 THEN density_label := 'HIGH';
    ELSIF shortest_path_count >= 2 OR mutual_count >= 3 THEN density_label := 'MEDIUM';
    END IF;

    SELECT strength_score INTO strength_val FROM public.relationship_intelligence WHERE user_a = viewer_id AND user_b = target_id;
    IF strength_val >= 0.8 THEN strength_label := 'Very strong';
    ELSIF strength_val >= 0.5 THEN strength_label := 'Strong';
    ELSIF strength_val >= 0.2 THEN strength_label := 'Moderate';
    ELSIF strength_val > 0.0 THEN strength_label := 'Weak';
    END IF;

    RETURN jsonb_build_object(
        'distance', result_distance,
        'mutualCount', COALESCE(mutual_count, 0),
        'mutualPreviewIds', mutual_previews,
        'densityLabel', density_label,
        'strengthLabel', strength_label,
        'nudgesReceived', COALESCE(nudge_count, 0)
    );
END;
$$;

-- 4. Bounded Path Discovery RPC
CREATE OR REPLACE FUNCTION public.get_connection_paths(target_user_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    viewer_id UUID := auth.uid();
    target_id UUID := target_user_id::uuid;
BEGIN
    RETURN (
        WITH RECURSIVE bfs_paths(user_id, depth, path) AS (
            SELECT p.id, 1, ARRAY[p.id]
            FROM public.profiles p
            WHERE public.is_social_connected(viewer_id, p.id)
          UNION ALL
            SELECT p_target.id, depth + 1, bp.path || p_target.id
            FROM public.profiles p_target
            JOIN bfs_paths bp ON public.is_social_connected(bp.user_id, p_target.id)
            JOIN public.user_privacy_settings privacy ON bp.user_id::text = privacy.user_id::text
            WHERE depth < 4
            AND (
                privacy.connections_visibility = 'EVERYONE'
                OR (privacy.connections_visibility = 'CONTACTS' AND public.is_social_connected(bp.user_id, viewer_id))
            )
            AND NOT p_target.id = ANY(bp.path)
        )
        SELECT COALESCE(jsonb_agg(masked_path), '[]'::jsonb)
        FROM (
            SELECT
                (
                    SELECT jsonb_agg(
                        CASE
                            WHEN node_id = target_id THEN node_id::text
                            WHEN EXISTS (SELECT 1 FROM public.user_privacy_settings WHERE user_id::text = node_id::text AND connections_visibility = 'NOBODY') THEN 'masked'
                            ELSE node_id::text
                        END
                    )
                    FROM unnest(path) AS node_id
                ) as masked_path
            FROM bfs_paths
            WHERE user_id = target_id
            LIMIT 3
        ) t
    );
END;
$$;

-- 5. Send Nudge RPC (Efficient Aggregation)
-- Accepts a count and merges with existing unread nudges.
CREATE OR REPLACE FUNCTION public.send_nudge(target_user_id TEXT, nudge_increment INT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    viewer_id UUID := auth.uid();
    target_id UUID := target_user_id::uuid;
    viewer_name TEXT;
    existing_notif_id UUID;
    total_nudges INT;
BEGIN
    -- 1. Increment global nudge count on profile
    UPDATE public.profiles SET nudges_received = nudges_received + nudge_increment WHERE id = target_id;

    -- 2. Get viewer name
    SELECT COALESCE(full_name, username, 'Someone') INTO viewer_name FROM public.profiles WHERE id = viewer_id;

    -- 3. Check for existing unread nudge from this sender
    SELECT id, nudge_count INTO existing_notif_id, total_nudges
    FROM public.notifications
    WHERE user_id = target_id
    AND source_id = viewer_id::text
    AND is_read = false
    AND type = 'NUDGE'
    LIMIT 1;

    IF existing_notif_id IS NOT NULL THEN
        -- Update existing notification
        UPDATE public.notifications
        SET nudge_count = nudge_count + nudge_increment,
            message = viewer_name || ' nudged you ' || (nudge_count + nudge_increment)::text || ' times.',
            created_at = now()
        WHERE id = existing_notif_id;
    ELSE
        -- Create new notification
        INSERT INTO public.notifications (
            id, user_id, type, priority, title, message, created_at, is_read, source_id, nudge_count
        ) VALUES (
            gen_random_uuid(),
            target_id,
            'NUDGE',
            'HIGH',
            'New Nudge!',
            CASE
                WHEN nudge_increment > 1 THEN viewer_name || ' nudged you ' || nudge_increment::text || ' times.'
                ELSE viewer_name || ' nudged you.'
            END,
            now(),
            false,
            viewer_id::text,
            nudge_increment
        );
    END IF;
END;
$$;
