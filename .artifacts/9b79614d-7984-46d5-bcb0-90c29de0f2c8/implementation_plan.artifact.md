# Theme-Aware Solid Background for Document Picker

The user wants the document picker to have a solid background color that matches the app's background and correctly adapts to both dark and light themes.

## Proposed Changes

### [Component] Chat UI

#### [MODIFY] [DocumentPickerBottomSheet.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/DocumentPickerBottomSheet.kt)
- Change `containerColor` to `MaterialTheme.colorScheme.surface` (solid background).
- Replace hardcoded hex colors with `MaterialTheme.colorScheme` colors:
    - `Color(0xFFF1F5F9)` (white-ish) -> `MaterialTheme.colorScheme.onSurface`
    - `Color(0xFF94A3B8)` (gray-ish) -> `MaterialTheme.colorScheme.onSurfaceVariant`
    - `Color(0xFF1E222A)` -> `MaterialTheme.colorScheme.surface`
- Update alpha-based backgrounds (e.g., `Color.White.copy(alpha = 0.06f)`) to use `MaterialTheme.colorScheme.onSurface` or `surfaceVariant` as a base.
- Ensure the `dragHandle` is theme-aware.

#### [MODIFY] [DocumentItem.kt](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/src/main/java/com/keeftalk/chat/ui/screens/chatdetail/DocumentItem.kt)
- Update item background and border colors to use `MaterialTheme.colorScheme` with appropriate alpha.
- Update text colors to `onSurface` and `onSurfaceVariant`.
- Update the selection checkbox colors to be consistent with the theme.

## Verification Plan

### Manual Verification
- Deploy the app and toggle between Dark and Light modes.
- Open the Document Picker and verify:
    - The background is a solid color matching the theme's surface/background.
    - All text is legible in both modes.
    - Icons and interactive elements (tabs, buttons) are correctly colored.
