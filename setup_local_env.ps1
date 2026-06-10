$JavaExe = Get-ChildItem "$HOME\Tools" -Recurse -Filter java.exe | Where-Object { $_.FullName -like "*\bin\java.exe" } | Select-Object -First 1
$MvnCmd = Get-ChildItem "$HOME\Tools" -Recurse -Filter mvn.cmd | Select-Object -First 1

$env:JAVA_HOME = Split-Path (Split-Path $JavaExe.FullName -Parent) -Parent
$env:MAVEN_HOME = Split-Path (Split-Path $MvnCmd.FullName -Parent) -Parent
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "MAVEN_HOME=$env:MAVEN_HOME"

java -version
mvn -version