-- ========================================================
-- MANDATORY EMAIL VERIFICATION & PROFILE SYNC (SUPABASE)
-- ========================================================

-- 1. Create a security definer function to check verification status
CREATE OR REPLACE FUNCTION public.is_email_verified()
RETURNS BOOLEAN AS $$
DECLARE
  confirmed_at TIMESTAMPTZ;
BEGIN
  SELECT email_confirmed_at INTO confirmed_at
  FROM auth.users
  WHERE id = auth.uid();

  RETURN confirmed_at IS NOT NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- 2. RLS Policies on public.profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "public read profiles" ON public.profiles;
DROP POLICY IF EXISTS "verified users can read profiles" ON public.profiles;
CREATE POLICY "verified users can read profiles"
ON public.profiles FOR SELECT
USING (public.is_email_verified());

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
ON public.profiles FOR INSERT
TO authenticated
WITH CHECK (auth.uid()::text = id::text);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
ON public.profiles FOR UPDATE
TO authenticated
USING (auth.uid()::text = id::text)
WITH CHECK (auth.uid()::text = id::text);

-- 3. Update Chat Policies
DROP POLICY IF EXISTS "chat members can read chats" ON public.chats;
CREATE POLICY "verified members can read chats"
ON public.chats FOR SELECT
USING (public.is_email_verified() AND public.is_chat_member(id));

-- 4. Update Message Policies
DROP POLICY IF EXISTS "members read messages" ON public.messages;
CREATE POLICY "verified members read messages"
ON public.messages FOR SELECT
USING (public.is_email_verified() AND public.is_chat_member(chat_id));

-- 5. TRIGGER: Exception-Safe Profile Creation / Update on User Creation / Update
CREATE OR REPLACE FUNCTION public.handle_new_user_profile()
RETURNS TRIGGER AS $$
BEGIN
  BEGIN
    INSERT INTO public.profiles (id, email, username, full_name, phone, country, country_code, phone_verified)
    VALUES (
      NEW.id,
      NEW.email,
      COALESCE(NULLIF(NEW.raw_user_meta_data->>'username', ''), 'user_' || substr(NEW.id::text, 1, 8)),
      NEW.raw_user_meta_data->>'full_name',
      NEW.raw_user_meta_data->>'phone',
      NEW.raw_user_meta_data->>'country',
      NEW.raw_user_meta_data->>'country_code',
      FALSE
    )
    ON CONFLICT (id) DO UPDATE SET
      email = EXCLUDED.email,
      username = COALESCE(NULLIF(public.profiles.username, ''), EXCLUDED.username),
      full_name = COALESCE(NULLIF(public.profiles.full_name, ''), EXCLUDED.full_name),
      phone = COALESCE(NULLIF(public.profiles.phone, ''), EXCLUDED.phone),
      country = COALESCE(NULLIF(public.profiles.country, ''), EXCLUDED.country),
      country_code = COALESCE(NULLIF(public.profiles.country_code, ''), EXCLUDED.country_code);
  EXCEPTION
    WHEN OTHERS THEN
      RAISE WARNING 'handle_new_user_profile warning for user %: %', NEW.id, SQLERRM;
  END;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Attach trigger AFTER INSERT OR UPDATE on auth.users
DROP TRIGGER IF EXISTS on_auth_user_confirmed ON auth.users;
DROP TRIGGER IF EXISTS on_auth_user_created_or_updated ON auth.users;
CREATE TRIGGER on_auth_user_created_or_updated
  AFTER INSERT OR UPDATE ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user_profile();
