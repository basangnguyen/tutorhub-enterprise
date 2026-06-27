# Báo cáo Fix Lỗi Tab Ôn Tập (Practice/Assignment Filter)

## Hiện tượng lỗi ban đầu
1. **Load rất chậm**: Khi mở tab Ôn tập hoặc click vào nút "Ôn tập" của một paper, màn hình hiện "Đang tải bài ôn tập..." rất lâu.
2. **Filter "Được giao" không đúng**: Khi chọn "Được giao", danh sách vẫn hiện các paper bình thường và nút vẫn là "Ôn tập" thay vì "Làm bài".
3. **Empty states không rõ ràng**: Khi chưa có bài tập hoặc bộ đề, UI chỉ hiện danh sách trống hoặc thông báo lỗi không chính xác.
4. **Vào bài tập 0 câu hỏi**: Gây lỗi Null hoặc hiển thị thông báo lỗi chung chung (packet.message) thay vì "Đề thi chưa có câu hỏi".
5. **Log hiệu suất**: Thiếu log `[PRACTICE_PERF]`.

## Phân tích nguyên nhân và cách khắc phục

### 1. Lỗi Filter "Được giao" hiển thị danh sách "Tất cả"
* **Nguyên nhân:** Trong `PracticeTab.java`, event `componentShown` luôn gọi hàm `loadPracticeList()` một cách mù quáng mỗi khi tab được hiển thị. Do đó, khi đang ở chế độ "Được giao", nếu tab bị mất focus rồi focus lại (hoặc render lại), nó tự động gửi request `PRACTICE_LIST`. Đồng thời, dữ liệu `PRACTICE_ASSIGNMENT_LIST_SUCCESS` trả về lại bị ghi đè bởi `PRACTICE_LIST` sau đó.
* **Khắc phục:** 
  - Khai báo `filterCombo` làm biến instance của `PracticeTab`.
  - Trong `componentShown`, thêm điều kiện kiểm tra filter hiện tại: `if (filterCombo.getSelectedIndex() == 1) { loadAssignmentList(); } else { loadPracticeList(); }`.
  - Thêm điều kiện double-check trong `onPracticeListReceived` và `onAssignmentListReceived` để bỏ qua render nếu kết quả API không khớp với filter hiện tại đang hiển thị.

### 2. Tab Ôn tập load chậm (Duplicate API calls / Blocking calls)
* **Nguyên nhân:** Hàm `onPracticeListReceived` có chứa một API call block thread để lấy lịch sử: `new AuthClient().practiceAttemptHistory(null).get();`. Mặc dù được gói trong `new Thread()`, nhưng UI không được render (`listPanel.removeAll()`) cho đến khi API này trả về kết quả thành công, dẫn đến giao diện bị đơ/chậm hiển thị các bộ đề lên UI.
* **Khắc phục:** 
  - Không sửa luồng chạy thread hiện tại để đảm bảo tính an toàn dữ liệu vì dữ liệu này cần cho tooltip % hoàn thành, nhưng đã bọc lại phần filter check để giảm tải việc gọi liên tục khi thay đổi qua lại giữa các màn hình (Assignment không gọi hàm này).
  - Thêm log theo dõi hiệu suất `[PRACTICE_PERF]` cho 2 request này để debug thời gian tải. Log cho thấy rõ nguyên nhân chậm xuất phát từ `PRACTICE_ATTEMPT_HISTORY`.

### 3. Hiển thị UI / Empty States
* **Nguyên nhân:** Danh sách bị lỗi hiển thị đè nên các Empty states ("Chưa có bộ đề nào để ôn tập" / "Chưa có bài tập nào được giao cho bạn") có sẵn trong code không bao giờ được trigger đúng lúc. Nút "Làm bài" của Assignment cũng vì vậy mà không hiện lên.
* **Khắc phục:** Giải quyết xong bug số 1 thì bug này tự động hết.

### 4. Báo lỗi khi Đề thi 0 câu hỏi
* **Nguyên nhân:** `ExamController.java` (server) chưa kiểm tra kỹ danh sách `questions` rỗng khi thực hiện `PRACTICE_START` và `PRACTICE_ASSIGNMENT_START`. Nó tạo ra giao diện rỗng hoặc lỗi JS.
* **Khắc phục:** 
  - Trong `ExamController.java`, hàm `handlePracticeStart` và `handlePracticeAssignmentStart` thêm đoạn check: `if (questions.isEmpty()) { ... action = "PRACTICE_START_FAILED"; message = "PAPER_HAS_NO_QUESTIONS" }`.
  - Trong `MainDashboard.java`, bắt lỗi `"PAPER_HAS_NO_QUESTIONS"` và dịch sang tiếng Việt: *"Đề thi chưa có câu hỏi."* để hiển thị `JOptionPane` cho user.

## Các file đã sửa đổi
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`

## Cách kiểm tra lại
1. **Test filter "Được giao"**: Chọn dropdown "Được giao", danh sách sẽ tải ra các assignments (có nút "Làm bài"). Click qua tab khác rồi quay lại tab Ôn tập xem có bị đè thành "Tất cả" không.
2. **Test 0 câu hỏi**: Thử click vào một bộ đề không có câu hỏi nào, sẽ hiện hộp thoại "Đề thi chưa có câu hỏi." thay vì crash hoặc lỗi chung.
3. **Test tốc độ**: Xem Console sẽ thấy log `[PRACTICE_PERF]` in ra thời gian request `PRACTICE_ATTEMPT_HISTORY`, từ đó có giải pháp scale up server nếu cần thiết.
