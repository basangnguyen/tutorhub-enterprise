<#
.SYNOPSIS
    Skeleton script for signing TutorHub Secure Exam release binaries.

.DESCRIPTION
    This script is a template/skeleton for signing the Rust executable and the Inno Setup installers.
    It does not contain hardcoded passwords or certificates. It runs in Dry-Run mode by default unless -ForceSign is passed.

.PARAMETER CertStoreThumbprint
    The SHA-1 thumbprint of the certificate installed in the Windows Certificate Store.

.PARAMETER PfxFilePath
    The path to the .pfx certificate file (if not using Certificate Store).

.PARAMETER ForceSign
    Actually executes the signtool command. If omitted, it just prints the commands (Dry-Run).
#>

[CmdletBinding()]
param (
    [string]$CertStoreThumbprint = "",
    [string]$PfxFilePath = "",
    [switch]$ForceSign
)

$ErrorActionPreference = "Stop"

Write-Host "=== TutorHub Secure Exam: Code Signing Tool ==="

# 1. Locate Signtool
# Try to find signtool in standard Windows SDK locations (x64)
$SignToolPaths = @(
    "C:\Program Files (x86)\Windows Kits\10\bin\10.0.22621.0\x64\signtool.exe",
    "C:\Program Files (x86)\Windows Kits\10\bin\10.0.19041.0\x64\signtool.exe",
    "C:\Program Files (x86)\Windows Kits\8.1\bin\x64\signtool.exe"
)

$SignTool = ""
foreach ($Path in $SignToolPaths) {
    if (Test-Path $Path) {
        $SignTool = $Path
        break
    }
}

# Fallback to PATH
if ([string]::IsNullOrEmpty($SignTool)) {
    if (Get-Command "signtool.exe" -ErrorAction SilentlyContinue) {
        $SignTool = "signtool.exe"
    } else {
        Write-Error "signtool.exe not found! Please install the Windows SDK."
        exit 1
    }
}
Write-Host "[INFO] Using signtool: $SignTool"

# 2. Files to sign
$FilesToSign = @(
    "tutorhub_lockdown\target\release\tutorhub_lockdown.exe",
    "dist\installer\TutorHubSecureExamSetup.exe",
    "dist\installer\TutorHubSecureExamSetup-jlink.exe"
)

# 3. Prepare Signtool arguments
$TimestampServer = "http://timestamp.digicert.com"
$BaseArgs = "sign /tr $TimestampServer /td sha256 /fd sha256"

if ([string]::IsNullOrEmpty($CertStoreThumbprint) -and [string]::IsNullOrEmpty($PfxFilePath)) {
    Write-Warning "No Certificate Store Thumbprint or PfxFilePath provided."
    Write-Warning "Running in verification-only or dry-run mode."
}

$SignCommandArgs = ""
if (-not [string]::IsNullOrEmpty($CertStoreThumbprint)) {
    $SignCommandArgs = "$BaseArgs /sha1 $CertStoreThumbprint"
    Write-Host "[INFO] Signing Mode: Windows Certificate Store (Thumbprint: $CertStoreThumbprint)"
} elseif (-not [string]::IsNullOrEmpty($PfxFilePath)) {
    if (-not (Test-Path $PfxFilePath)) {
        Write-Error "PFX file not found at: $PfxFilePath"
    }
    
    # Read password from environment variable for security
    $PfxPassword = $env:TSE_CERT_PASSWORD
    if ([string]::IsNullOrEmpty($PfxPassword)) {
        Write-Warning "TSE_CERT_PASSWORD environment variable is empty! It may prompt for password."
        $SignCommandArgs = "$BaseArgs /f `"$PfxFilePath`""
    } else {
        $SignCommandArgs = "$BaseArgs /f `"$PfxFilePath`" /p `"$PfxPassword`""
    }
    Write-Host "[INFO] Signing Mode: PFX File ($PfxFilePath)"
}

# 4. Execute Signing
foreach ($File in $FilesToSign) {
    if (-not (Test-Path $File)) {
        Write-Warning "File not found, skipping: $File"
        continue
    }

    if ($ForceSign -and $SignCommandArgs -ne "") {
        Write-Host "Signing: $File"
        # Using Invoke-Expression to handle the constructed argument string easily
        $FullCmd = "& `"$SignTool`" $SignCommandArgs `"$File`""
        Invoke-Expression $FullCmd
        
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to sign $File"
        } else {
            Write-Host "[SUCCESS] Signed $File"
            Write-Host "Verifying signature..."
            & $SignTool verify /pa /v $File
        }
    } else {
        Write-Host "`n[DRY-RUN] Would sign: $File"
        Write-Host "[DRY-RUN] Command: signtool $SignCommandArgs `"$File`""
    }
}

Write-Host "`n=== Code Signing Execution Finished ==="
if (-not $ForceSign) {
    Write-Host "NOTE: This was a Dry-Run. Pass -ForceSign and valid parameters to execute."
}
