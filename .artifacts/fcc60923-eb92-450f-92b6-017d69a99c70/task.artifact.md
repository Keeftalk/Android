# Tasks: Skeleton-First Startup Architecture

- [ ] **Phase 1: ViewModel Optimization**
    - [ ] Update `MainViewModel` to use Fast Path data for initial state
    - [ ] Add `isHydrated` to `MainViewModel`
    - [ ] Consolidate repository observation into Tier 3
- [ ] **Phase 2: Unified MainActivity Root**
    - [ ] Remove `Crossfade` and conditional branching in `setContent`
    - [ ] Initialize `KeeftalkTheme` with SharedPreferences data directly
- [ ] **Phase 3: Progressive UI Refactor**
    - [ ] Update `MainScaffold` to handle "Fast Mode" vs "Hydrated Mode"
    - [ ] Refactor `TopBar` and `BottomBar` for progressive enhancement
    - [ ] Ensure `ChatListFragment` is hosted in a stable container
- [ ] **Phase 4: Cleanup & Verification**
    - [ ] Remove `MainSkeleton` and other redundant skeleton components
    - [ ] Verify zero-latency theme transitions
    - [ ] Print final performance report
