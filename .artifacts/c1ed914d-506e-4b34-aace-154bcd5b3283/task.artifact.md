# Task List - Fix Room Migration for `vault_items`

- `[x]` Update `MIGRATION_82_83` in `KeeftalkDatabase.kt` to include `vault_items` changes
- `[x]` Verify database initialization
- `[x]` Confirm `vault_items` indices are correctly created
- `[x]` Broaden Room migration fix for other missing indices (`message_attachment`, etc.)
- `[x]` Improve `AppModule` to handle migration failures by forcing destructive migration
- `[x]` Fix unrelated build error in `ChatDetailScreen.kt`
- `[x]` Update remote `full_keeftalk_schema.sql` and provide sync script
