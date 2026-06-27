# TutorHub Secure Exam - Deep Research: Kiến trúc vận hành kỳ thi

Ngày cập nhật: 2026-06-17  
Phạm vi: nghiên cứu kiến trúc, không sửa source code, không chạy lockdown trên máy chính, không commit.

Tài liệu này thiết kế lại phần "nghiệp vụ vận hành kỳ thi" cho TutorHub Secure Exam (TSE). Mục tiêu không phải thay Quick Settings, Rust lockdown hay Final Submit hiện có, mà là xây dựng lộ trình để TutorHub có thể vận hành kỳ thi đầy đủ như một LMS/proctoring platform thực thụ: giáo viên tạo ngân hàng câu hỏi, tạo đề, phát hành kỳ thi, thí sinh làm bài trong môi trường an toàn, hệ thống autosave, nộp bài, chấm điểm, lưu log và xuất kết quả.

---

## 1. Kết luận điều hành

TSE hiện đã có phần "secure shell" khá mạnh:

- Parent/Login/Configuration flow đã có.
- Child Exam/JCEF đã có.
- Rust lockdown core đã được tích hợp theo hướng VM-safe.
- Final Submit đã có cơ chế `submit_payload.enc`, fallback `autosave_payload.enc`.
- Server đã có packet `TSE_GET_CONFIG_LIST`, `EXAM_START_REQUEST`, `EXAM_SUBMIT`.

Nhưng phần "exam operation" vẫn đang ở mức MVP kỹ thuật:

- Câu hỏi đang gắn trực tiếp vào `exam_id`, chưa có ngân hàng câu hỏi và đề thi độc lập.
- Chưa có mô hình `paper` / `attempt` / `assignment` / `result` rõ ràng.
- `ClientHandler.java` đang chứa quá nhiều logic render HTML, kiểm tra mật khẩu, tạo session, submit, validate và save answer.
- `EXAM_START_REQUEST` đang dùng payload dạng chuỗi `examId|password`, khó mở rộng và dễ lỗi.
- `EXAM_SUBMIT` đã lưu đáp án nhưng chưa có grading/result workflow hoàn chỉnh.
- `exam_sessions` bị unique theo `(exam_id, user_id)`, nên không hỗ trợ tốt nhiều lần thi, retry, attempt history.
- Có hai nơi tạo schema exam (`DatabaseManager` và `ExamDatabaseManager`) với kiểu dữ liệu khác nhau (`JSONB` và `TEXT`), dễ drift.

Hướng đúng là không viết lại toàn bộ ngay. Nên đi theo migration từng lớp:

1. Chuẩn hóa schema và migration.
2. Tách packet handler/service khỏi `ClientHandler`.
3. Thêm `question_banks`, `exam_papers`, `exam_assignments`, `exam_attempts`.
4. Chuyển dần start/submit sang DTO JSON versioned.
5. Giữ Final Submit hiện có, nhưng làm nó idempotent và có attempt token.
6. Sau khi chắc phần lõi, mới làm UI tạo đề/chấm điểm/monitoring.

---

## 2. Nguồn nghiên cứu và bài học chính

### 2.1. Tài liệu nội bộ đã đọc

- `docs/MASTER_SECURE_EXAM_BLUEPRINT_v4_.md`
- `docs/secure_exam_tasks_v2.md`
- `docs/secure_exam_rust_and_seb_learning_sources_ONLY_3_DOCS.md`
- `docs/tse_brightness_full_research_fix.md`
- `docs/tse_brightness_vm_acceptance_result.md`
- `docs/tse_parent_popup_ui_sync_phase.md`
- `docs/tse_exam_operation_architecture_research.md`
- `seb-reference` tag `v3.10.1`

### 2.2. Nguồn chính thức online đã đối chiếu

- Safe Exam Browser overview: SEB là môi trường browser/kiosk an toàn, kết nối tới LMS hoặc e-assessment system. Nó khóa máy khách, không thay LMS. Nguồn: https://safeexambrowser.org/about_overview_en.html
- Safe Exam Browser Windows manual: Browser Exam Key / Config Key dùng để xác thực đúng SEB version + config với quiz/LMS. Nguồn: https://safeexambrowser.org/windows/win_usermanual_en.html
- Moodle Quiz activity: câu hỏi được tạo và lưu riêng trong Question bank, có thể tái sử dụng ở nhiều quiz; quiz có attempts và results. Nguồn: https://docs.moodle.org/en/Quiz_activity
- Moodle Question bank: question bank cho phép tạo, preview, edit, phân loại, share, version câu hỏi. Nguồn: https://docs.moodle.org/en/Question_bank
- Open edX Proctored Exams: proctored exam là timed exam được giám sát bởi proctoring software, tách phần học liệu/vấn đề/đề khỏi lớp kiểm soát thi. Nguồn: https://docs.openedx.org/en/latest/educators/concepts/proctored_exams/about_proctored_exams.html
- 1EdTech QTI: chuẩn trao đổi item/test/result giữa authoring tools, item banks, assessment delivery và scoring/analytics. Nguồn: https://www.1edtech.org/standards/qti/index
- Canvas quiz submissions/items APIs: tách quiz item, item bank, submission và event. Nguồn: https://developerdocs.instructure.com/services/canvas/resources/quiz_submissions và https://developerdocs.instructure.com/services/canvas/resources/new_quiz_items

### 2.3. Bài học từ SEB v3.10.1

SEB làm tốt:

- Chia hệ thống thành Runtime, Client, Browser, Settings, Monitoring, Server, Lockdown.
- Runtime có khái niệm session start/stop rõ ràng.
- Browser settings có Start URL, Browser Exam Key, Config Key, Quit URL, cache/cookie policy, URL filter.
- Server handshake có bước select exam và nhận key liên quan integrity.
- Client không chứa ngân hàng câu hỏi, không chấm điểm, không tự làm LMS.

SEB không làm:

- Không tạo câu hỏi.
- Không xây đề.
- Không chấm điểm.
- Không quản lý lớp học, assignment, result như LMS.
- Không thay thế Moodle/Open edX/Canvas.

Bài học cho TutorHub:

- TutorHub phải tự đóng vai LMS/e-assessment server.
- TSE client chỉ nên làm nhiệm vụ lock, render, collect answer, autosave local, submit payload.
- Server phải là nguồn sự thật cho exam package, attempt state, answer validation, grading và result.
- Không nên gửi answer key/correct option xuống client.
- Không nên để child JCEF tự quyết định pass/fail.

---

## 3. Audit hiện trạng source

### 3.1. Database/schema

Nguồn chính:

- `src/main/java/com/mycompany/tutorhub_enterprise/server/DatabaseManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/db/ExamDatabaseManager.java`

Schema hiện có:

| Table | Vai trò hiện tại | Vấn đề |
|---|---|---|
| `exams` | Kỳ thi/bài thi | Thiếu `paper_id`, assignment policy, publish lifecycle rõ ràng |
| `questions` | Câu hỏi | Đang gắn trực tiếp `exam_id`, không tái sử dụng tốt |
| `exam_sessions` | Phiên thi | `UNIQUE(exam_id,user_id)` giới hạn attempt; chưa có token/hash/deadline rõ |
| `exam_answers` | Đáp án | Gắn theo `session_id`, chưa phân biệt autosave/final |
| `anticheat_events` | Log chống gian lận | Có ích, cần mở rộng thành audit/event stream |
| `question_bank_categories` | Danh mục ngân hàng | Có trong `ExamDatabaseManager`, nhưng thiếu `question_banks` thật |

Vấn đề lớn:

- Có hai schema creator cho cùng module exam. Đây là rủi ro migration.
- Một bên dùng `JSONB`, một bên dùng `TEXT`.
- Comment trong `ExamDatabaseManager.java` bị lỗi encoding.
- Chưa có bảng version/migration nên khó kiểm soát deploy.

### 3.2. Server packet flow

Nguồn chính:

- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/ExamDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamService.java`

Flow hiện tại:

- `TSE_GET_CONFIG_LIST`: lấy `ACTIVE exams` và trả danh sách exam.
- `EXAM_START_REQUEST`: nhận `examId|password`, kiểm tra login, kiểm tra exam active, kiểm tra password trong `security_config`, tạo/update session, render HTML, trả `htmlContent`.
- `EXAM_SUBMIT`: nhận `sessionId|payload`, kiểm tra session thuộc user, validate question thuộc exam, save answer, update session `SUBMITTED`, trả `EXAM_SUBMIT_ACK`.

Điểm ổn:

- Đã có path thật từ server tới client.
- Submit có kiểm tra owner/session/status cơ bản.
- Submit đã validate question thuộc exam.
- Parent ưu tiên final payload trước autosave.

Điểm yếu:

- `ClientHandler.java` đang là god class.
- HTML render bằng string concat trong server packet handler.
- Payload pipe-separated không versioned, khó mở rộng.
- Mật khẩu exam đọc từ `security_config` dạng plain text.
- `ExamService.handleSubmitExam` gần như stub, trong khi `ClientHandler` tự xử lý submit.
- `createSession` dùng conflict update, dễ làm mất nghĩa attempt.

### 3.3. Client TSE flow

Nguồn chính:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/services/NetworkTSEExamService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/services/SecureNetworkTSEExamService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEExamChildClient.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEProductionParentSubmitLabLauncher.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/OfflineExamCache.java`

Điểm ổn:

- `TSEExamService` interface đã tách được mock/network/secure wrapper.
- Child viết `submit_payload.enc` atomically rồi thoát nhanh.
- Autosave file tồn tại và không làm child thoát.
- Parent đã có recovery scan.

Điểm yếu:

- `NetworkTSEExamService.login` đang hardcode `userId=1`.
- `OfflineExamCache` SQLite đang lưu thô, không gắn attempt/session token, chưa encrypted.
- `TSEStartExamResult` nhận `htmlContent` thay vì exam package typed.
- `SecureNetworkTSEExamService` dùng dummy `tek_hash=test_hash` và soft-lock config trong một số path.

---

## 4. Kiến trúc mục tiêu

### 4.1. Nguyên tắc thiết kế

1. Server là nguồn sự thật.
2. Client không bao giờ nhận đáp án đúng.
3. Mỗi lần làm bài là một `attempt` độc lập, có token, deadline, trạng thái.
4. Đề thi là snapshot có version, không phụ thuộc câu hỏi đang bị sửa sau khi publish.
5. Autosave và final submit phải idempotent.
6. Tất cả state transition phải có audit log.
7. Không phá Final Submit hiện có trong giai đoạn chuyển đổi.

### 4.2. Phân tách trách nhiệm

| Module | Trách nhiệm |
|---|---|
| Teacher/Admin UI | Tạo ngân hàng câu hỏi, tạo đề, cấu hình kỳ thi, giao bài, xem kết quả |
| Server Exam API | Auth, authorization, package generation, attempt lifecycle, autosave, submit, grading |
| Parent TSE | Login, chọn exam, nhận attempt/package metadata, launch child, nhận final payload, submit server |
| Child JCEF | Render bài thi, thu thập câu trả lời, autosave local, viết final payload |
| Rust Lockdown | Lock OS, process scan, keyboard/screen protection, heartbeat |
| Database | Nguồn sự thật cho question/paper/exam/attempt/answer/result/audit |

---

## 5. Data model đề xuất

### 5.1. Nhóm question bank

```sql
question_banks(
  id,
  owner_id,
  organization_id,
  name,
  description,
  visibility,
  created_at,
  updated_at
)

question_bank_categories(
  id,
  bank_id,
  parent_id,
  name,
  description,
  sort_order
)

questions(
  id,
  bank_id,
  category_id,
  author_id,
  question_type,
  title,
  stem_json,
  difficulty,
  default_points,
  tags_json,
  status,
  version,
  created_at,
  updated_at
)

question_options(
  id,
  question_id,
  option_key,
  content_json,
  sort_order
)

question_answer_keys(
  question_id,
  version,
  answer_key_encrypted,
  grading_config_json,
  created_at
)
```

Ghi chú:

- `question_options` không nên chứa `is_correct` public.
- Đáp án đúng nên tách sang `question_answer_keys`, server-side only.
- Có thể giữ `content JSONB` hiện tại giai đoạn đầu, nhưng target nên rõ option/key/grading hơn.

### 5.2. Nhóm paper/exam

```sql
exam_papers(
  id,
  owner_id,
  title,
  description,
  version,
  status,
  total_points,
  shuffle_questions,
  shuffle_options,
  created_at,
  published_at
)

exam_paper_questions(
  id,
  paper_id,
  question_id,
  question_version,
  section_id,
  sort_order,
  points,
  required,
  question_snapshot_json,
  grading_policy_json
)

exams(
  id,
  creator_id,
  paper_id,
  classroom_id,
  title,
  description,
  duration_mins,
  open_at,
  close_at,
  status,
  security_policy_id,
  attempt_policy_json,
  created_at,
  updated_at
)

exam_security_policies(
  id,
  name,
  config_json,
  require_password,
  password_hash,
  created_at,
  updated_at
)

exam_assignments(
  id,
  exam_id,
  target_type,
  target_id,
  open_at,
  close_at,
  attempts_allowed,
  extra_time_mins,
  created_at
)
```

### 5.3. Nhóm attempt/submission/result

```sql
exam_attempts(
  id,
  exam_id,
  paper_id,
  user_id,
  attempt_no,
  status,
  started_at,
  deadline_at,
  submitted_at,
  closed_at,
  session_token_hash,
  client_nonce,
  question_order_json,
  option_order_json,
  package_hash,
  client_info_json,
  last_seen_at,
  violation_count,
  trust_score_avg,
  tek_hash,
  created_at
)

exam_attempt_answers(
  id,
  attempt_id,
  question_id,
  answer_data_json,
  answer_hash,
  source,
  version,
  time_spent_sec,
  change_count,
  updated_at,
  UNIQUE(attempt_id, question_id)
)

exam_autosaves(
  id,
  attempt_id,
  autosave_version,
  payload_hash,
  answer_count,
  source,
  created_at
)

exam_submissions(
  id,
  attempt_id,
  payload_hash,
  saved_answer_count,
  final_payload_used,
  status,
  received_at,
  client_build,
  server_message,
  UNIQUE(attempt_id)
)

exam_results(
  id,
  attempt_id,
  total_score,
  max_score,
  percentage,
  grade_status,
  graded_by,
  graded_at,
  feedback_json,
  published_at
)

exam_audit_logs(
  id,
  actor_id,
  exam_id,
  attempt_id,
  event_type,
  severity,
  details_json,
  created_at
)
```

Migration tương thích:

- Không drop bảng cũ.
- Tạo bảng mới trước.
- Backfill mỗi `exams` hiện tại thành một `exam_papers` legacy.
- Backfill `questions.exam_id` thành `exam_paper_questions`.
- Map `exam_sessions` cũ sang `exam_attempts`.
- Giữ `exam_answers` cũ cho backward compatibility, rồi dần chuyển sang `exam_attempt_answers`.

---

## 6. Protocol/API đề xuất

### 6.1. Quy tắc chung

- Bỏ dần payload `examId|password`.
- Dùng JSON DTO có `protocolVersion`.
- Packet action vẫn có thể giữ trong Socket hiện tại để không rewrite network.
- Mỗi response có `requestId`, `success`, `errorCode`, `message`.
- Idempotency key cho `EXAM_SUBMIT` và `EXAM_AUTOSAVE`.

### 6.2. Teacher/Admin packets

| Packet | Mục đích |
|---|---|
| `QUESTION_BANK_CREATE` | Tạo ngân hàng câu hỏi |
| `QUESTION_CREATE` | Tạo câu hỏi |
| `QUESTION_UPDATE` | Sửa câu hỏi draft |
| `QUESTION_PUBLISH_VERSION` | Publish version câu hỏi |
| `EXAM_PAPER_CREATE` | Tạo đề |
| `EXAM_PAPER_ADD_QUESTION` | Gắn câu hỏi vào đề |
| `EXAM_PAPER_PUBLISH` | Chốt snapshot đề |
| `EXAM_CREATE` | Tạo kỳ thi từ paper |
| `EXAM_ASSIGN` | Giao kỳ thi cho lớp/user |
| `EXAM_PUBLISH` | Mở kỳ thi |
| `EXAM_RESULTS_GET` | Lấy kết quả |

### 6.3. Student/TSE packets

```json
{
  "action": "EXAM_START_REQUEST",
  "protocolVersion": 1,
  "requestId": "uuid",
  "data": {
    "examId": 123,
    "password": "optional",
    "captcha": "optional",
    "clientBuild": "tutorhub_tse_v1",
    "deviceInfo": {},
    "lockdownCapabilities": {}
  }
}
```

Response:

```json
{
  "action": "EXAM_START_RESPONSE",
  "success": true,
  "data": {
    "attemptId": "uuid",
    "sessionToken": "one-time-token",
    "examId": 123,
    "deadlineAt": "2026-06-17T10:30:00+07:00",
    "durationMinutes": 40,
    "packageHash": "sha256",
    "packageMode": "JSON_PACKAGE_V1",
    "lockdownConfig": {}
  }
}
```

Core student packets:

| Packet | Mục đích |
|---|---|
| `TSE_GET_CONFIG_LIST` | Lấy danh sách exam được phép tham gia |
| `EXAM_START_REQUEST` | Tạo attempt và nhận token |
| `EXAM_PACKAGE_REQUEST` | Lấy package câu hỏi theo attempt |
| `EXAM_AUTOSAVE` | Đồng bộ autosave server |
| `EXAM_SUBMIT` | Nộp final payload |
| `EXAM_HEARTBEAT` | Báo còn sống, trạng thái lockdown |
| `EXAM_VIOLATION` | Gửi sự kiện chống gian lận |
| `EXAM_RECOVERY_REQUEST` | Khôi phục attempt khi crash |

---

## 7. Exam package

Target package nên là JSON có chữ ký/hash, không phải HTML string tạo trực tiếp trong `ClientHandler`.

```json
{
  "packageVersion": 1,
  "attemptId": "uuid",
  "examId": 123,
  "paperId": 55,
  "paperVersion": 3,
  "examTitle": "Kiểm tra toán",
  "durationMinutes": 40,
  "deadlineAt": "...",
  "questions": [
    {
      "questionId": 1,
      "type": "MCQ_SINGLE",
      "points": 1,
      "stem": {},
      "options": [
        { "optionKey": "A", "content": {} }
      ]
    }
  ],
  "questionOrder": [1, 2, 3],
  "optionOrder": { "1": ["B", "A", "C", "D"] },
  "uiPolicy": {},
  "securityPolicy": {},
  "packageHash": "sha256",
  "serverSignature": "optional"
}
```

Không được có:

- Đáp án đúng.
- `is_correct`.
- Scoring template đủ để suy ra đáp án đúng.
- Secret key.

Giai đoạn chuyển tiếp:

- Vẫn có thể trả `htmlContent`.
- Nhưng đưa render HTML ra `ExamHtmlRenderer`.
- Sau đó chuyển dần sang `ExamPackageRenderer` trong JCEF.

---

## 8. Luồng vận hành đầy đủ

### 8.1. Luồng giáo viên/admin

1. Tạo question bank.
2. Tạo category/tag/difficulty.
3. Tạo câu hỏi ở trạng thái draft.
4. Preview câu hỏi.
5. Publish version câu hỏi.
6. Tạo exam paper.
7. Chọn câu hỏi hoặc random rule từ bank.
8. Chốt snapshot paper.
9. Tạo exam instance từ paper.
10. Cấu hình thời gian, duration, số lần thi, password/token/security policy.
11. Assign cho lớp/user.
12. Publish exam.
13. Theo dõi attempts, autosave, violations, submitted.
14. Chấm tự động/trắc nghiệm.
15. Chấm tay/tự luận.
16. Publish result.

### 8.2. Luồng thí sinh

1. Mở TSE.
2. Đăng nhập.
3. Lấy danh sách kỳ thi được assign.
4. Chọn kỳ thi.
5. Nhập captcha/password nếu exam yêu cầu.
6. Server tạo attempt và session token.
7. Parent launch child + Rust lockdown.
8. Child load exam package.
9. Làm bài, autosave local/server.
10. Final Submit ghi `submit_payload.enc`.
11. Parent gửi payload lên server.
12. Server nhận idempotent, save answers, close attempt.
13. Rust unlock, app thoát sạch.
14. Kết quả hiển thị khi giáo viên publish.

---

## 9. Autosave và Final Submit

### 9.1. Giữ phần đang tốt

Giữ cơ chế:

- Child viết `autosave_payload.enc`.
- Child viết `submit_payload.enc`.
- Parent ưu tiên `submit_payload.enc`.
- Fallback autosave chỉ dùng khi có crash/lỗi rõ ràng.

### 9.2. Cần nâng cấp

- `payloadHash` để chống duplicate/mismatch.
- `attemptId` thay `sessionId` integer.
- `autosaveVersion` tăng dần.
- Server accept duplicate final submit nếu cùng `payloadHash`.
- Server từ chối submit khác hash nếu attempt đã `SUBMITTED`.
- Local autosave cần encrypted và bind theo attempt token.

### 9.3. State machine đề xuất

```text
ASSIGNED
  -> STARTING
  -> IN_PROGRESS
  -> SUBMITTING
  -> SUBMITTED
  -> AUTO_GRADED
  -> MANUAL_GRADING
  -> GRADED
  -> RESULT_PUBLISHED
```

State lỗi:

```text
EXPIRED
ABANDONED
LOCKDOWN_FAILED
SUBMIT_FAILED_RECOVERABLE
SUBMIT_FAILED_FINAL
INVALIDATED
```

---

## 10. Grading và result

### 10.1. Chấm tự động

Áp dụng cho:

- MCQ single.
- MCQ multiple.
- True/false.
- Short answer có exact/regex/fuzzy rules.

Quy tắc:

- Chấm server-side.
- Dùng `question_answer_keys`.
- Lưu per-question score và feedback.
- Không publish result ngay nếu teacher chưa cho phép.

### 10.2. Chấm thủ công

Áp dụng cho:

- Essay.
- Upload/file answer.
- Bài có rubric.

Cần UI:

- Danh sách bài cần chấm.
- Xem answer + audit summary.
- Rubric score.
- Comment/feedback.
- Publish result.

---

## 11. UI/UX đề xuất

### 11.1. Teacher/Admin

Màn hình nên tách:

- Question Banks.
- Papers.
- Exams.
- Assignments.
- Attempts.
- Results.
- Security/Audit.

Không nhồi tất cả vào một tab.

### 11.2. Student/TSE

Màn hình nên có:

- Login.
- Exam list.
- Exam detail/pre-check.
- Secure exam screen.
- Submit confirmation.
- Result waiting/status.

Trong exam:

- Question navigator.
- Timer rõ.
- Save state rõ: "Đã lưu lúc ..."
- Warning khi mất mạng.
- Submit flow một nút chính, confirm rõ.

### 11.3. Admin monitoring

Nên làm sau MVP:

- Live attempts.
- Last seen.
- Violation count.
- Trust score.
- Lockdown status.
- Submit status.

---

## 12. Security & integrity

### 12.1. Cần sửa dần

- Không lưu password exam plain text trong `security_config`.
- Không hardcode userId/token sau login.
- Không gửi answer key xuống client.
- Không để child quyết định score.
- Không tin payload nếu thiếu attempt token/hash.

### 12.2. Cơ chế nên có

- `sessionToken` random, hash server-side.
- Token bind theo user + exam + attempt + device info.
- `packageHash` và `payloadHash`.
- Audit log mọi state transition.
- Rate limit start/submit.
- Deadline server-side.
- Recovery path có xác thực.

### 12.3. Học từ SEB nhưng không copy

Nên học:

- Config key / exam key tư duy integrity.
- Start session / stop session rõ ràng.
- Kiosk/browser split.
- Client log/debug có cấu trúc.

Không nên copy:

- Source code SEB.
- UI/assets/branding.
- Kiến trúc phụ thuộc LMS ngoài nếu TutorHub muốn tự làm LMS.

---

## 13. So sánh hiện tại và mục tiêu

| Mảng | Hiện tại | Mục tiêu |
|---|---|---|
| Câu hỏi | `questions.exam_id` | `question_banks` + versioned questions |
| Đề thi | Chưa có paper | `exam_papers` snapshot |
| Kỳ thi | `exams` đơn giản | `exams` + assignment + security policy |
| Lượt thi | `exam_sessions` unique user/exam | `exam_attempts` nhiều attempt, tokenized |
| Start | `examId|password` | JSON DTO + token + package hash |
| Package | HTML string | JSON package signed/hash, renderer local |
| Autosave | File local | Local encrypted + server autosave |
| Submit | Save answers, set submitted | Idempotent final submission + grading pipeline |
| Result | Chưa rõ | `exam_results`, publish workflow |
| Audit | anticheat basic | `exam_audit_logs` + anticheat event stream |

---

## 14. Roadmap 20 phase

### Phase 0 - Baseline freeze & backup

- Chụp schema hiện tại.
- Export sample data.
- Ghi lại packet đang dùng.
- Không sửa UI.

### Phase 1 - Schema migration foundation

- Tạo bảng migration/version.
- Chuẩn hóa một nơi quản lý exam schema.
- Thêm bảng mới nhưng chưa chuyển logic.

### Phase 2 - Extract server exam module

- Tách `ExamPacketHandler`.
- Tách `ExamStartService`, `ExamSubmissionService`, `ExamPackageService`.
- `ClientHandler` chỉ route packet.

### Phase 3 - DTO protocol v1

- Thêm JSON DTO cho `EXAM_START_REQUEST/RESPONSE`.
- Giữ backward compatibility với payload cũ.

### Phase 4 - Question bank MVP

- CRUD bank/category/question draft.
- Không cần UI đẹp vội.

### Phase 5 - Paper builder MVP

- Tạo `exam_papers`.
- Add questions.
- Snapshot version.

### Phase 6 - Exam assignment

- Assign exam cho user/class.
- `TSE_GET_CONFIG_LIST` chỉ trả exam user được phép làm.

### Phase 7 - Attempt lifecycle

- Thêm `exam_attempts`.
- Token, deadline, status.
- Map session cũ sang attempt.

### Phase 8 - Exam package v1

- Generate package không chứa đáp án.
- Hash package.
- Short-term vẫn render HTML từ package.

### Phase 9 - Autosave server sync

- `EXAM_AUTOSAVE`.
- Local encrypted cache.
- Recovery from autosave.

### Phase 10 - Final submit idempotency

- `payloadHash`.
- Duplicate safe submit.
- Strict state transition.

### Phase 11 - Auto grading

- MCQ/true-false grading.
- Store per-question score.

### Phase 12 - Result model

- Result summary.
- Publish/unpublish result.

### Phase 13 - Teacher result UI

- Attempts list.
- Export CSV.
- View answer details.

### Phase 14 - Manual grading

- Essay grading UI.
- Rubric/comment.

### Phase 15 - Monitoring dashboard

- Live status, heartbeat, violation.

### Phase 16 - Integrity hardening

- TEK/package token binding.
- Client build validation.
- Security policy hash.

### Phase 17 - Import/export

- CSV/QTI import prototype.
- Không làm trước khi core stable.

### Phase 18 - Performance/indexing

- Index attempts, answers, audit.
- Pagination.
- Query profiling.

### Phase 19 - Observability

- Structured logs.
- Request correlation id.
- Submit failure diagnostics.

### Phase 20 - Release hardening

- Migration rehearsal.
- VM acceptance.
- Security checklist.
- Docs/operator runbook.

---

## 15. Phase đầu tiên nên làm ngay

Nên làm **Phase 0 + Phase 1 nhỏ** trước:

1. Tạo tài liệu schema hiện tại.
2. Tạo migration table.
3. Chọn một nơi duy nhất quản lý schema exam.
4. Thêm bảng mới ở trạng thái additive.
5. Không đổi flow submit.
6. Không đổi Rust.
7. Không đổi Quick Settings.
8. Không đổi Child final payload.

Không nên làm ngay:

- Không viết lại toàn bộ UI tạo đề.
- Không đổi sang REST/gRPC ngay.
- Không bỏ Socket Packet hiện tại.
- Không sửa Final Submit khi chưa có test baseline.
- Không drop bảng cũ.
- Không đổi Secure Exam child flow.

---

## 16. File nên sửa trong phase triển khai đầu tiên

Dự kiến khi bắt đầu code, chỉ nên đụng:

- `src/main/java/com/mycompany/tutorhub_enterprise/server/DatabaseManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/db/ExamDatabaseManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/ExamDAO.java`
- Thêm package `server/exam/...` cho service/DTO nếu cần
- Thêm docs migration trong `docs/`

Không đụng:

- Rust lockdown.
- Quick Settings.
- `TSEExamChildClient.java` submit path.
- `TSEProductionParentSubmitLabLauncher.java` final submit path, trừ khi Phase 10.

---

## 17. Checklist trả lời 17 câu hỏi nghiên cứu

1. Đã đọc docs nội bộ và xác định phần còn đúng/sai.
2. Đã nghiên cứu SEB v3.10.1 theo hướng học kiến trúc, không copy code.
3. Đã đối chiếu Moodle/Open edX/Canvas/QTI official docs.
4. Đã audit schema và source hiện tại.
5. Đã xác định TutorHub phải tự làm LMS/e-assessment server, không chỉ secure client.
6. Đã thiết kế flow giáo viên/admin.
7. Đã thiết kế flow thí sinh.
8. Đã đề xuất database mới.
9. Đã đề xuất packet/API.
10. Đã đề xuất exam package.
11. Đã đề xuất autosave/final submit.
12. Đã đề xuất grading/result.
13. Đã đề xuất UI/UX.
14. Đã đề xuất security/integrity.
15. Đã nêu migration/backward compatibility.
16. Đã chia roadmap 20 phase.
17. Đã chỉ rõ bước đầu tiên nên làm và những việc không nên làm ngay.

---

## 18. Kết luận

TSE hiện không nên bị xem là "chưa có gì". Phần secure shell, child submit và parent recovery là nền rất đáng giữ. Vấn đề nằm ở tầng nghiệp vụ kỳ thi: mô hình dữ liệu, attempt lifecycle, package, grading và result.

Hướng phát triển tốt nhất là biến TutorHub thành một LMS/e-assessment server nhỏ nhưng đúng kiến trúc, còn TSE là secure delivery client. Đi theo lộ trình additive migration sẽ giúp giữ phần đang chạy, tránh phá Final Submit, đồng thời mở đường để sau này đạt mức sản phẩm giống Moodle/Open edX/Canvas kết hợp SEB, nhưng có nhận diện và kiểm soát riêng của TutorHub.
