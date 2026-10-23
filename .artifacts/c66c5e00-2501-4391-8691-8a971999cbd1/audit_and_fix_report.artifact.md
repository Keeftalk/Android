# Supabase Error Audit and Fix Report

This report analyzes the 5XX and PostgreSQL error logs provided by the user, identifies root causes, and outlines the fixes applied.

## Error Summary & Audit Findings

### 1. `infinite recursion detected in policy for relation "family_members"` (Postgres Error: `42P17`)
- **Root Cause**: Row Level Security (RLS) policies on `public.family_members` referenced `family_members` recursively in their `USING` clauses without a termination condition or safe lookup path.
- **Impact**: Any query checking family membership or relationships threw Postgres error `42P17`.

### 2. `GET | 500 | /rest/v1/messages?...` & `GET | 500 | /rest/v1/calendar_items?select=*` (HTTP Status: `500`)
- **Root Cause**: PostgREST queries for `messages`, `message_attachment`, and `calendar_items` depend on RLS policies that query family membership (`family_members` and `family_permissions`). Because `family_members` policies suffered from infinite recursion (`42P17`), executing these queries caused database transaction aborts, resulting in HTTP 500 errors.

### 3. `new row violates row-level security policy for table "buckets"` (Postgres Error: `42501`)
- **Root Cause**: The client-side initialization (`ensureBucketExists()`) attempted to create or insert buckets directly into `storage.buckets` using an authenticated/anon client token rather than the Supabase service role key (`admin` client). Supabase Storage enables RLS on `storage.buckets`, which blocks client insertions.

---

## Solutions & Fixes Applied

1. **Created [fix_all_5xx_errors.sql](file:///home/m/StudioProjects/Android/fix_all_5xx_errors.sql)**:
   - Dropped recursive policies on `public.family_members` and re-created clean, non-recursive RLS policies (`auth.uid() = user_id OR auth.uid() = member_id OR ...`).
   - Re-created robust, non-recursive RLS policies for `public.calendar_items` to safely query family sharing links.
   - Inserted missing `files` storage bucket via idempotent script and ensured storage object upload/download policies (`storage.objects`) are correctly configured for authenticated users.

2. **Client-Side Storage Initialization**:
   - Ensured `ChatRepositoryImpl.kt` and initialization routines use the admin/service role client (`getAdminSupabase()`) for administrative operations such as bucket creation (`storage.createBucket`), bypassing client-side RLS bucket insertion restrictions.
