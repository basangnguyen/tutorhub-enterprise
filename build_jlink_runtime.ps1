$ErrorActionPreference = "Stop"

Write-Host "=== TutorHub Secure Exam: Build jlink Mini Runtime Lab ==="

$JlinkOutputDir = "dist\runtime-jlink"
$LabDir = "dist\TutorHubSecureExam_jlink_lab"
$SourcePortable = "dist\TutorHubSecureExam"

if (-not (Test-Path $SourcePortable)) {
    Write-Error "Portable folder '$SourcePortable' does not exist! Run build_portable.ps1 first."
    exit 1
}

# 1. Prepare Jlink runtime
if (Test-Path $JlinkOutputDir) {
    Write-Host "Removing old jlink output directory..."
    Remove-Item -Recurse -Force $JlinkOutputDir
}

# The modules required by our application (Swing, JCEF, Net, DB, DPAPI/JNA, Logging)
# jdk.charsets is highly recommended for DB connectivity and Windows default encodings.
$RequiredModules = "java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.sql,java.xml,jdk.crypto.ec,jdk.unsupported,jdk.charsets"

Write-Host "Running jlink to create mini runtime with modules: $RequiredModules"
jlink --no-header-files --no-man-pages --compress=2 --add-modules $RequiredModules --output $JlinkOutputDir

if (-not (Test-Path "$JlinkOutputDir\bin\java.exe")) {
    Write-Error "jlink failed to create java.exe!"
    exit 1
}
Write-Host " [OK] jlink runtime created at: $JlinkOutputDir"

# 2. Prepare Lab directory
if (Test-Path $LabDir) {
    Write-Host "Removing old Lab directory..."
    Remove-Item -Recurse -Force $LabDir
}
New-Item -ItemType Directory -Force -Path $LabDir | Out-Null

Write-Host "Copying App, Configs, and run.bat to Lab..."
Copy-Item -Recurse -Force "$SourcePortable\app" "$LabDir\"
Copy-Item -Force "$SourcePortable\run.bat" "$LabDir\"

if (Test-Path "$SourcePortable\logs") {
    Copy-Item -Recurse -Force "$SourcePortable\logs" "$LabDir\"
} else {
    New-Item -ItemType Directory -Force -Path "$LabDir\logs" | Out-Null
}

if (Test-Path "$SourcePortable\temp_jcef") {
    Copy-Item -Recurse -Force "$SourcePortable\temp_jcef" "$LabDir\"
} else {
    New-Item -ItemType Directory -Force -Path "$LabDir\temp_jcef" | Out-Null
}

Write-Host "Copying jlink runtime into Lab..."
Copy-Item -Recurse -Force $JlinkOutputDir "$LabDir\runtime"

Write-Host "`n=== Jlink Lab Built Successfully! ==="
Write-Host "Lab Directory: $LabDir"
Write-Host "Please CD into $LabDir and run run.bat to test the minimized runtime."
