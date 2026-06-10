# Create VBScript
$vbsContent = @"
Set WshShell = CreateObject("WScript.Shell")
WshShell.CurrentDirectory = "d:\Ban_sao_du_an"
WshShell.Run "cmd /c """"C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"""" exec:java -Dexec.mainClass=""""com.mycompany.tutorhub_enterprise.client.LoginFrame""""", 0
Set WshShell = Nothing
"@
Set-Content -Path "d:\Ban_sao_du_an\TutorHub.vbs" -Value $vbsContent

# Convert PNG to ICO
Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile("d:\Ban_sao_du_an\src\main\resources\images\logomoi.png")
$iconHandle = $bmp.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($iconHandle)
$fs = New-Object System.IO.FileStream("d:\Ban_sao_du_an\TutorHub.ico", [System.IO.FileMode]::Create)
$icon.Save($fs)
$fs.Close()
$bmp.Dispose()

# Create Shortcut
$WshShell = New-Object -ComObject WScript.Shell
$DesktopPath = $WshShell.SpecialFolders.Item("Desktop")
$Shortcut = $WshShell.CreateShortcut("$DesktopPath\TutorHub Enterprise.lnk")
$Shortcut.TargetPath = "wscript.exe"
$Shortcut.Arguments = "`"d:\Ban_sao_du_an\TutorHub.vbs`""
$Shortcut.IconLocation = "d:\Ban_sao_du_an\TutorHub.ico"
$Shortcut.WorkingDirectory = "d:\Ban_sao_du_an"
$Shortcut.Save()

Write-Host "Shortcut created successfully!"
