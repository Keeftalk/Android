# Automated Signing Configuration Plan

This plan aims to configure the Android project to automatically sign the app using your custom keystore (`/home/m-abidi/Desktop/keys.jks`). This will ensure that both debug and release builds use the same SHA-1 fingerprint, resolving the Google OAuth "Error 10".

## User Review Required

> [!IMPORTANT]
> **Keystore Password**: I need the password for `/home/m-abidi/Desktop/keys.jks` and the alias `key0` to complete the configuration. Please provide it, or you can manually update the generated `keystore.properties` file.

## Proposed Changes

### [Build Configuration]

#### [NEW] [keystore.properties](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/keystore.properties)
Create a properties file to store sensitive keystore information (passwords, alias, path) without hardcoding them in the build script.

#### [MODIFY] [build.gradle.kts](file:///home/m-abidi/AndroidStudioProjects/Keeftalk2/app/build.gradle.kts)
- Add logic to read `keystore.properties`.
- Define a `signingConfigs` block named `config`.
- Update `buildTypes` (both `debug` and `release`) to use this signing configuration.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:signingReport` to verify that both `debug` and `release` variants are now using the SHA-1: `DF:CA:3D:92:73:12:F2:FF:98:D6:72:AB:00:24:09:8D:43:84:B0:3A`.

### Manual Verification
- Build a debug APK and verify its signature.
- Verify that Gmail login works without configuration errors.
