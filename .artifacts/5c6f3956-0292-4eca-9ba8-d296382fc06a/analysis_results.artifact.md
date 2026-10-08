# Keeftalk Deep Audit Report: My Habits & Full App Supabase Sync

This audit report provides a comprehensive analysis of the **My Habits** feature and the **app-wide Supabase synchronization** architecture in Keeftalk. All findings are derived directly from the project's codebase, schema definitions, migrations, and architecture.

---

## 1. My Habits Architecture Audit

### Current Implementation Overview
* **Local Database (Room):** Defined in [HabitEntities.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/local/entities/HabitEntities.kt) and [NativeHabitDao.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/local/dao/NativeHabitDao.kt). Tables include `habits`, `habit_schedules`, `habit_checkins`, `habit_grace_uses`, and `habit_milestones`.
* **Repository & Engine:** [HabitsRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/HabitsRepositoryImpl.kt) handles local persistence and reminder scheduling via `HabitReminderManager`. [HabitEngine.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/domain/engine/HabitEngine.kt) computes streaks, due dates, automaticity, mastery, and recovery engine mechanics (levels 1–4 recovery challenges for missed habits).
* **UI Layer:** [HabitsScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/habits/HabitsScreen.kt), [HabitsViewModel.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/habits/HabitsViewModel.kt), and [AddEditHabitSheet.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/habits/AddEditHabitSheet.kt) provide category tagging, slot filtering (Morning, Midday, Evening, etc.), preview modes for past dates, stats, and grace pool management.

### Identified Gaps, Bugs & Architectural Inconsistencies
1. **Remote Sync is Currently a No-Op:** [HabitsRemoteDataSource.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/remote/HabitsRemoteDataSource.kt) implements `HabitsRemoteDataSourceNoOp`. While [habits_supabase_schema.sql](file:///home/m/StudioProjects/Android/app/src/main/assets/habits_supabase_schema.sql) exists in assets, no active network synchronization, background sync worker, or Supabase Realtime channel is wired up for habits.
2. **User ID Fallback Vulnerability:** [HabitsRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/HabitsRepositoryImpl.kt) falls back to `"local_user"` if the Supabase session is unauthenticated. When synced to Supabase (where `user_id` is a UUID foreign key referencing `auth.users(id)`), `"local_user"` will cause foreign key constraint violations.
3. **Missing Habits Settings:** There is currently no dedicated settings section for Habits in the Keeftalk settings architecture (`UserSettings` / `SettingsRepository`).
4. **Conflict Resolution & Offline Queue:** Unlike calendar or vault modules which utilize sync queue DAOs (e.g., `CalendarSyncQueueDao`, `VaultSyncQueueDao`), habits lack an offline mutation queue to retry failed pushes when offline.

---

## 2. Recommended My Habits Settings Structure

To fit Keeftalk's unified settings architecture (seen in [SettingsRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/SettingsRepositoryImpl.kt) and [UserSettingsDto.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/remote/UserSettingsDto.kt)), we should add a `user_habit_settings` JSONB column (or property within `user_settings`) to control habit behavior and persist/sync across devices.

### Recommended Settings Properties (`UserHabitSettings`)
* `defaultReminderTime`: `String` (default `"08:00"`) — Default reminder time when creating a new habit.
* `defaultCategory`: `String` (default `"drink"`) — Default category preset.
* `weekStartDay`: `String` (default `"MONDAY"`) — First day of the week for calendars and streak views.
* `gracePoolAlertsEnabled`: `Boolean` (default `true`) — Notify user when grace pool is running low (1 or 0 remaining).
* `autoArchiveMastered`: `Boolean` (default `false`) — Automatically archive or collapse mastered habits.
* `hideCompletedToday`: `Boolean` (default `false`) — Option to hide habits completed for the day.
* `syncHabitsAcrossDevices`: `Boolean` (default `true`) — Enable/disable cloud sync for habits.

---

## 3. Complete App-Wide Supabase Sync Audit

Keeftalk features robust Supabase integration across several modules, but exhibits inconsistencies in others:
* **Chat & Messages:** Uses Postgrest and Realtime channels (`ChatRepositoryImpl.kt`), with message remote mediators and encryption support.
* **Settings:** Uses a unified [user_settings](file:///home/m/StudioProjects/Android/supabase_settings_migration.sql) table with JSONB columns for chat, call, note, vault, calendar, email, notifications, privacy, parental controls, and browser settings, backed by real-time subscriptions ([SettingsRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/SettingsRepositoryImpl.kt)).
* **Calendar & Notes:** Uses dedicated tables (`calendar_events`, `notes`, etc.) with RLS policies and sync queues.
* **Habits:** Currently entirely local-only with a no-op remote data source and no real-time subscription.

---

## 4. What Is Missing or Incorrect in Supabase

1. **Habits Tables Missing from Active Supabase Instance:** While [habits_supabase_schema.sql](file:///home/m/StudioProjects/Android/app/src/main/assets/habits_supabase_schema.sql) is defined in assets, it needs to be executed as an official Supabase migration.
2. **Habit Settings in `user_settings`:** The `user_settings` table in [supabase_settings_migration.sql](file:///home/m/StudioProjects/Android/supabase_settings_migration.sql) lacks a `habit_settings JSONB` column.
3. **Realtime Publication:** Habit tables (`habits`, `habit_schedules`, `habit_checkins`, `habit_grace_uses`) are not yet added to the Supabase Realtime publication.

---

## 5. Exact Database Changes Required

### SQL Migration Script (`supabase_habits_and_settings_migration.sql`)
```sql
-- 1. Add habit_settings to unified user_settings
ALTER TABLE IF EXISTS public.user_settings
ADD COLUMN IF NOT EXISTS habit_settings JSONB DEFAULT '{}';

-- 2. Create Habits Tables in Supabase
CREATE TABLE IF NOT EXISTS public.habits (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    name TEXT NOT NULL,
    icon TEXT NOT NULL,
    category TEXT NOT NULL,
    base_days INTEGER NOT NULL,
    created_date_epoch_day BIGINT NOT NULL,
    reminder_time_minutes INTEGER,
    slot TEXT NOT NULL,
    habit_type TEXT NOT NULL,
    amount INTEGER,
    unit TEXT,
    colorHex TEXT DEFAULT '#00CCCC' NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS public.habit_schedules (
    id TEXT PRIMARY KEY,
    habit_id TEXT REFERENCES public.habits(id) ON DELETE CASCADE NOT NULL,
    effective_from_epoch_day BIGINT NOT NULL,
    days_mask_string TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS public.habit_checkins (
    id TEXT PRIMARY KEY,
    habit_id TEXT REFERENCES public.habits(id) ON DELETE CASCADE NOT NULL,
    epoch_day BIGINT NOT NULL,
    completed BOOLEAN NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS public.habit_grace_uses (
    id TEXT PRIMARY KEY,
    habit_id TEXT REFERENCES public.habits(id) ON DELETE CASCADE NOT NULL,
    forgiven_epoch_day BIGINT NOT NULL,
    used_epoch_day BIGINT NOT NULL,
    used_month_key TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    sync_state TEXT DEFAULT 'SYNCED' NOT NULL
);

CREATE TABLE IF NOT EXISTS public.habit_milestones (
    key TEXT PRIMARY KEY,
    unlocked_at_epoch_day BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- 3. Enable RLS
ALTER TABLE public.habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_checkins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_grace_uses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_milestones ENABLE ROW LEVEL SECURITY;

-- 4. RLS Policies
CREATE POLICY "Users can manage their own habits" ON public.habits
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can manage their own schedules" ON public.habit_schedules
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id));

CREATE POLICY "Users can manage their own checkins" ON public.habit_checkins
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id));

CREATE POLICY "Users can manage their own grace uses" ON public.habit_grace_uses
    FOR ALL USING (auth.uid() IN (SELECT user_id FROM public.habits WHERE id = habit_id));

-- 5. Add to Supabase Realtime Publication
ALTER PUBLICATION supabase_realtime ADD TABLE public.habits;
ALTER PUBLICATION supabase_realtime ADD TABLE public.habit_schedules;
ALTER PUBLICATION supabase_realtime ADD TABLE public.habit_checkins;
ALTER PUBLICATION supabase_realtime ADD TABLE public.habit_grace_uses;
```

---

## 6. Required Storage/Bucket Changes
* No media storage buckets are strictly required for core habits functionality. However, if custom habit badge icons or attachments are introduced, a `habit_assets` bucket with authenticated read/write policies can be added.

---

## 7. Required Android-Side Sync Changes
1. **Implement `HabitsRemoteDataSourceImpl`**: Replace [HabitsRemoteDataSourceNoOp](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/remote/HabitsRemoteDataSource.kt) with an active Supabase Postgrest & Realtime data source.
2. **Update `HabitsRepositoryImpl`**:
   * Handle authenticated `user_id` properly instead of falling back to `"local_user"`.
   * Add push/pull synchronization methods triggered on app launch and network reconnect.
3. **Add `UserHabitSettings` DTO & Domain Model**: Wire up habit settings into [UserSettings.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/domain/model/UserSettings.kt), [UserSettingsDto.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/remote/UserSettingsDto.kt), and [SettingsRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/SettingsRepositoryImpl.kt).

---

## 8. Migration Plan
1. **Phase 1: Database Migration** — Run the Supabase SQL migration script to create tables, update `user_settings`, enable RLS, and configure Realtime publication.
2. **Phase 2: Data Models & DTOs** — Add `UserHabitSettings` and update Supabase DTOs.
3. **Phase 3: Repository & Remote Data Source** — Replace `HabitsRemoteDataSourceNoOp` with full Supabase sync logic and realtime subscriptions.
4. **Phase 4: Settings Screen Integration** — Build `HabitSettingsScreen` and integrate into the main Keeftalk settings navigation.
5. **Phase 5: Verification** — Test offline-first behavior, conflict resolution, check-in rules (Rule S9), and real-time multi-device sync.

---

## 9. Potential Data-Loss & Security Risks
* **Foreign Key Violations:** Inserting habits with `user_id = "local_user"` while enforcing `auth.uid() = user_id` in Supabase will reject writes. Proper session validation is required.
* **Race Conditions on Check-Ins:** Concurrent check-in updates from multiple devices on the same `epoch_day` should use last-write-wins based on `updated_at` timestamps or upsert conflict resolution.
* **RLS Misconfiguration:** Ensuring sub-tables (`habit_checkins`, `habit_schedules`) properly verify ownership via parent `habits.user_id`.

---

## 10. Final Implementation Checklist
- [ ] Execute Supabase SQL migration for habits and settings.
- [ ] Implement `HabitsRemoteDataSourceImpl` with Postgrest & Realtime.
- [ ] Update `HabitsRepositoryImpl` to use authenticated Supabase user ID.
- [ ] Add `UserHabitSettings` domain model and DTO.
- [ ] Create `HabitSettingsScreen` adhering to Keeftalk settings UI patterns.
- [ ] Verify offline persistence and online synchronization.
