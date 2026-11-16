# LimeRoot Research 0.4.2

Read-only Android device and security diagnostics, with a custom LimeRoot emblem and user-controlled UTF-8 TXT export. Developed with Xiaomi Redmi 9T (`lemon`) as the initial target, while using Android APIs that work across devices.

## Diagnostics
- Device model, manufacturer, Android version/API, build, security patch, kernel and supported ABIs.
- Verified Boot, bootloader lock, VBMeta and dm-verity properties when exposed by the manufacturer.
- SELinux enforcement state and current process context.
- Current app process isolation: Linux CapEff, NoNewPrivs and Seccomp from `/proc/self/status`.
- Build signing tags/type and whether the installed app build is debuggable.
- Limited checks for common `su` / Magisk paths.
- Root-readiness research tab: read-only kernel configuration hints and upstream version-range comparison for CVE-2026-43499 and CVE-2026-43284.
- Readability probes for selected kernel diagnostic files.
- All checks run off the UI thread; refreshes are guarded against duplicate starts and failures are presented without crashing the app.

## Root vulnerability applicability (0.4.2)
- Adds read-only kernel-config probes for FUTEX, USER_NS, XFRM_USER, IPv4/IPv6 ESP and RXRPC when /proc/config.gz is readable.
- Reads max_user_namespaces when exposed by the device.
- Compares the reported kernel version with public upstream ranges for CVE-2026-43499 (GhostLock) and CVE-2026-43284 (DirtyFrag).
- Separates a version-range match from proof of exploitability. Xiaomi/Qualcomm may backport fixes without changing the displayed kernel version; app-visible config and namespace limits cannot prove runtime reachability.
- No exploit PoC, privilege-escalation attempt, module loader, shell payload, network lookup, bootloader operation or partition write is included.

The Redmi 9T's reported 4.19.157 kernel overlaps the upstream version ranges for both CVEs, but the actual patch state and runtime prerequisites remain unknown without vendor patch records and further device-specific evidence. A matching version is a research lead, not a promise of temporary root.

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
GitHub Actions runs the report unit tests, builds an R8-optimized release APK, verifies archive integrity and APK signature, and rejects unexpected Android permissions. Download the `limeroot-0.4.2-release` artifact from the workflow run.

The CI APK is signed with the temporary Android debug signing key for test installation; it is not a Play Store production signing key. Repeated CI builds may have different signing keys, so Android may require uninstalling a prior CI test build before installing a newer one.

## Stack
Kotlin · Jetpack Compose · Material 3 · Android Gradle Plugin 8.6.1 · JDK 17
