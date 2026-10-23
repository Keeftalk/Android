# Audit and Fix Report

## 1. Avatar Caching `MalformedURLException` Audit & Fix

### Issue
Logs showed:
```
AvatarManager tn.keeftalk.chat W Failed to proactively cache avatar for ... from content://media/picker/0/com.android.providers.media.photopicker/media/1000056175
java.net.MalformedURLException: unknown protocol: content
  at java.net.URL.<init>(URL.java:608)
  at tn.keeftalk.chat.util.PersistentAvatarManager.downloadAndCacheAvatar(PersistentAvatarManager.kt:71)
```

### Root Cause
`PersistentAvatarManager.downloadAndCacheAvatar` attempted to instantiate `java.net.URL(url)` for all avatar URLs. When users selected an avatar from the local device PhotoPicker or Gallery (`content://` URI), `java.net.URL` threw a `MalformedURLException` because `content://` is not a network protocol supported by `java.net.URL`.

### Fix
Updated `[PersistentAvatarManager.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/util/PersistentAvatarManager.kt)` to inspect the URI scheme:
- For `content://` and `file://` URIs, it now uses `context.contentResolver.openInputStream(uri)`.
- For `http://` and `https://` URIs, it uses `java.net.URL.openConnection()`.
- Added robust fallback handling for local file paths and network errors.

---

## 2. Supabase RLS Policy Infinite Recursion Audit & Fix (Error 42P17)

### Issue
Logs showed:
```
syncChatMessages failed ...
io.github.jan.supabase.postgrest.exception.PostgrestRestException: infinite recursion detected in policy for relation "family_members"
Code: 42P17
```

### Root Cause
PostgreSQL Row Level Security (RLS) policies on `family_members` (or policies on other tables referencing `family_members` in a circular subquery without `SECURITY DEFINER`) resulted in infinite recursion when evaluating queries on messages, chats, or family data.

### Fix
Execute the provided [fix_family_members_recursion.sql](file:///home/m/StudioProjects/Android/fix_family_members_recursion.sql) in your Supabase SQL Editor:
1. Drop recursive policies on `public.family_members`.
2. Re-create clean, non-recursive policies:
   ```sql
   DROP POLICY IF EXISTS "Users can view their family links" ON public.family_members;
   DROP POLICY IF EXISTS "Users can manage their family links" ON public.family_members;

   CREATE POLICY "Users can view their family links" ON public.family_members
       FOR SELECT
       USING (auth.uid() = user_id OR auth.uid() = member_id);

   CREATE POLICY "Users can manage their family links" ON public.family_members
       FOR ALL
       USING (auth.uid() = user_id)
       WITH CHECK (auth.uid() = user_id);

   ALTER TABLE public.family_members ENABLE ROW LEVEL SECURITY;
   ```
3. Ensure dependent functions use `SECURITY DEFINER` to safely bypass unnecessary policy re-evaluation recursion.
