$ErrorActionPreference='Stop'
$url='https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip'
$zipPath='d:\Ban_sao_du_an\ffmpeg.zip'
$extractPath='d:\Ban_sao_du_an\ffmpeg_ext'
$toolsPath='d:\Ban_sao_du_an\tools'

New-Item -ItemType Directory -Force -Path $toolsPath | Out-Null
Write-Host 'Downloading FFmpeg using curl...'
curl.exe -L -o $zipPath $url
Write-Host 'Extracting...'
Expand-Archive -Path $zipPath -DestinationPath $extractPath -Force
Copy-Item "$extractPath\ffmpeg-master-latest-win64-gpl\bin\ffmpeg.exe" -Destination "$toolsPath\ffmpeg.exe" -Force
Remove-Item $zipPath -Force
Remove-Item $extractPath -Recurse -Force
Write-Host 'FFmpeg installed successfully.'
