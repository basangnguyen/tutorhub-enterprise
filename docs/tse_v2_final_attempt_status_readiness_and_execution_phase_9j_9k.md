# Phase 9J + 9K: Final Attempt Status Readiness Gate & Execution Ledger

## 1. Overview
This document details the implementation of Phase 9J and 9K in the V2 server-side final submission process. This phase represents the absolute final step of the server-side grading process, transitioning the exam attempt to its terminal state.

### Phase 9J: Final Attempt Status Readiness Gate
Validates that an attempt is fully ready to be transitioned to `COMPLETED`. 

Readiness conditions:
- Result Publication Verification must pass.
- Final Result Handoff must be ready.
- `exam_results` must exist in the database (verified via read-only probe).
- The `v2_result_publication_ledger` record must exist and have status `RESULT_PUBLISHED`.
- The current attempt status in the database MUST be `SUBMITTED`.

### Phase 9K: Controlled Final Attempt Status Execution + Ledger
Executes the transition and records it in an immutable ledger.

Execution rules:
- Reads the Readiness Gate result. If not ready, execution is aborted.
- Utilizes `DatabaseManager.getConnection()` with `setAutoCommit(false)` to wrap the status update and ledger insert in a single atomic transaction.
- Uses a CAS (Compare-And-Swap) approach: `UPDATE exam_attempts SET status = 'COMPLETED' WHERE id = ? AND status = 'SUBMITTED'`.
- If the CAS update succeeds, a record is inserted into `v2_final_attempt_status_ledger` with `ATTEMPT_STATUS_COMPLETED`.
- If the ledger insert fails, the transaction is rolled back, reverting the status update.
- Supports idempotency: If the CAS update fails because the status is already `COMPLETED` and the ledger record exists, it returns a successful idempotent response.

## 2. Technical Boundaries
- **NO `exam_results` Mutations:** This phase explicitly forbids any modifications to the `exam_results` table.
- **NO Legacy Submission:** The legacy `EXAM_SUBMIT` logic and `V2LegacySubmitService` are bypassed.
- **NO Sensitive Data Leaks:** The results payload does not include `answerKey`, `perQuestionResults`, or raw answers.
- **Strict Final Status:** The hardcoded target final status is `COMPLETED`.

## 3. Database Structures

### `v2_final_attempt_status_ledger`
This new table keeps an immutable history of final status transitions.
Columns:
- `id` (Primary Key)
- `submit_record_id` (Foreign Key referencing `v2_submit_records`)
- `attempt_id` (String)
- `user_id` (Int)
- `exam_id` (Int)
- `paper_id` (Int)
- `previous_status` (String, expected: 'SUBMITTED')
- `target_status` (String, expected: 'COMPLETED')
- `status_update_status` (String: 'ATTEMPT_STATUS_COMPLETED')
- `executed_at` (Timestamp)

## 4. Verification and Testing
All code logic in this phase was successfully validated offline using mocked DAOs.

- `V2FinalAttemptStatusReadinessServiceTest.java`
- `V2FinalAttemptStatusExecutionServiceTest.java`

Security scans (`findstr` for forbidden words) passed successfully. The full Maven build and Portable Build script passed with `0` errors.

## 5. Next Steps
- Phase 10: Integrate V2 into the primary student exam flow.
