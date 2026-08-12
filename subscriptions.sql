-- =========================================
-- KEEFTALK SUBSCRIPTION & STORAGE QUOTA SYSTEM
-- =========================================

-- 1. SUBSCRIPTION ENUMS & TABLES
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'subscription_plan') THEN
        CREATE TYPE public.subscription_plan AS ENUM (
            'FREE',
            'PLUS_MONTHLY',
            'PLUS_YEARLY',
            'PRO_MONTHLY',
            'FAMILY_MONTHLY'
        );
    END IF;
END $$;

-- Subscriptions Table (Authority for entitlements)
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan public.subscription_plan NOT NULL DEFAULT 'FREE',
    status TEXT NOT NULL DEFAULT 'active', -- 'active', 'cancelled', 'expired', 'in_grace_period'
    purchase_token TEXT UNIQUE,
    order_id TEXT,
    expiry_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Families Table
CREATE TABLE IF NOT EXISTS public.families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id TEXT NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Family Members
CREATE TABLE IF NOT EXISTS public.family_members (
    family_id UUID REFERENCES public.families(id) ON DELETE CASCADE,
    user_id TEXT REFERENCES public.profiles(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (family_id, user_id)
);

-- 2. PROFILE EXTENSIONS
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='profiles' AND column_name='plan_type') THEN
        ALTER TABLE public.profiles ADD COLUMN plan_type public.subscription_plan DEFAULT 'FREE';
        ALTER TABLE public.profiles ADD COLUMN storage_limit BIGINT DEFAULT 5368709120; -- 5GB
        ALTER TABLE public.profiles ADD COLUMN storage_used BIGINT DEFAULT 0;
        ALTER TABLE public.profiles ADD COLUMN is_family_owner BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- 3. STORAGE ACCOUNTING FUNCTIONS
CREATE OR REPLACE FUNCTION public.calculate_user_storage_usage(target_user_id TEXT)
RETURNS BIGINT AS $$
DECLARE
    f_id UUID;
    total_usage BIGINT;
BEGIN
    -- Check if user is in a family
    SELECT family_id INTO f_id FROM public.family_members WHERE user_id = target_user_id;

    IF f_id IS NOT NULL THEN
        -- Shared family usage: sum of all files owned by all family members
        SELECT SUM(COALESCE(file_size, 0) + COALESCE(thumbnail_size, 0))
        INTO total_usage
        FROM public.files
        WHERE owner_id IN (SELECT user_id FROM public.family_members WHERE family_id = f_id)
        AND (status != 'DELETED' OR status IS NULL);
    ELSE
        -- Personal usage
        SELECT SUM(COALESCE(file_size, 0) + COALESCE(thumbnail_size, 0))
        INTO total_usage
        FROM public.files
        WHERE owner_id = target_user_id
        AND (status != 'DELETED' OR status IS NULL);
    END IF;

    RETURN COALESCE(total_usage, 0);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to update storage_used on profile
CREATE OR REPLACE FUNCTION public.update_profile_storage_usage()
RETURNS TRIGGER AS $$
DECLARE
    target_id TEXT;
    f_id UUID;
BEGIN
    target_id := COALESCE(NEW.owner_id, OLD.owner_id);

    -- Find if this user is in a family
    SELECT family_id INTO f_id FROM public.family_members WHERE user_id = target_id;

    IF f_id IS NOT NULL THEN
        -- Update all members of the family to keep their 'storage_used' in sync for the UI
        UPDATE public.profiles
        SET storage_used = public.calculate_user_storage_usage(target_id)
        WHERE id IN (SELECT user_id FROM public.family_members WHERE family_id = f_id);
    ELSE
        -- Update only the user
        UPDATE public.profiles
        SET storage_used = public.calculate_user_storage_usage(target_id)
        WHERE id = target_id;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_file_change ON public.files;
CREATE TRIGGER on_file_change
AFTER INSERT OR UPDATE OR DELETE ON public.files
FOR EACH ROW EXECUTE FUNCTION public.update_profile_storage_usage();

-- 4. UPLOAD LIMIT ENFORCEMENT
CREATE OR REPLACE FUNCTION public.check_file_upload_limits()
RETURNS TRIGGER AS $$
DECLARE
    u_plan public.subscription_plan;
    u_limit BIGINT;
    u_used BIGINT;
    max_file_size BIGINT;
BEGIN
    -- Get plan and current usage from profile SSOT
    SELECT plan_type, storage_limit, storage_used INTO u_plan, u_limit, u_used
    FROM public.profiles WHERE id = NEW.owner_id;

    -- 1. Check total quota
    IF (u_used + NEW.file_size) > u_limit THEN
        RAISE EXCEPTION 'Storage quota exceeded. Please upgrade your Keeftalk plan.';
    END IF;

    -- 2. Check max file size based on plan
    max_file_size := CASE
        WHEN u_plan = 'FREE' THEN 104857600 -- 100MB
        WHEN u_plan IN ('PLUS_MONTHLY', 'PLUS_YEARLY') THEN 1073741824 -- 1GB
        ELSE 5368709120 -- 5GB (Pro/Family)
    END;

    IF NEW.file_size > max_file_size THEN
        RAISE EXCEPTION 'File size exceeds %MB limit for your current plan.', (max_file_size / 1048576);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS before_file_insert ON public.files;
CREATE TRIGGER before_file_insert
BEFORE INSERT ON public.files
FOR EACH ROW EXECUTE FUNCTION public.check_file_upload_limits();

-- 5. ENTITLEMENT SYNC FUNCTION (Called by Edge Function)
CREATE OR REPLACE FUNCTION public.sync_subscription_entitlement(
    target_user_id TEXT,
    new_plan public.subscription_plan,
    new_limit BIGINT,
    new_status TEXT,
    new_expiry TIMESTAMPTZ,
    new_purchase_token TEXT,
    new_order_id TEXT
)
RETURNS VOID AS $$
BEGIN
    -- 1. Update or Insert Subscription Record
    INSERT INTO public.subscriptions (user_id, plan, status, purchase_token, order_id, expiry_date, updated_at)
    VALUES (target_user_id, new_plan, new_status, new_purchase_token, new_order_id, new_expiry, now())
    ON CONFLICT (purchase_token) DO UPDATE SET
        status = EXCLUDED.status,
        expiry_date = EXCLUDED.expiry_date,
        updated_at = now();

    -- 2. Update Profile SSOT
    UPDATE public.profiles
    SET plan_type = new_plan,
        storage_limit = new_limit
    WHERE id = target_user_id;

    -- 3. If Family plan, ensure family record exists
    IF new_plan = 'FAMILY_MONTHLY' THEN
        INSERT INTO public.families (owner_id)
        VALUES (target_user_id)
        ON CONFLICT DO NOTHING;

        -- Auto-add owner to their own family members if not present
        INSERT INTO public.family_members (family_id, user_id)
        SELECT id, owner_id FROM public.families WHERE owner_id = target_user_id
        ON CONFLICT DO NOTHING;

        UPDATE public.profiles SET is_family_owner = TRUE WHERE id = target_user_id;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. FAMILY MEMBER BENEFIT INHERITANCE
CREATE OR REPLACE FUNCTION public.sync_family_member_benefits()
RETURNS TRIGGER AS $$
DECLARE
    f_owner_id TEXT;
    owner_plan public.subscription_plan;
BEGIN
    -- If a new family member is added
    IF TG_OP = 'INSERT' THEN
        SELECT owner_id INTO f_owner_id FROM public.families WHERE id = NEW.family_id;
        SELECT plan_type INTO owner_plan FROM public.profiles WHERE id = f_owner_id;

        IF owner_plan = 'FAMILY_MONTHLY' THEN
            UPDATE public.profiles
            SET plan_type = 'PLUS_MONTHLY', -- Inherit Plus benefits
                storage_limit = 2199023255552 -- 2TB (Shared pool)
            WHERE id = NEW.user_id;
        END IF;
    END IF;

    -- If a member is removed
    IF TG_OP = 'DELETE' THEN
        UPDATE public.profiles
        SET plan_type = 'FREE',
            storage_limit = 5368709120, -- 5GB
            storage_used = public.calculate_user_storage_usage(OLD.user_id)
        WHERE id = OLD.user_id;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_family_member_change ON public.family_members;
CREATE TRIGGER on_family_member_change
AFTER INSERT OR DELETE ON public.family_members
FOR EACH ROW EXECUTE FUNCTION public.sync_family_member_benefits();
