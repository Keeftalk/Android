# Bandwidth Limiting Implementation Plan

Implement plan-based upload and download bandwidth limits for Keeftalk's unified file storage system. This will ensure that transfer speeds are governed by the user's subscription level while maintaining the integrity of encrypted, chunked, and resumable transfers.

## Proposed Changes

### [Core Components]

#### [NEW] [BandwidthPolicy.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/util/BandwidthPolicy.kt)
Create a centralized configuration for subscription-based bandwidth limits.
- Define constants for Bytes per Second (Bps) based on the requirement (1 MB = 1,048,576 bytes).
- Map `SubscriptionPlan` to upload and download limits.
- Use `null` or an `UNLIMITED` constant to represent no cap.

#### [NEW] [BandwidthLimiter.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/util/BandwidthLimiter.kt)
Implement the throttling logic using a token bucket or simple time-windowed calculation.
- `throttle(bytes: Int, limitBps: Long?)`: A suspending function that calculates if a delay is needed to stay under the limit.
- Support `null` limits for the "Unlimited" feature.
- Ensure thread safety for concurrent transfers if shared (or create per-transfer instances).

### [Unified File System]

#### [MODIFY] [FileUploadManager.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/util/FileUploadManager.kt)
Integrate the limiter into the upload stream.
1. Resolve the current `SubscriptionPlan` at the start of the upload.
2. Inside the chunk-processing loop (within the `writer` channel), call the limiter after each encrypted chunk is written.
3. Ensure the limiter uses the **ciphertext size** for accurate network-level throttling.

#### [MODIFY] [FileDownloadManager.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/util/FileDownloadManager.kt)
Integrate the limiter into the download stream.
1. Resolve the current `SubscriptionPlan` (inject `AuthRepository` if needed).
2. Inside the download loop, call the limiter after reading an encrypted chunk from the `ByteChannel`.
3. Ensure the limiter is applied *before* decryption to accurately reflect download bandwidth usage.

### [Data Layer]

#### [MODIFY] [Profile.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/domain/model/Profile.kt)
Verify that `SubscriptionPlan` enum covers all cases (Free, Plus, Pro, Family). The `BandwidthPolicy` will use these enum values.

## Verification Plan

### Automated Tests
- **Unit Tests**: Create `BandwidthLimiterTest` to verify that bytes are throttled correctly within a margin of error for all plans.
- **Integration Tests**: Mock the network layer and verify that `FileUploadManager` and `FileDownloadManager` complete transfers at the expected speeds (within a simulation environment).

### Manual Verification
- Deploy the app to a device.
- Perform a large file upload/download on a **Free** plan and verify speed (approx 2.5 MB/s upload, 5 MB/s download).
- Upgrade the user to **Plus** via the database/console and repeat the test to verify increased limits (25 MB/s upload, 50 MB/s download).
- Upgrade to **Pro** and verify that no artificial cap is imposed (transfer runs at maximum device/network capability).
- Verify that background transfers and chat media downloads are equally throttled.
- Verify that plan changes take effect for subsequent transfers.
