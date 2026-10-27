# ESCATE NEXO — NX07A build and emulator smoke test
# Does not change HOME default, reset Git, clean files or touch physical devices.
# Usage: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\NEXO_NX07A_GATE.ps1

$ErrorActionPreference = 'Stop'

$repoDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$originExpected = 'https://github.com/Codiname01/Escate-Nexo-Launcher.git'
$branchExpected = 'feature/nexo-v0.1-spaces-selector'
$packageExpected = 'pe.escate.nexo.debug'
$sdk = 'C:\Sdk'
$jdk = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'

Write-Host "`n=== ESCATE NEXO / GATE-NX07A ===" -ForegroundColor Cyan

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
    Write-Host "[PASS] NX07A BUILD" -ForegroundColor Green

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

        & $adb -s $serial shell am start -W -n "$packageExpected/app.lawnchair.LawnchairLauncher"
        if ($LASTEXITCODE -ne 0) { throw 'ADB LAUNCH FAILED' }
        Write-Host '[PASS] Inicio solicitado en el emulador; falta prueba visual del selector.' -ForegroundColor Green
    }
    else {
        Write-Host '[INFO] Sin emulador unico: se omite instalacion.' -ForegroundColor Yellow
    }
    Write-Host "`n=== NX07A BUILD Y AUDITORIA FINALIZADOS ===" -ForegroundColor Cyan
}
finally {
    Pop-Location
}
