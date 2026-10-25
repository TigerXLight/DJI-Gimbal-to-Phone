[CmdletBinding()]
param(
    [string]$AdbPath = "",
    [string]$Serial = "",
    [string]$ApkPath = "",
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
$packageName = "com.androiddjibridge"
$activityName = "com.androiddjibridge.app.MainActivity"
$markerTag = "DJI_BRIDGE_ACCEPTANCE"

function Resolve-Adb {
    param([string]$RequestedPath)
    if ($RequestedPath) {
        if (-not (Test-Path -LiteralPath $RequestedPath)) {
            throw "ADB was not found at $RequestedPath"
        }
        return (Resolve-Path -LiteralPath $RequestedPath).Path
    }
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "ADB was not found on PATH. Pass -AdbPath to platform-tools\adb.exe."
    }
    return $command.Source
}

function Invoke-Adb {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)
    if ($script:Serial) {
        & $script:Adb @("-s", $script:Serial) @Arguments
    } else {
        & $script:Adb @Arguments
    }
    if ($LASTEXITCODE -ne 0) {
        throw "ADB command failed with exit code ${LASTEXITCODE}: adb $($Arguments -join ' ')"
    }
}

function Invoke-AdbNoThrow {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)
    if ($script:Serial) {
        [void](& $script:Adb @("-s", $script:Serial) @Arguments 2>&1)
    } else {
        [void](& $script:Adb @Arguments 2>&1)
    }
    return [int]$LASTEXITCODE
}

function Get-AdbState {
    $lines = @(& $script:Adb devices 2>$null)
    foreach ($line in $lines) {
        if ($line -match "^$([regex]::Escape($script:Serial))\s+(\S+)") {
            return $Matches[1]
        }
    }
    return "missing"
}

function Wait-ForAdbReadyAfterReboot {
    param([int]$TimeoutSeconds = 180)
    $goneDeadline = (Get-Date).AddSeconds(20)
    do {
        $state = Get-AdbState
        if ($state -ne "device") {
            break
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $goneDeadline)

    $readyDeadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $readyDeadline) {
        $state = Get-AdbState
        if ($state -eq "device") {
            Add-Content -LiteralPath $script:NotesPath -Value ("{0} | ADB_READY_AFTER_REBOOT" -f (Get-Date -Format o))
            return $true
        }
        if ($state -eq "unauthorized") {
            Write-Host "ADB sees the Xperia but it is UNAUTHORIZED. Unlock the phone and authorize USB debugging, then press ENTER." -ForegroundColor Yellow
            Add-Content -LiteralPath $script:NotesPath -Value ("{0} | ADB_UNAUTHORIZED_AFTER_REBOOT" -f (Get-Date -Format o))
            [void](Read-Answer "Press ENTER after authorizing USB debugging")
        } else {
            Write-Host ("Waiting for ADB serial {0}: {1}" -f $script:Serial, $state)
            Start-Sleep -Seconds 2
        }
    }
    Add-Content -LiteralPath $script:NotesPath -Value ("{0} | ADB_NOT_READY_AFTER_REBOOT" -f (Get-Date -Format o))
    return $false
}

function Read-Answer {
    param([Parameter(Mandatory = $true)][string]$Prompt)
    $answer = Read-Host $Prompt
    Add-Content -LiteralPath $script:NotesPath -Value ("{0} | {1}" -f (Get-Date -Format o), $Prompt)
    if ($answer) {
        Add-Content -LiteralPath $script:NotesPath -Value ("{0} | ANSWER | {1}" -f (Get-Date -Format o), $answer)
    }
    return $answer
}

function Pause-ForAction {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host ""
    Write-Host $Message -ForegroundColor Yellow
    [void](Read-Answer "Press ENTER when complete")
}

function Write-Marker {
    param([Parameter(Mandatory = $true)][string]$Text)
    Invoke-Adb @("shell", "log", "-t", $markerTag, $Text)
    Add-Content -LiteralPath $script:NotesPath -Value ("{0} | MARKER | {1}" -f (Get-Date -Format o), $Text)
}

function Record-PhysicalSample {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$Action
    )
    Write-Marker (("{0}_READY" -f $Label))
    Pause-ForAction "$Action Then press Enter so the captured log can be closed for this sample."
    Write-Marker (("{0}_DONE" -f $Label))
    Record-TestResult $Label
}

function Record-TestResult {
    param([Parameter(Mandatory = $true)][string]$Label)
    do {
        $result = (Read-Answer ("{0} result [PASS/FAIL/N/A/OBSERVATION]" -f $Label)).Trim().ToUpperInvariant()
        if (@("PASS", "FAIL", "N/A", "OBSERVATION") -contains $result) {
            Add-Content -LiteralPath $script:NotesPath -Value ("{0} | RESULT | {1} | {2}" -f (Get-Date -Format o), $Label, $result)
            return $result
        }
        Write-Host "Enter PASS, FAIL, N/A, or OBSERVATION." -ForegroundColor Yellow
    } while ($true)
}

$script:Adb = Resolve-Adb $AdbPath
$defaultApk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not $ApkPath) { $ApkPath = $defaultApk }
if (-not (Test-Path -LiteralPath $ApkPath)) {
    throw "APK was not found at $ApkPath. Build it first or pass -ApkPath."
}

$deviceLines = @(& $script:Adb devices)
$attached = @($deviceLines | Where-Object { $_ -match '^\S+\s+device\s*$' } | ForEach-Object { ($_ -split '\s+')[0] })
if ($Serial) {
    if ($attached -notcontains $Serial) {
        throw "Requested ADB serial $Serial is not currently attached."
    }
} elseif ($attached.Count -eq 1) {
    $script:Serial = $attached[0]
} elseif ($attached.Count -eq 0) {
    throw "No Android device is attached. Connect the Xperia and authorize USB debugging first."
} else {
    throw "Multiple Android devices are attached. Re-run with -Serial <device-serial>."
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$captureRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("DJI-Gimbal-Bridge-acceptance-{0}" -f $stamp)
New-Item -ItemType Directory -Path $captureRoot -Force | Out-Null
$script:NotesPath = Join-Path $captureRoot "acceptance-notes.txt"
$logPath = Join-Path $captureRoot "logcat.txt"
Add-Content -LiteralPath $script:NotesPath -Value "DJI Gimbal Pairing consumer acceptance prepared run"
Add-Content -LiteralPath $script:NotesPath -Value ("ADB serial: {0}" -f $script:Serial)
Add-Content -LiteralPath $script:NotesPath -Value ("APK: {0}" -f (Resolve-Path -LiteralPath $ApkPath).Path)

Write-Host "Device: $script:Serial"
Write-Host "Evidence folder: $captureRoot"
Write-Host "This procedure does not send shell key events or Bluetooth commands."

if (-not $SkipInstall) {
    Pause-ForAction "FIRST USE: confirm the phone is unlocked and ready for the consumer setup flow."
    Invoke-Adb @("install", "-r", (Resolve-Path -LiteralPath $ApkPath).Path)
}

Invoke-Adb @("logcat", "-c")
$logArguments = @("-s", $script:Serial, "logcat", "-v", "threadtime")
$logProcess = Start-Process -FilePath $script:Adb -ArgumentList $logArguments -WindowStyle Hidden -RedirectStandardOutput $logPath -PassThru
try {
    Invoke-Adb @("shell", "am", "start", "-n", "$packageName/$activityName")
    Pause-ForAction "TEST A: in the app, use Find DJI Gimbal, select the nearby OM8, and confirm the app shows Connected."
    Pause-ForAction "TEST A: leave the app and return; confirm the remembered device remains selected."

    Pause-ForAction "TEST B/C: leave the bridge UI, open Sony Video Pro, and confirm the bridge remains available without a Start Bridge press."
    Record-PhysicalSample "SONY_START_1" "Press the OM8 Record button once and confirm Sony Video Pro reaches REC."
    Record-PhysicalSample "SONY_STOP_1" "Press the OM8 Record button once and confirm Sony Video Pro reaches Stby."

    Pause-ForAction "TEST D: if another camera app is already configured safely, foreground it and verify its own preset becomes active; otherwise record Not Applicable."
    Record-TestResult "TEST_D"

    Pause-ForAction "TEST E: turn the OM8 off and confirm a clean disconnected/reconnecting state."
    Pause-ForAction "TEST E: turn the OM8 on and wait for automatic reconnect and ready status without pressing Start Bridge."
    Record-PhysicalSample "POWER_CYCLE_START" "Press Record once and confirm START/REC after power-cycle recovery."
    Record-PhysicalSample "POWER_CYCLE_STOP" "Press Record once and confirm STOP/Stby after power-cycle recovery."

    Pause-ForAction "TEST F: use Android system UI to turn Bluetooth OFF and confirm the app reports Bluetooth off/disconnected."
    Pause-ForAction "TEST F: turn Bluetooth ON in Android system UI and wait for remembered OM8 reconnect."
    Record-PhysicalSample "BLUETOOTH_START" "Press Record once and confirm START/REC after Bluetooth recovery."
    Record-PhysicalSample "BLUETOOTH_STOP" "Press Record once and confirm STOP/Stby after Bluetooth recovery."

    # `am force-stop` is not a representative process-recreation test on Xperia:
    # Android clears the user-enabled Accessibility service state for the package.
    # Kill the process without revoking that user permission, then relaunch the UI.
    Invoke-Adb @("shell", "am", "kill", $packageName)
    Invoke-Adb @("shell", "am", "start", "-n", "$packageName/$activityName")
    Pause-ForAction "TEST G: after app/service recreation, confirm the remembered gimbal, camera presets, mappings, and connection remain available."
    Record-PhysicalSample "PROCESS_START" "Press Record once and confirm START/REC after process recreation."
    Record-PhysicalSample "PROCESS_STOP" "Press Record once and confirm STOP/Stby after process recreation."

    Pause-ForAction "TEST H: save any notes, then type REBOOT at the next prompt only if you are ready to reboot the Xperia."
    $reboot = Read-Answer "Type REBOOT to run adb reboot, or anything else to skip"
    if ($reboot -eq "REBOOT") {
        $rebootExit = Invoke-AdbNoThrow @("reboot")
        Add-Content -LiteralPath $script:NotesPath -Value ("{0} | ADB_REBOOT_SUBMITTED | exit={1}" -f (Get-Date -Format o), $rebootExit)
        if (Wait-ForAdbReadyAfterReboot) {
            Pause-ForAction "TEST H: after Android has booted, verify selection, presets, mappings, Accessibility state, background behavior, and OM8 recovery."
            Invoke-Adb @("shell", "am", "start", "-n", "$packageName/$activityName")
            Record-PhysicalSample "REBOOT_START" "Open Sony Video Pro if needed, press Record once, and confirm START/REC after reboot."
            Record-PhysicalSample "REBOOT_STOP" "Press Record once and confirm STOP/Stby after reboot."
        } else {
            Write-Host "ADB did not recover for $script:Serial. Evidence is preserved; no am start was attempted." -ForegroundColor Yellow
            Add-Content -LiteralPath $script:NotesPath -Value ("{0} | TEST_H_ABORTED | ADB unavailable; no am start attempted" -f (Get-Date -Format o))
        }
    } else {
        Add-Content -LiteralPath $script:NotesPath -Value "Phone reboot skipped by operator."
    }

    for ($i = 1; $i -le 5; $i++) {
        Record-PhysicalSample ("REPEAT_{0}_START" -f $i) ("Press Record once for repeated pair {0} and confirm START/REC." -f $i)
        Record-PhysicalSample ("REPEAT_{0}_STOP" -f $i) ("Press Record once for repeated pair {0} and confirm STOP/Stby." -f $i)
    }

    Pause-ForAction "TEST J/K: while connected, inspect Android Bluetooth Settings, Quick Settings Bluetooth details, and the app's foreground notification. Record whether OM8 appears connected, paired, or only app-visible."
    Write-Marker "ACCEPTANCE_OBSERVATIONS_COMPLETE"
    Write-Host ""
    Write-Host "Acceptance actions complete. Preserve the evidence folder: $captureRoot" -ForegroundColor Green
    Write-Host "Timing extraction: Select-String -Path '$logPath' -Pattern 'OM8_TIMING|SONY_ACTION_CLICK|SONY_REC_VERIFIED|OM8_REC_PRESS'"
    Write-Host "Use each START/STOP sample's T0=FFF4 callback, T1=edge decode, T2=ACTION_CLICK, T3=authoritative Sony verification. Report individual samples and START/STOP min/median/max."
}
finally {
    if ($logProcess -and -not $logProcess.HasExited) {
        Stop-Process -Id $logProcess.Id -Force
    }
}
