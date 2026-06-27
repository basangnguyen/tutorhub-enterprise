# Phase 5: BatteryService Integration

## Objective
Tách logic lấy trạng thái pin khỏi `ExamFooterStatusBar`, chuyển vào `BatteryService` sử dụng kiến trúc State Store.

## Changes Made
1. **Created `BatteryService.java`**: 
   - Quản lý lifecycle với `ScheduledExecutorService`.
   - Lấy trạng thái từ `TSEBatteryStatusProvider`.
   - Gọi `stateStore.updateBattery(...)`.
2. **Updated `ExamFooterStatusBar.java`**:
   - Khởi tạo `batteryService` và gọi `initialize()`.
   - Update `stateStore.addListener` để gọi `cluster.updateBattery()`.
   - Xóa `SwingWorker` cho pin trong `updateStatus()`.
   - Đóng `batteryService` trong `stopStatusPolling()`.

## Result
Kiến trúc an toàn hơn, không còn `SwingWorker` rò rỉ.
