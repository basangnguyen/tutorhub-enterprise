# Phase 6G.5: V2 Answer Selection State Regression Gate - VM/Legacy Verification

## 1. Muc tieu phase

Phase 6G.5 la regression gate sau Phase 6G. Muc tieu la xac minh answer selection state prototype trong Child `V2_DEBUG` van an toan truoc khi thiet ke autosave hoac submit.

Phase nay chi test, security scan va docs. Khong them tinh nang moi.

## 2. Phase 6G code verification

Da kiem tra cac file:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2AnswerSelectionState.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2ReadOnlyExamPanel.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEExamChildClient.java
src/test/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2AnswerSelectionStateTest.java
src/test/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2SelectionPanelTest.java
```

Ket qua:

- `TSEV2AnswerSelectionState` chi luu `questionId -> optionId` trong `LinkedHashMap` RAM.
- `TSEV2ReadOnlyExamPanel` chi cap nhat state khi click radio button.
- Progress text duoc cap nhat bang `Answered X / Y`.
- V2_DEBUG success branch hien thi tab `Safe Summary` va `Selection Prototype`.
- Khong tim thay file write trong state/panel Phase 6G.
- Khong tim thay network send trong state/panel Phase 6G.
- Khong co autosave trong state/panel Phase 6G.
- Khong co submit trong state/panel Phase 6G.
- Khong co scoring hoac dung/sai trong state/panel Phase 6G.

Ghi chu: `TSEExamChildClient.java` van co legacy submit/autosave path san co. Phase 6G chi dung V2_DEBUG branch de add tab `Selection Prototype`; khong thay doi legacy submit/start behavior trong gate nay.

## 3. Manual VM selection test result

Trang thai: **PENDING**.

Ly do:

- Session hien tai dang tren may `LENOVO 83DV`.
- `HypervisorPresent=True` khong du de coi day la VM test-safe.
- Theo rule du an, khong chay lockdown GUI / `run_input_test.bat` tren may vat ly.

Bang chung thay the:

- `TSEV2AnswerSelectionStateTest`: cover initial count, select, change same question, clear, snapshot.
- `TSEV2SelectionPanelTest`: cover panel render, question/option text, initial `Answered 0 / 2`, simulated radio click -> `Answered 1 / 2`, no Submit/Save/Finish button, and sensitive marker blocking.
- Targeted Phase 6G tests pass: 9 tests, 0 failures.

Khong bao manual PASS vi chua quan sat UI that trong VM.

## 4. Legacy run_input_test result

Trang thai: **PENDING**.

Ly do:

- `run_input_test.bat --exam-id 3` la legacy lockdown/JCEF GUI flow.
- Khong chay tren may Lenovo vat ly theo rule VM-only.

Can xac nhan trong VM:

- Legacy JCEF exam van mo/render binh thuong.
- Legacy `EXAM_START_REQUEST` khong bi anh huong.
- Legacy `EXAM_SUBMIT` khong bi anh huong.
- Final Submit khong bi anh huong.
- Rust/Quick Settings khong bi sua trong phase nay.

## 5. Security scan result

### Narrow Phase 6G scan

Da chay:

```powershell
findstr /I /N "keyB64 aesKey secretKey rawKey getEncoded sessionToken plaintextJson plaintext isCorrect answerKey correctOption passwordHash EXAM_SUBMIT submit_payload autosave_payload" `
src\main\java\com\mycompany\tutorhub_enterprise\client\exam\ui\TSEV2AnswerSelectionState.java `
src\main\java\com\mycompany\tutorhub_enterprise\client\exam\ui\TSEV2ReadOnlyExamPanel.java `
src\main\java\com\mycompany\tutorhub_enterprise\client\exam\ui\TSEExamChildClient.java `
src\test\java\com\mycompany\tutorhub_enterprise\client\exam\ui\TSEV2AnswerSelectionStateTest.java `
src\test\java\com\mycompany\tutorhub_enterprise\client\exam\ui\TSEV2SelectionPanelTest.java
```

Phan loai hit:

- `TSEV2ReadOnlyExamPanel.java`: cac token `iscorrect`, `answerkey`, `correctoption`, `sessiontoken`, `keyb64`, `passwordhash` chi nam trong blacklist render-blocking.
- `TSEV2AnswerSelectionStateTest.java`: cac token chi dung de assert snapshot khong chua secret marker.
- `TSEV2SelectionPanelTest.java`: `answerKey` la negative test de dam bao UI block sensitive marker.
- `TSEExamChildClient.java`: cac hit `keyB64`, `SUBMIT_PAYLOAD`, `submit_payload`, `autosave_payload` thuoc legacy child flow san co, khong phai code Phase 6G moi.

Scan them tren state/panel/test Phase 6G cho cac dau hieu file/network/submit:

```text
Files.
FileOutputStream
FileWriter
BufferedWriter
NetworkManager
sendPacket
Socket
HttpClient
EXAM_SUBMIT
submit_payload
autosave_payload
writeFinalPayload
writeAutosave
submitExam
Runtime.getRuntime
ProcessBuilder
```

Ket qua: khong co hit trong `TSEV2AnswerSelectionState`, `TSEV2ReadOnlyExamPanel` va cac test Phase 6G.

### Full repo scan

Da chay full scan theo prompt. Ket qua:

```text
TOTAL_FULL_SCAN_MATCHES=1880
```

Phan lon hit thuoc:

- legacy server/client code
- docs cu
- backup/decompiled/test helper
- dependency docs trong `node_modules`

Khong co bang chung Phase 6G tao leak moi.

## 6. Maven build result

Targeted Phase 6G test:

```text
mvn -Dtest=TSEV2AnswerSelectionStateTest,TSEV2SelectionPanelTest test
BUILD SUCCESS
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

Full build:

```text
mvn clean install
BUILD SUCCESS
Tests run: 82, Failures: 0, Errors: 0, Skipped: 0
```

Ghi chu:

- Co JVM/OpenJFX/SQLite warning san co.
- Co log negative IPC test `INVALID_NONCE_OR_HANDOFF`, nhung test pass va build success.

## 7. Portable build result

Da chay:

```powershell
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
```

Ket qua:

```text
Build Portable Folder completed successfully:
D:\Ban_sao_du_an\dist\TutorHubSecureExam
```

Script portable build co skip test trong buoc Maven noi bo, nhung full `mvn clean install` da duoc chay rieng va pass truoc do.

## 8. Bugs found

Khong phat hien bug code moi trong Phase 6G qua static verification, security scan hep, targeted tests va full Maven build.

Manual VM selection va legacy GUI regression van pending nen chua the ket luan UI runtime pass bang mat.

## 9. Bugs fixed

Khong sua code trong Phase 6G.5.

Chi tao/cap nhat tai lieu regression.

## 10. Rui ro con lai

- Chua co manual VM acceptance cho V2_DEBUG selection UI.
- Chua co legacy `run_input_test.bat --exam-id 3` acceptance trong VM sau Phase 6G.
- `TSEExamChildClient.java` van chua tach rieng legacy child flow va V2 debug flow thanh module rieng; viec nay co the lam code review ton suc hon o cac phase sau.
- Phase 6G state chua co persistence, autosave draft format, encryption boundary hoac submit contract.

## 11. Go/No-Go cho phase tiep theo

**No-Go cho autosave/submit implementation** cho den khi co VM manual acceptance that.

Cho phep di tiep mot phase thiet ke/analyze khong runtime neu can:

- Phase 6G.6: VM manual acceptance only.
- Phase 6H-design: Autosave draft design, khong code submit/autosave runtime.

Khuyen nghi truoc khi code autosave:

1. Chay VM V2_DEBUG selection UI va legacy `run_input_test.bat --exam-id 3`.
2. Xac nhan khong tao `submit_payload.enc`/`autosave_payload.enc` trong V2_DEBUG selection.
3. Chi khi VM gate pass moi sang autosave draft implementation.
