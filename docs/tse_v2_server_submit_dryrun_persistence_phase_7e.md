# Phase 7E: Server-side Submit Payload Persistence - Dry-run DB Only

## 1. Gate
- Phase 7D.5 gate document exists: `docs/tse_v2_backend_submit_dryrun_regression_phase_7d_5.md`.
- Phase 7D.5 status: GO for Phase 7E.
- Scope remains dry-run DB persistence only.

## 2. Goal
Phase 7E adds server-side persistence for a validated V2 dry-run submit payload. This is not a real exam submit flow.

## 3. Added DB Table
The additive migration is owned by `V2SubmitDryRunPayloadDAO.ensureSchema()`:

```sql
CREATE TABLE IF NOT EXISTS v2_submit_dryrun_payloads (
  id BIGSERIAL PRIMARY KEY,
  user_id INT NOT NULL,
  exam_id INT NOT NULL,
  paper_id INT NOT NULL,
  attempt_id VARCHAR(64),
  package_hash VARCHAR(128),
  payload_hash VARCHAR(128) NOT NULL,
  payload_json TEXT NOT NULL,
  answered_count INT NOT NULL,
  unanswered_count INT NOT NULL,
  complete BOOLEAN NOT NULL DEFAULT FALSE,
  validation_status VARCHAR(32) NOT NULL DEFAULT 'VALIDATED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Indexes:
- `idx_v2_submit_dryrun_attempt`
- `idx_v2_submit_dryrun_payload_hash`

## 4. Added Code
- `V2SubmitDryRunRecord`
- `V2SubmitDryRunPayloadDAO`
- `V2SubmitDryRunPersistenceResult`
- `V2SubmitDryRunPersistenceService`
- `V2SubmitDryRunPersistenceServiceTest`

## 5. Socket Action
New action:

```text
EXAM_SUBMIT_V2_DRYRUN_PERSIST
```

Responses:
- `EXAM_SUBMIT_V2_DRYRUN_PERSIST_OK`
- `EXAM_SUBMIT_V2_DRYRUN_PERSIST_ERROR`

The response DTO contains only safe metadata:
- recordId
- examId
- paperId
- attemptId
- payloadHash
- answeredCount
- unansweredCount
- complete
- persistedAt

It does not return answer lists or selected option IDs.

## 6. Feature Flag
```text
tse.v2.submitDryRunPersistence.enabled
```

Default:

```text
false
```

When disabled, persistence returns:

```text
ERROR_FEATURE_DISABLED
```

## 7. Security Rules
Before DB write, payload JSON is rejected if it contains any of:
- `sessionToken`
- `keyB64`
- `plaintextJson`
- `plaintext`
- `answerKey`
- `isCorrect`
- `correctOption`
- `password`
- `passwordHash`
- `score`
- `gradingResult`

Unsafe payloads return:

```text
ERROR_V2_SUBMIT_DRYRUN_PAYLOAD_UNSAFE
```

## 8. Non-goals Confirmed
- No grading.
- No score calculation.
- No `exam_results` write.
- No attempt status update to `SUBMITTED`.
- No legacy `EXAM_SUBMIT` call.
- No legacy `submit_payload.enc`.
- No Final Submit, Rust, Quick Settings, Taskbar, Parent, or JCEF changes.

## 9. Targeted Test Result
Command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" "-Dtest=V2SubmitDryRunPersistenceServiceTest" test
```

Result:

```text
PASS - 14 tests, 0 failures, 0 errors, 0 skipped
```

## 10. Security Scan Result
The scan found expected hits only:
- `INSERT` only into `v2_submit_dryrun_payloads`.
- Unsafe keywords only in blacklist checks and negative tests.
- Existing legacy `EXAM_SUBMIT` hits remain in `ClientHandler` but are not called by the new action.
- No dry-run persistence path writes `exam_results` or marks attempts `SUBMITTED`.

## 11. Full Build Result
Command:

```powershell
mvn clean install
```

Result:

```text
PASS - 160 tests, 0 failures, 0 errors, 0 skipped
```

## 12. Portable Build Result
Command:

```powershell
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
```

Result:

```text
PASS - dist/TutorHubSecureExam rebuilt successfully
```

## 13. VM GUI Status
`run_input_test.bat --exam-id 3`: PENDING - VM-only / skipped by fast-track rule.

## 14. Phase Conclusion
Phase 7E is implemented as a dry-run-only server persistence layer. Full Maven and portable build both pass. VM-only legacy GUI acceptance remains pending by project rule.
