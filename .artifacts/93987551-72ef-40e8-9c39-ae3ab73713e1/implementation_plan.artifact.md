# Implementation Plan - Supabase E2EE & File Schema

This plan provides the complete PostgreSQL schema and RLS policies required to support the new E2EE and Unified File Architecture in Supabase.

## Proposed Changes

### [Component: Database - Tables]

#### [NEW] [full_keeftalk_schema.sql](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/full_keeftalk_schema.sql)
- **`user_security_settings`**: Stores the wrapped Account Encryption Key (AEK) and recovery metadata.
- **`conversation_keys`**: Stores encrypted Per-Conversation Keys (PCK) with epoch support.
- **`files`**: Central repository for physical file records, supporting opaque paths and per-user deduplication.
- **`vault_items`**: Stores user-specific vault references to physical files.
- **`messages`**: Updated with columns for ciphertext, nonces, and crypto versioning.
- **`notes`**, **`calendar_items`**: Updated to support encrypted content storage.

### [Component: Database - RLS Policies]
- Strict ownership policies for `user_security_settings`, `vault_items`, and `files`.
- Shared access policies for `conversation_keys` and `messages` based on chat membership.
- Storage policies for the `files` bucket to allow authenticated reads/writes while maintaining zero-knowledge security.

## Verification Plan

### Manual Verification
1.  **Schema Execution**: Apply the SQL to a Supabase project.
2.  **API Testing**: Perform mock inserts from the Supabase dashboard to verify RLS blocks unauthorized access.
3.  **App Integration**: Run the Keeftalk app on a fresh database and verify that signup (initializes AEK) and Vault upload (populates `files` and `vault_items`) work correctly.
