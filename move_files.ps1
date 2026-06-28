$sourceDir = "d:\Ban_sao_du_an\src\main\Quizhub"
$targetBaseDir = "d:\Ban_sao_du_an\src\main\java"

Get-ChildItem -Path $sourceDir -Filter *.java | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    if ($content -match 'package\s+([a-zA-Z0-9_\.]+);') {
        $package = $matches[1]
        $packagePath = $package -replace '\.', '\'
        $destDir = Join-Path $targetBaseDir $packagePath
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Force -Path $destDir | Out-Null
        }
        $destFile = Join-Path $destDir $_.Name
        Move-Item -Path $_.FullName -Destination $destFile -Force
        Write-Host "Moved $($_.Name) to $destDir"
    } else {
        Write-Host "No package found in $($_.Name)"
    }
}
