-- Surgical script to add thumbnail columns to existing public.files table
ALTER TABLE public.files ADD COLUMN IF NOT EXISTS thumbnail_remote_path TEXT;
ALTER TABLE public.files ADD COLUMN IF NOT EXISTS thumbnail_local_path TEXT;
ALTER TABLE public.files ADD COLUMN IF NOT EXISTS thumbnail_size BIGINT;
ALTER TABLE public.files ADD COLUMN IF NOT EXISTS thumbnail_width INTEGER;
ALTER TABLE public.files ADD COLUMN IF NOT EXISTS thumbnail_height INTEGER;
ALTER TABLE public.files ADD COLUMN IF NOT EXISTS thumbnail_hmac TEXT;
