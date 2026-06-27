[CmdletBinding()]
param (
    [switch]$UseJlinkRuntime
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

Write-Host "Building Portable Folder for TutorHub Secure Exam..."

# 1. Build Maven project
Write-Host "Running mvn clean install..."
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed."
}

# 2. Setup folders
$DistDir = Join-Path $ProjectRoot "dist"
$PortableDir = Join-Path $DistDir "TutorHubSecureExam"

if (Test-Path $PortableDir) {
    Remove-Item -Recurse -Force $PortableDir -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Force -Path (Join-Path $PortableDir "app") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PortableDir "runtime") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PortableDir "logs") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PortableDir "temp_jcef") | Out-Null

# 3. Copy Fat JAR
$FatJar = Join-Path $ProjectRoot "target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar"
if (-not (Test-Path $FatJar)) {
    Write-Error "Fat JAR not found."
}
Copy-Item $FatJar -Destination (Join-Path $PortableDir "app\") -Force

# 4. Copy JRE/JDK
if ($UseJlinkRuntime) {
    Write-Host "Using jlink mini runtime..."
    $JlinkOutputDir = Join-Path $ProjectRoot "dist\runtime-jlink"
    if (-not (Test-Path $JlinkOutputDir)) {
        Write-Host "jlink runtime not found. Building it now..."
        powershell -ExecutionPolicy Bypass -File (Join-Path $ProjectRoot "build_jlink_runtime.ps1")
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to build jlink runtime."
            exit 1
        }
    }
    Copy-Item -Recurse -Force "$JlinkOutputDir\*" (Join-Path $PortableDir "runtime\")
} else {
    $OldErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $JavaHomeLine = (java -XshowSettings:properties -version 2>&1 | Select-String "java.home")
    $ErrorActionPreference = $OldErrorAction

    $JavaHome = $JavaHomeLine -replace ".*java.home = ", ""
    $JavaHome = $JavaHome.Trim()

    if (-not (Test-Path $JavaHome)) {
        Write-Error "Failed to detect valid JAVA_HOME: $JavaHome"
    }
    Write-Host "Copying Java Runtime from $JavaHome..."
    & robocopy $JavaHome (Join-Path $PortableDir "runtime") /E /MT:8 /XD src /XF *.zip
    if ($LASTEXITCODE -ge 8) {
        Write-Error "Robocopy failed to copy JRE."
    }
}

# 4b. Copy bundled JCEF binaries
$cefSource = Join-Path $env:USERPROFILE ".jcef_core_tse_binaries"
$cefDest = Join-Path $PortableDir "jcef"

if (Test-Path $cefDest) {
    Remove-Item $cefDest -Recurse -Force
}

if (Test-Path $cefSource) {
    Write-Host "Copying bundled CEF binaries from $cefSource..."
    Copy-Item -Path $cefSource -Destination $cefDest -Recurse -Force
    
    if (!(Test-Path (Join-Path $cefDest "chrome_elf.dll"))) {
        throw "CEF copy failed: chrome_elf.dll not found in $cefDest"
    }

    if (!(Test-Path (Join-Path $cefDest "locales"))) {
        throw "CEF copy failed: locales folder not found in $cefDest"
    }

    Write-Host "[BUILD] Bundled CEF binaries copied with folder structure preserved."
    Write-Host "[BUILD] CEF locales found: dist\TutorHubSecureExam\jcef\locales"
} else {
    Write-Warning "JCEF binaries not found at $cefSource! Portable build might download them at runtime."
}

# 5. Create application.properties
$AppPropertiesPath = Join-Path $PortableDir "app\application.properties"
$AppPropertiesContent = @"
server.host=localhost
server.port=7860
app.env=dev
log.level=INFO
secure.desktop.enabled=true
"@
Set-Content -Path $AppPropertiesPath -Value $AppPropertiesContent -Encoding UTF8

# 6. Create run.bat
$RunBatPath = Join-Path $PortableDir "run.bat"
$RunBatContent = @"
@echo off
set "APP_HOME=%~dp0"
if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"
pushd "%APP_HOME%"

set "JAVA_EXE=%APP_HOME%\runtime\bin\java.exe"
set "JAR_FILE=%APP_HOME%\app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar"
set "CONFIG_FILE=%APP_HOME%\app\application.properties"
set "MAIN_CLASS=com.mycompany.tutorhub_enterprise.client.exam.ui.TSEProductionParentSubmitLabLauncher"

echo Debug Info:
echo APP_HOME: %APP_HOME%
echo JAVA_EXE: %JAVA_EXE%
echo JAR_FILE: %JAR_FILE%
echo CONFIG_FILE: %CONFIG_FILE%
echo MAIN_CLASS: %MAIN_CLASS%

if not exist "%JAVA_EXE%" (
    echo ERROR: JAVA_EXE not found!
    pause
    exit /b 1
)
if not exist "%JAR_FILE%" (
    echo ERROR: JAR_FILE not found!
    pause
    exit /b 1
)

echo Starting TutorHub Secure Exam Production Launcher...
"%JAVA_EXE%" -Dtutorhub.app.root="%APP_HOME%" -Dtutorhub.app.jar="%JAR_FILE%" -Dtutorhub.config="%CONFIG_FILE%" -cp "%JAR_FILE%" %MAIN_CLASS% %*

popd
pause
"@

# 6b. Create run_gui.bat
$RunGuiBatPath = Join-Path $PortableDir "run_gui.bat"
$RunGuiBatContent = @"
@echo off
set "APP_HOME=%~dp0"
if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"
pushd "%APP_HOME%"

set "JAVA_EXE=%APP_HOME%\runtime\bin\javaw.exe"
set "JAR_FILE=%APP_HOME%\app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar"
set "CONFIG_FILE=%APP_HOME%\app\application.properties"
set "MAIN_CLASS=com.mycompany.tutorhub_enterprise.client.exam.ui.TSEProductionParentSubmitLabLauncher"

if not exist "%JAVA_EXE%" (
    exit /b 1
)
if not exist "%JAR_FILE%" (
    exit /b 1
)

start "" "%JAVA_EXE%" -Dtutorhub.app.root="%APP_HOME%" -Dtutorhub.app.jar="%JAR_FILE%" -Dtutorhub.config="%CONFIG_FILE%" -cp "%JAR_FILE%" %MAIN_CLASS% %*

popd
"@
Set-Content -Path $RunGuiBatPath -Value $RunGuiBatContent -Encoding ASCII
if (-not ($RunBatContent -match "MAIN_CLASS")) {
    Write-Error "MAIN_CLASS not found in run.bat content!"
    exit 1
}
if (-not ($RunBatContent -match "-cp")) {
    Write-Error "-cp not found in run.bat content!"
    exit 1
}
if (-not ($RunBatContent -match "-Dtutorhub.app.root")) {
    Write-Error "-Dtutorhub.app.root not found in run.bat content!"
    exit 1
}
if (-not ($RunBatContent -match "CONFIG_FILE")) {
    Write-Error "CONFIG_FILE not found in run.bat content!"
    exit 1
}
if (-not ($RunBatContent -match "%APP_HOME:~0,-1%")) {
    Write-Error "%APP_HOME:~0,-1% not found in run.bat content!"
    exit 1
}
if ($RunBatContent -match "\^") {
    Write-Error "Caret (^) found in run.bat content!"
    exit 1
}


Set-Content -Path $RunBatPath -Value $RunBatContent -Encoding ASCII

# 6c. Create run_input_test.bat (debug-only Vietnamese input test panel)
$RunInputTestBatPath = Join-Path $PortableDir "run_input_test.bat"
$RunInputTestBatContent = @"
@echo off
set "APP_HOME=%~dp0"
if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"
pushd "%APP_HOME%"

set "JAVA_EXE=%APP_HOME%\runtime\bin\java.exe"
set "JAR_FILE=%APP_HOME%\app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar"
set "CONFIG_FILE=%APP_HOME%\app\application.properties"
set "MAIN_CLASS=com.mycompany.tutorhub_enterprise.client.exam.ui.TSEProductionParentSubmitLabLauncher"

echo Debug Info:
echo APP_HOME: %APP_HOME%
echo JAVA_EXE: %JAVA_EXE%
echo JAR_FILE: %JAR_FILE%
echo CONFIG_FILE: %CONFIG_FILE%
echo MAIN_CLASS: %MAIN_CLASS%
echo TSE input test panel: enabled

if not exist "%JAVA_EXE%" (
    echo ERROR: JAVA_EXE not found!
    pause
    exit /b 1
)
if not exist "%JAR_FILE%" (
    echo ERROR: JAR_FILE not found!
    pause
    exit /b 1
)

echo Starting TutorHub Secure Exam Production Launcher with input test panel...
"%JAVA_EXE%" -Dtutorhub.tse.inputTest=true -Dtutorhub.app.root="%APP_HOME%" -Dtutorhub.app.jar="%JAR_FILE%" -Dtutorhub.config="%CONFIG_FILE%" -cp "%JAR_FILE%" %MAIN_CLASS% %*

popd
pause
"@
Set-Content -Path $RunInputTestBatPath -Value $RunInputTestBatContent -Encoding ASCII

Write-Host "Build Portable Folder completed successfully: $PortableDir"
