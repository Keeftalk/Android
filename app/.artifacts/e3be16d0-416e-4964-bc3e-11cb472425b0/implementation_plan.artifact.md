# Implementation Plan: 6-Digit OTP for Forgot Password

Transition the "Forgot Password" flow from sending a reset link to a 6-digit OTP verification flow using Supabase Auth.

## User Review Required

> [!IMPORTANT]
> This change assumes that the Supabase project is configured to allow OTP verification for password recovery. If Supabase is only configured for links, the OTP verification might fail unless "Secure password change" or similar settings are adjusted in the Supabase Dashboard.

## Proposed Changes

### Domain Layer

#### [MODIFY] [AuthRepository.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/domain/repository/AuthRepository.kt)
- Add `sendPasswordResetOtp(email: String): Result<Unit>`
- Add `verifyPasswordResetOtp(email: String, otp: String): Result<Unit>`
- Keep `updatePassword(newPassword: String): Result<Unit>` as is.

### Data Layer

#### [MODIFY] [AuthRepositoryImpl.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/data/repository/AuthRepositoryImpl.kt)
- Implement `sendPasswordResetOtp` using `supabase.auth.resetPasswordForEmail(email)`. (This triggers the email, which should contain an OTP if configured).
- Implement `verifyPasswordResetOtp` using `supabase.auth.verifyOtp(type = OtpType.Email.RECOVERY, email = email, token = otp)`.

### Feature Layer (Forgot Password)

#### [MODIFY] [ForgotPasswordState.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/feature/auth/forgotpassword/state/ForgotPasswordState.kt)
- Add `VERIFY_OTP` to `ForgotPasswordStep` enum.
- Add `otp: String` to `ForgotPasswordState`.
- Add `isOtpValid: Boolean` (computed or state) to track if the entered OTP is complete.

#### [MODIFY] [ForgotPasswordViewModel.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/feature/auth/forgotpassword/viewmodel/ForgotPasswordViewModel.kt)
- Rename `sendResetLink()` to `sendOtp()`.
- Update logic to call `authRepository.sendPasswordResetOtp(email)`.
- Add `onOtpChange(otp: String)` to handle OTP input.
- Add `verifyOtp()` to call `authRepository.verifyPasswordResetOtp(email, otp)`.
- Upon successful verification, transition to the next step (which will be `CREATE_NEW_PASSWORD`).

#### [MODIFY] [ForgotPasswordScreen.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/feature/auth/forgotpassword/ui/screens/ForgotPasswordScreen.kt)
- Add `OtpInputScreen` step to the `AnimatedContent`.
- Implement `OtpInputScreen` with 6 individual digit fields or a single specialized field.
- Update `ProfileConfirmation` to call `viewModel.sendOtp()` and button text to "Send OTP".
- Add a new `CREATE_NEW_PASSWORD` step to `ForgotPasswordStep` and include `CreateNewPasswordScreen` in the `AnimatedContent` for a seamless flow.

## Verification Plan

### Automated Tests
- Update `ForgotPasswordViewModelTest.kt` to cover the new OTP sending and verification logic.
- Mock `AuthRepository` to simulate successful and failed OTP verification.

### Manual Verification
1.  Open the app and go to "Forgot Password".
2.  Search for an account.
3.  Confirm the account and click "Send OTP".
4.  Verify that an email is received (requires Supabase configuration).
5.  Enter the 6-digit OTP in the app.
6.  Verify that it transitions to the "Create New Password" screen.
7.  Complete the password reset and verify login with the new password.
