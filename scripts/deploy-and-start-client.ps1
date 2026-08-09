$ErrorActionPreference = 'Stop'

$gameDirectory = 'C:\Users\gyai\Documents\ProjectS-TestClient'
$modDirectory = Join-Path $gameDirectory 'mods'
$launcher = 'C:\XboxGames\Minecraft Launcher\Content\Minecraft.exe'
$artifacts = @(
    @{ Source = Join-Path $PSScriptRoot '..\client-core\build\libs\projects-client-0.1.0.jar'; Name = 'projects-client-0.1.0.jar' },
    @{ Source = Join-Path $PSScriptRoot '..\devtools\build\libs\projects-devtools-0.1.0.jar'; Name = 'projects-devtools-0.1.0.jar' }
)

if (-not (Test-Path -LiteralPath $modDirectory)) { throw "Developer test mods directory was not found: $modDirectory" }
if (-not (Test-Path -LiteralPath $launcher)) { throw "Minecraft Launcher was not found: $launcher" }
foreach ($artifact in $artifacts) { if (-not (Test-Path -LiteralPath $artifact.Source)) { throw "Required ProjectS artifact was not found: $($artifact.Source)" } }

Get-ChildItem -LiteralPath $modDirectory -File | Where-Object { $_.Name -match '^projects-(client|devtools)-[0-9].*\.jar$' } | ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }
foreach ($artifact in $artifacts) { Copy-Item -LiteralPath $artifact.Source -Destination (Join-Path $modDirectory $artifact.Name) -Force; Write-Host "Deployed ProjectS mod: $($artifact.Name)" }

$gameProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.Name -eq 'javaw.exe' -and $_.CommandLine -like '*--gameDir C:\Users\gyai\Documents\ProjectS-TestClient*' }
foreach ($gameProcess in $gameProcesses) { $process = Get-Process -Id $gameProcess.ProcessId -ErrorAction SilentlyContinue; if ($null -eq $process) { continue }; $null = $process.CloseMainWindow(); if (-not $process.WaitForExit(30000)) { throw 'Minecraft did not close within 30 seconds; it was left running.' } }
Get-Process -Name 'Minecraft', 'MinecraftLauncher' -ErrorAction SilentlyContinue | ForEach-Object { if (-not $_.HasExited) { $null = $_.CloseMainWindow(); $null = $_.WaitForExit(10000) } }
Start-Process -FilePath $launcher
Write-Host 'Restarted Minecraft Launcher. It did not start Minecraft.'
