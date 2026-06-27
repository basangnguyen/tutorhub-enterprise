# Phase 6J: Local Draft Restore Prototype - Debug Only

## 1. Muc tieu phase

Phase 6J them restore local encrypted draft cho Child `V2_DEBUG` sau Phase 6I. He thong co the doc:

```text
v2_answer_draft_autosave.enc
v2_answer_draft_autosave.meta.json
```

sau do decrypt bang `draftKey` dang nam trong RAM, validate snapshot va apply selection state vao UI debug.

## 2. Vi sao debug-only

Phase nay chi chung minh contract restore local cho debug flow. No khong thay doi luong thi production/mac dinh cua hoc sinh, khong submit, khong gui backend va khong cham diem.

## 3. Restore flow

```text
Child V2_DEBUG
  -> tao V2DebugDraftContext voi RAM-only draftKey
  -> render TSEV2ReadOnlyExamPanel
  -> neu .enc/.meta.json ton tai:
       -> validate safe meta
       -> verify encrypted file SHA-256
       -> decrypt AES-GCM bang RAM-only draftKey
       -> validate payload blacklist
       -> parse TSEV2AnswerDraftSnapshot
       -> validate snapshot safe
       -> validate snapshot match render model
       -> apply vao TSEV2AnswerSelectionState
       -> sync radio buttons va progress
```

UI status:

```text
Encrypted draft restored.
No encrypted draft found.
Restore failed - <safe errorCode>
```

## 4. Match validation

Restore chi duoc apply neu:

- `examId` match current render model.
- `paperId` match current render model.
- `attemptId` match neu ca snapshot va model deu co value.
- `packageHash` match neu ca snapshot va model deu co value.
- `questionCount` match.
- Moi `questionId` trong snapshot ton tai trong render model.
- Moi `selectedOptionId` thuoc dung `questionId`.

Neu mismatch thi throw:

```text
ERROR_DRAFT_CONTEXT_MISMATCH
```

## 5. Draft key strategy: RAM-only

`draftKey` van chi nam trong RAM. Phase 6J khong:

- Ghi key ra disk.
- Ghi key vao meta.
- Log key.
- Truyen key qua command line.
- Truyen key qua environment variable.

## 6. In-same-session restore limitation

Restore chi co y nghia khi cung debug context con giu `draftKey` trong RAM. Neu app crash/restart that, key moi se khong decrypt duoc draft cu. Key persistence va crash-safe restore la phase rieng, chua nam trong 6J.

## 7. Out-of-scope

Phase 6J khong lam:

- Submit.
- `EXAM_SUBMIT`.
- Backend/network send.
- Legacy `EXAM_START_REQUEST`.
- Final Submit.
- Rust.
- Quick Settings/Taskbar.
- Parent Bridge/JCEF Bridge.
- STUDENT default V2 flow.
- Scoring/correct answer.
- Key persistence.
- Plaintext draft JSON on disk.

## 8. Security validation

Error code moi:

```text
ERROR_DRAFT_CONTEXT_MISMATCH
ERROR_DRAFT_DECRYPT_FAILED
ERROR_DRAFT_HASH_MISMATCH
ERROR_DRAFT_META_UNSAFE
ERROR_DRAFT_PAYLOAD_UNSAFE
```

Narrow security scan result:

```text
PASS with classified hits only.
```

Classified hits:

- `SecretKey` appears only as Java API type/parameter for RAM-only key.
- `Files.write` appears only in encrypted/safe-meta write path or negative tests for tamper/unsafe data.
- Sensitive strings appear only in blacklist validation or negative tests.
- No production `HttpClient`, backend send, `Socket`, `EXAM_SUBMIT`, or `submit_payload` implementation was added in Phase 6J files.

## 9. Unit test result

Targeted command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" "-Dtest=TSEV2LocalEncryptedDraftRestoreTest,TSEV2SelectionPanelTest" test
```

Result:

```text
BUILD SUCCESS
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
```

Coverage:

- Save then restore with correct key.
- Apply restore into selection state.
- Panel restore reflects selected option and progress.
- Wrong key fails safely.
- Tampered `.enc` fails safely.
- Unsafe meta with `answers` / `selectedOptionId` is rejected.
- Unsafe payload markers are rejected.
- Package hash mismatch is rejected.
- Option belonging mismatch is rejected.
- Restore does not create submit/autosave legacy payload files.
- Restore does not call network/backend markers.

## 10. Maven build result

```text
BUILD SUCCESS
Tests run: 117, Failures: 0, Errors: 0, Skipped: 0
```

## 11. Portable build result

```text
Build Portable Folder completed successfully:
D:\Ban_sao_du_an\dist\TutorHubSecureExam
```

Note: `build_portable.ps1` still skips tests in its internal Maven call. Full `mvn clean install` was run separately and passed 117 tests.

## 12. Rui ro con lai

- VM manual legacy `run_input_test.bat --exam-id 3` remains pending because lockdown GUI must run only in VM test-safe environment.
- Crash/restart restore is intentionally unsupported until key persistence is designed.
- If an old `.enc` exists from a previous debug process, restore may safely fail with `ERROR_DRAFT_DECRYPT_FAILED` because the RAM key is different.
- Phase 6J remains debug-only and must not be treated as production answer persistence.

## 13. Phase tiep theo de xuat

Recommended next phase:

```text
Phase 6J.5: Local Draft Restore Regression Gate - VM/Legacy Verification
```

Scope:

- Verify V2_DEBUG restore UI in VM.
- Verify legacy `run_input_test.bat --exam-id 3`.
- Confirm Final Submit/Rust/Quick Settings are unaffected.
- Decide whether key persistence/crash-safe restore deserves a separate design phase.
