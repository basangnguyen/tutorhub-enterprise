$ErrorActionPreference = "Stop"
Write-Host "=== TutorHub Secure Exam: Installed App Validation ==="

$WshShell = New-Object -ComObject WScript.Shell

$CheckedPaths = @()
$InstallDir = $null

# List of potential standard paths
$PotentialPaths = @(
    $env:TUTORHUB_INSTALL_DIR,
    "$env:LOCALAPPDATA\TutorHubSecureExam",
    "$env:LOCALAPPDATA\Programs\TutorHubSecureExam",
    "$env:ProgramFiles\TutorHubSecureExam",
    "${env:ProgramFiles(x86)}\TutorHubSecureExam"
)

# Potential shortcuts
$PotentialShortcuts = @()
$PotentialShortcuts += Get-ChildItem -Path ([Environment]::GetFolderPath("Desktop")) -Filter "*TutorHub*.lnk" -ErrorAction SilentlyContinue
$PotentialShortcuts += Get-ChildItem -Path "$env:PUBLIC\Desktop" -Filter "*TutorHub*.lnk" -ErrorAction SilentlyContinue
$PotentialShortcuts += Get-ChildItem -Path "$env:APPDATA\Microsoft\Windows\Start Menu\Programs" -Filter "*TutorHub*.lnk" -Recurse -ErrorAction SilentlyContinue

# 1. Check standard paths
foreach ($Path in $PotentialPaths) {
    if ([string]::IsNullOrWhiteSpace($Path)) { continue }
    $CheckedPaths += $Path
    if (Test-Path $Path) {
        $InstallDir = $Path
        Write-Host " [INFO] Found install directory from standard paths."
        break
    }
}

# 2. If not found, try reading shortcuts
if ($null -eq $InstallDir) {
    foreach ($ShortcutFile in $PotentialShortcuts) {
        if ($null -eq $ShortcutFile) { continue }
        $ShortcutPath = $ShortcutFile.FullName
        $CheckedPaths += "Shortcut: $ShortcutPath"
        
        $Shortcut = $WshShell.CreateShortcut($ShortcutPath)
        $TargetPath = $Shortcut.TargetPath
        $WorkingDir = $Shortcut.WorkingDirectory

        if ([string]::IsNullOrWhiteSpace($WorkingDir) -eq $false -and (Test-Path $WorkingDir)) {
            $InstallDir = $WorkingDir
            Write-Host " [INFO] Found install directory from shortcut WorkingDirectory: $ShortcutPath"
            Write-Host " [INFO] Detected shortcut: $ShortcutPath"
            break
        } elseif ([string]::IsNullOrWhiteSpace($TargetPath) -eq $false) {
            $Dir = Split-Path $TargetPath
            if (Test-Path $Dir) {
                $InstallDir = $Dir
                Write-Host " [INFO] Found install directory from shortcut TargetPath: $ShortcutPath"
                Write-Host " [INFO] Detected shortcut: $ShortcutPath"
                break
            }
        }
    }
}

if ($null -eq $InstallDir -or -not (Test-Path $InstallDir)) {
    Write-Error "Could not find TutorHub Secure Exam install directory!"
    Write-Host "`nPaths checked:"
    $CheckedPaths | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host " [OK] Detected install directory: $InstallDir"

# 3. Check Core Files & Folders
$RunBatPath = Join-Path $InstallDir "run.bat"
$JavaExePath = Join-Path $InstallDir "runtime\bin\java.exe"
$FatJarPath = Join-Path $InstallDir "app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar"
$ConfigPath = Join-Path $InstallDir "app\application.properties"
$LogsPath = Join-Path $InstallDir "logs"
$TempJcefPath = Join-Path $InstallDir "temp_jcef"

$RequiredItems = @(
    $RunBatPath,
    $JavaExePath,
    $FatJarPath,
    $ConfigPath,
    $LogsPath,
    $TempJcefPath
)

foreach ($Item in $RequiredItems) {
    if (-not (Test-Path $Item)) {
        Write-Error "Required file/folder not found: $Item"
        exit 1
    }
    Write-Host " [OK] Found: $Item"
}

# 4. Check run.bat integrity
$RunBatContent = Get-Content $RunBatPath -Raw
$EnDash = [char]0x2013
$EmDash = [char]0x2014

if ($RunBatContent.Contains($EnDash) -or $RunBatContent.Contains($EmDash)) {
    Write-Host "[FAIL] run.bat contains Unicode dash characters." -ForegroundColor Red
    exit 1
}
if ($RunBatContent.Contains("^")) {
    Write-Host "[FAIL] run.bat contains caret (^)." -ForegroundColor Red
    exit 1
}
Write-Host " [OK] run.bat integrity is valid (No Unicode dash or caret)."

Write-Host "`nValidation Passed: The installed application structure is fully valid!"
