@echo off
title Dong goi TutorHub
echo =========================================================
echo BAT DAU DONG GOI UNG DUNG BANG JPACKAGE (JAVA NATIVE)
echo =========================================================
echo.
echo Buoc 1: Xoa thu muc cu (neu co)...
if exist "dist" rmdir /s /q "dist"
mkdir dist

echo.
echo Buoc 2: Chay jpackage de tich hop Java vao thang phan mem...
echo Qua trinh nay co the mat khoang 1-2 phut, dung tat cua so nhe!

jpackage --type app-image --name TutorHub --input target --main-jar TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar --main-class com.mycompany.tutorhub_enterprise.client.LoginFrame --dest dist

echo.
echo =========================================================
echo DONG GOI THANH CONG!
echo Sếp hay vao thu muc "dist\TutorHub" se thay file TutorHub.exe
echo =========================================================
pause
