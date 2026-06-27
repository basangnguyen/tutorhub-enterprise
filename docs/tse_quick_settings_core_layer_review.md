# Phase 3.5: Core Data Layer Review & Hardening

**Author**: Antigravity AI Architect  
**Date**: 2026-06-14  
**Scope**: Taskbar & Quick Settings (Core Layer)  

---

## 1. Các class đã tạo và review
1. `TSESecurityPolicy.java`
2. `QuickSettingsSnapshot.java`
3. `QuickSettingsStateStore.java`
4. `QuickSettingsController.java`

## 2. Những điểm đã đúng ở Phase 1-3
- State store hoàn toàn thread-safe bằng `AtomicReference` không lock (lock-free).
- Đã tách được `SecurityPolicy` để config behavior mà không đụng vào UI logic.
- Snapshot đã thiết kế có version, builder rõ ràng và immutable.
- Đã hỗ trợ `Gson` serialize ra payload chung (Dùng cho cả Parent HTML và Child JCEF).

## 3. Những điểm đã chỉnh sửa và hoàn thiện trong Phase 3.5
- **TSESecurityPolicy**: Thêm các hàm helper tiện dụng `canSetVolume()`, `canSetBrightness()`, `canConnectWifi()`, `canRefresh()`.
- **QuickSettingsSnapshot**:
  - Thêm `clampPercent()` để tự động giới hạn `volume`, `brightness`, `battery` vào khoảng [0-100].
  - Thêm `nullSafe()` cho các chuỗi `wifiStatus`, `wifiSsid`, `clockTime`, `clockDate` để tránh sinh ra "null" vô tình lúc render frontend.
  - Cung cấp `initial(TSESecurityPolicy)` factory khởi tạo mặc định.
- **QuickSettingsStateStore**: 
  - Đảm bảo `listeners` được dọn sạch qua method `shutdown()`.
  - Thêm `getListenerCount()` để tiện bề debug/theo dõi tài nguyên memory leak sau này.
- **QuickSettingsController**: 
  - Trong các hàm update (VD `setVolume`), nếu SecurityPolicy từ chối, Controller sẽ **không bỏ qua** im lặng mà gọi `stateStore.updateVolume(...)` với parameter error là `Chức năng bị khóa trong phòng thi`. Từ đó UI sẽ có thể feedback ngay cho user (thông qua snapshot event) mà không cần popup Alert.
  - Implement `shutdown()` để delegate dọn dẹp qua Store.

## 4. Những điểm chưa làm
- Chưa có các Service phần cứng thật (`VolumeService`, `BatteryService`, v.v.).
- UI hiện tại ở `ExamFooterStatusBar` hay `TSEParentHtmlQuickSettingsPopup` chưa listen vào `StateStore`.
- Chưa map event chuột hay phím từ giao diện HTML sang Controller.

## 5. Vì sao chưa tích hợp UI/Service thật
- **Kiến trúc Clean Architecture**: Buộc phải hoàn thiện Model và logic quản lý quyền (Policy) trên môi trường cô lập trước khi ráp giao diện vào. Ráp ngay lúc này sẽ dính dáng quá nhiều tới thread-safety của Java Swing hoặc COM (JNA) có thể làm crash toàn bộ app.
- Feasibility spike (Phase 0) cũng khuyên: cần có core layer chạy thông suốt để mock data tốt.

## 6. Phase tiếp theo khuyến nghị
**Phase 4 & 5**: Xây dựng `ClockService` (dễ nhất) và `BatteryService` (ít rủi ro). Tách luồng SwingWorker đang chạy trên footer.
**Phase 6**: Tái cấu trúc `TSEVolumeController` thành `VolumeService` với luồng COM tách biệt để chặn leak.
