-- ====================================================================
-- KEEFTALK EXTENSION STORE SCHEMA & MIGRATION (IDEMPOTENT / SAFE RETRY)
-- Migration: 20260305_extension_store.sql
-- ====================================================================

-- 1. PUBLISHERS TABLE
CREATE TABLE IF NOT EXISTS public.store_publishers (
    id TEXT PRIMARY KEY, -- e.g. 'official.keeftalk.com'
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    display_name TEXT NOT NULL,
    description TEXT DEFAULT '',
    website TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_developer_approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. EXTENSIONS TABLE
CREATE TABLE IF NOT EXISTS public.store_extensions (
    id TEXT PRIMARY KEY, -- e.g. 'com.keeftalk.dictionary.english.pro'
    slug TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'TOOL', -- DICTIONARY, THEME, EMOJI_PACK, ICON_PACK, SOUND_PACK, FEED_CARD, FILE_HANDLER, TOOL
    publisher_id TEXT NOT NULL REFERENCES public.store_publishers(id) ON DELETE CASCADE,
    short_description TEXT DEFAULT '',
    long_description TEXT DEFAULT '',
    category TEXT NOT NULL DEFAULT 'tools', -- dictionaries, themes, icons, code, tools, security, focus, media, study, finance
    age_rating TEXT NOT NULL DEFAULT '4+',
    languages TEXT[] NOT NULL DEFAULT '{"English"}',
    compatible_modules TEXT[] NOT NULL DEFAULT '{}',
    min_app_version TEXT NOT NULL DEFAULT '3.0.0',
    current_version TEXT NOT NULL DEFAULT '1.0.0',
    status TEXT NOT NULL DEFAULT 'PUBLISHED', -- DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, WITHDRAWN, REVOKED
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    download_count BIGINT NOT NULL DEFAULT 0,
    install_count BIGINT NOT NULL DEFAULT 0,
    rating_avg NUMERIC(3,2) NOT NULL DEFAULT 0.00,
    rating_count INTEGER NOT NULL DEFAULT 0,
    cover_url TEXT,
    screenshot01 TEXT,
    screenshot02 TEXT,
    screenshot03 TEXT,
    screenshot04 TEXT,
    screenshot05 TEXT,
    screenshot06 TEXT,
    screenshot07 TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. EXTENSION RELEASES TABLE
CREATE TABLE IF NOT EXISTS public.store_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    extension_id TEXT NOT NULL REFERENCES public.store_extensions(id) ON DELETE CASCADE,
    version TEXT NOT NULL,
    package_object_key TEXT NOT NULL,
    package_hash_sha256 TEXT NOT NULL,
    package_size_bytes BIGINT NOT NULL DEFAULT 0,
    release_notes TEXT DEFAULT '',
    min_app_version TEXT NOT NULL DEFAULT '3.0.0',
    signing_key_id TEXT NOT NULL DEFAULT 'official-keeftalk-key-2026',
    status TEXT NOT NULL DEFAULT 'PUBLISHED', -- DRAFT, PENDING_REVIEW, APPROVED, PUBLISHED, REJECTED, REVOKED
    published_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT store_releases_extension_version_key UNIQUE (extension_id, version)
);

-- 4. MEDIA ASSETS TABLE (optional legacy/alternative asset tracking)
CREATE TABLE IF NOT EXISTS public.store_media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    extension_id TEXT NOT NULL REFERENCES public.store_extensions(id) ON DELETE CASCADE,
    release_version TEXT NOT NULL,
    asset_type TEXT NOT NULL CHECK (asset_type IN ('COVER', 'SCREENSHOT')),
    object_key TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    file_size_bytes BIGINT DEFAULT 0,
    hash_sha256 TEXT DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. RATINGS & REVIEWS TABLE
CREATE TABLE IF NOT EXISTS public.store_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    extension_id TEXT NOT NULL REFERENCES public.store_extensions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    author_name TEXT NOT NULL DEFAULT 'Keeftalk User',
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title TEXT DEFAULT '',
    review_text TEXT DEFAULT '',
    helpful_count INTEGER NOT NULL DEFAULT 0,
    developer_reply TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. INSTALLATIONS TABLE
CREATE TABLE IF NOT EXISTS public.store_installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    extension_id TEXT NOT NULL REFERENCES public.store_extensions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    device_id TEXT NOT NULL DEFAULT 'default_device',
    installed_version TEXT NOT NULL,
    country_code TEXT NOT NULL DEFAULT 'GLOBAL',
    status TEXT NOT NULL DEFAULT 'INSTALLED', -- INSTALLED, UNINSTALLED
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ====================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ====================================================================

ALTER TABLE public.store_publishers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_extensions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_releases ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_media_assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_ratings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.store_installations ENABLE ROW LEVEL SECURITY;

-- 1. Publishers
DROP POLICY IF EXISTS "Public read approved publishers" ON public.store_publishers;
CREATE POLICY "Public read approved publishers" ON public.store_publishers FOR SELECT USING (TRUE);

-- 2. Extensions
DROP POLICY IF EXISTS "Public read published extensions" ON public.store_extensions;
CREATE POLICY "Public read published extensions" ON public.store_extensions FOR SELECT USING (TRUE);

-- 3. Releases
DROP POLICY IF EXISTS "Public read published releases" ON public.store_releases;
CREATE POLICY "Public read published releases" ON public.store_releases FOR SELECT USING (TRUE);

-- 4. Media Assets
DROP POLICY IF EXISTS "Public read media assets" ON public.store_media_assets;
CREATE POLICY "Public read media assets" ON public.store_media_assets FOR SELECT USING (TRUE);

-- 5. Ratings & Reviews
DROP POLICY IF EXISTS "Public read ratings" ON public.store_ratings;
CREATE POLICY "Public read ratings" ON public.store_ratings FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "Anyone insert ratings" ON public.store_ratings;
CREATE POLICY "Anyone insert ratings" ON public.store_ratings FOR INSERT WITH CHECK (TRUE);

-- 6. Installations
DROP POLICY IF EXISTS "Anyone insert installations" ON public.store_installations;
CREATE POLICY "Anyone insert installations" ON public.store_installations FOR INSERT WITH CHECK (TRUE);

-- ====================================================================
-- RATING STATS TRIGGER FUNCTION
-- ====================================================================

CREATE OR REPLACE FUNCTION public.update_extension_rating_stats()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.store_extensions
    SET rating_count = (
            SELECT COUNT(*) FROM public.store_ratings WHERE extension_id = COALESCE(NEW.extension_id, OLD.extension_id)
        ),
        rating_avg = COALESCE((
            SELECT ROUND(AVG(rating)::numeric, 2) FROM public.store_ratings WHERE extension_id = COALESCE(NEW.extension_id, OLD.extension_id)
        ), 0.00),
        updated_at = NOW()
    WHERE id = COALESCE(NEW.extension_id, OLD.extension_id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_store_rating_stats ON public.store_ratings;
CREATE TRIGGER trg_store_rating_stats
AFTER INSERT OR UPDATE OR DELETE ON public.store_ratings
FOR EACH ROW EXECUTE FUNCTION public.update_extension_rating_stats();

-- ====================================================================
-- INSTALLATION COUNT TRIGGER FUNCTION
-- ====================================================================

CREATE OR REPLACE FUNCTION public.update_extension_install_stats()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.store_extensions
    SET install_count = (
            SELECT COUNT(*) FROM public.store_installations WHERE extension_id = NEW.extension_id AND status = 'INSTALLED'
        ),
        updated_at = NOW()
    WHERE id = NEW.extension_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_store_install_stats ON public.store_installations;
CREATE TRIGGER trg_store_install_stats
AFTER INSERT ON public.store_installations
FOR EACH ROW EXECUTE FUNCTION public.update_extension_install_stats();

-- ====================================================================
-- INITIAL OFFICIAL PUBLISHER SEED
-- ====================================================================

INSERT INTO public.store_publishers (id, display_name, description, website, is_verified, is_developer_approved)
VALUES (
    'official.keeftalk.com',
    'Keeftalk Official',
    'Official Keeftalk extensions platform development team.',
    'https://keeftalk.com',
    TRUE,
    TRUE
)
ON CONFLICT (id) DO NOTHING;
