# LimeRoot Research

A polished, read-only Android diagnostics app focused on Xiaomi Redmi 9T (`lemon`, Qualcomm SM6115).

## Features
- Dark charcoal interface with lime accents and three sections: Overview, Device, Diagnostics.
- Android build identifiers, kernel version, ABI, security patch and selected boot properties.
- Readability checks for selected kernel diagnostic paths.
- Plain-text diagnostic report sharing.
- No root exploit, bootloader unlocker, flashing routine or partition writer.

## Interpretation
A restricted path is a normal Android permission boundary; it does not prove a vulnerability is present or absent. The app checks only a few conventional `su` paths and cannot prove the device is unrooted.

## Build
GitHub Actions builds a debug APK after a push to `main` or by manual dispatch. Download the `limeroot-debug-apk` artifact from the workflow run.

## Stack
Kotlin · Jetpack Compose · Material 3 · Android Gradle Plugin 8.6.1 · JDK 17
