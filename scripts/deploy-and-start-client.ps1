$ErrorActionPreference = 'Stop'

$gameDirectory = 'C:\Users\gyai\Documents\ProjectS-TestClient'
$modDirectory = Join-Path $gameDirectory 'mods'
$artifact = Join-Path $PSScriptRoot '..\build\libs\projects-client-0.1.0.jar'
$destination = Join-Path $modDirectory 'projects-client-0.1.0.jar'
$launcher = 'C:\XboxGames\Minecraft Launcher\Content\Minecraft.exe'

if (-not (Test-Path -LiteralPath $artifact)) {
    throw "Client mod jar was not found: $artifact"
}
if (-not (Test-Path -LiteralPath $launcher)) {
    throw "Minecraft Launcher was not found: $launcher"
}

Copy-Item -LiteralPath $artifact -Destination $destination -Force
Write-Host "Deployed client mod: $destination"

$gameProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {
        $_.Name -eq 'javaw.exe' -and
        $_.CommandLine -like '*--gameDir C:\Users\gyai\Documents\ProjectS-TestClient*'
    }

foreach ($gameProcess in $gameProcesses) {
    $process = Get-Process -Id $gameProcess.ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) { continue }

    Write-Host "Requesting Minecraft to close (PID $($process.Id))..."
    $null = $process.CloseMainWindow()
    if (-not $process.WaitForExit(30000)) {
        throw 'Minecraft did not close within 30 seconds. It was left running to avoid data loss.'
    }
}

$launcherProcesses = Get-Process -Name 'Minecraft', 'MinecraftLauncher' -ErrorAction SilentlyContinue
foreach ($launcherProcess in $launcherProcesses) {
    if ($launcherProcess.HasExited) { continue }
    $null = $launcherProcess.CloseMainWindow()
    $null = $launcherProcess.WaitForExit(10000)
}

Start-Process -FilePath $launcher
Write-Host 'Restarted Minecraft Launcher. Select the ProjectS-TestClient profile.'
