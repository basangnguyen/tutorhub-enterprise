# Phase 8F + 8G: Controlled Attempt Status Execution + Execution Ledger

## Overview
This phase marks the completion of the status transition pipeline for TutorHub Secure Exam V2. It introduces the secure execution logic necessary to update an attempt's status to `SUBMITTED`, while logging every execution in a designated execution ledger for idempotency and auditability.

**Critical Note:** This phase transitions the status to `SUBMITTED` safely, but absolutely **NO grading, NO `exam_results` write, and NO answerKey access** takes place. This remains purely an orchestration pipeline step.

## Components Created

### 1. `V2ExamAttemptStatusDAO`
A narrow data access object dedicated to reading and updating the status of an `exam_attempts` record. It uses a Compare-And-Set mechanism combined with transactional execution.

### 2. `V2AttemptStatusExecutionLedgerDAO`
Data access object and schema ensure (`v2_attempt_status_execution_ledger`) for recording execution operations. Each operation is made idempotent via `submit_record_id`.

### 3. `V2AttemptStatusExecutionService`
The core business logic that strictly checks:
- The global feature flag (`tse.v2.attemptStatusExecution.enabled`).
- The readiness orchestrator check (`READY_FOR_REAL_SUBMIT_STATUS_EXECUTION_DRAFT`).
- The Draft DAO verification and target attempt status (`SUBMITTED`).
- User ID mismatch validation.
- Valid `currentStatus` transitions (`IN_PROGRESS`, `STARTING`, `STARTED`, `DOING`, `IN_EXAM`).
- Idempotency via `V2AttemptStatusExecutionLedgerRecord`.

### 4. `V2AttemptStatusExecutionResult`
A safe DTO object. Does not return or expose grading information.

### 5. `V2AttemptStatusExecutionServiceTest`
Comprehensive unit tests mimicking Offline/Mock DAOs to ensure transition requirements and rejections are strictly enforced, ensuring rollback on failure and protecting against dirty database states.

## Integration & Gate Guards
- **Feature Flag**: `tse.v2.attemptStatusExecution.enabled` (Default: `false`).
- **Client Action**: `EXAM_SUBMIT_V2_ATTEMPT_STATUS_EXECUTE`.
- Both are fully integrated into `ClientHandler.java` and `V2SubmitActions.java`.

## Strict Boundaries Maintained
- No Legacy `EXAM_SUBMIT` integration.
- No `exam_results`.
- Transactional consistency enforced: `attempt_status` update and `execution_ledger` insert run on the same `Connection`. Rollbacks prevent a `SUBMITTED` state without a corresponding ledger entry.
