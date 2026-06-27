# Phase 6F: Read-only V2 Package Render in Child - Debug Only (Gate)

## 1. Mục tiêu
Phase này bổ sung tính năng render đề thi V2 dưới dạng read-only trong Child mode (`V2_DEBUG`). Sau khi `TSEV2ChildDebugLoader` load và giải mã thành công `V2ExamHandoffBundle`, giao diện sẽ hiển thị thêm tab "Read-only Package" bên cạnh tab "Safe Summary" hiện tại. Mọi dữ liệu nhạy cảm (sessionToken, keys, đáp án, v.v.) đều bị loại bỏ trước khi render.

## 2. File thay đổi / tạo mới
- **TSEV2ChildDebugLoadResult.java**: Bổ sung `TSEV2ReadOnlyExamRenderModel renderModel` để truyền dữ liệu an toàn sang UI mà không yêu cầu parse lại (do IPC key bị consume 1 lần).
- **TSEV2ChildDebugLoader.java**: Gắn render model vào result sau khi parse và gọi hàm `sanitizeForReadOnlyRender()`.
- **TSEV2ReadOnlyOptionView.java**: Model chứa `id` và `content` của Option.
- **TSEV2ReadOnlyQuestionView.java**: Model chứa `id`, `content`, `orderIndex` và danh sách Option.
- **TSEV2ReadOnlyExamRenderModel.java**: Model chứa meta data cơ bản (`examId`, `paperId`, `questionCount`, v.v.) và danh sách `TSEV2ReadOnlyQuestionView`.
- **TSEV2ChildReadOnlyRenderLoader.java**: Lớp chịu trách nhiệm chuyển đổi và làm sạch (sanitize) dữ liệu từ `V2ExamHandoffBundle` sang `TSEV2ReadOnlyExamRenderModel`. Không bao giờ include keys, token hay answer.
- **TSEV2ReadOnlyExamPanel.java**: Giao diện (Panel) render danh sách câu hỏi dạng Read-only (Label và Disabled RadioButton). Hiển thị rõ Footer thông báo tính năng đang bị khóa.
- **TSEExamChildClient.java**: Update luồng `launchV2DebugSkeleton()`, đưa kết quả vào 2 tab: "Safe Summary" và "Read-only Package".
- **TSEV2ChildReadOnlyRenderLoaderTest.java**: Test đảm bảo dữ liệu được map chính xác và tuyệt đối không rò rỉ (leak) dữ liệu nhạy cảm (`isCorrect`, `answerKey`, `sessionToken`).
- **TSEV2ReadOnlyExamPanelTest.java**: Test khởi tạo UI và kiểm tra text hiển thị để khẳng định không bị rò rỉ.

## 3. Kết quả Test (Regression)
- `mvn clean install` pass. Các unit test bổ sung (`TSEV2ChildReadOnlyRenderLoaderTest` và `TSEV2ReadOnlyExamPanelTest`) pass.
- Đảm bảo legacy test (`run_input_test.bat`) vẫn hoạt động.
- Không gây ảnh hưởng tới Phase 6E.

## 4. Rủi ro còn lại
- Chưa có Autosave, submit, và chức năng làm bài thực sự. V2 mới chỉ dừng lại ở prototype render đề.
- Phần chọn đáp án (`RadioButton`) bị disable đúng theo đặc tả Phase 6F.

## 5. Task tiếp theo
- Chuẩn bị tiến hành Phase 6G-6H để xử lý Action của học sinh, lưu trạng thái (Autosave) và Submit qua Child Client, theo lộ trình V2.
