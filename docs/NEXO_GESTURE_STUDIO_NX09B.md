# ESCATE NEXO — NX09B Gesture Studio: QA controls (experimental)

**Parent:** NX09A (`bb7493dfa171c6eff6361e4e3ff65b00f2e6b2bd`). NX08C (`aa781b27ced097aaff9f7d55428a48b345e76569`) stays as the known working phone build. NX09B is NOT installed on the Redmi and must not overwrite it until emulator QA.

## Changes

1. **Modo ensayo (default ON):** Stroke and multi-touch recognizers display the detected gesture, but execute no action. Persistent preference `nexo_gesture_studio_v1/practice_only`. Tapping the mode control toggles between practice-only and execution.
2. **Sensibilidad de letras:** precise (squared distance error threshold 0.065), normal (0.095), flexible (0.140), persisted in `nexo_gesture_studio_v1/shape_sensitivity`. Affects only gesture letters M/T, not NX08C icon swipes or 2-finger gestures.
3. **No reconocido:** direct feedback when a stroke or 2-finger movement is below threshold, with diagnostics under Android `NEXO_STUDIO`.
4. **Restore Studio actions:** confirmation dialog removes only the five gesture action keys in `nexo_gesture_studio_v1`, then refreshes the editor. Does not clear the SharedPreferences file or touch space apps, icon gesture mapping, dock, Launcher3 workspace database, wallpaper, or system settings.
5. Reuses NX09A action editor (choose apps or spaces), canvas, and 5 gesture recognizers. Still lives *inside the Studio pad*, not globally on wallpaper.

## Release gates

- GITHUB FIRST: branch `feature/nexo-nx09b-studio-qa-controls`, check exact SHA against `git ls-remote`, clean checkout; source changes committed and published.
- BUILD: not yet measured on user's Windows checkout. Run `scripts/NEXO_NX09B_GATE.ps1` after checking out published SHA; Gradle `assembleLawnWithQuickstepGithubDebug`.
- AAPT: package `pe.escate.nexo.debug`, label ESCATE NEXO, version should end with the commit 7-char SHA.
- INSTALL: one **emulator** only. Never use this gate to install on any physical Android device.
- FUNCTIONAL: still pending, especially the one-stroke M/T recognition quality on a finger, and 2-pointer pinch/rotate reliability.

## Pixel 7 test matrix

1. Open Studio from the NEXO panel and from background long-press menu.
2. Ensure **Modo ensayo ACTIVADO** initially. Draw M. If it recognizes, the status says `Ensayo: Detectado: Dibujar M`, but the space must NOT switch.
3. Toggle to **Modo ejecución ACTIVADO**. Draw M: space should switch to Música and close the Studio dialog.
4. Test T -> Trabajo similarly, then open Studio again.
5. Change letter sensitivity Precisa / Normal / Flexible; verify recognizer logs shape scores and selected tolerance affects whether a stroke matches.
6. Draw an arbitrary squiggle to see `Gesto no reconocido`, no launch.
7. Reassign M to an app in Studio. Verify opening that app only when execution is active.
8. Restore actions; confirm M resets to Música, T to Trabajo, and icon swipe assignment Chrome -> another app remains saved in the separate `nexo_icon_gestures_v1`.
9. Two-finger pinch, spread and rotation; check the message in practice mode first and the action in execution mode second. On Pixel emulator, Ctrl + mouse may simulate multitouch depending on configuration.
10. Ensure ordinary dock icons, the app drawer, normal tap and swipe-up NX08C continue to behave normally. No launcher crash.
11. Capture `adb logcat -d -s NEXO_STUDIO:D NEXO_GESTURE:D AndroidRuntime:E *:S` if there is a mismatch.
12. Check phone screen truncation: Studio view should scroll; pad should be tall enough for letters.

## Out of scope

Global empty-desktop gesture interception, screen-edge gestures, drawing letters anywhere in the OS, guaranteed HyperOS system navigation compatibility. Those need a later dedicated QA and explicit opt-in.

## Status at publication

Source and validation script published to GitHub. **Not claimed**: compile PASS, emulator PASS, installed-phone PASS. The user's original NX08C remains the preferred phone build.
