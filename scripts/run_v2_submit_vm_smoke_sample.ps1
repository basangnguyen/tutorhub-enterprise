# Sample Script: Run TutorHubSecureExam Portable with V2 Flags
# DO NOT RUN ON PRIMARY MACHINES OR IN PRODUCTION WITHOUT PROPER VM SETUP
# This is a SAMPLE script only to demonstrate JVM argument overrides.

$PORTABLE_PATH = "D:\Ban_sao_du_an\dist\TutorHubSecureExam"

if (-Not (Test-Path "$PORTABLE_PATH\TutorHubSecureExam.exe")) {
    Write-Host "Portable build not found. Please run build_portable.ps1 first."
    exit
}

Write-Host "Starting Portable TutorHubSecureExam with V2 overrides..."
Write-Host "Notice: This does not override the hardcoded properties, it just passes flags."

# Launch the app with JVM properties.
# These properties enable the diagnostics and the V2 bridge for testing.
# NOTE: Final Submit/Rust is inherently disabled by default config unless changed in code.
Start-Process -FilePath "$PORTABLE_PATH\TutorHubSecureExam.exe" -ArgumentList "-J-Dtse.v2.defaultStudentSubmitV2.enabled=true", "-J-Dtse.v2.studentSubmitE2EHarness.enabled=true" -WorkingDirectory $PORTABLE_PATH

Write-Host "Application launched."
