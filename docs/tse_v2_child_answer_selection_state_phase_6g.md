# Phase 6G: V2 Answer Selection State Prototype - Debug Only

## 1. Muc tieu phase

Phase 6G bo sung kha nang chon dap an cuc bo trong Child `V2_DEBUG` sau Phase 6F. UI V2 da render duoc de thi an toan, nay co the chon option tren man hinh va cap nhat tien do:

```text
Answered X / Y
```

Pham vi chi la debug prototype trong RAM, khong dua vao luong thi production/mac dinh cua hoc sinh.

## 2. Vi sao chi la local state prototype

Giai doan nay dung de kiem tra rang Child V2 co the gan du lieu cau hoi/option vao UI Swing va quan ly lua chon cua thi sinh o muc UI. Viec autosave, ma hoa payload bai lam, submit backend va dong bo attempt se duoc thiet ke rieng sau khi state selection on dinh.

## 3. State model design

Da tao:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2AnswerSelectionState.java
```

State toi thieu:

```text
questionId -> selectedOptionId
```

API:

```text
selectOption(int questionId, int optionId)
getSelectedOption(int questionId)
clearSelection(int questionId)
getAnsweredCount()
getTotalQuestionCount()
snapshot()
```

State khong luu:

```text
isCorrect
answerKey
correctOption
score
sessionToken
keyB64
```

## 4. UI selection prototype

Da cap nhat:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2ReadOnlyExamPanel.java
```

Thay doi chinh:

- Radio button duoc bat cho mode debug selection.
- Khi click option, `TSEV2AnswerSelectionState` cap nhat trong RAM.
- Footer hien thi `Answered X / Y`.
- Footer van hien thi canh bao: `Debug selection only. Autosave and submit are disabled.`
- Khong them nut Submit, Save hoac Finish.
- Tieu de UI doi thanh `V2 Package Render - Selection State Prototype`.

Ten class duoc giu lai de giam diff va khong lam vo cac phase truoc.

## 5. TSEExamChildClient integration

Trong `V2_DEBUG` branch:

- Neu load/decrypt/parse fail: van hien thi Safe Summary error.
- Neu success: hien thi `JTabbedPane`:
  - Tab 1: `Safe Summary`
  - Tab 2: `Selection Prototype`
- Khong mo legacy JCEF exam trong V2_DEBUG.
- Khong thay doi legacy `EXAM_START_REQUEST`.
- Khong thay doi legacy `EXAM_SUBMIT`.

## 6. Out of scope

Phase 6G khong lam:

- Autosave.
- Submit.
- Ghi file bai lam.
- Tao `submit_payload.enc`.
- Tao `autosave_payload.enc`.
- Goi `EXAM_SUBMIT`.
- Tinh diem dung/sai.
- Render `isCorrect`, `answerKey`, `correctOption`.
- Sua Rust.
- Sua Quick Settings/Taskbar.
- Sua Parent Bridge/JCEF Bridge.
- Dua V2 vao STUDENT flow mac dinh.

## 7. Security validation

Truoc khi render selection UI, panel kiem tra:

- Render model khong null.
- Questions khong null/rong.
- Options khong null.
- Text render khong chua cac marker nhay cam:
  - `isCorrect`
  - `answerKey`
  - `correctOption`
  - `sessionToken`
  - `keyB64`
  - `password`
  - `passwordHash`

Neu vi pham, panel khong render selection UI va chi hien thi error an toan.

Security search da chay theo prompt tren toan repo. Ket qua co nhieu match trong legacy code, docs cu, test helper va thu muc dependency, khong sua bua vi khong thuoc Phase 6G.

Scan hep tren cac file Phase 6G cho thay:

- Khong co call `EXAM_SUBMIT` trong code moi.
- Khong co code tao `submit_payload.enc`.
- Khong co code tao `autosave_payload.enc`.
- Khong co file write/network call trong state/panel moi.
- Cac marker nhay cam trong `TSEV2ReadOnlyExamPanel` chi la danh sach token dung de chan render.
- Test co dung chuoi `answerKey` co chu dich de xac nhan UI khong leak marker nay.

## 8. Unit test result

Da tao:

```text
src/test/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2AnswerSelectionStateTest.java
src/test/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2SelectionPanelTest.java
```

Phase-specific test:

```text
mvn -Dtest=TSEV2AnswerSelectionStateTest,TSEV2SelectionPanelTest test
BUILD SUCCESS
Tests run: 9, Failures: 0, Errors: 0
```

Coverage chinh:

- Initial answered count = 0.
- Select option tang answered count.
- Doi option cung question khong tang dem them.
- Clear selection giam answered count.
- Snapshot chi chua question/option ids.
- Panel render cau hoi/option.
- Click radio cap nhat `Answered 1 / Y`.
- Khong co Submit/Save/Finish button.
- Marker nhay cam bi chan truoc khi render.

## 9. Manual/debug test result

Chua chay `run_input_test.bat` trong session nay vi day la luong lockdown GUI va AGENTS.md yeu cau chi test tren VM test-safe. Current docs truoc do cung ghi nhan session la may LENOVO vat ly, khong phai VM test-safe.

Manual VM test can chay sau:

```powershell
cd D:\Ban_sao_du_an\dist\TutorHubSecureExam
.\run_input_test.bat --exam-id 3
```

## 10. Maven build result

Da chay:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install
```

Ket qua:

```text
BUILD SUCCESS
Tests run: 82, Failures: 0, Errors: 0
```

## 11. Portable build result

Da chay:

```powershell
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
```

Ket qua:

```text
Build Portable Folder completed successfully:
D:\Ban_sao_du_an\dist\TutorHubSecureExam
```

## 12. Legacy run_input_test result

Chua chay trong session nay do rule VM-only. Trang thai: pending VM acceptance.

Can xac nhan tren VM:

- Legacy JCEF exam van mo/render binh thuong.
- Legacy `EXAM_START_REQUEST` khong bi anh huong.
- Legacy `EXAM_SUBMIT` khong bi anh huong.
- Final Submit khong bi anh huong.
- Rust/Quick Settings khong bi sua trong Phase 6G.

## 13. Rui ro con lai

- UI selection hien tai chi la Swing prototype, chua co state persistence.
- Chua co autosave draft format, encryption format hoac submit contract cho V2.
- Chua co VM manual acceptance cho click selection tren Child V2_DEBUG.
- `TSEV2ReadOnlyExamPanel` giu ten cu de giam diff; co the doi ten sang selection-specific class o phase sau neu muon sach kien truc hon.

## 14. Phase tiep theo de xuat

Nen lam mot trong hai huong:

1. **Phase 6G.5: Answer State Regression Gate**
   - Test manual tren VM.
   - Them test edge cases cho model co question/options loi.
   - Xac nhan khong leak secret bang security scan.

2. **Phase 6H: Autosave Draft Design - No Submit Yet**
   - Thiet ke format draft local.
   - Thiet ke encryption boundary.
   - Chua goi backend submit cho den khi co regression gate rieng.
