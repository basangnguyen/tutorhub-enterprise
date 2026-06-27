# Debug Script Cleanup Policy

Tài liệu này xác định chính sách xử lý 81 debug scripts (Python) đã được sử dụng trong suốt quá trình triển khai Student Submit V2 từ Phase 10 đến Phase 13.

## Tình Trạng Hiện Tại
- **Hành động thực tế**: CHƯA XÓA.
- **Trạng thái Git**: KHÔNG XÓA, KHÔNG GIT CLEAN, KHÔNG ADD, KHÔNG COMMIT.

## Phân Loại Policy Xử Lý (Tương Lai)

Các script sẽ được xử lý vào một Phase Cleanup chuyên biệt sau này với 4 mức ưu tiên khuyến nghị:

1. **KEEP_TEMPORARILY**: 
   - Các script `patch_*` liên quan đến Phase 13 hoặc fix test mới nhất.
   - Lý do: Đề phòng cần patch lại nếu test fail.

2. **SAFE_TO_ARCHIVE**:
   - Các script generate code, mock data, audit file như `create_*` hoặc script tạo docs.
   - Hành động: Đề xuất di chuyển vào `tools/debug_scripts_archive/`.

3. **REVIEW_BEFORE_DELETE**:
   - Các script `fix_duplicates_*.py` hoặc script fix logic. Có thể chứa logic phức tạp.
   - Cần review lại nội dung regex trước khi vứt bỏ.

4. **DO_NOT_COMMIT**:
   - TẤT CẢ debug scripts không bao giờ được phép nằm trong lịch sử commit của repo. Phải loại trừ bằng `.gitignore` hoặc xoá thủ công trước khi push lên nhánh.
