# Tasks: Fix PDF Sending & Enhanced Media Locking

- `[/]` Phase 1: Robust PDF Sending
    - `[ ]` Map `PDF` type to `FILE` for Supabase in `ChatRepositoryImpl.kt`
    - `[ ]` Apply filename sanitization to all upload paths
- `[/]` Phase 2: Unified Locking Logic
    - `[ ]` Update `ChatDetailScreen.kt` for 100% receiver blocking on locked media
    - `[ ]` Add `isLocked` handling to `PdfPreviewContent.kt`
- `[/]` Phase 3: Viewer Controls
    - `[ ]` Add lock/unlock toggle to `PdfViewerScreen.kt`
    - `[ ]` Add lock/unlock toggle to `MediaViewerScreen.kt`
- `[/]` Phase 4: Integration & Verification
    - `[ ]` Connect navigation in `MainActivity.kt`
    - `[ ]` Final testing with complex filenames and multi-page PDFs
