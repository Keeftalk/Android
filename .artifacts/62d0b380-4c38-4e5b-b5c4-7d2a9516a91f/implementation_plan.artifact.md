# Update `sitemap.xml` for Multi-Language Support

The goal is to update the existing `sitemap.xml` to include all 170 localized pages (17 pages across 10 languages). We will also implement `hreflang` (xhtml:link) tags for better SEO, allowing search engines to understand the relationship between translated versions of the same page.

## User Review Required

> [!NOTE]
> The sitemap will grow significantly (from 15 to 170 URLs).
> I will add `verified.html` and `legal/licenses.html` which were missing from the original sitemap but are relevant pages.

## Proposed Changes

### [MODIFY] [sitemap.xml](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/website/sitemap.xml)

- Add all URLs for the 9 new language directories (`zh`, `hi`, `es`, `fr`, `ar`, `bn`, `pt`, `ru`, `ur`).
- For each URL, include `<xhtml:link rel="alternate" ... />` entries for all 10 language versions (including the root English version).
- Update `<lastmod>` to the current date (2026-08-09).
- Ensure consistent priorities and change frequencies (if used).

## Verification Plan

### Automated Verification
- I will verify the syntax of the generated `sitemap.xml`.
- I will check that all 10 language versions are present for a sample page (e.g., `index.html`).
- I will verify that the number of `<url>` entries matches the expected count (170).

### Manual Verification
- The user can inspect the `sitemap.xml` to ensure all localized URLs are correct.
