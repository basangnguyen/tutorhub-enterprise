$ErrorActionPreference = "Stop"

$Version = "1.0.0"
$ReleaseDir = "dist\release\TutorHubSecureExam-$Version"
$InstallerDir = "dist\installer"

Write-Host "=== TutorHub Secure Exam: Final Release Packager ==="

# 1. Validate Installers exist
$FullInstaller = Join-Path $InstallerDir "TutorHubSecureExamSetup.exe"
$JlinkInstaller = Join-Path $InstallerDir "TutorHubSecureExamSetup-jlink.exe"

if (-not (Test-Path $FullInstaller)) { Write-Error "Missing $FullInstaller. Please run build_installer.ps1 first." ; exit 1 }
if (-not (Test-Path $JlinkInstaller)) { Write-Error "Missing $JlinkInstaller. Please run build_installer.ps1 -UseJlinkRuntime first." ; exit 1 }

# 2. Create Release Folder
if (Test-Path $ReleaseDir) {
    Write-Host "Removing old release directory..."
    Remove-Item -Recurse -Force $ReleaseDir
}
New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ReleaseDir "docs") | Out-Null

# 3. Copy Installers & Docs
Write-Host "Copying installers to release package..."
Copy-Item $FullInstaller -Destination $ReleaseDir -Force
Copy-Item $JlinkInstaller -Destination $ReleaseDir -Force

Write-Host "Copying documentation..."
$FilesToCopy = @(
    "docs\tse_release_validation_checklist.md",
    "docs\tse_code_signing_plan.md",
    "README_RELEASE.md",
    "RELEASE_NOTES.md"
)

# Optional plan artifact if exists
if (Test-Path "implementation_plan.md") {
    $FilesToCopy += "implementation_plan.md"
}

foreach ($File in $FilesToCopy) {
    if (Test-Path $File) {
        $Dest = if ($File -match "^docs\\") { Join-Path $ReleaseDir $File } else { Join-Path $ReleaseDir (Split-Path $File -Leaf) }
        Copy-Item $File -Destination $Dest -Force
    } else {
        Write-Warning "File not found for packaging: $File"
    }
}

# 4. Generate Checksums
Write-Host "Generating SHA-256 checksums..."
$ChecksumFile = Join-Path $ReleaseDir "checksums.sha256"

$FilesToHash = @(
    "TutorHubSecureExamSetup.exe",
    "TutorHubSecureExamSetup-jlink.exe"
)

$ChecksumContent = ""
foreach ($File in $FilesToHash) {
    $FilePath = Join-Path $ReleaseDir $File
    if (Test-Path $FilePath) {
        $Hash = (Get-FileHash $FilePath -Algorithm SHA256).Hash
        $ChecksumContent += "$Hash  $File`r`n"
    }
}

Set-Content -Path $ChecksumFile -Value $ChecksumContent -Encoding UTF8

Write-Host "`n=== Release Package Built Successfully! ==="
Write-Host "Output: $ReleaseDir"
Write-Host "Please review the checksums.sha256 and README_RELEASE.md."
