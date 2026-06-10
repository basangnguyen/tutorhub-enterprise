@echo off
echo =========================================================
echo BAT DAU DONG GOI JAVA SERVER (TUTOR_SERVER) CHO ĐÁM MÂY
echo =========================================================

echo.
echo Buoc 1: Dang don dep va bien dich lai ma nguon...
call "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile

echo.
echo Buoc 2: Dong goi Server thanh file .jar duy nhat...
call "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" package -P server -DskipTests

echo.
echo =========================================================
echo DONG GOI THANH CONG!
echo Sep hay kiem tra thu muc "target" de lay file "TutorServer.jar"
echo =========================================================
pause
