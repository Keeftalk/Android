# Walkthrough: New Contacts Section in Calls

I have successfully added the **Contacts** section to the Calls screen. This update includes a dynamic tab layout and advanced contact management features.

## Changes Made

### UI Enhancements
- **Dynamic Tabs**: The "All", "Missed", and "Contacts" tabs now follow a 60/20/20 width distribution. The active tab expands to 60% with a smooth animation, while the others shrink to 20%.
- **Contacts Tab**: A new tab that displays a searchable list of your contacts, now including contacts directly from your phone's address book. Each contact entry allows initiating a call directly.
- **Permission Handling**: Added a specific onboarding card to request `READ_CONTACTS` permission when accessing the Contacts tab.
- **Contacts Menu**: Added a menu in the Contacts section with options to:
    - **Import contacts**: Import contacts from a `.vcf` file.
    - **Export contacts**: Export your local contacts to a `.vcf` file.
    - **Sync**: A robust two-way synchronization logic that pushes local contacts to the cloud and pulls remote updates, ensuring your address book is consistent across devices.

### Technical Implementation
- **PhoneContactManager**: A new utility class `com.keeftalk.chat.data.local.PhoneContactManager` fetches system contacts using `ContactsContract`.
- **VcfUtils**: A new utility class `com.keeftalk.chat.util.VcfUtils` handles the parsing and generation of VCF data.
- **ViewModel Logic**: `CallListViewModel` now merges local database contacts with system phone contacts into a single reactive flow.
- **Supabase Integration**:
    - Created `ContactDto` for type-safe interaction with Supabase Postgrest.
    - Implemented a secure two-way sync in `ChatRepositoryImpl` using Row Level Security (RLS) on the backend.
- **File Interop**: Integrated Android's Storage Access Framework to allow users to select and save VCF files securely.

## Verification Results

### UI/UX
- Verified that switching tabs triggers the 60/20/20 width animation.
- Verified that the "Contacts" section correctly filters results based on the search query.

### Data Management
- Tested VCF parsing with sample data.
- Verified that exported VCF files contain valid contact information.
- Confirmed that the "Sync" action correctly triggers the repository's synchronization logic.

> [!TIP]
> You can now easily back up your contacts by exporting them to a VCF file or keeping them in sync with your Supabase account.
