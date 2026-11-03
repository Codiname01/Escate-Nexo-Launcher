# ESCATE NEXO — NX10A build and emulator smoke test
# Does not change HOME default, reset Git, clean files or touch physical devices.
# Usage: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\NEXO_NX10A_GATE.ps1

$ErrorActionPreference = 'Stop'

$repoDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$originExpected = 'https://github.com/Codiname01/Escate-Nexo-Launcher.git'
$branchExpected = 'feature/nexo-nx10a-independent-desktop-layouts'
$packageExpected = 'pe.escate.nexo.debug'
$sdk = 'C:\Sdk'
$jdk = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'

Write-Host "`n=== ESCATE NEXO / GATE-NX10A ===" -ForegroundColor Cyan

if (!(Test-Path (Join-Path $repoDir '.git'))) {
    throw 'No se encontro un repositorio Git.'
}

$origin = (& git -C $repoDir remote get-url origin).Trim()
if ($LASTEXITCODE -ne 0 -or $origin -ne $originExpected) {
    throw "Origen Git incorrecto: $origin"
}

$branch = (& git -C $repoDir branch --show-current).Trim()
$head = (& git -C $repoDir rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $branch -ne $branchExpected) {
    throw "Rama incorrecta: $branch"
}

$dirty = @(& git -C $repoDir status --porcelain)
if ($LASTEXITCODE -ne 0 -or $dirty.Count -gt 0) {
    throw 'Hay cambios locales; protege tu trabajo antes de compilar.'
}

$remote = @(& git ls-remote --heads $origin "refs/heads/$branchExpected")
if ($LASTEXITCODE -ne 0 -or $remote.Count -ne 1) {
    throw 'No se pudo verificar la rama remota.'
}
$remoteSha = ($remote[0] -split '\s+')[0]
if ($remoteSha -ne $head) {
    throw "SHA local distinto de GitHub: $head / $remoteSha"
}
Write-Host "[PASS] GitHub SHA: $head" -ForegroundColor Green

foreach ($path in @(
    "$sdk\platforms\android-37.2",
    "$sdk\build-tools\37.0.0\aapt.exe",
    "$sdk\platform-tools\adb.exe",
    "$jdk\bin\java.exe",
    (Join-Path $repoDir 'gradlew.bat')
)) {
    if (!(Test-Path -LiteralPath $path)) {
        throw "Falta una herramienta: $path"
    }
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"

Push-Location $repoDir
try {
    # Gate de presencia de la nueva pantalla y accesos antes de compilar.
    $studio = Join-Path $repoDir 'lawnchair\src\app\lawnchair\nexo\NexoGestureStudio.kt'
    $popup = Join-Path $repoDir 'lawnchair\src\app\lawnchair\ui\popup\LauncherOptionsPopup.kt'
    $panel = Join-Path $repoDir 'lawnchair\src\app\lawnchair\nexo\NexoHomePanel.kt'
    if (!(Test-Path $studio) -or
        !(Select-String -LiteralPath $popup -Pattern 'nexo_studio' -Quiet) -or
        !(Select-String -LiteralPath $panel -Pattern 'NexoGestureStudio.show' -Quiet)) {
        throw 'NX10A: Gesture Studio o sus puntos de entrada no se encuentran'
    }

    foreach ($gateWord in @('practice_only', 'shape_sensitivity', 'nexo_studio_restore', 'simulateLetter', 'nexo_studio_simulate_m', 'nexo_studio_simulate_t', 'showLargeCanvas', 'touch_down x=', 'touch_move points=', 'touch_up points=', 'nexo_studio_large_canvas', 'nexo_studio_mouse_hint')) {
        $studioText = Get-Content -LiteralPath $studio -Raw
        $stringsText = Get-Content -LiteralPath (Join-Path $repoDir 'lawnchair\res\values\strings.xml') -Raw
        if (-not ($studioText.Contains($gateWord) -or $stringsText.Contains($gateWord))) {
            throw "NX10A QA feature missing: $gateWord"
        }
    }
    $desktop = Join-Path $repoDir 'lawnchair\src\app\lawnchair\nexo\NexoDesktopGesture.kt'
    $launcher = Join-Path $repoDir 'lawnchair\src\app\lawnchair\LawnchairLauncher.kt'
    if (!(Test-Path $desktop)) { throw 'NX10A Desktop Gesture missing' }
    $desktopSource = Get-Content -LiteralPath $desktop -Raw
    $launcherSource = Get-Content -LiteralPath $launcher -Raw
    $panelSource = Get-Content -LiteralPath $panel -Raw
    foreach ($signal in @(
        'getBoolean(ENABLED, false)',
        'safeBlankPoint',
        'intersectsItem',
        'override fun onTouchEvent',
        'capture_down',
        'requestDisallowInterceptTouchEvent(true)',
        'NexoGestureStudio.performDesktopStroke'
    )) {
        if (!$desktopSource.Contains($signal)) {
            throw "NX10A missing desktop safety gate: $signal"
        }
    }
    foreach ($signal in @('dragLayer.addView(', 'refreshNexoDesktopDrawingVisibility',
        'setNexoDesktopDrawingEnabled', 'NexoDesktopGesture.isEnabled')) {
        if (!$launcherSource.Contains($signal)) {
            throw "NX10A missing native dispatch hook: $signal"
        }
    }
    if (!$panelSource.Contains('launcher.setNexoDesktopDrawingEnabled')) {
        throw 'NX10A missing reversible toggle'
    }
    $nexoStore = Join-Path $repoDir 'lawnchair\src\app\lawnchair\nexo\NexoWorkspaceStore.kt'
    $nexoSurface = Join-Path $repoDir 'lawnchair\src\app\lawnchair\nexo\NexoWorkspaceSurface.kt'
    $nexoSpaces = Join-Path $repoDir 'lawnchair\src\app\lawnchair\nexo\NexoSpaces.kt'
    foreach ($path in @($nexoStore, $nexoSurface, $nexoSpaces)) {
        if (!(Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "NX10A missing file: $path"
        }
    }
    $storeText = Get-Content -LiteralPath $nexoStore -Raw
    $surfaceText = Get-Content -LiteralPath $nexoSurface -Raw
    $spacesText = Get-Content -LiteralPath $nexoSpaces -Raw
    foreach ($signal in @('nexo_workspace_layouts_v1', 'nexo_independent_workspaces_v1',
        'getBoolean(MODE_KEY, false)', 'Snapshot', 'commit()', 'kind == "folder"')) {
        if (!$storeText.Contains($signal)) {
            throw "NX10A store missing: $signal"
        }
    }
    foreach ($signal in @('fun attach()', 'fun detach()', 'fun render()',
        'private fun chooseApp(', 'private fun editFolder(', 'private fun newFolder(',
        'NexoWorkspaceStore.save')) {
        if (!$surfaceText.Contains($signal)) {
            throw "NX10A surface missing: $signal"
        }
    }
    foreach ($signal in @('NexoWorkspaceSurface(this)',
        'setNexoIndependentWorkspaceEnabled', 'refreshNexoWorkspace',
        'workspace.visibility = if (active) View.INVISIBLE else View.VISIBLE')) {
        if (!$launcherSource.Contains($signal)) {
            throw "NX10A launcher missing: $signal"
        }
    }
    foreach ($signal in @('launcher.setNexoIndependentWorkspaceEnabled',
        'nexo_workspace_mode_on', 'nexo_workspace_mode_off')) {
        if (!$panelSource.Contains($signal)) {
            throw "NX10A toggle missing: $signal"
        }
    }
    if (!$spacesText.Contains('NexoWorkspaceStore.isEnabled')) {
        throw 'NX10A spaces integration missing'
    }
    Write-Host '[PASS] NX10A INDEPENDENT DESKTOP SAFETY AUDIT' -ForegroundColor Green
    Write-Host '[PASS] NX10A DESKTOP SAFETY AUDIT' -ForegroundColor Green
    Write-Host '[PASS] NX10A STUDIO FEATURE AUDIT' -ForegroundColor Green

    & .\gradlew.bat assembleLawnWithQuickstepGithubDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'Gradle BUILD FAILED' }

    $short = $head.Substring(0, 7)
    $apkDir = Join-Path $repoDir 'build\outputs\apk\lawnWithQuickstepGithub\debug'
    $apk = Get-ChildItem -LiteralPath $apkDir -Filter '*.apk' -File |
        Where-Object { $_.Name.Contains($short) } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (!$apk) { throw "No se encontro APK correspondiente al commit $short" }

    $badging = @(& "$sdk\build-tools\37.0.0\aapt.exe" dump badging $apk.FullName)
    if ($LASTEXITCODE -ne 0) { throw 'Falló la inspeccion con aapt.' }

    $packageLine = $badging | Where-Object { $_ -like 'package:*' } | Select-Object -First 1
    $labelLine = $badging | Where-Object { $_ -like 'application-label:*' } | Select-Object -First 1
    Write-Host $packageLine
    Write-Host $labelLine
    if ($packageLine -notmatch "name='pe\.escate\.nexo\.debug'") {
        throw "Package ID inesperado: $packageLine"
    }
    if ($labelLine -notmatch 'ESCATE NEXO') {
        throw "Nombre inesperado: $labelLine"
    }

    $hash = Get-FileHash -LiteralPath $apk.FullName -Algorithm SHA256
    Write-Host "[PASS] APK: $($apk.FullName)" -ForegroundColor Green
    Write-Host "SHA256: $($hash.Hash)"
    Write-Host "[PASS] NX10A BUILD" -ForegroundColor Green

    $adb = "$sdk\platform-tools\adb.exe"
    $emulators = @(
        & $adb devices |
        Where-Object { $_ -match '^emulator-\d+\s+device(\s|$)' } |
        ForEach-Object { ($_.Trim() -split '\s+')[0] }
    )

    if ($emulators.Count -eq 1) {
        $serial = $emulators[0]
        Write-Host "Emulador: $serial"
        & $adb -s $serial install -r $apk.FullName
        if ($LASTEXITCODE -ne 0) { throw 'ADB INSTALL FAILED' }

        # Validate that Android installed this exact commit.
        $installed = @(& $adb -s $serial shell dumpsys package $packageExpected)
        if ($LASTEXITCODE -ne 0 -or -not ($installed -match ([regex]::Escape($short)))) {
            throw "El emulador no refleja la version $short"
        }
        & $adb -s $serial shell am force-stop $packageExpected
        if ($LASTEXITCODE -ne 0) { throw 'ADB FORCE-STOP FAILED' }
        & $adb -s $serial shell am start -W -n "$packageExpected/app.lawnchair.LawnchairLauncher"
        if ($LASTEXITCODE -ne 0) { throw 'ADB LAUNCH FAILED' }
        Write-Host '[PASS] Inicio solicitado en el emulador; probar M/T y gestos de dos dedos en Studio; conservar NX08C.' -ForegroundColor Green
        # Check the version actually installed, never just the APK on disk.
        $installedPackage = @(& $adb -s $serial shell pm path $packageExpected)
        if ($LASTEXITCODE -ne 0 -or !($installedPackage -match '^package:')) {
            throw 'El paquete no quedo instalado en el emulador.'
        }
        $installedInfo = @(& $adb -s $serial shell dumpsys package $packageExpected)
        if ($LASTEXITCODE -ne 0 -or !($installedInfo -match ([regex]::Escape($short)))) {
            throw "No se confirmo el commit instalado ($short)"
        }
        Write-Host '[PASS] NX10A INSTALADO CON VERSION CORRECTA' -ForegroundColor Green
        Write-Host '[QA NX10A] NX10A OFF: el escritorio original conserva iconos, carpetas, widgets, dock.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Pulsar Escritorios propios: DESACTIVADOS para entrar en modo propio.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Trabajo: agregar dos apps, crear carpeta, editar contenido y reordenar.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Musica: agregar una app DISTINTA, comprobar que Trabajo no se comparte.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Estudio y Creacion: comprobar escritorio independiente vacio.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Volver a Trabajo, comprobar persistencia despues de relanzar.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Apagar modo propio: escritorio Launcher3 original vuelve intacto.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] App drawer, dock y gestos NX08C siguen funcionando; NX09F pausado ON NX10.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Widgets propios todavia NO disponibles en NX10A, fase NX10B.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Desactivado por defecto. Comprobar que M/T sobre escritorio no cambian espacio.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Pulsar Dibujar en escritorio: DESACTIVADO para ACTIVAR.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] En Trabajo, dibuja M en zona vacia CENTRAL (sin tocar panel ni iconos): Musica.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Mientras ON, un swipe iniciado en zona central se reserva para dibujar; fuera navega.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Logs esperados: capture_down, capture_up, gesture=draw_m action=space:music.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] En Musica, dibujar T sobre fondo vacio central, en un trazo; debe activar Trabajo.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Verificar iconos, carpetas, widgets, dock, cajon, swipe normal.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Desactivar interruptor y comprobar que M/T dejan de activar espacios.' -ForegroundColor Yellow
        Write-Host '[QA NX10A] Logs: adb -s emulator-5554 logcat -d -s NEXO_DESKTOP:D NEXO_STUDIO:D AndroidRuntime:E *:S' -ForegroundColor Yellow
        Write-Host '[QA] PRUEBA REAL: Studio > Lienzo grande para mouse > clic izquierdo sostenido dentro del cuadro.' -ForegroundColor Yellow
        Write-Host '[QA] En modo ensayo: debe mostrar clic capturado, puntos y Detectado M/T; no cambiar de espacio.' -ForegroundColor Yellow
        Write-Host '[QA] En modo ejecucion: M de un solo trazo cambia Trabajo a Musica; T vuelve a Trabajo.' -ForegroundColor Yellow
        Write-Host '[QA] Si falla: adb -s emulator-5554 logcat -d -s NEXO_STUDIO:D AndroidRuntime:E *:S' -ForegroundColor Yellow
        Write-Host '[QA] SIN DIBUJAR: Studio > Simular M y Simular T. Modo ensayo no ejecuta acciones.' -ForegroundColor Yellow
        Write-Host '[QA] Luego desactiva ensayo y usa Simular M: cambia a Musica.' -ForegroundColor Yellow
        Write-Host '[QA] Modo ensayo: dibujar M/T NO debe ejecutar accion.' -ForegroundColor Yellow
        Write-Host '[QA] Cambiar a modo ejecucion: M debe cambiar a Musica.' -ForegroundColor Yellow
        Write-Host '[QA] Sensibilidad precisa/normal/flexible, reconocer M/T.' -ForegroundColor Yellow
        Write-Host '[QA] Restaurar acciones con confirmacion, verificar que los gestos NX08C siguen.' -ForegroundColor Yellow
        Write-Host '[QA] Mantener pulsado fondo > Studio; dibuja M, T (1 trazo).' -ForegroundColor Yellow
        Write-Host '[QA] Ctrl + mouse sobre lienzo para simular 2 dedos (segun emulador).' -ForegroundColor Yellow
        Write-Host '[QA] Configura cada gesto con una app y prueba persistencia.' -ForegroundColor Yellow
        Write-Host '[QA] Prueba NX08C: Chrome dock + deslizar arriba sigue funcionando.' -ForegroundColor Yellow
        Write-Host '[TEST] En Musica, arrastra arriba sobre Chrome del dock: abre Contactos.' -ForegroundColor Yellow
        Write-Host '[TEST] En el cajon vertical, haz lo mismo sobre Chrome.' -ForegroundColor Yellow
        Write-Host '[TEST] Si falla, captura: adb logcat -d -s NEXO_GESTURE:D GESTURE_HANDLER:D AndroidRuntime:E *:S' -ForegroundColor Yellow
    }
    else {
        & $adb devices -l
        throw 'NX10A requiere exactamente un emulador iniciado. La APK fue compilada y auditada, pero NO instalada.'
    }
    Write-Host "`n=== NX10A BUILD Y AUDITORIA FINALIZADOS ===" -ForegroundColor Cyan
}
finally {
    Pop-Location
}
