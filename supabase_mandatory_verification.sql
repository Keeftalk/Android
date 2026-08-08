-- ========================================================
-- MANDATORY EMAIL VERIFICATION ENFORCEMENT (SUPABASE)
-- ========================================================

-- 1. Create a security definer function to check verification status
-- This function can be used in RLS policies to block unverified users.
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

-- 2. Update Profiles Policies
-- We restrict profile visibility to ONLY verified users.
-- Note: A user can still see THEIR OWN profile metadata during the verification screen
-- if we allow a specific exception, but for maximum security, we block everything.

DROP POLICY IF EXISTS "public read profiles" ON public.profiles;
CREATE POLICY "verified users can read profiles"
ON public.profiles FOR SELECT
USING (public.is_email_verified());

-- Allow unverified users to INSERT their initial profile (happens during signup/first login)
-- But we already have "users can insert own profile" which is fine.

-- 3. Update Chat Policies
-- Ensure only verified members can see or interact with chats.
DROP POLICY IF EXISTS "chat members can read chats" ON public.chats;
CREATE POLICY "verified members can read chats"
ON public.chats FOR SELECT
USING (public.is_email_verified() AND public.is_chat_member(id));

-- 4. Update Message Policies
DROP POLICY IF EXISTS "members read messages" ON public.messages;
CREATE POLICY "verified members read messages"
ON public.messages FOR SELECT
USING (public.is_email_verified() AND public.is_chat_member(chat_id));

-- 5. TRIGGER: Auto-create Profile on Confirmation (Optional but recommended)
-- This ensures the 'profiles' entry exists as soon as the email is clicked.
CREATE OR REPLACE FUNCTION public.handle_new_user_profile()
RETURNS TRIGGER AS $$
BEGIN
  -- We only create the profile if it doesn't exist and the email is confirmed
  IF NEW.email_confirmed_at IS NOT NULL AND NOT EXISTS (SELECT 1 FROM public.profiles WHERE id = NEW.id) THEN
    INSERT INTO public.profiles (id, email, username, full_name)
    VALUES (
      NEW.id,
      NEW.email,
      COALESCE(NEW.raw_user_meta_data->>'username', 'user_' || substr(NEW.id::text, 1, 8)),
      NEW.raw_user_meta_data->>'full_name'
    );
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- Trigger on auth.users update (since confirmed_at changes from null to a value)
DROP TRIGGER IF EXISTS on_auth_user_confirmed ON auth.users;
CREATE TRIGGER on_auth_user_confirmed
  AFTER UPDATE OF email_confirmed_at ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user_profile();
