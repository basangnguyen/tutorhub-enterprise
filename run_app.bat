@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-24"
cd /d "d:\Ban_sao_du_an"
"C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" exec:java -Dexec.mainClass="com.mycompany.tutorhub_enterprise.client.LoginFrame"
