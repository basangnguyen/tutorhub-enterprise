$cp = (Get-Content "d:\Ban_sao_du_an\cp.txt" -Raw).Trim()
$argList = "-cp `"target\classes;$cp`" com.mycompany.tutorhub_enterprise.client.LoginFrame"
Start-Process -FilePath "javaw" -ArgumentList $argList -WorkingDirectory "d:\Ban_sao_du_an"
