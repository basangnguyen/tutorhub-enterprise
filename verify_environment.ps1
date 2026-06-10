# verify_environment.ps1
Write-Host "Bắt đầu kiểm tra môi trường..." -ForegroundColor Cyan

$errorCount = 0

function Check-Command ($cmdName, $successMsg, $failMsg) {
    if (Get-Command $cmdName -ErrorAction SilentlyContinue) {
        Write-Host "[OK] $successMsg" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $failMsg" -ForegroundColor Red
        $global:errorCount++
    }
}

function Check-File ($filePath, $successMsg, $failMsg) {
    if (Test-Path $filePath) {
        Write-Host "[OK] $successMsg" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $failMsg" -ForegroundColor Red
        $global:errorCount++
    }
}

function Check-EnvVar ($varName, $successMsg, $failMsg) {
    if ($env:$varName) {
        Write-Host "[OK] $successMsg" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $failMsg" -ForegroundColor Red
        $global:errorCount++
    }
}

Check-Command "git" "Git đã được cài đặt." "Chưa cài đặt Git. Hãy tải và cài từ https://git-scm.com/"
Check-Command "java" "Java/JDK đã được cài đặt." "Chưa cài đặt Java. Hãy tải và cài JDK 24."
Check-EnvVar "JAVA_HOME" "Biến môi trường JAVA_HOME đã được thiết lập." "Biến môi trường JAVA_HOME chưa được thiết lập. Hãy trỏ tới thư mục cài đặt JDK 24."
Check-Command "mvn" "Maven đã được cài đặt." "Chưa cài đặt Maven. Bạn có thể dùng Maven đi kèm NetBeans hoặc cài đặt riêng từ https://maven.apache.org/"
Check-Command "cargo" "Rust (cargo) đã được cài đặt." "Chưa cài đặt Rust. Nếu bạn muốn build lại LockdownCore, hãy cài từ https://rustup.rs/"

Check-File "pom.xml" "Tìm thấy pom.xml." "Không tìm thấy pom.xml ở thư mục hiện tại."
Check-File "tutorhub_lockdown\Cargo.toml" "Tìm thấy tutorhub_lockdown/Cargo.toml." "Không tìm thấy file Cargo.toml của module Rust."
Check-File "src\main\resources\tools\TutorHub_LockdownCore.exe" "Tìm thấy TutorHub_LockdownCore.exe." "Không tìm thấy TutorHub_LockdownCore.exe. Bạn có thể cần build lại từ Rust (xem tài liệu)."

# Kiểm tra cấu hình
if ((Test-Path "src\main\resources\application.properties") -or (Test-Path "src\main\resources\application.local.properties")) {
    Write-Host "[OK] Đã có file cấu hình application.properties hoặc application.local.properties." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Không tìm thấy file cấu hình! Vui lòng copy application.example.properties thành application.properties và điền thông tin." -ForegroundColor Red
    $errorCount++
}

if ($errorCount -eq 0) {
    Write-Host "`nMôi trường SẴN SÀNG để chạy ứng dụng!" -ForegroundColor Green
} else {
    Write-Host "`nTìm thấy $errorCount lỗi. Vui lòng khắc phục theo các hướng dẫn phía trên trước khi tiếp tục." -ForegroundColor Red
}
