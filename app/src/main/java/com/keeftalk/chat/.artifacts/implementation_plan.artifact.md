# Fix Vault Upload Failure After Cold Restart

This plan addresses the "StandaloneCoroutine was cancelled" error occurring during Vault uploads after an app cold restart. The root cause is a race condition between authentication/security initialization and the UI lifecycle, leading to transient UI state changes that cancel the upload's coroutine scope.

## User Review Required

> [!IMPORTANT]
> The fix involves moving the actual upload orchestration to a repository-level scope. This means uploads will continue in the background even if the user navigates away from the Vault screen. The UI will observe these uploads via a new state flow in the repository.

## Proposed Changes

### 1. Security & Auth Readiness

#### [MODIFY] [SecurityManager.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/security/crypto/SecurityManager.kt)
- Make `initializeForUser` idempotent by checking if the context is already `READY` for the same user.
- Ensure concurrent calls to `getEncryptionContext` wait on the same state flow.

#### [MODIFY] [AuthRepositoryImpl.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/data/repository/AuthRepositoryImpl.kt)
- Dampen the `NotAuthenticated` session status during startup. Avoid immediate `isLogged = false` emissions if a session might be restoring.
- Add an `awaitReady()` function that suspends until both Auth and Security are fully initialized.

### 2. Upload Pipeline Robustness

#### [MODIFY] [FileUploadManager.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/util/FileUploadManager.kt)
- Properly handle `CancellationException` in the upload pipeline to ensure structured concurrency is respected while providing meaningful failure logs for non-cancellation errors.
- Ensure the `channel` writer is tied to the parent coroutine's lifecycle correctly.

#### [MODIFY] [VaultRepositoryImpl.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/data/repository/VaultRepositoryImpl.kt)
- Implement an `activeUploads` flow to track long-running uploads.
- Move the core upload logic into the repository's `scope` (SupervisorJob + IO) so it survives transient UI changes.
- Ensure `uploadFile` waits for system readiness before proceeding.

### 3. UI Synchronization

#### [MODIFY] [VaultViewModel.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/ui/vault/VaultViewModel.kt)
- Observe the `activeUploads` flow from `VaultRepository` instead of managing local `uploadJobs`.
- Delegate upload start/cancel to the repository.

## Verification Plan

### Automated Tests
- Run `VaultRepositoryImplTest.kt` (if it exists) to verify upload logic.
- Run `SecurityManagerTest.kt` to ensure idempotency and readiness waiting.

### Manual Verification
1. **Fresh Login:** Verify uploads work after initial login.
2. **Cold Restart Reproduction:**
   - Force-stop the app.
   - Launch and wait for UI.
   - Immediately upload a file.
   - Verify it no longer fails with `StandaloneCoroutine was cancelled` and eventually succeeds.
3. **Navigation during Upload:**
   - Start a large upload.
   - Navigate to Chat and back.
   - Verify upload progress is preserved and continues.
4. **Auth Bounce Simulation:**
   - Verify that transient "NotAuthenticated" states during startup do not cause the UI to flip to the Auth screen.
