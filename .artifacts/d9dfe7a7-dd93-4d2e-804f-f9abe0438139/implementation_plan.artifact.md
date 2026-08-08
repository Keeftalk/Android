# Implementation Plan - Open Source Licenses Page

Add a dedicated "Open Source Licenses" page to the Keeftalk website to comply with the attribution requirements of the various libraries used in the application.

## User Review Required

> [!NOTE]
> - I will create a new page `legal/licenses.html` that lists all major third-party libraries and their respective licenses (Apache 2.0, MIT, BSD, etc.).
> - This page will be linked in the global footer of all website pages.
> - **Exhaustiveness**: While I will include all libraries found in the `build.gradle.kts`, I will group them by license type for better readability.

## Proposed Changes

### [NEW] [licenses.html](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/website/legal/licenses.html)
- Create a new branded page under the `legal/` directory.
- Use the established Tailwind design and hardcoded header/footer.
- Categorize libraries:
    - **Apache License 2.0**: AndroidX, Jetpack Compose, Kotlin Coroutines, Ktor, Supabase, Coil, Retrofit, OkHttp, etc.
    - **MIT License**: Microsoft Graph, MSAL, SLF4J, Compose Markdown.
    - **BSD 3-Clause**: WebRTC, SQLCipher.
    - **Google ML Kit & Play Services**: Standard Google service attributions.

### [MODIFY] All Website Pages (16+ files)
- **Global Footer Update**: Add a new link `Open Source Licenses` next to `Privacy Policy` and `Terms of Service` in the `<footer>` section.
- **Files to modify**:
    - `index.html`, `about.html`, `security.html`, `contact.html`, `404.html`, `verified.html`
    - `features/messaging.html`, `calls.html`, `email.html`, `notes.html`, `agenda.html`, `vault.html`, `wallet.html`
    - `legal/privacy.html`, `terms.html`, `privacy-full.html`, `terms-full.html`

## Verification Plan

### Manual Verification
- **Link Check**: Open each page and ensure the "Open Source Licenses" link appears in the footer and points correctly to `legal/licenses.html` (root) or `../legal/licenses.html` (sub-pages).
- **Page Design**: Verify the new licenses page renders correctly in Light and Dark modes.
- **Content Accuracy**: Ensure all major libraries from the `app/build.gradle.kts` are mentioned.
