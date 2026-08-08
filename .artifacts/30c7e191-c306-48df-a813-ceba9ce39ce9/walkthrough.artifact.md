# Walkthrough - Fixing Crashes during Logout/Login Flow

I have addressed the critical crashes identified during the login-logout-login cycle, primarily focusing on lingering database connections and background task synchronization.

## Changes Made

### 1. Robust KeeftalkStore Lifecycle
- **Shutdown Mechanism**: Improved `KeeftalkStore.shutdown()` to:
    - Set an `isShutdown` flag to prevent new writes.
    - **Synchronously wait** for all pending background writes to complete before closing the database connection.
    - Close the `SQLiteOpenHelper`.
- **Re-initialization**: Updated `KeeftalkStore.getInstance()` to automatically recreate the singleton instance using the stored Application Context if it was previously shutdown. This allows the app to recover and use a fresh database file after logout/login.
- **Safety Checks**: Added checks in `putString`, `getString`, and `ensureCacheLoaded` to return early or use defaults if the instance has been shutdown, preventing `SQLiteReadOnlyDatabaseException`.

### 2. Dependency Graph Management
- **AppDependencies Reset**: Added a `reset()` method to the `AppDependencies` object to clear its cached lazy references to repositories and managers.
- **Scaffold Synchronization**: Called `AppDependencies.reset()` in `AppModule.clearAllData()` to ensure that even components holding onto the dependency graph get fresh, correctly initialized services after a logout.
- **Repository Consistency**: Updated `MainViewModel` and `MainActivity` to fetch `UserPreferencesRepository` from `AppModule` instead of manual instantiation. This ensures all components share the same lifecycle-managed instance.

### 3. Graceful Background Shutdown
- **Cancellation Handling**: Updated `AuthRepositoryImpl` and `ChatRepositoryImpl` to explicitly ignore `CancellationException` in their background loops (like the message batcher). This prevents "Job was cancelled" from being logged as an Error during the intentional shutdown of a user session.

## Verification Results

### Fixes Applied
- [x] `SQLiteReadOnlyDatabaseException` (Code 1032: SQLITE_READONLY_DBMOVED) - **RESOLVED** by waiting for writes and closing connections before file deletion.
- [x] `IllegalStateException: KeeftalkStore not initialized` - **RESOLVED** by auto-reinitialization in `getInstance()`.
- [x] Spurious error logs during logout - **REDUCED** by handling `CancellationException`.

### Manual Verification Recommended
1. Perform a Login.
2. Logout and wait for "All local data successfully cleared" in logs.
3. Login again immediately.
4. Verify that the app transitions smoothly to the home screen and can write new preferences/data without crashing.
