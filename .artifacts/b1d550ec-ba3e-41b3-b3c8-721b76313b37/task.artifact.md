# Task List - Fix Stuck Login

- [x] Refactor `MainViewModel` startup logic <!-- id: 0 -->
    - [x] Remove blocking `coroutineScope` from `initialize` <!-- id: 1 -->
    - [x] Ensure all Tier 3 tasks are launched independently <!-- id: 2 -->
    - [x] Add `refreshLoginStatus()` to trigger state updates <!-- id: 3 -->
- [x] Update `MainActivity` navigation triggers <!-- id: 4 -->
    - [x] Call `refreshLoginStatus()` in `AuthScreen` success callback <!-- id: 5 -->
- [x] Improve `StartupOrchestrator` diagnostics <!-- id: 6 -->
    - [x] Add logging for task start/end and tier completion <!-- id: 7 -->
- [x] Verify fix through manual simulation or code review <!-- id: 8 -->
