# Phase 6H: V2 Answer Draft Snapshot Contract - In-memory Only

## 1. Muc tieu phase

Phase 6H tao contract snapshot cho trang thai chon dap an V2 sau Phase 6G. Snapshot gom state `questionId -> selectedOptionId` thanh mot object/JSON an toan trong RAM de phase sau co the thiet ke autosave rieng.

## 2. Vi sao chua autosave that

Phase nay chi chot contract va validation. Chua autosave that vi autosave can them boundary rieng cho ma hoa, luu file, retry, cleanup, va regression voi Final Submit. Lam contract truoc giup tranh tron UI state, disk write va submit logic vao cung mot phase.

## 3. Snapshot contract

DTO moi:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2AnswerDraftSnapshot.java
```

Schema:

```text
snapshotVersion
flow = PAPER_START_V2
examId
paperId
attemptId
packageHash
questionCount
answeredCount
createdAt
updatedAt
answers
snapshotHash
```

## 4. Answer item schema

DTO moi:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2AnswerDraftItem.java
```

Schema:

```text
questionId
selectedOptionId
answeredAt
```

## 5. Validation selectedOption belongs to question

Service moi:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2AnswerDraftSnapshotService.java
```

Validation chinh:

- `examId` va `paperId` phai hop le.
- `questionCount` trong render model phai khop danh sach questions.
- `TSEV2AnswerSelectionState.totalQuestionCount` phai khop danh sach questions.
- `answeredCount` phai khop so answers.
- Moi `selectedOptionId` phai thuoc dung `questionId`.
- `packageHash` neu co thi khong duoc rong.

## 6. In-memory only guarantee

Phase 6H chi tao Java object va JSON string trong RAM. Khong co file writer, khong co network client, khong co backend call, khong tao draft file.

## 7. Out-of-scope: autosave/submit/scoring

Khong lam trong phase nay:

- Autosave that.
- `autosave_payload.enc`.
- `submit_payload.enc`.
- `EXAM_SUBMIT`.
- Backend submit.
- Final Submit.
- Rust.
- Quick Settings/Taskbar.
- Parent Bridge/JCEF Bridge.
- Tinh dung/sai, cham diem, render answer key.

## 8. Security validation

Snapshot khong chua:

```text
sessionToken
keyB64
AES key
SecretKey
plaintext JSON
isCorrect
answerKey
correctOption
grading_config
password
passwordHash
score
isCorrectAnswer
```

Service co blacklist validation de chan marker nhay cam trong metadata snapshot. `snapshotHash` dung SHA-256 tren canonical text cua snapshot, khong bao gom chinh truong hash.

## 9. Unit test result

Da chay:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -Dtest=TSEV2AnswerDraftSnapshotServiceTest test
```

Ket qua:

```text
BUILD SUCCESS
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

Coverage chinh:

- Empty selection -> answeredCount = 0.
- One selected answer -> one answer item.
- Change selected option -> chi giu option moi nhat.
- selectedOptionId khong thuoc question -> reject.
- JSON co examId/paperId/attemptId/packageHash/answeredCount/answers.
- JSON khong co sessionToken/keyB64/plaintext/isCorrect/answerKey/correctOption/password/passwordHash/score.
- snapshotHash on dinh voi cung input.
- Contract khong co autosave/submit/network markers.

## 10. Maven build result

Da chay:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install
```

Ket qua:

```text
BUILD SUCCESS
Tests run: 93, Failures: 0, Errors: 0, Skipped: 0
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

Ghi chu: `build_portable.ps1` chay Maven voi test skipped theo script san co. Full `mvn clean install` da chay rieng va pass 93 tests.

## 11.1. Security scan result

Da chay scan hep theo prompt tren cac file Phase 6H:

```text
TSEV2AnswerDraftSnapshot.java
TSEV2AnswerDraftItem.java
TSEV2AnswerDraftSnapshotService.java
TSEV2AnswerDraftSnapshotServiceTest.java
```

Ket qua phan loai:

- Khong co `FileOutputStream`.
- Khong co `Files.write`.
- Khong co `HttpClient`.
- Khong co production `Socket`.
- Khong co production `EXAM_SUBMIT`.
- Khong co production `submit_payload` / `autosave_payload`.
- Cac hit `sessiontoken`, `keyb64`, `plaintext`, `answerkey`, `correctoption`, `passwordhash` chi nam trong blacklist validation hoac negative tests.

## 12. Rui ro con lai

- Manual VM V2_DEBUG selection UI acceptance van pending tu Phase 6G.5.
- Legacy `run_input_test.bat --exam-id 3` van pending do VM-only.
- Snapshot moi chi la contract trong RAM; phase sau can thiet ke encryption/disk lifecycle rieng.
- Chua duoc go la autosave/submit implementation; Phase 6H chi la RAM contract.

## 13. Phase tiep theo de xuat

Sau khi VM gate pass, phase tiep theo nen la autosave draft design/implementation rieng, co encryption boundary, file lifecycle, retry strategy va regression voi Final Submit.
