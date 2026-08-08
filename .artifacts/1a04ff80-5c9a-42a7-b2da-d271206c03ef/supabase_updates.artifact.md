# Supabase Consolidated Migration SQL

Copy and paste the following code into your **Supabase SQL Editor** to apply all necessary changes for the new sharing features.

```sql
-- ==========================================
-- 1. UPDATE MESSAGE TYPES
-- ==========================================
-- Drop existing constraint if it exists (standard Supabase name)
ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_type_check;

-- Re-add the constraint with new sharing types included
ALTER TABLE messages ADD CONSTRAINT messages_type_check
CHECK (type IN (
    'TEXT', 'IMAGE', 'VIDEO', 'VOICE', 'FILE', 'PDF', 'LOCATION',
    'CONTACT', 'POLL', 'CALL_LOG',
    'SHARED_NOTE', 'SHARED_EMAIL', 'SHARED_VAULT_FILE', 'SHARED_AGENDA'
));

-- ==========================================
-- 2. ENHANCE VAULT SYNC
-- ==========================================
-- Ensure metadata column exists for sharing state tracking
ALTER TABLE vault_items ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

-- Create an index for shared item discovery
CREATE INDEX IF NOT EXISTS idx_vault_items_is_shared ON vault_items ((metadata->>'isShared'));

-- ==========================================
-- 3. ENHANCE EMAIL SYNC
-- ==========================================
-- Add sharing flag to mail messages
ALTER TABLE mail_messages ADD COLUMN IF NOT EXISTS is_shared BOOLEAN DEFAULT FALSE;

-- Create index for fast retrieval of shared emails
CREATE INDEX IF NOT EXISTS idx_mail_messages_shared ON mail_messages (is_shared) WHERE is_shared = TRUE;
```

> [!CAUTION]
> If you have existing custom constraints on the `messages` table with a different name, replace `messages_type_check` with your specific constraint name.
