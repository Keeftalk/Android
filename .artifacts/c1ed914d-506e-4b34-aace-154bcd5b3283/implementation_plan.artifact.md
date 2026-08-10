# Room Migration Fix for `vault_items`

The application is crashing due to a Room migration error where the `vault_items` table is missing expected indices after upgrading to version 83/84. Specifically, indices for `file_id`, `folder_id`, and `user_id` are expected but not found in the actual database schema.

## User Review Required

> [!IMPORTANT]
> The fix involves modifying existing migration logic. If some users have already successfully migrated partially (though unlikely given the crash), this might cause "index already exists" errors if not handled with `IF NOT EXISTS`.

## Proposed Changes

### Database Migration (Local)

#### [MODIFY] [KeeftalkDatabase.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/data/local/KeeftalkDatabase.kt)

- Keep version at **87**.
- Keep all added migrations (`84_85`, `85_86`, `86_87`).

#### [MODIFY] [AppModule.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/di/AppModule.kt)

- Update the database initialization catch block to handle `IllegalStateException` (migration errors).
- If a migration error occurs, delete the database and rebuild to trigger destructive migration.

### Database Sync (Remote/Supabase)

#### [MODIFY] [full_keeftalk_schema.sql](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/full_keeftalk_schema.sql)

- Add missing thumbnail columns to `public.files` table:
    - `thumbnail_remote_path` (TEXT)
    - `thumbnail_local_path` (TEXT)
    - `thumbnail_size` (BIGINT)
    - `thumbnail_width` (INTEGER)
    - `thumbnail_height` (INTEGER)
    - `thumbnail_hmac` (TEXT)

#### [NEW] [add_thumbnail_columns.sql](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/.artifacts/c1ed914d-506e-4b34-aace-154bcd5b3283/scratch/add_thumbnail_columns.sql)

- Create a surgical SQL script to add these columns to an existing Supabase instance.

## Verification Plan

### Automated Tests
- Run the app and verify it no longer crashes during database initialization.
- Since I cannot easily run instrumented tests here, I will rely on the app's successful startup.

### Manual Verification
- Deploy the app to the device.
- Check Logcat for "Database opened successfully."
