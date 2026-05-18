param(
    [string]$JavaHome = "C:\Program Files\Amazon Corretto\jdk21.0.4_7",
    [string]$VsDevCmd = "C:\Program Files (x86)\Microsoft Visual Studio\18\BuildTools\Common7\Tools\VsDevCmd.bat",
    [switch]$NoInstaller,
    [switch]$KeepRunningNuvio
)

$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

Write-Host "== Nuvio Windows release build ==" -ForegroundColor Cyan
Write-Host "Repo: $RepoRoot"

if (!(Test-Path ".\gradlew.bat")) {
    throw "gradlew.bat not found. Are you running this from the Nuvio repo?"
}

if (!(Test-Path $JavaHome)) {
    throw "JAVA_HOME path not found: $JavaHome"
}

if (!(Test-Path $VsDevCmd)) {
    throw "Visual Studio DevCmd not found: $VsDevCmd"
}

$env:JAVA_HOME = $JavaHome

$VersionFile = Join-Path $RepoRoot "iosApp\Configuration\Version.xcconfig"

if (!(Test-Path $VersionFile)) {
    throw "Version file not found: $VersionFile"
}

$VersionLines = Get-Content $VersionFile

$MarketingVersion = ($VersionLines | Where-Object { $_ -match '^MARKETING_VERSION=' }) -replace '^MARKETING_VERSION=', ''
$CurrentProjectVersion = ($VersionLines | Where-Object { $_ -match '^CURRENT_PROJECT_VERSION=' }) -replace '^CURRENT_PROJECT_VERSION=', ''

$MarketingVersion = $MarketingVersion.Trim()
$CurrentProjectVersion = $CurrentProjectVersion.Trim()

if ([string]::IsNullOrWhiteSpace($MarketingVersion)) {
    throw "MARKETING_VERSION not found in $VersionFile"
}

if ([string]::IsNullOrWhiteSpace($CurrentProjectVersion)) {
    throw "CURRENT_PROJECT_VERSION not found in $VersionFile"
}

$PortableZipName = "Nuvio-$MarketingVersion`_$CurrentProjectVersion-x64-portable.zip"
$ReleaseDir = Join-Path $RepoRoot "release-assets"
$PortableZipPath = Join-Path $ReleaseDir $PortableZipName

Write-Host "Version: $MarketingVersion build $CurrentProjectVersion" -ForegroundColor Cyan
Write-Host "Portable ZIP: $PortableZipName" -ForegroundColor Cyan

if (!$KeepRunningNuvio) {
    Write-Host "Stopping running Nuvio.exe processes..." -ForegroundColor Yellow
    Get-Process Nuvio -ErrorAction SilentlyContinue | Stop-Process -Force
}

New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null

Write-Host "Stopping Gradle daemon..." -ForegroundColor Yellow
.\gradlew.bat --stop

Write-Host "Building release distributable..." -ForegroundColor Green
cmd.exe /c "call `"$VsDevCmd`" -arch=x64 -host_arch=x64 && .\gradlew.bat :composeApp:createReleaseDistributable --no-configuration-cache"

if (!$NoInstaller) {
    Write-Host "Building Inno installer..." -ForegroundColor Green
    cmd.exe /c "call `"$VsDevCmd`" -arch=x64 -host_arch=x64 && .\gradlew.bat :composeApp:packageReleaseInnoExe --no-configuration-cache"
} else {
    Write-Host "Skipping Inno installer because -NoInstaller was provided." -ForegroundColor Yellow
}

$PortableDir = Join-Path $RepoRoot "composeApp\build\compose\binaries\main-release\app\Nuvio"

if (!(Test-Path $PortableDir)) {
    throw "Portable distributable folder not found: $PortableDir"
}

if (Test-Path $PortableZipPath) {
    Write-Host "Removing existing ZIP: $PortableZipPath" -ForegroundColor Yellow
    Remove-Item $PortableZipPath -Force
}

Write-Host "Creating portable ZIP..." -ForegroundColor Green

# Portable updater marker: must be next to `Nuvio.exe` inside the ZIP.
# We create it only for the portable ZIP step (after Inno packaging) to avoid
# changing the app image that Inno uses.
$PortableMarkerPath = Join-Path $PortableDir "Nuvio.portable"
# Create an actually empty marker file (no newline bytes).
New-Item -ItemType File -Path $PortableMarkerPath -Force | Out-Null
Compress-Archive -Path $PortableDir -DestinationPath $PortableZipPath -CompressionLevel Optimal

# ZIP is already created; clean marker from the app image folder.
Remove-Item $PortableMarkerPath -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "== Build outputs ==" -ForegroundColor Cyan

Write-Host ""
Write-Host "Portable ZIP:" -ForegroundColor Cyan
Get-Item $PortableZipPath | Select-Object FullName, Length, LastWriteTime | Format-List

Write-Host "Distributable EXE:" -ForegroundColor Cyan
Get-ChildItem "composeApp\build\compose\binaries\main-release\app\Nuvio" -Recurse -File -Filter "Nuvio.exe" |
    Sort-Object LastWriteTime -Descending |
    Select-Object FullName, Length, LastWriteTime -First 10 |
    Format-Table -AutoSize

if (!$NoInstaller) {
    Write-Host ""
    Write-Host "Installer Inno:" -ForegroundColor Cyan
    Get-ChildItem "composeApp\build\compose\binaries\main-release\inno" -Recurse -File -Filter "*.exe" |
        Sort-Object LastWriteTime -Descending |
        Select-Object FullName, Length, LastWriteTime -First 10 |
        Format-Table -AutoSize
}

Write-Host ""
Write-Host "Done." -ForegroundColor Green