@echo off
echo ==============================================
echo KHOI DONG TUTOR SERVER
echo ==============================================
set "PORT=8888"
"C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" exec:java -Dexec.mainClass="com.mycompany.tutorhub_enterprise.server.TutorServer"
pause
