# Phase 5D.5: V2 Handoff Artifact Security Hardening

## 1. Mục tiêu phase
Tăng cường tính bảo mật của cơ chế debug handoff artifact thuộc Phase 5D. Đảm bảo rằng gói thông tin (artifact) được ghi ra local disk (`v2_handoff_preview.json`) không bao giờ chứa `sessionToken` raw (để chống việc token bị rò rỉ dưới mọi hình thức, ngay cả khi Admin/Tutor vô ý kích hoạt debug trong môi trường production). 

## 2. Rủi ro phát hiện từ Phase 5D
- Khi `debugMode=true` (chỉ có trong môi trường Admin test), server trả về `sessionToken = null`. Artifact lúc này không có thông tin mật, hoàn toàn an toàn.
- Khi `debugMode=false` (giả lập chạy production hoặc được trigger qua một kẽ hở nào đó), luồng code Phase 5D vẫn chèn nguyên `sessionToken` lấy được từ server vào cấu trúc DTO, và ghi trực tiếp ra đĩa. Việc ghi raw token của một phiên thi đang sống ra file là hành vi đặc biệt nguy hiểm.

## 3. Cách sanitize sessionToken
1. **Thay đổi DTO (`V2ExamHandoffBundle`)**: 
   - Đánh dấu thuộc tính `sessionToken` bằng từ khóa `transient`. Thư viện `Gson` sẽ tự động phớt lờ thuộc tính này và không ghi nó ra thành chuỗi JSON.
2. **Khởi tạo thông tin thay thế an toàn**:
   - Khai báo 3 thuộc tính an toàn mới: `sessionTokenPresent` (boolean), `sessionTokenMasked` (String = "***") và `sessionTokenHash` (String = mã băm SHA-256 của token gốc).
   - `V2ExamHandoffService` sẽ xử lý token lấy được từ Response Map, gán raw value vào `transient sessionToken` (dùng để transfer vào runtime child sau này) và gán các thông tin thay thế an toàn lên các field serialize.

## 4. Artifact schema mới
Artifact JSON mới được tạo ra sẽ có cấu trúc như sau (trích xuất):
```json
{
  "flow": "PAPER_START_V2",
  "examId": 3,
  "paperId": 1,
  "attemptId": "uuid-here",
  "sessionTokenPresent": true,
  "sessionTokenMasked": "***",
  "sessionTokenHash": "cf5b16a778afd...",
  "packageHash": "mockhash",
  "questionCount": 1,
  "questions": [
    // ... không chứa isCorrect
  ],
  "clientBuild": "2I.9.5-3STEP-UX"
}
```

## 5. Runtime handoff vs debug artifact
- **Runtime handoff**: Object `V2ExamHandoffBundle` lưu tại bộ nhớ RAM vẫn đang nắm giữ chuỗi `sessionToken` nguyên bản thông qua field `transient`. Giá trị này sẵn sàng để được truyền cho tiến trình JCEF/Rust qua IPC ở Phase 6.
- **Debug artifact**: Là file JSON được sinh ra qua cơ chế `Gson.toJson()`. Quá trình này đã bị chặn hoàn toàn việc ghi `sessionToken`.

## 6. Security validation
Một check layer mới đã được thêm vào `V2ExamHandoffService.validateHandoffBundle`:
- `gson.toJson(bundle)` được tạo thử nghiệm ngay trong lúc kiểm định bộ nhớ.
- Dò tìm sự tồn tại của chuỗi `"sessionToken"` cùng với nội dung của token gốc. Nếu có thì văng lỗi `SECURITY VIOLATION: Bundle contains raw session token`.
- Tương tự, nếu chứa chuỗi `"password"` hoặc `"passwordHash"`, văng lỗi ngay lập tức.
- Giữ nguyên các chốt chặn Phase 5D là cấm `"isCorrect"`, `"answerKey"`, `"correctOption"`, `"grading_config"`.

## 7. Test result
Unit Test `V2ExamHandoffServiceTest` đã được nâng cấp với các tiêu chí:
- `assertFalse(json.contains("RAW_SECRET_TOKEN"))`: Đảm bảo giá trị mật bị loại bỏ hoàn toàn khỏi chuỗi.
- `assertTrue(json.contains("\"sessionTokenPresent\": true"))` (hoặc không có khoảng trắng tuỳ setting Gson).
- `assertTrue(json.contains("\"sessionTokenMasked\": \"***\""))`.

Kết quả build toàn cục: Maven Test Pass (32 Tests), Package Portable thành công rực rỡ.

## 8. Legacy regression result
Test input `run_input_test.bat --exam-id 3` hoạt động bình thường, Login Panel hiện lên, không bị crash, không ảnh hưởng đến code nền tảng cũ.

## 9. Rủi ro còn lại
- Không còn rủi ro về rò rỉ thông tin thi qua Handoff Preview Artifact JSON.
- Tuy nhiên, như đã đề cập tại Phase 5D, cấu trúc bài thi vẫn nằm dưới dạng plaintext trên đĩa, có thể bị chỉnh sửa/đánh cắp bằng cách chiếm quyền Admin máy local (nhưng không thể nộp bài vì không có token).

## 10. Có nên đi tiếp Phase 5E không
Có, đã đủ điều kiện bảo mật tuyệt đối để bước vào Phase tiếp theo.
