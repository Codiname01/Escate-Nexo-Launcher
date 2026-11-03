# ESCATE NEXO NX09F — Exclusive opt-in desktop draw zone

## Evidence from user's video
- NX09E GitHub First build, installed version verified: PASS on Pixel 7 emulator.
- NEXO panel displays `Dibujar en escritorio: ACTIVADO`.
- During attempted desktop handwriting, the top NEXO panel disappears and native Launcher3 page / All Apps scrolling animation is visible. The space does not change. NX09D large Studio canvas still recognizes physical M strokes (`draw_m`, `space:music`).
- This is consistent with the NX09E architecture: `LawnchairLauncher.dispatchTouchEvent` passed every event through `super.dispatchTouchEvent` **before** calling a passive observer. Touch navigation could consume / cancel the gesture.

## NX09F fix
- Replace the passive observer with an **exclusive drawing view** inside the native launcher `DragLayer`, only visible while enabled and launcher state NORMAL.
- OFF by default, persistent toggle in NEXO panel: `nexo_desktop_gestures_v1/enabled`.
- `ACTION_DOWN` beginning in a safe EMPTY central region is consumed by NEXO before native page scrolling; the drawing view requests that its parent not intercept until UP / CANCEL. If starting elsewhere, returns false and Launcher3 receives events normally.
- Validate each successive point and reject when the path leaves the safe region, or if multiple pointers appear; reject short/straight, too-fast, or too-long strokes.
- The visible stroke is drawn directly in the workspace while the pointer is down; log `NEXO_DESKTOP capture_down/capture_up` and recognized `gesture=draw_m/draw_t action=...`.
- Continue reusing the NX09D Gesture Studio classifier and action mapping; do not fork a separate algorithm.
- Skip regions with icons, folders, widgets, the NEXO panel, hotseat or system screen edges.
- When switch is disabled or launcher state is not NORMAL, drawing view is GONE and the native touch behavior is restored.

### Crucial UX tradeoff
This **cannot** both reserve an initial stroke and pass the same initial movement to native Launcher3 page navigation. While ON, normal page swipes that **start inside the safe central drawing zone** are reserved by NEXO even if the stroke fails recognition. Outside the zone, native navigation remains; setting OFF restores all native navigation. Explicitly test and report this rather than claiming full gesture coexistence.

## GitHub First release gate
- Branch `feature/nexo-nx09f-exclusive-draw-zone`, exact published SHA checked by `git ls-remote`.
- Script `scripts/NEXO_NX09F_GATE.ps1` compiles and installs **only** on one `emulator-N`; never targets Redmi, doesn't uninstall or clear data or reset Git.
- Retain NX08C on physical Redmi; no user-provided permission to install NX09F there.
- No independent Android compile PASS or emulator QA PASS is claimed until logs demonstrate them.

## QA matrix
1. With drawing OFF, normal page swipes and All Apps work in the central space.
2. Turn ON via NEXO panel; ON label explicitly says *zona central reservada*. Panel still clickable.
3. In Trabajo draw a one-stroke M inside empty center between panel and icon rows, at least 44dp away from screen edges. Confirm `capture_down`, `capture_up`, `gesture=draw_m action=space:music`, and NEXO Música.
4. In Música draw T in one continuous path. Confirm `draw_t action=space:work`, and Trabajo.
5. ON: swipe from inside center may be consumed as drawing; outside center still moves pages.
6. Open Studio canvas, simulate M/T, icon gestures NX08C, dock and drawer; check they remain functional.
7. OFF: no NX09F capture events or accidental changes. Restart launcher and confirm persisted setting.
8. Test invalid strokes and interrupted pointer: `capture_cancel`, `capture_left_safe_area`, `capture_not_letter`, no actions.
9. Check crashes, especially older unrelated `NoSuchFieldError ic_qsb_search` in All Apps, separately; NX09F does not attempt that fix.

## Diagnostics
```powershell
& 'C:\Sdk\platform-tools\adb.exe' -s emulator-5554 logcat -d -v time -s 'NEXO_DESKTOP:D' 'NEXO_STUDIO:D' 'AndroidRuntime:E' '*:S'
```

## Release status
GitHub source and gate published; static assertions performed; awaiting user's Gradle build and emulator QA.
