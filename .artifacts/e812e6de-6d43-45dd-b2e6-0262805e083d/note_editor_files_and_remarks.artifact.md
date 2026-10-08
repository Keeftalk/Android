# Note Editor Files & Architecture Summary

This document lists all the files comprising the **Note Editor** module in the Keeftalk app, along with their roles and architectural remarks. You can provide this summary directly to Claude for refactoring, polishing, or adding advanced features.

---

## 1. UI Screens & ViewModels

### [NativeNoteEditorScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/NativeNoteEditorScreen.kt)
- **Role**: The main Jetpack Compose screen for editing a note. Renders the top bar, title input, block list renderer, metadata sheet, and floating action buttons.
- **Remarks / Areas to Polish**:
  - Handles block navigation and keyboard focus transitions.
  - Can be optimized for smoother scrolling with large block lists (LazyColumn performance).

### [NoteEditorViewModel.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/NoteEditorViewModel.kt)
- **Role**: ViewModel managing the note state, block mutations, undo/redo history stacks, debounced auto-saving (`scheduleSave`), image uploads, and `@mentions` queries.
- **Remarks / Areas to Polish**:
  - History stack memory management (`history` and `redoStack` lists).
  - Robustness of debounced save job cancellation on ViewModel clear.

### [NotesScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/NotesScreen.kt)
- **Role**: Overview screen displaying lists/grids of notes, search, filtering, pin/archive management, and navigation to individual notes.

### [UnifiedNotesViewModel.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/UnifiedNotesViewModel.kt)
- **Role**: ViewModel managing the aggregate collection of notes across containers and tags.

---

## 2. Editor Components & Utilities

### [NoteBlockEditor.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/editor/components/NoteBlockEditor.kt)
- **Role**: Contains block-specific editor components:
  - `TextBlockEditor` (Rich text formatting with parts)
  - `ImageBlockEditor` (Image rendering and picking)
  - `TableBlockEditor` (Table rows/columns editing)
  - `CodeBlockEditor` (Syntax/code block editing)
  - `MathBlockEditor` (Math formula editing)
  - `ChecklistBlockEditor` (Checkbox items list)

### [EditorToolbar.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/editor/components/EditorToolbar.kt)
- **Role**: Floating/bottom formatting toolbar providing tools for bold, italic, underline, strike, lists, checklist, text alignment, media inserts (image, video, table, code, math), mentions, color picker, and sharing.

### [EditorCommon.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/editor/EditorCommon.kt)
- **Role**: Common editor models and enums (`ToolbarAction`, styles, block types).

### [NoteRichTextUtils.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/editor/NoteRichTextUtils.kt)
- **Role**: Helper utilities for parsing and applying rich text formatting spans across text blocks.

### [NotesDesign.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/editor/NotesDesign.kt)
- **Role**: Design tokens and styling definitions specific to notes and blocks.

### [FixedRichTextEditor.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/screens/FixedRichTextEditor.kt)
- **Role**: Supporting component for fixed rich text input fields.

---

## 3. Data, Domain & Persistence

### [Note.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/domain/model/Note.kt)
- **Role**: Domain model representing a Note, including block serialization (JSON), title, update timestamps, pinned/archived flags, and share states.

### [EditorModel.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/domain/model/EditorModel.kt)
- **Role**: Data structures for editor elements and hierarchy.

### [NoteEntity.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/local/entities/NoteEntity.kt) & [NoteShareEntity.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/local/entities/NoteShareEntity.kt)
- **Role**: Room database entities for local persistence and sharing metadata.

### [NoteDao.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/local/dao/NoteDao.kt)
- **Role**: Room Data Access Object providing reactive `Flow` and sync queries for notes and shared users.

### [NoteRepository.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/domain/repository/NoteRepository.kt) & [NoteRepositoryImpl.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/repository/NoteRepositoryImpl.kt)
- **Role**: Repository interface and implementation handling offline-first local storage, end-to-end cryptographic encryption of sensitive metadata/content, and Supabase remote synchronization.

### [NoteDto.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/data/remote/NoteDto.kt)
- **Role**: Network DTO for Supabase backend synchronization.

---

## 4. Utilities & Settings

- **[NotesImageLoader.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/util/NotesImageLoader.kt)**: Image caching and loading helper for note attachments.
- **[NotesLogger.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/util/NotesLogger.kt)**: Dedicated logging utility for note operations.
- **[NotesUtils.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/util/NotesUtils.kt)**: General helper functions for note processing.
- **[NoteSettingsScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/settings/NoteSettingsScreen.kt)**: Settings screen configuration for notes.

---

## Common Remarks & Potential Enhancements for Claude
1. **Block Serialization & Migration**: Ensuring schema evolution when adding new block types (`NoteBlock` subclasses).
2. **Auto-save & Conflict Resolution**: Tuning the debounce delay in `NoteEditorViewModel` (`scheduleSave`) to prevent excessive writes while ensuring instantaneous persistence.
3. **Undo/Redo Stack Optimization**: Bounding history size to avoid memory growth during heavy editing sessions.
4. **Encryption Key Handling**: Ensuring notes leverage `KeyManager.getNotesKey()` securely during offline storage and remote sync.
5. **Keyboard & Focus Interoperability**: Refining smooth scrolling and focus restoration when switching between block types (e.g. Text vs. Checklist vs. Table).
