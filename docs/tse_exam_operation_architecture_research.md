# Nghiên cứu Kiến trúc Vận hành Kỳ thi (Exam Operation Architecture Research)

Tài liệu này trình bày kết quả nghiên cứu về kiến trúc vận hành kỳ thi, tham khảo từ Safe Exam Browser (SEB) v3.10.1 và hiện trạng của TutorHub, từ đó đưa ra thiết kế kiến trúc chuẩn để TutorHub có thể vận hành kỳ thi một cách chuyên nghiệp.

## 1. Tóm tắt nghiên cứu SEB

Trong mô hình SEB, một kỳ thi được vận hành như sau:

**1. SEB không tự tạo câu hỏi:** SEB hoàn toàn không có tính năng tạo câu hỏi hay đề thi. Nó chỉ đóng vai trò là một "Secure Browser" (trình duyệt bảo mật) và "Lockdown Client" (khoá môi trường máy tính).

**2. Trách nhiệm của LMS / Server:** Hệ thống LMS (Learning Management System, ví dụ: Moodle, Canvas) hoặc Server chịu trách nhiệm hoàn toàn về nghiệp vụ thi. Bao gồm: quản lý câu hỏi, tạo đề, cấp quyền, cấu hình bài thi, chấm điểm, lưu trữ.

**3. Câu hỏi và đề thi được tạo ở đâu?** Được tạo trên hệ thống Server/LMS bởi quản trị viên hoặc giáo viên.

**4. Kỳ thi được publish / gửi sang client thế nào?** 
- Server tạo một "Start URL" và xuất file cấu hình `.seb`.
- Khi client chạy file cấu hình này, SEB sẽ đọc Start URL và điều hướng trình duyệt nhúng tới URL của bài thi.

**5. Client nhận đề thi bằng cách nào?**
- Client đóng vai trò như một trình duyệt thông thường nhưng bị khoá chặt. Nó load nội dung bài thi bằng HTML/JS/CSS từ Server gửi về.
- Giao tiếp giữa SEB và Server đi kèm với các mã xác thực như **Browser Exam Key (BEK)** và **Config Key** để Server biết đây là truy cập hợp lệ từ SEB chứ không phải Chrome/Firefox.

**6. Client render câu hỏi, lưu câu trả lời, autosave và final submit như thế nào?**
- Client render giao diện dựa vào HTML từ Server.
- Các thao tác click chọn đáp án, điền chữ được thực hiện bằng form HTML.
- **Autosave / Final Submit** là do các đoạn script JS trên trang web gửi yêu cầu (AJAX/Fetch) về Server, giống như mọi ứng dụng web khác. SEB không can thiệp sâu vào nội dung submit mà chỉ cung cấp môi trường chạy an toàn.

**7. Server nhận bài làm, kiểm tra, lưu trữ, chấm điểm như thế nào?**
- Server nhận các POST request chứa dữ liệu trả lời, kiểm tra hợp lệ, lưu vào database, đối chiếu với đáp án đúng để tính điểm và hiển thị kết quả.

## 2. Phân tích hiện trạng TutorHub

Sau khi kiểm tra mã nguồn TutorHub hiện tại (đặc biệt trong `ClientHandler.java`), đây là đánh giá:

| Module hiện có | File / Class liên quan | Đã làm được gì | Thiếu gì | Rủi ro |
|---|---|---|---|---|
| **1. Tạo kỳ thi** | `ExamDAO.java`, Database | Tạo được metadata kỳ thi (Tên, cấu hình) | Thiếu UI Quản lý và tính năng gán đề thi | Dữ liệu rác nếu không ràng buộc |
| **2. Danh sách kỳ thi** | `ClientHandler.java` | Server trả về danh sách `ACTIVE` exams | Thiếu phân quyền (giáo viên xem hết, HS xem đề được giao) | Học sinh thấy kỳ thi không dành cho mình |
| **3. Tham gia kỳ thi** | `TSEExamChildClient`, `TSELoginPanel` | Trình duyệt khoá màn hình và tham gia được | Chưa xử lý attempt token an toàn 100% | Bypass exam session nếu fake request |
| **4. Màn hình làm bài** | MHTML mock hoặc WebView | Render HTML dạng Mock | Thiếu render câu hỏi động từ DB thực | Lỗi UI khi cấu trúc câu hỏi phức tạp |
| **5. Autosave** | Chưa rõ | Chưa có hoặc mới là mock | Thiếu cơ chế lưu cache offline và sync logic | Mất bài khi rớt mạng |
| **6. Final Submit** | `ClientHandler` `EXAM_SUBMIT` | Nhận file/payload và ACK | Chấm điểm tự động và validate submission payload | Bị sửa payload trước khi mã hoá |
| **7. Server / API** | `ClientHandler.java` (Socket) | Xử lý packet `EXAM_START_REQUEST` | Cần chuyển thành RESTful API hoặc chuẩn hoá Socket payload | Payload bị thay đổi dễ dẫn đến crash |
| **8. Database** | `CheckDB.java` | Có bảng `exams` | Rất thiếu bảng: Question Bank, Options, Attempts | Kiến trúc dữ liệu không mở rộng được |

**Kết luận nhanh:** TutorHub hiện tại thiếu toàn bộ nghiệp vụ **Quản lý Câu Hỏi (Question Bank)**, **Tạo Đề Thi (Exam Paper Builder)**, **Chấm điểm (Grading)** và **Kết quả (Result Dashboard)**. Hiện tại nó chỉ giống như một Lockdown Client rỗng tuếch chưa có não (LMS).

## 3. Luồng vận hành kỳ thi đề xuất

Dưới đây là thiết kế luồng vận hành chuẩn:

1. **Giáo viên / Admin:** Tạo Ngân hàng câu hỏi (Question Bank) trên Server.
2. **Giáo viên / Admin:** Tạo các câu hỏi riêng lẻ và gán vào Question Bank.
3. **Giáo viên / Admin:** Tạo Đề thi (Exam Paper) bằng cách nhặt câu hỏi từ Ngân hàng.
4. **Giáo viên / Admin:** Tạo Kỳ thi (Exam), thiết lập thời gian, chính sách (cho lùi lại không, xáo trộn không).
5. **Giáo viên / Admin:** Gán Đề thi vào Kỳ thi và đổi trạng thái thành `PUBLISHED`.
6. **Học sinh:** Đăng nhập TutorHub (Parent App), thấy danh sách Kỳ thi đang `PUBLISHED`.
7. **Học sinh:** Bấm "Tham gia". Parent app gọi Server tạo một `Exam Session/Attempt` và nhận về `Start Token`.
8. **Secure Client (Child App):** Mở lên, khoá máy (Lockdown), dùng `Start Token` gọi API tải **Exam Package** (chứa cấu hình, danh sách câu hỏi).
9. **Secure Client:** Render câu hỏi. Học sinh làm bài.
10. **Secure Client:** Lưu local trả lời và tự động `Autosave` gửi lên Server (định kỳ 1 phút/lần).
11. **Secure Client:** Học sinh bấm "Nộp Bài" -> **Final Submit**. Gửi file mã hoá `submit_payload.enc`.
12. **Server:** Nhận Final Submit, giải mã, validate, tính điểm tự động. Trạng thái attempt = `SUBMITTED`.
13. **Học sinh / Giáo viên:** Xem kết quả tại Result Dashboard.

## 4. Đề xuất Database Schema

Thiết kế cơ sở dữ liệu quan hệ (PostgreSQL / MySQL) đề xuất:

```sql
-- 1. users: Chứa giáo viên, học sinh (Đã có)

-- 2. question_banks: Ngân hàng câu hỏi
CREATE TABLE question_banks (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    creator_id INT REFERENCES users(id),
    created_at TIMESTAMP
);

-- 3. questions: Câu hỏi thực tế
CREATE TABLE questions (
    id SERIAL PRIMARY KEY,
    bank_id INT REFERENCES question_banks(id),
    type VARCHAR(50), -- SINGLE_CHOICE, MULTIPLE_CHOICE, ESSAY
    content TEXT,
    difficulty INT,
    default_score FLOAT
);

-- 4. question_options: Các lựa chọn cho câu hỏi trắc nghiệm
CREATE TABLE question_options (
    id SERIAL PRIMARY KEY,
    question_id INT REFERENCES questions(id),
    content TEXT,
    is_correct BOOLEAN
);

-- 5. exam_papers: Đề thi (Tập hợp câu hỏi)
CREATE TABLE exam_papers (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    creator_id INT REFERENCES users(id)
);

-- 6. exam_paper_questions: Mapping Đề thi và Câu hỏi
CREATE TABLE exam_paper_questions (
    paper_id INT REFERENCES exam_papers(id),
    question_id INT REFERENCES questions(id),
    score FLOAT,
    order_index INT,
    PRIMARY KEY(paper_id, question_id)
);

-- 7. exams: Kỳ thi (Phần hiện có nhưng cần mở rộng)
CREATE TABLE exams (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    paper_id INT REFERENCES exam_papers(id),
    status VARCHAR(50), -- DRAFT, PUBLISHED, ACTIVE, CLOSED
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_mins INT,
    security_config JSONB
);

-- 8. exam_attempts: Lượt thi của từng học sinh
CREATE TABLE exam_attempts (
    id UUID PRIMARY KEY, -- Dùng UUID để chống đoán ID
    exam_id INT REFERENCES exams(id),
    user_id INT REFERENCES users(id),
    status VARCHAR(50), -- NOT_STARTED, IN_PROGRESS, SUBMITTED, AUTO_SUBMITTED, INVALID
    started_at TIMESTAMP,
    submitted_at TIMESTAMP,
    final_score FLOAT
);

-- 9. exam_answers: Câu trả lời chi tiết cho từng attempt (Autosave)
CREATE TABLE exam_answers (
    attempt_id UUID REFERENCES exam_attempts(id),
    question_id INT REFERENCES questions(id),
    selected_option_ids JSONB,
    essay_content TEXT,
    PRIMARY KEY(attempt_id, question_id)
);

-- 10. audit_logs: Log hoạt động thi
CREATE TABLE audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id),
    attempt_id UUID REFERENCES exam_attempts(id),
    action VARCHAR(255),
    details JSONB,
    created_at TIMESTAMP
);
```

## 5. Đề xuất API (REST hoặc Socket Packet)

**Quản lý Câu hỏi & Đề thi (Role Teacher/Admin):**
- `POST /api/question-banks`: Tạo ngân hàng.
- `GET /api/question-banks`: Lấy danh sách ngân hàng câu hỏi.
- `POST /api/questions`: Tạo câu hỏi mới kèm options.
- `GET /api/question-banks/{id}/questions`: Lấy danh sách câu hỏi trong ngân hàng.
- `POST /api/exam-papers`: Tạo đề thi.
- `GET /api/exam-papers/{id}`: Xem chi tiết đề thi.
- `POST /api/exam-papers/{id}/questions`: Add câu hỏi vào đề.

**Quản lý Kỳ thi (Role Teacher/Admin):**
- `GET /api/exams`: Danh sách kỳ thi.
- `POST /api/exams`: Tạo kỳ thi mới.
- `GET /api/exams/{id}`: Xem chi tiết kỳ thi.
- `POST /api/exams/{id}/assign-paper`: Gán đề vào kỳ thi.
- `POST /api/exams/{id}/publish`: Đổi trạng thái sang PUBLISHED.

**Làm bài (Role Student):**
- `POST /api/exams/{id}/start`: Bắt đầu thi -> Trả về `attemptId`.
- `GET /api/exams/{id}/package` hoặc `GET /api/attempts/{attemptId}/package`: Tải package câu hỏi của lượt thi đó (Đã xáo trộn nếu cần).
- `POST /api/attempts/{attemptId}/autosave`: Lưu nháp định kỳ. Body: danh sách câu trả lời hiện tại.
- `POST /api/attempts/{attemptId}/submit`: Final submit bài thi.

**Kết quả:**
- `GET /api/exams/{id}/results`: Xem kết quả của toàn bộ học sinh trong kỳ thi.
- `GET /api/attempts/{attemptId}/result`: Xem kết quả chi tiết của 1 lượt thi.

## 6. Thiết kế Exam Package

Khi Child App gọi `/api/attempts/{attemptId}/package`, Server trả về cấu trúc JSON sau:

```json
{
  "examId": 3,
  "attemptId": "a1b2c3d4-...",
  "title": "Thi thử Toán Quốc Gia",
  "durationMinutes": 60,
  "startedAt": "2026-06-17T08:00:00Z",
  "expiresAt": "2026-06-17T09:00:00Z",
  "policy": {
    "shuffleQuestions": false,
    "shuffleOptions": true,
    "allowBack": true
  },
  "questions": [
    {
      "questionId": 101,
      "type": "SINGLE_CHOICE",
      "content": "1 + 1 bằng mấy?",
      "options": [
        { "optionId": 10, "content": "2" },
        { "optionId": 11, "content": "3" }
      ],
      "score": 1
    }
  ],
  "packageHash": "SHA256-hash-cua-toan-bo-data",
  "signature": "RSA-signature-tu-server"
}
```

**Phân tích Package:**
- **Signature / Hash:** Có nên ký số package không? Rất cần thiết. Chữ ký (Signature) đảm bảo file package không bị sửa đổi ở Proxy trung gian hoặc bởi tool cheat.
- **Cache Local:** Client nên lưu cache package này bằng SQLite nội bộ hoặc mã hoá AES xuống file. Nếu rớt mạng hoặc sập app, mở lại không cần chép lại đề.
- **Đáp án đúng:** TUYỆT ĐỐI KHÔNG GỬI đáp án (`is_correct`) xuống client. Điểm số chỉ được chấm ở Server.
- **Final Submit hiện tại:** Vẫn giữ nguyên cơ chế tạo `submit_payload.enc`. File `.enc` sẽ chứa mảng các `answer` và `attemptId`, mã hoá bằng AES/TEK gửi về Server.

## 7. UI Cần Bổ Sung

Theo thứ tự ưu tiên (Làm ở Web Admin hoặc Desktop Teacher Role):
1. **Question Bank Management:** UI quản lý ngân hàng câu hỏi. Nằm trong Teacher Portal. API CRUD Question Banks.
2. **Create Question Dialog:** Giao diện tạo câu hỏi (có Rich Text Editor). API tạo question và options. Có Validate nội dung trống.
3. **Question List:** Xem các câu hỏi trong Bank.
4. **Exam Paper Builder:** Giao diện kéo thả câu hỏi vào đề thi, nhập điểm cho từng câu. API `POST exam-papers/{id}/questions`.
5. **Assign Paper to Exam:** Select/Dropdown chọn đề thi khi sửa/tạo kỳ thi.
6. **Publish Exam:** Nút chuyển trạng thái kỳ thi thành PUBLISHED.
7. **Student Join Exam:** Nút bắt đầu thi ở danh sách môn học, gọi API `start` để lấy `attemptId`.
8. **Exam Taking UI (Bên Child App):** Giao diện làm bài thực tế (Hiển thị list câu hỏi bên trái, nội dung bên phải, đồng hồ đếm ngược). Gọi API tải package và render.
9. **Review Answers:** Xác nhận lại các câu đã làm trước khi Submit.
10. **Result Dashboard:** Bảng điểm cho giáo viên, hiển thị lịch sử và log.

## 8. Kịch bản làm bài an toàn (Exam Taking Flow)

1. **Bắt đầu:** Client nhận `examId` và `attemptId` (từ Parent App).
2. **Tải Package:** Client gọi Server lấy `Exam Package`.
3. **Verify:** Client Verify chữ ký/Hash của Package. Nếu fail -> Chặn thi.
4. **Render:** Hiển thị câu hỏi và đồng hồ đếm ngược (được tính từ `expiresAt - now` do Server cấp, không tin tưởng giờ máy tính).
5. **Lưu Local:** Khi học sinh chọn đáp án, ghi ngay xuống SQLite cache hoặc Memory cache (`answer_state`).
6. **Autosave loop:** Cứ 1 phút, lấy data từ `answer_state` gọi `POST /autosave` lên server. Server cập nhật bảng `exam_answers`.
7. **Safe Refresh (Mất mạng):** Nếu rớt mạng, giao diện hiện cảnh báo, học sinh VẪN được chọn đáp án (chỉ lưu local). Khi có mạng lại, tự đồng bộ đẩy lên Server.
8. **Hết giờ / Nộp tự nguyện:** Kích hoạt luồng `Final Submit`.
9. **Ghi payload:** Gói toàn bộ dữ liệu trả lời thành JSON, gọi code C++ / Rust / Java hiện tại để mã hoá ra file `submit_payload.enc`.
10. **Parent nhận:** Child App thoát, Parent App đọc file `submit_payload.enc` và gửi qua Socket/API lên Server.
11. **Server xử lý:** Server nhận submit, giải mã `.enc`, chấm điểm, khoá attempt `status = SUBMITTED`.
12. **Cleanup:** Client xoá mọi cache liên quan tới kỳ thi để bảo mật.

## 9. Security Checklist

- [x] **Không cho sửa câu hỏi sau khi publish:** Nếu kỳ thi đã chuyển `PUBLISHED` và có người thi, chặn toàn bộ API sửa/xoá câu hỏi.
- [x] **Mỗi học sinh chỉ 1 attempt:** Khi bấm Start, kiểm tra nếu có attempt `IN_PROGRESS` thì trả về đúng attempt đó.
- [x] **Attempt Token:** Cần mã hoá hoặc dùng UUID khó đoán để làm `attemptId`.
- [x] **Signature cho Package:** Package tải về phải đi kèm Hash và RSA Signature.
- [x] **Submit Payload Context:** Payload Final Submit phải chứa `examId`, `attemptId`, `userId`, `timestamp` để chống submit chéo bài người khác.
- [x] **Autosave đè Final Submit:** Nếu Final Submit gửi trước, Server chuyển trạng thái thành `SUBMITTED`. Mọi request Autosave đến sau sẽ bị từ chối thẳng.
- [x] **Server chống submit trùng:** Nếu trạng thái đã `SUBMITTED` hoặc `AUTO_SUBMITTED`, Server không chấm lại (Idempotent).
- [x] **Audit Log:** Lưu lại toàn bộ sự kiện "Start", "Autosave", "Disconnect", "Submit".
- [x] **Logging Terminal:** TUYỆT ĐỐI không log content (nội dung câu trả lời) ra `stdout/stderr` (console) để chống học sinh scan log lấy đáp án.
- [x] **Windows Settings / Ứng dụng:** Trạng thái Lockdown phải được duy trì 100% trong quá trình thi.
- [x] **Không lưu plaintext payload:** Chỉ ghi file `submit_payload.enc`, xoá file gốc hoặc giữ trên RAM để mã hoá.

## 10. Kế hoạch triển khai (Roadmap)

**Phase A: Research & schema design**
- **Mục tiêu:** Hiểu kiến trúc SEB và thiết kế lại luồng cho TutorHub.
- **Tình trạng:** Hoàn thành (Bản tài liệu này).

**Phase B: Question Bank CRUD**
- **Mục tiêu:** API và Database cơ bản cho Ngân hàng câu hỏi.
- **API cần thêm:** `POST/GET /question-banks`, `POST/GET /questions`.

**Phase C: Create Question UI**
- **Mục tiêu:** Giao diện cho Admin tạo câu hỏi.
- **Rủi ro:** Xử lý render Markdown / Rich Text công thức Toán.

**Phase D: Exam Paper Builder**
- **Mục tiêu:** Lắp ráp câu hỏi thành 1 Đề thi (Exam Paper).
- **Điều kiện pass:** Tạo thành công Đề thi và lưu DB với tổng điểm tự động tính.

**Phase E: Assign Paper to Exam**
- **Mục tiêu:** Ràng buộc Exam <-> Exam Paper.

**Phase F: Publish Exam**
- **Mục tiêu:** Khoá cấu hình, đổi trạng thái sang PUBLISHED.

**Phase G: Student Start Attempt**
- **Mục tiêu:** API sinh `attemptId` và lưu thời gian bắt đầu.
- **Test:** Verify user chỉ có tối đa 1 active session.

**Phase H: Exam Package API**
- **Mục tiêu:** API trả về danh sách câu hỏi không có đáp án, kèm Hash/Signature.

**Phase I: Client Render Questions**
- **Mục tiêu:** UI Child App load câu hỏi động từ DB.

**Phase J: Autosave**
- **Mục tiêu:** Đồng bộ local storage và Server.
- **Rủi ro:** Race condition khi mạng chậm chập chờn.

**Phase K: Final Submit integration**
- **Mục tiêu:** Đóng gói đáp án và gọi logic `submit_payload.enc` hiện tại mà KHÔNG ĐỔI cơ chế mã hoá lõi.

**Phase L: Grading**
- **Mục tiêu:** So khớp `exam_answers` với `question_options.is_correct` trên Server để tính `final_score`.

**Phase M: Result Dashboard**
- **Mục tiêu:** Admin xem điểm.

**Phase N: Audit & hardening**
- **Mục tiêu:** Đánh giá an toàn, kiểm thử Penetration.

## 11. Rủi ro và Khuyến nghị

1. **Rủi ro dữ liệu:** Việc thiết kế DB phức tạp có thể làm gián đoạn các bảng cũ. Cần migration script cẩn thận.
2. **Rủi ro rớt mạng:** Học sinh thi ở nhà dễ rớt mạng. Cơ chế Autosave + Offline Cache là BẮT BUỘC.
3. **Khuyến nghị:** KHÔNG đụng chạm/thay đổi cách mã hoá của `submit_payload.enc` hiện tại để tránh break code Rust/C++ sẵn có. Ta chỉ tạo ra cục dữ liệu JSON rồi đút vào hàm mã hoá đó. 

---
Tài liệu này đóng vai trò bản thiết kế cho quá trình phát triển Module Quản lý và Vận hành Kỳ thi của TutorHub trong các Phase tiếp theo.
