# TSE V2 Debug Scripts & Repo Hygiene Audit

Tài liệu này đánh giá (audit) các script Python tạm thời được tạo trong suốt các Phase 10, 11 và 12 nhằm mục đích patch lỗi compile, fix test suite, chỉnh sửa cấu trúc thư mục tự động, v.v.

Mục tiêu là cung cấp thông tin tổng quan, dọn dẹp repo mà không dùng lệnh `git clean`, để đảm bảo an toàn.

## Tóm tắt Audit
Tổng số debug scripts tìm thấy ở thư mục gốc: ~80 files.
Phần lớn các script được tạo ra tự động để phục vụ tác vụ sửa lỗi (code patching). Chúng không thuộc source code production hay test code chính thức của dự án.

## Nhóm Scripts theo mục đích

### 1. Nhóm Fix / Compile (fix_*.py)
**Mục đích**: Được dùng trong các phase cũ để sửa lỗi compile, cập nhật class name, fix lỗi syntax và Unicode.
**Gồm các file**: `fix_compile.py`, `fix_compile_services.py`, `fix_compile_services_2.py`, `fix_syntax.py`, `fix_mojibake_strings.py`, `fix_vietnamese.py`, `fix_unicode_cfr.py`, v.v.
**Khuyến nghị**: 
- **Review Later / Safe to delete manually after backup**. 
- Exclude from commit.

### 2. Nhóm Patch (patch_*.py, patch*.py)
**Mục đích**: Thay thế đoạn code nhỏ trong các service, ClientHandler, và test files. Đặc biệt nhiều script được dùng để chèn mock objects hoặc điều chỉnh logic assertion (như `PRE_WRITE_FAILURE`).
**Gồm các file**: `patch.py`, `patch3.py` ... `patch7.py`, `patch_actions.py`, `patch_ch.py`, `patch_test.py`, `patch_test7.py`, `patch_phase12.py`, v.v.
**Khuyến nghị**:
- **Keep temporarily** để làm bằng chứng về quá trình patch code.
- Mute trong `.gitignore` hoặc xoá thủ công nếu thấy không cần thiết nữa. 
- Exclude from commit.

### 3. Nhóm Replace (replace*.py)
**Mục đích**: Tương tự nhóm Patch, được dùng để tìm và thay thế chuỗi string hàng loạt (find and replace).
**Gồm các file**: `replace.py`, `replace2.py` ... `replace18.py`.
**Khuyến nghị**:
- **Safe to delete manually**. 
- Exclude from commit.

### 4. Nhóm Test & Recreate (create_*.py, recreate_*.py, regen_*.py)
**Mục đích**: Tạo boilerplate cho các class test mới, build các script mock data.
**Gồm các file**: `create_tests_11.py`, `recreate_tests.py`, `regen_tests.py`, v.v.
**Khuyến nghị**:
- **Keep temporarily** nếu cần tham khảo cách generate test class.
- Exclude from commit.

### 5. Nhóm Inspect / Extract / Inject (inject_*.py, extract_*.py, inspect_*.py)
**Mục đích**: Trích xuất code, inspect DOM/file tree hoặc inject handlers.
**Gồm các file**: `extract_vine.py`, `inject_rag.py`, `inject_exam_handler.py`, `inspect_bar.py`, v.v.
**Khuyến nghị**:
- **Safe to delete manually**.
- Exclude from commit.

## Phụ Lục A — Full Debug Script Filename Inventory
Danh sách đầy đủ các file đã được audit (N=81 files):
- add_jcodec.py
- app.py
- check_functions.py
- create_tests_11.py
- create_tests_11g.py
- diff_cases.py
- extract_vine.py
- find_panels.py
- fix_all_mojibake.py
- fix_brace.py
- fix_cfr_casts.py
- fix_ch.py
- fix_clienthandler.py
- fix_client_exam.py
- fix_compile.py
- fix_compile_services.py
- fix_compile_services_2.py
- fix_compile_services_3.py
- fix_compile_services_4.py
- fix_decompiled.py
- fix_duplicates.py
- fix_examcreator.py
- fix_examservice.py
- fix_examtab.py
- fix_examtaking.py
- fix_exam_creation.py
- fix_final.py
- fix_last.py
- fix_missing_functions.py
- fix_mojibake_strings.py
- fix_network_port.py
- fix_network_url.py
- fix_setup.py
- fix_sidebar.py
- fix_syntax.py
- fix_tests.py
- fix_tests_11.py
- fix_tests_11g.py
- fix_unicode_cfr.py
- fix_unicode_raw.py
- fix_vietnamese.py
- get_cases.py
- inject_clean.py
- inject_exam_handler.py
- inject_exam_handler2.py
- inject_exam_tab.py
- inject_extracted.py
- inject_rag.py
- inspect_bar.py
- patch.py
- patch3.py
- patch4.py
- patch5.py
- patch7.py
- patch_actions.py
- patch_actions2.py
- patch_ch.py
- patch_ch_11.py
- patch_ch_11d.py
- patch_ch_11g.py
- patch_ch_11k.py
- patch_ch_exam.py
- patch_clienthandler.py
- patch_test.py
- patch_test2.py
- patch_test3.py
- patch_test4.py
- patch_test5.py
- patch_test6.py
- patch_test7.py
- patch_tests_11.py
- patch_tests_11d.py
- patch_tests_11_v2.py
- recreate_tests.py
- regen_tests.py
- replace.py
- replace2.py ... replace18.py
- search_recovered.py
- test.py
- test_ws.py
- update_neon_config.py
- patch_phase12.py
- patch_phase12_tests.py
