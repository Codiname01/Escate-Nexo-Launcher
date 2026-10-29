# ESCATE NEXO — NX08C / Native Icon Gesture Bridge

Base: NX08B `35b60b64dbbfd1af5ab038a66760d4316d72462f`.

## Evidence from emulator (user-provided)
- NX08B **build PASS** (643 Gradle tasks, all up-to-date).
- NX08B **installed PASS** on `emulator-5554`: `16.Dev.(35b60b6)`.
- NEXO panel top icon gesture works.
- Normal home and dock icons still do not respond.
- All-apps drawer never supported in NX08B.
- Existing preferences show `music|com.android.chrome/com.google.android.apps.chrome.Main` => Contacts. In **Música**, Chrome swipe should open Contacts, not Camera.

## Technical hypothesis
The Lawnchair `DirectionalGestureListener` triggers a configured action only when Android emits a `fling`, with a distance and velocity threshold. Native `BubbleTextView` touch dispatch and Launcher3 parent interception can cancel such flings. The NEXO panel uses a distance-based on-UP detector, so its gestures can work even with a slower emulator mouse drag. This is a **hypothesis** until testing confirms it.

## NX08C implementation
- `src/com/android/launcher3/BubbleTextView.java`:
  - New NEXO-specific ACTION_DOWN / MOVE / UP handler with raw-screen displacement; threshold max(38dp, 2x touch slop). Handles workspace and hotseat icons (DISPLAY_WORKSPACE), folder children (DISPLAY_FOLDER) and normal drawer icon cells (DISPLAY_ALL_APPS).
  - Only arms when an explicit active-space NEXO mapping exists for a main-profile app and no native Lawnchair SWIPE_UP gesture takes precedence.
  - Requests touch-stream ownership from parents for mapped icons, releasing on UP/CANCEL or sideways/downward movement. Nonmapped app icons and normal gestures are unchanged.
  - ACTION_UP dispatches mapped app and cancels native click/long press if a NEXO gesture was handled.
  - Logs DOWN and SWIPE_UP under `NEXO_GESTURE`.
- `NexoIconGesture.kt`: @JvmStatic bridge, resolves launcher activity, emits dispatch log with space/component/target found.
- `IconGestureListener.kt`: exposes whether an explicit Lawnchair swipe-up already exists; native mapping takes precedence.
- No new permissions, HOME takeover, AccessibilityService, database reset or app-data deletion.

## QA / Expected behavior
1. GH First checkout and SHA match, clean tree; Gradle BUILD SUCCESSFUL; APK package and version audited.
2. adb install -r to exactly one emulator; check installed `versionName`, not just built APK.
3. Space Música, Chrome -> Contacts from prior preference.
4. Chrome on HOME/dock: tap opens Chrome; a deliberate upward mouse drag over the icon opens Contacts.
5. In All Apps: same upward gesture on Chrome opens Contacts. Other icons and drawer scrolling remain normal.
6. In panel NEXO: gesture remains functional.
7. Space Trabajo with no Chrome mapping: regular Chrome click and drawer scrolling unchanged.
8. If an app has native Lawnchair SWIPE_UP mapping: it takes precedence on desktop icon.
9. Logcat must show `NEXO_GESTURE: DOWN`, `SWIPE_UP`, `dispatch...`; absence of DOWN indicates nonmatching component/profile/display or missing MotionEvent dispatch.
10. Verify long-press (popup/drag) on Chrome after mapping; if broken, BLOCK: revise parent intercept timing.
11. Verify press-hold-drag of a mapped icon, Android mouse vs touch, scroll in drawer.

## Status
Published implementation, **not yet compiled or runtime verified**. Must not install to physical Redmi or declare PASS before emulator QA.
