# Phase 6I: Local Encrypted Draft Autosave Prototype - Debug Only

## 1. Muc tieu phase

Phase 6I them prototype autosave cuc bo cho Child `V2_DEBUG` sau khi Phase 6H da co `TSEV2AnswerDraftSnapshot` trong RAM. Khi nguoi dung chon dap an trong Selection Prototype, he thong tao snapshot va ghi ban nhap da ma hoa ra disk.

Pham vi chi la local debug autosave. Khong submit, khong gui server, khong thay luong legacy student flow.

## 2. Vi sao debug-only

Autosave trong phase nay dung de kiem tra boundary tu state UI sang encrypted disk artifact. Key hien tai chi song trong RAM, nen chua dam bao crash-safe restore sau khi app tat hoac crash. Crash-safe recovery se can phase rieng ve key strategy va lifecycle.

## 3. Autosave encrypted file design

File encrypted:

```text
v2_answer_draft_autosave.enc
```

Design:

- Payload plaintext JSON chi duoc tao trong RAM.
- Payload duoc ma hoa bang AES-GCM voi IV sinh bang `SecureRandom`.
- File `.enc` chi chua wrapper encrypted bytes: algorithm, IV va ciphertext.
- Ghi atomic bang temp file chua encrypted bytes roi move replace.
- Khong ghi plaintext draft JSON ra disk.

Default debug directory:

```text
<UserHome>/.tutorhub-secure-exam/debug/v2-drafts/<attemptId-or-debug>/
```

## 4. Meta file schema

File metadata:

```text
v2_answer_draft_autosave.meta.json
```

Schema safe:

```text
schemaVersion
flow
examId
paperId
attemptId
packageHash
questionCount
answeredCount
snapshotHash
encryptedFileSha256
encFileName
createdAt
updatedAt
```

Meta khong chua:

```text
answers
selectedOptionId
sessionToken
keyB64
AES key
SecretKey value
plaintext JSON
isCorrect
answerKey
correctOption
password/passwordHash
score
```

## 5. Draft key strategy hien tai: RAM-only

`SecretKey` duoc generate khi Child `V2_DEBUG` khoi tao autosave handler va chi duoc giu trong RAM. Phase 6I khong:

- Ghi key ra disk.
- Ghi key vao meta.
- Log key.
- Truyen key qua command line.
- Truyen key qua environment variable.

## 6. In-memory snapshot -> encrypted autosave flow

Flow:

```text
User selects option
  -> TSEV2AnswerSelectionState update
  -> TSEV2AnswerDraftSnapshotService.createSnapshot(...)
  -> TSEV2LocalEncryptedDraftAutosaveService.saveEncryptedDraft(...)
  -> write v2_answer_draft_autosave.enc
  -> write v2_answer_draft_autosave.meta.json
  -> footer: Encrypted local draft autosave: saved
```

If save fails, UI keeps current RAM selection and shows a safe short error code:

```text
Encrypted local draft autosave: failed - VALIDATION_ERROR
Encrypted local draft autosave: failed - SECURITY_ERROR
Encrypted local draft autosave: failed - SAVE_FAILED
```

No full path, raw JSON, key, token, or plaintext is shown in UI.

## 7. Out-of-scope: submit/backend/scoring/crash-safe restore

Phase 6I does not implement:

- Submit.
- `EXAM_SUBMIT`.
- Backend/network send.
- `submit_payload.enc`.
- Legacy `EXAM_START_REQUEST` changes.
- Final Submit changes.
- Rust changes.
- Quick Settings/Taskbar changes.
- Parent Bridge/JCEF Bridge changes.
- STUDENT default V2 flow.
- Scoring or correct-answer calculation.
- Crash-safe restore after process restart.

## 8. Security validation

Created:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2LocalEncryptedDraftAutosaveService.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEV2DraftAutosaveMeta.java
```

Validation:

- Snapshot is validated by Phase 6H service before encrypt.
- Payload JSON is checked for blocked sensitive/scoring/network markers.
- Meta JSON is checked for answers, selected option ids, key/token/plaintext/scoring markers.
- Encrypted file SHA-256 is stored in safe meta and checked on load.
- Wrong key and tampered encrypted file fail safely.

Narrow security scan result:

```text
PASS with classified hits only.
```

Classified hits:

- `SecretKey` appears as RAM-only Java API type/parameter.
- `Files.write` appears only in atomic write helpers for encrypted bytes/safe meta, and in tests for tamper simulation.
- Sensitive marker strings appear only in validation blacklists or negative tests.
- No `HttpClient`, backend send, production `Socket`, `EXAM_SUBMIT`, or `submit_payload` implementation appears in the Phase 6I files.

## 9. Unit test result

Targeted Phase 6I test command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" "-Dtest=TSEV2LocalEncryptedDraftAutosaveServiceTest,TSEV2SelectionPanelTest" test
```

Result:

```text
BUILD SUCCESS
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

Coverage:

- Save creates `.enc` and `.meta.json`.
- `.enc` does not contain readable `questionId`, `selectedOptionId`, or answers JSON.
- `.meta.json` excludes answers, selected option ids, key/token/plaintext/scoring markers.
- Correct RAM key loads the snapshot and preserves snapshot hash.
- Wrong key fails safely.
- Tampered `.enc` fails hash validation.
- Invalid selected option is rejected before autosave.
- No network/backend/submit markers are written.
- Panel click invokes autosave handler and shows safe saved/failure status.

## 10. Maven build result

```text
BUILD SUCCESS
Tests run: 104, Failures: 0, Errors: 0, Skipped: 0
```

## 11. Portable build result

```text
Build Portable Folder completed successfully:
D:\Ban_sao_du_an\dist\TutorHubSecureExam
```

Note: `build_portable.ps1` runs Maven with tests skipped as part of the existing packaging script. Full `mvn clean install` was run separately and passed 104 tests.

## 12. Rui ro con lai

- VM manual legacy `run_input_test.bat --exam-id 3` remains pending because lockdown GUI must only run in a VM test-safe environment.
- Autosave restore after app restart is intentionally not supported because draft key is RAM-only.
- The `.enc` file contains ciphertext wrapper metadata such as algorithm and IV, not plaintext answers.
- Phase 6I is still debug-only and must not be treated as production answer persistence.

## 13. Phase tiep theo de xuat

Recommended next phase after VM gate:

```text
Phase 6I.5: Local Encrypted Draft Autosave Regression Gate - VM/Legacy Verification
```

Scope:

- Verify V2_DEBUG selection autosave on VM.
- Verify legacy `run_input_test.bat --exam-id 3`.
- Confirm Final Submit and Rust are unaffected.
- Decide whether crash-safe restore/key strategy can start in a separate phase.
