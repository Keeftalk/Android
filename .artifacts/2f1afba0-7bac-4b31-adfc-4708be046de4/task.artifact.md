# Tasks: Fix Startup & Jitter

- [ ] **Startup Fixes**
    - [ ] Move SQLCipher loading to background in `KeeftalkApplication`
    - [ ] Restore immediate fragment transaction in `MainActivity`
    - [ ] Re-implement dual-adapter strategy in `ChatListFragment`
- [ ] **Scrolling Jitter Fixes**
    - [ ] Implement aspect-ratio based placeholders in `ChatMessageBubbles`
    - [ ] Tune `RecyclerView` parameters in `ChatDetailScreen`
- [ ] **Verification**
    - [ ] Cold start test
    - [ ] Media-heavy chat scroll test
