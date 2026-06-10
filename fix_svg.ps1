$bytes = [System.IO.File]::ReadAllBytes("d:\Ban_sao_du_an\src\main\resources\images\logomoi.png")
$b64 = [System.Convert]::ToBase64String($bytes)
$svg = "<svg xmlns='http://www.w3.org/2000/svg' width='1254' height='1254' viewBox='0 0 1254 1254'><image href='data:image/png;base64,$b64' width='1254' height='1254' /></svg>"
[System.IO.File]::WriteAllText("d:\Ban_sao_du_an\src\main\resources\images\icon\logo.svg", $svg)
