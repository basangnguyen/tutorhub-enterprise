# Phase 7A.5: Full Regression Test & Baseline Lock

## 1. Mục tiêu
Kiểm tra toàn bộ taskbar/footer sau khi đã thêm `ClockService`, `BatteryService`, `VolumeService` và `NetworkService`. Phase này thực hiện test tổng thể mà không thay đổi bất kỳ code nào.

## 2. Kết quả Build
1. **Maven Build**: PASS
   - Cả quá trình compile và test (`mvn clean install`) đều chạy mượt mà, không gặp lỗi.
2. **Portable Build**: PASS
   - Quá trình đóng gói (`build_portable.ps1`) sao chép thư mục JCEF, policy và dependencies đầy đủ.

## 3. Kết quả Manual UI Test
1. **Footer có hiển thị Clock HH:mm không**: PASS
2. **Battery icon còn hiển thị đúng không**: PASS
3. **WiFi icon còn hiển thị đúng không**: PASS
4. **Volume icon còn hiển thị đúng không**: PASS
5. **Kéo volume slider có đổi âm lượng thật không**: PASS
6. **Mute/unmute có hoạt động thật không**: PASS
7. **VIE/ENG vẫn gõ tiếng Việt được không**: PASS
8. **Quick Settings mở/đóng bình thường không**: PASS
9. **Không có thanh ngang đen**: PASS
10. **Không có Java exception**: PASS
11. **Không mở Windows Settings / Control Panel / Network UI**: PASS
12. **Power/Exit vẫn bị block trong Exam**: PASS
13. **Safe Refresh vẫn hoạt động**: PASS
14. **Final Submit ghi được submit_payload.enc**: PASS
15. **Parent nhận FINAL submit payload**: PASS
16. **Rust exit code = 0**: PASS
17. **Không còn process Java/Rust treo sau khi submit**: PASS

## 4. Kiểm tra Log
- **[TSE_CLOCK_SERVICE]**: Hoạt động ngầm bình thường (không ghi log lỗi, chứng tỏ timer chạy tốt).
- **[TSE_BATTERY_SERVICE]**: Cập nhật trạng thái tốt thông qua Store.
- **[TSE_VOLUME_SERVICE]**: Ghi nhận hoạt động, có chu trình COM `COM initialized` và `COM uninitialized` chuẩn xác.
- **[TSE_NETWORK_SERVICE]**: Ghi nhận hoạt động polling (ví dụ: `refreshNow`, `status connected ssid=...`).
- **Final Submit & Rust Exit Code**: Không có bất thường, chu trình thoát chạy an toàn (`Rust process exited code: 0`).
- **Tiến trình rác**: Không phát hiện process JCEF, Java hay Rust nào treo lại.

## 5. Kết luận
Tất cả các dịch vụ (Clock, Battery, Volume, Network) đã được đưa vào kiến trúc Service -> Store -> UI một cách ổn định, không gây crash hoặc rò rỉ luồng.
**Kiến trúc đủ độ tin cậy để chuyển sang Phase 8.**
