# TSE V2 Submit VM Activation Profile

Tài liệu này hướng dẫn cách kích hoạt an toàn luồng Submit V2 Mới trên môi trường Virtual Machine (VM) / Môi trường Test mà không làm thay đổi các thiết lập mặc định của production.

## Chú Ý Quan Trọng
- Tuyệt đối **không** thay đổi các tham số này trực tiếp vào file `application.properties` ở production.
- Production environment luôn luôn cần duy trì `tse.v2.defaultStudentSubmitV2.enabled=false`.
- Profile VM này sử dụng JVM arguments (hoặc runtime args) để override config khi chạy ứng dụng.

## Các Flags Cần Bật Trong VM/Test

Để bật toàn bộ V2 Pipeline và Legacy Fallback Guard, vui lòng cung cấp các flag sau khi khởi động ứng dụng:

```properties
tse.v2.defaultStudentSubmitV2.enabled=true
tse.v2.studentSubmitRuntimeAdapter.enabled=true
tse.v2.studentSubmitV2ExecutionBridge.enabled=true
tse.v2.studentSubmitLegacyFallbackRuntimeGuard.enabled=true
tse.v2.studentSubmitIntegrationRegressionGate.enabled=true
```

## Cách Sử Dụng
- Xem file cấu hình mẫu `v2_submit_vm_test_profile.sample.properties` để có tham chiếu.
- Sử dụng kèm kịch bản Smoke Test tại `tse_v2_portable_exam_submit_smoke_test.md` để xác nhận tính toàn vẹn của hệ thống trước và sau khi bật flag.
