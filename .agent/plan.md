# Project Plan

Implement the Authentication and User Discovery module for Keeftalk v2. This involves setting up Supabase backend (schema, RLS), implementing the Auth repository with multi-identifier support, building Login/Signup screens in Compose, and developing a user search system that allows starting chats while deduplicating existing conversations. Maintain offline-first principles and clean architecture.

## Project Brief

# Project Brief: Keeftalk v2 (Auth & Discovery)

Keeftalk v2 focuses on establishing a secure, scalable foundation for user identity and social discovery. This phase implements a robust authentication system and a searchable user directory, integrated with a Supabase backend.

## Features

*   **Multi-Identifier Authentication:** Secure signup and login supporting email, unique usernames, or phone numbers.
*   **User Profiles:** Automatic profile creation on signup, including full name, unique username, and metadata.
*   **User Discovery & Search:** Debounced, indexed search by username or full name.
*   **Chat Initialization:** Logic to deduplicate conversations and start/open chats seamlessly.
*   **Supabase Integration:** PostgreSQL schema with RLS policies, indexed search, and Realtime sync.
*   **Offline-First:** Local caching of user session and profiles using Room and DataStore.

## High-Level Technical Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material Design 3)
*   **Architecture:** Clean Architecture (MVVM)
*   **Backend:** Supabase Auth & PostgreSQL
*   **Persistence:** Room & DataStore
*   **Networking:** Supabase Kotlin SDK, Ktor (used by Supabase SDK)

## Supabase Schema Plan
- `profiles`: user_id (UUID), username (unique), full_name, email, phone, avatar_url, created_at, last_seen.
- `conversations`: id, participant_1, participant_2, last_message, last_message_time.
- `messages`: id, conversation_id, sender_id, content, type, timestamp, status.

## Security (RLS)
- Users can only read/edit their own private profile data.
- User search respects privacy (public profile access).
- Users can only access conversations they are participants in.
- Users can only read messages in their authorized conversations.

## Implementation Steps
**Total Duration:** 8h 29m 25s

### Task_1_Foundation_Infrastructure: Initialize the core data layer and networking infrastructure. This includes setting up the Room database for messages, contacts, and groups, DataStore for user preferences, and implementing the LAN peer discovery mechanism using Network Service Discovery (NSD) or UDP broadcasts to enable offline communication.
- **Status:** COMPLETED
- **Updates:** Implemented Room database (users, chats, messages, chat_members), DataStore for user preferences, and LAN discovery manager using NSD. Project builds successfully. Architecture follows clean architecture principles to be cross-platform ready.
- **Acceptance Criteria:**
  - Room database schema is correctly implemented for offline-first messaging
  - LAN peer discovery logic successfully identifies other devices on the same network
  - DataStore handles basic user settings
  - Project builds successfully
- **Duration:** 1h 33m 42s

### Task_2_Adaptive_Messaging_UI: Develop the core messaging UI using Jetpack Compose and Material 3. Implement the Chat List and Chat Detail screens for 1-on-1 messaging with real-time updates from Room via Flow. Use Navigation 3 for state-driven navigation and the Compose Adaptive library for responsive layouts across different form factors.
- **Status:** COMPLETED
- **Updates:** Implemented adaptive Chat List and Chat Detail screens using ListDetailPaneScaffold. Integrated Navigation 3 for state-driven navigation. UI is real-time via Flow from Room. Follows M3 and provided design images. Works on mobile and tablet.
- **Acceptance Criteria:**
  - Chat List and Chat Detail screens are functional with 1-on-1 messaging
  - Navigation 3 is correctly integrated for screen transitions
  - The UI follows Material Design 3 and matches designs in /home/m-abidi/Downloads/91e84bb2-1cc3-45da-adfb-6f0ae53973f4.jpeg and /home/m-abidi/Downloads/3fdf9c5b-565f-4f1f-a7d3-b40248f267e6.jpeg
  - UI is responsive and works on mobile and tablet form factors
- **Duration:** 1h 3m 5s

### Task_3_Advanced_Features_Sync: Extend messaging capabilities to support group chats and media sharing. Integrate CameraX for image capture and Coil for image loading. Implement the incremental synchronization logic to handle message persistence and consistency between local storage and LAN/remote peers.
- **Status:** COMPLETED
- **Updates:** Implemented group chat functionality, integrated CameraX for photo capture and Coil for image loading. Developed IncrementalSyncManager for time-based synchronization and merging of local/LAN messages. Media messaging UI and persistence are functional.
- **Acceptance Criteria:**
  - Group chat creation and messaging are functional
  - CameraX integration allows capturing and sending photos
  - Coil efficiently loads and displays media
  - Incremental sync logic ensures messages are consistent across sessions and peer connections
- **Duration:** 1h 3m 13s

### Task_4_Final_Polish_Verify: Refine the app's visual identity and perform final stability checks. Apply a vibrant Material 3 color scheme with light/dark support and full Edge-to-Edge display. Create an adaptive app icon matching the app's function. Conduct a final verification to ensure all requirements are met.
- **Status:** COMPLETED
- **Updates:** The measurement crash was resolved by replacing ListItem with custom Row-based layouts and flattening the UI hierarchy. Final verification by the critic agent confirmed that the app is stable, functional, and aligns with the Material 3 design and offline-first requirements. The app works correctly on mobile and is architecturally ready for tablet layouts. All core features (1-on-1/Group messaging, LAN discovery, Media sharing) are implemented and verified.
- **Acceptance Criteria:**
  - Vibrant Material 3 theme and dynamic color support are active
  - Full Edge-to-Edge display is implemented correctly
  - Adaptive app icon is present and functional
  - Build passes and app does not crash
  - Final run and verify by critic_agent confirms alignment with user requirements and UI designs
- **Duration:** 3h 35m 52s

### Task_5_Supabase_Auth_Integration: Integrate Supabase for authentication and profile management. Configure the backend schema (profiles, conversations, messages) with RLS policies and implement repositories for multi-identifier login (email, username, phone). Set up local caching for profiles and sessions using Room and DataStore to maintain the offline-first architecture.
- **Status:** COMPLETED
- **Updates:** Integrated Supabase Kotlin SDK. Implemented AuthRepository with multi-identifier support (email, phone, username). Configured Supabase schema with SQL script (profiles, chats, messages) and RLS policies. Set up local caching for sessions via DataStore and profiles via Room. Implemented Auth UI (Login/Signup) following M3.
- **Acceptance Criteria:**
  - Supabase schema and RLS policies are correctly configured
  - Auth repository supports multi-identifier login and signup
  - User sessions and profile data are cached locally
  - Project builds successfully
- **Duration:** 1h 13m 33s

### Task_6_Discovery_UI_Verification: Develop the Authentication and User Discovery UI using Jetpack Compose and Material 3. Build Login/Signup screens and a searchable user directory with debounced search functionality. Implement chat initialization logic that deduplicates conversations and integrate it with the existing messaging system. Instruct critic_agent to verify the full flow for stability and requirement alignment.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Login and Signup screens are functional and secure
  - User discovery search is debounced and returns indexed results
  - Chat initialization correctly deduplicates conversations
  - The implemented UI must match the design provided in /home/m-abidi/Downloads/91e84bb2-1cc3-45da-adfb-6f0ae53973f4.jpeg and /home/m-abidi/Downloads/3fdf9c5b-565f-4f1f-a7d3-b40248f267e6.jpeg
  - Build passes, all existing tests pass, and app does not crash
  - Final run and verify by critic_agent confirms alignment with user requirements and UI designs
- **StartTime:** 2026-07-02 20:34:22 CET

