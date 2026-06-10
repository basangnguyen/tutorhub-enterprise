Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile("d:\Ban_sao_du_an\src\main\resources\images\logomoi.png")
$dest = New-Object System.Drawing.Bitmap(256, 256)
$g = [System.Drawing.Graphics]::FromImage($dest)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.DrawImage($src, 0, 0, 256, 256)
$g.Dispose()

$ms = New-Object System.IO.MemoryStream
$dest.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
$pngBytes = $ms.ToArray()
$ms.Dispose()
$dest.Dispose()
$src.Dispose()

$icoFile = [System.IO.File]::OpenWrite("d:\Ban_sao_du_an\TutorHub_HQ.ico")
$writer = New-Object System.IO.BinaryWriter($icoFile)

# Header
$writer.Write([uint16]0) # Reserved
$writer.Write([uint16]1) # Type (1 = ICO)
$writer.Write([uint16]1) # Image count

# Directory entry
$writer.Write([byte]0)   # Width (0 = 256)
$writer.Write([byte]0)   # Height (0 = 256)
$writer.Write([byte]0)   # Color count
$writer.Write([byte]0)   # Reserved
$writer.Write([uint16]1) # Planes
$writer.Write([uint16]32)# BPP
$writer.Write([uint32]$pngBytes.Length) # Size
$writer.Write([uint32]22) # Offset

# Image data
$writer.Write($pngBytes)
$writer.Close()
$icoFile.Close()

# Update shortcut
$WshShell = New-Object -ComObject WScript.Shell
$DesktopPath = $WshShell.SpecialFolders.Item("Desktop")
$Shortcut = $WshShell.CreateShortcut("$DesktopPath\TutorHub Enterprise.lnk")
$Shortcut.IconLocation = "d:\Ban_sao_du_an\TutorHub_HQ.ico, 0"
$Shortcut.Save()

# Force Explorer to refresh icons
(New-Object -ComObject Shell.Application).NameSpace(0).ParseName("$DesktopPath\TutorHub Enterprise.lnk").InvokeVerb("properties")
