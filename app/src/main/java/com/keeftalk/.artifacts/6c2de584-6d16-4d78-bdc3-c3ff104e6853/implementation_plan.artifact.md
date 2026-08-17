# Implementation Plan - Signup Page Simplification

The goal is to simplify the signup process by removing the password confirmation and username fields. A default username will be generated automatically, and the password will be visible by default.

## User Review Required

> [!IMPORTANT]
> - The **Username** field will be removed from the signup screen. A unique username will be generated based on the user's Full Name (e.g., `john_doe_1234`).
> - The **Confirm Password** field will be removed.
> - The **Password** field will show the password text by default to reduce entry errors since confirmation is removed.

## Proposed Changes

### UI Components

#### [MODIFY] [AuthComponents.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/ui/components/AuthComponents.kt)
- Add `initialPasswordVisible` parameter to `PremiumTextField` to allow setting the initial visibility of password fields.

### Authentication Logic

#### [MODIFY] [AuthViewModel.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/ui/auth/AuthViewModel.kt)
- Remove `_signupConfirmPassword` and related methods.
- Update `isSignupValid` and `validateSignup()` to:
    - Exclude `signupConfirmPassword` and `signupUsername` from mandatory UI checks.
    - Remove the "passwords must match" validation.
- Update `signup()` to:
    - Generate a default username if `_signupUsername` is empty.
    - The generation logic will take the `fullName`, sanitize it (lowercase, remove spaces), and append a random 4-digit suffix to ensure uniqueness.

### Signup Screen

#### [MODIFY] [AuthScreen.kt](file:///home/m-abidi/keeftalk/app/src/main/java/com/keeftalk/chat/ui/auth/AuthScreen.kt)
- Remove the `username` field and suggestions UI in `SignUpForm`.
- Remove the `confirmPassword` field in `SignUpForm`.
- Set `initialPasswordVisible = true` for the `password` field.

## Verification Plan

### Automated Tests
- Unit tests for `AuthViewModel` to verify:
    - `isSignupValid` returns true without username/confirm password if other fields are valid.
    - `signup()` correctly generates a unique username from the full name.
    - `signup()` still succeeds with the generated username.

### Manual Verification
- Deploy the app and navigate to the Sign Up tab.
- Verify that only Full Name, Email, Phone, and Password fields are visible.
- Verify that the password is visible by default.
- Complete the signup and verify that the account is created successfully (checking that a username was indeed generated and saved).
