@echo off
setlocal

echo.
echo ==============================================================
echo TutorHub Secure Exam - V2 Child Skeleton Mode Test
echo ==============================================================

if not exist "target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo [ERROR] JAR file not found. Please build the project first.
    exit /b 1
)

:: Create a fake meta file for testing
echo {"handoffId": "test-v2-skeleton-id", "encryptedFileSha256": "fakehash123", "flow": "PAPER_START_V2"} > test_v2_meta.json

echo [INFO] Running TSEExamChildClient with V2 Debug Mode arguments...
java -cp "target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" com.mycompany.tutorhub_enterprise.client.exam.ui.TSEExamChildClient --v2-handoff-meta test_v2_meta.json --v2-handoff-enc test_v2_pkg.enc --v2-debug-only


echo [INFO] Test finished.
endlocal
