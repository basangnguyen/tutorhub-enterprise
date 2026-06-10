@echo off
echo BAT DAU QUA TRINH DONG GOI TUTORHUB...
echo ----------------------------------------------------
echo Buoc 1: Bien dich ma nguon va gop thu vien (Maven)
call "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean package -DskipTests
if errorlevel 1 (
    echo LTI: Bien dich Maven that bai!
    pause
    exit /b
)

echo.
echo Buoc 2: Boc loi Java (JRE) va tao file .exe (JPackage)
if exist "dist_app" rmdir /s /q "dist_app"
jpackage --type app-image --name TutorHub --input target/ --main-jar TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.mycompany.tutorhub_enterprise.client.LoginFrame --icon TutorHub_HQ.ico --dest dist_app
if errorlevel 1 (
    echo LOI: JPackage that bai!
    pause
    exit /b
)

echo.
echo HOAN TAT DONG GOI!
echo Phan mem nam tai thu muc: D:\Ban_sao_du_an\dist_app\TutorHub
pause
