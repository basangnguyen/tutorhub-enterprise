@echo off
set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
"C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" compile exec:java -Dexec.mainClass="com.mycompany.tutorhub_enterprise.client.LoginFrame" -Dexec.classpathScope=runtime
