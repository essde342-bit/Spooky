# LimeRoot Research 0.3.0

A read-only Android diagnostics app focused on Xiaomi Redmi 9T (`lemon`, Qualcomm SM6115).

## Features
- Dark interface with Overview, Device and Diagnostics sections.
- Device/build identifiers, kernel version, ABI, security patch and selected boot properties.
- Read-only checks for selected kernel diagnostic paths and common `su` locations.
- Plain-text report sharing, started only by the user.
- Device inspection and property collection run off the UI thread.
- System properties are read in one bounded process invocation; only an allowlist of six properties is retained.

## Security posture
- No requested Android runtime/system permissions.
- No `INTERNET` permission and cleartext network traffic is disabled.
- Android app backup is disabled.
- No root exploit, shell script, bootloader unlocker, flashing routine, partition writer, dynamic code loader or file-writing feature.
- The launcher activity is the only app component declared in the manifest.
- The report includes device/build identifiers (including the build fingerprint); sharing is optional and initiated by the user.

This is a small diagnostic app, not a security product. No software can be guaranteed 100% secure. A missing `su` path does not prove the device is unrooted, and restricted kernel paths do not prove that an exploit exists or does not exist.

## Build
GitHub Actions builds an R8-optimized release APK with resource shrinking after each push to `main`. The workflow also checks APK archive integrity and rejects unexpected Android permissions. Download the `limeroot-0.3.0-optimized` artifact from the workflow run.

The CI APK is signed with the temporary Android debug signing key so that it can be installed for testing. It is not a Play Store production-signing key; later CI builds may require uninstalling the prior test build before installing a newly signed one.

## Stack
Kotlin · Jetpack Compose · Material 3 · Android Gradle Plugin 8.6.1 · JDK 17
