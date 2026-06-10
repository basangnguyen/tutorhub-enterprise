$ErrorActionPreference = "Stop"

$DistDir = Join-Path (Get-Location) "dist"
$PortableDir = Join-Path $DistDir "TutorHubSecureExam"

Write-Host "Validating Portable Folder at: $PortableDir"

$filesToCheck = @(
    "run.bat",
    "runtime\bin\java.exe",
    "app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar",
    "app\application.properties"
)

$dirsToCheck = @(
    "logs",
    "temp_jcef"
)

foreach ($f in $filesToCheck) {
    $path = Join-Path $PortableDir $f
    if (-not (Test-Path -Path $path -PathType Leaf)) {
        Write-Error "File missing: $f"
        exit 1
    }
}

foreach ($d in $dirsToCheck) {
    $path = Join-Path $PortableDir $d
    if (-not (Test-Path -Path $path -PathType Container)) {
        Write-Error "Directory missing: $d"
        exit 1
    }
}

$RunBatPath = Join-Path $PortableDir "run.bat"
$RunBatContent = Get-Content -Path $RunBatPath -Raw

# Validation for specific strings
$mustContain = @(
    "MAIN_CLASS",
    "-cp",
    "-Dtutorhub.app.jar"
)

foreach ($str in $mustContain) {
    if (-not ($RunBatContent -match $str)) {
        Write-Error "run.bat is missing required string: $str"
        exit 1
    }
}

# Validation against invalid characters
if ($RunBatContent -match "\^") {
    Write-Error "run.bat contains caret (^)"
    exit 1
}



Write-Host "Validation Passed: The portable folder is structurally correct and run.bat is valid."

Write-Host "`nTo perform the Clean Machine Test, please follow these steps manually:"
Write-Host "1. Copy the folder `dist\TutorHubSecureExam` to another location, e.g., `D:\TSE_Portable_Test`."
Write-Host "2. Ensure you do not use IDE/NetBeans."
Write-Host "3. Run the copied `run.bat`."
Write-Host "4. Verify the Launcher opens without 'Java Usage' errors."
Write-Host "5. Verify Rust Secure Desktop, Child JCEF, and Submit functionality."
Write-Host "6. Verify cleanup and recovery as described in the requirements."

exit 0
