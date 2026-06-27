# VM Smoke Test Execution Packet

Tài liệu này chứa hướng dẫn chi tiết để chạy VM Smoke Test kiểm tra tính năng Student Submit V2.

## Trạng Thái Hiện Tại
VM smoke execution: PENDING / NOT RUN - VM environment not available
run_input_test.bat: PENDING / NOT RUN - script not found or VM-only

## 1. Điều kiện máy VM
- Windows 10/11 x64.
- Đã cài đặt Java 24 (tương tự môi trường release).
- Không có phần mềm capture màn hình bị cấm (trừ khi test tính năng block).

## 2. Cách chạy portable build
- Copy thư mục `D:\Ban_sao_du_an\dist\TutorHubSecureExam` sang máy VM.
- Chạy `TutorHubSecureExam.exe` hoặc script khởi động tương đương.

## 3. Cách bật VM test-profile flags
- Có thể dùng JVM args: `-Dtse.v2.defaultStudentSubmitV2.enabled=true`
- Hoặc dùng file `v2_submit_vm_test_profile.sample.properties` chép vào thư mục config trên VM.

## 4. Xác nhận EXAM_SUBMIT default false -> legacy
- Khi KHÔNG bật cờ V2, nhấn Nộp Bài.
- Phải thấy request gửi đi đi qua `EXAM_SUBMIT` legacy path.
- Không có lỗi, hoạt động như bản cũ.

## 5. Xác nhận EXAM_SUBMIT VM flags true -> V2 bridge
- Khi bật cờ V2, nhấn Nộp Bài.
- Phải thấy adapter gọi vào `EXAM_SUBMIT_V2_*` bridge.

## 6. Xác nhận V2 đi đủ 7 stages
- Kiểm tra logs server: Preflight -> Materialize -> Submit Status -> Drafts -> Publication -> Final Status -> Handoff.

## 7. Xác nhận fallback pre-write
- Nếu tắt mạng trước khi nhấn nộp, fallback mechanism cho phép retry.

## 8. Xác nhận fallback post-write forbidden
- Nếu mất mạng sau khi đã materialize/publish, cấm fallback, bắt buộc gọi handoff logic để sync sau.

## 9. Kiểm tra không tạo submit_payload.enc
- Tìm trong thư mục `C:\Users\<User>\.tutorhub` hoặc thư mục cài đặt, đảm bảo không có file `submit_payload.enc` được dump bừa bãi.

## 10. Kiểm tra response không lộ payload/answers/score
- Dùng Wireshark hoặc proxy (nếu HTTP) hoặc đọc log, DTO trả về từ Bridge chỉ được có status, error code, không chứa list `answers` hay `score`.
