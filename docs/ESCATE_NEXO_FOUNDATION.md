# ESCATE NEXO — Gate NX01: foundation

**Status:** planned architecture; baseline build NOT yet validated.  
**Upstream:** [Lawnchair 16](https://github.com/LawnchairLauncher/lawnchair), branch `16-dev`.  
**Baseline commit:** `e53dcc5c4210928c09224bb678db514a190f3102` (verified in fork on 2026-10-25).  
**Working branch:** `feature/nexo-v0.1-foundation`.

## Goal

Create a distinctive, modular Android home launcher based on the working Lawnchair 16/Launcher3 engine. Keep upstream attribution and the applicable license notices. Avoid changing core launcher code before the upstream build has passed.

## Confirmed from the fork (2026-10-25)

- Default branch: `16-dev`.
- Root build file: `build.gradle` (Groovy); `settings.gradle` (Groovy).
- Build configuration declares Android compile SDK 37 with minor API level 2, Android build tools 37.0.0, minimum SDK 26 and target SDK 37.
- A Git submodule exists at `platform_frameworks_libs_systemui` with branch `16-dev`. The submodule's exact commit must be checked out as recorded by Git.
- The current project already includes Kotlin/Compose support; do not migrate or replace the existing build system.
- The previous speculative NEXO setting `minSdk=29` is NOT the upstream baseline and must not be imposed without a separate decision.

## Architecture (target, not yet implemented)

1. **Launcher base:** retain Lawnchair's homescreen, app drawer, app launching, widgets and Android integration.
2. **NEXO Spaces:** logical activity-based arrangements (Trabajo, Estudio, Creación, Música). Single-page home is an explicit UX target.
3. **NEXO Command:** a constrained in-app command palette (open an app, switch a space, show app drawer, help). No unrestricted shell execution.
4. **NEXO Modules:** versioned interfaces and internal modules only for V0.1; no executable third-party extensions.
5. **NEXO Storage:** local preferences, no account or remote telemetry required by NEXO features.
6. **NEXO UI:** futuristic identity, accessible controls, and reversible settings layered over the upstream launcher.

## Proposed order / GitHub First

- **NX01A / baseline preflight:** clone the verified branch with submodules on Windows; inspect Android Studio, Gradle, JDK, and installed SDKs. No upstream code edits.
- **NX01B / baseline build:** run the correct existing Gradle build task and tests; save logs; compare build against the upstream baseline SHA.
- **NX02 / safe identity:** change app display name and branding, keeping copyright notices and changing app IDs only after manifest and provider audit.
- **NX03 / Spaces:** introduce one-screen space switching with persistent selections, preserving Android launcher functionality.
- **NX04 / Commands:** add a small allowlisted command palette.
- **NX05 / tests & APK:** publish a CI-validated debug APK and its SHA-256.

## Safety and compatibility

- Do not replace the phone's default launcher until the new APK passes smoke testing and rollback is documented.
- On Xiaomi/Redmi HyperOS, system navigation gestures may be restricted for third-party launchers; test with stock navigation options.
- Upstream's minimum SDK does not guarantee every feature runs on every device.
- Respect all licenses, notices and third-party attribution. Do not rename Lawnchair copyright or claim original authorship of upstream code.
- GitHub commits and APK checksum must be verified before issuing local installation commands.

## V0.1 acceptance criteria

- Launcher recognized as a home app; original launcher still recoverable.
- Installed apps appear and launch without duplicate/hidden app anomalies.
- One fixed home screen, vertical drawer, app search.
- Four manually selected Spaces and their preferences survive restart.
- Commands `apps`, `ayuda`, `espacio trabajo`, `espacio musica`, and app lookup work without shell privileges.
- No crashes in repeated opens and no invasive permissions introduced by NEXO.
- Baseline and modified builds are repeatable on a declared toolchain.

## Evidence requirements

Each gate must record: branch, full commit SHA, file changes, tests run, result (PASS/FAIL), build artifact checksum when applicable, and known blockers. Unverified work remains **pending**, never described as built or installed.
