# LimeRoot Research 0.4.0

Read-only Android device and security diagnostics, with a user-controlled UTF-8 TXT export. Developed with Xiaomi Redmi 9T (`lemon`) as the initial target, while using Android APIs that work across devices.

## Diagnostics
- Device model, manufacturer, Android version/API, build, security patch, kernel and supported ABIs.
- Verified Boot, bootloader lock, VBMeta and dm-verity properties when exposed by the manufacturer.
- SELinux enforcement state and current process context.
- Current app process isolation: Linux CapEff, NoNewPrivs and Seccomp from `/proc/self/status`.
- Build signing tags/type and whether the installed app build is debuggable.
- Limited checks for common `su` / Magisk paths.
- Readability probes for selected kernel diagnostic files.
- All checks run off the UI thread; refreshes are guarded against duplicate starts and failures are presented without crashing the app.

## TXT report
Use **Download TXT report** in the Overview or Diagnostics screen. Android's system document picker asks where to save the file; LimeRoot does not request broad storage access.

The UTF-8 report includes:
- Time of scan and a count of normal, informational, warning and critical observations.
- Device/build/kernel information.
- Boot, SELinux and process-isolation observations.
- Every diagnostic check with a plain-language explanation.
- Limitations and privacy notes.

The report includes model, device codename and Build fingerprint. It does not collect IMEI, serial number, phone number, location, installed-app list, personal files, passwords or keys. Review the preview in the app before saving or sharing.

## Security boundaries
- No root exploit, shell script, bootloader unlocker, flashing routine, partition writer, dynamic code loader or network client.
- No requested Android permissions. AndroidX may add one app-private signature-level permission for its internal receiver handling.
- Backup disabled and cleartext network traffic disabled.
- System-property reads use a fixed allowlist of property names; no shell is invoked.
- The app only performs read-only diagnostics and asks the user before creating a report.

This is not a root tool and not a 100% security guarantee. A missing `su` path does not prove the phone is unrooted. A restricted kernel file does not prove an exploit exists or does not exist. SELinux, Verified Boot and sandbox observations are signals, not a full firmware audit.

## Build and tests
GitHub Actions runs the report unit tests, builds an R8-optimized release APK, verifies archive integrity and APK signature, and rejects unexpected Android permissions. Download the `limeroot-0.4.0-release` artifact from the workflow run.

The CI APK is signed with the temporary Android debug signing key for test installation; it is not a Play Store production signing key. Repeated CI builds may have different signing keys, so Android may require uninstalling a prior CI test build before installing a newer one.

## Stack
Kotlin · Jetpack Compose · Material 3 · Android Gradle Plugin 8.6.1 · JDK 17
