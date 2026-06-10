[CmdletBinding()]
param (
    [switch]$UseJlinkRuntime
)

$ErrorActionPreference = "Stop"

$PortableDir = "dist\TutorHubSecureExam"
$IssFile = "installer\TutorHubSecureExam.iss"
$OutputDir = "dist\installer"

Write-Host "=== TutorHub Secure Exam: Build Inno Setup Installer ==="

# 1. Build portable folder first
if ($UseJlinkRuntime) {
    Write-Host "Building portable folder with Jlink runtime..."
    powershell -ExecutionPolicy Bypass -File .\build_portable.ps1 -UseJlinkRuntime
} else {
    Write-Host "Building portable folder with full JRE..."
    powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
}
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to build portable folder!"
    exit 1
}
# 2. Check if ISCC.exe exists
$IsccPath = "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe"
if (-not (Test-Path $IsccPath)) {
    Write-Error "Inno Setup Compiler (ISCC.exe) not found at '$IsccPath'!"
    Write-Host "Please download and install Inno Setup 6 from https://jrsoftware.org/isdl.php"
    exit 1
}

# 3. Ensure output dir exists
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

# 4. Run ISCC
$OutputBaseFilename = "TutorHubSecureExamSetup"
if ($UseJlinkRuntime) {
    $OutputBaseFilename = "TutorHubSecureExamSetup-jlink"
}

Write-Host "Compiling $IssFile with Inno Setup (Output: $OutputBaseFilename.exe)..."
& $IsccPath "/F$OutputBaseFilename" $IssFile

if ($LASTEXITCODE -ne 0) {
    Write-Error "Inno Setup compilation failed with exit code $LASTEXITCODE"
    exit 1
}

Write-Host "Installer built successfully at $OutputDir\$OutputBaseFilename.exe"
