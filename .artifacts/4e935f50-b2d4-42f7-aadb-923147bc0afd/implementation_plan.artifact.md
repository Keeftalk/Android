# Implementation Plan - Fix My Docs: Offline Mermaid & Markdown TOC / Internal Links

## Goal
Fix offline Mermaid rendering and Markdown Table of Contents / internal links in the "My Docs" feature, ensuring robust offline functionality, unique heading ID generation, smooth internal navigation, error handling, and consistent rendering in preview mode.

## User Review Required

> [!IMPORTANT]
> - **Local Asset Bundling**: We will bundle `mermaid.min.js` and `marked.min.js` locally in `src/main/assets/` to ensure 100% offline functionality without CDN dependency.
> - **Unified Markdown Preview Viewer**: Markdown preview will be rendered via a robust WebView-based Markdown + Mermaid renderer supporting native TOC anchor scrolling, unique heading IDs, and internal link interception.

## Proposed Changes

### Assets & Parsers
#### [NEW] [mermaid.min.js](file:///home/m/StudioProjects/Android/app/src/main/assets/mermaid.min.js)
- Bundle Mermaid JS locally for offline diagram rendering.

#### [NEW] [marked.min.js](file:///home/m/StudioProjects/Android/app/src/main/assets/marked.min.js)
- Bundle Marked JS locally for offline Markdown parsing in preview mode.

#### [MODIFY] [DocFormatParsers.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/docs/viewer/DocFormatParsers.kt)
- Update `DocOutlineItem` to include `id: String`.
- Update `extractMarkdownOutline` to generate stable, unique heading IDs handling spaces, special characters, nested headings, and duplicate heading names.

### UI & Viewers
#### [MODIFY] [MermaidDiagramView.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/docs/viewer/MermaidDiagramView.kt)
- Update to load bundled local Mermaid script (`file:///android_asset/mermaid.min.js`) instead of CDN.
- Enhance syntax error handling and graceful error card display.

#### [MODIFY] [DocViewerScreen.kt](file:///home/m/StudioProjects/Android/app/src/main/java/tn/keeftalk/chat/ui/docs/viewer/DocViewerScreen.kt)
- Replace basic `DocMarkdownView` with a unified Markdown + Mermaid WebView view utilizing local `marked.min.js` and `mermaid.min.js`.
- Intercept internal links (`#section-name`) to scroll smoothly within the viewer without opening the external browser.
- Keep external links (`http://`, `https://`) functional with confirmation dialog (`onOpenInBrowser`).
- Update `DocOutlineSheet` to trigger smooth scrolling to the selected outline item ID in the viewer.

## Verification Plan

### Automated Tests
- Build project successfully with `gradle_build("app:assembleDebug")`.
- Verify unit tests pass.

### Manual Verification
- Test in airplane mode with long Markdown documents, nested headings, duplicate headings, and Mermaid diagrams.
- Verify Table of Contents entries and internal heading links scroll directly to the correct section.
- Verify Mermaid diagrams render correctly without internet connection.
- Verify external links open confirmation dialog while internal links do not.
