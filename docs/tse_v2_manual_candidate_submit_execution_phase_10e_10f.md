# Phase 10E + 10F: Manual Candidate V2 Submit Execution Trigger + Execution Audit Ledger

## Overview
Phase 10E/10F introduces the ability to manually trigger a candidate's V2 submission execution while creating an Audit Ledger record to track this operation. Since the full scoring pipeline requires `answerKeyResolver` and `payloadParser` (which are pending schema finalization), this phase strictly operates in a **PREPARE-ONLY** mode.

## Key Constraints Enforced
- **No full scoring chain**: `V2ScoreDraftService` throws `NullPointerException` due to null dependencies. Thus, execution stops before scoring.
- **No final status update**: Candidate attempts are not marked as `SUBMITTED`, `COMPLETED`, or `GRADED`.
- **No legacy interference**: The existing legacy `EXAM_SUBMIT` flow is left entirely untouched.
- **Data safety**: The `v2_manual_candidate_execution_ledger` table strictly excludes sensitive information such as `payloadJson`, raw answers, or scores.

## Implemented Components
1. **Feature Flags**: Added `tse.v2.manualCandidateSubmitExecution.enabled` and `tse.v2.manualCandidateExecutionAudit.enabled`.
2. **Actions**: `EXAM_SUBMIT_V2_MANUAL_CANDIDATE_SUBMIT_EXECUTE` and `EXAM_SUBMIT_V2_MANUAL_CANDIDATE_EXECUTION_AUDIT`.
3. **Execution Ledger**: 
   - `V2ManualCandidateExecutionLedgerRecord`
   - `V2ManualCandidateExecutionLedgerDAO`
   - Table `v2_manual_candidate_execution_ledger` saves the `attempt_id`, `execution_status`, and `execution_mode` ("PREPARE_ONLY").
4. **Execution Service**:
   - `V2ManualCandidateSubmitExecutionService`
   - Verifies the Gate Service, payload presence, and creates the "PREPARED_ONLY" audit ledger.
5. **Audit Service**:
   - `V2ManualCandidateExecutionAuditService` reads the ledger and verifies consistency.

## Idempotency
If the attempt has already been prepared, the execution service idempotently returns the existing ledger information rather than inserting duplicates.

## Verification
- Unit Tests: Developed offline/mock unit tests verifying feature flags, prepare-only success, payload validation, and idempotency.
- Security Scan: `findstr` confirmed no sensitive fields are stored in the database or returned in DTOs.
- Maven Build: Passed all 421 unit test cases.
