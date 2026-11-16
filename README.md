# LimeRoot Research

**LimeRoot Research** is an open-source, read-only Android diagnostics app initially configured for Xiaomi Redmi 9T (codename `lemon`, Qualcomm SM6115).

## Current status

This is the first diagnostic-only milestone. It does **not** obtain root, unlock the bootloader, exploit kernel vulnerabilities, modify partitions, or change system settings.

It reports:
- Android build and device identifiers
- Kernel release and architecture
- Android security patch level
- Verified Boot properties (when readable)
- Whether selected diagnostic paths are readable by this app

A permission-denied result means Android restricts access; it does not prove a vulnerability is absent.

## Build

The GitHub Actions workflow builds a debug APK on pushes and manual runs. Download the artifact named `limeroot-debug-apk` from the workflow run.

## Safety

Use only on devices you own or are authorized to test. Do not treat diagnostic output as proof that a public exploit works on a particular firmware.
