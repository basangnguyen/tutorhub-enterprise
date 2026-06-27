# Phase 7P + 7Q: Real Submit Transition Safety Design & Hardening

## 1. Current No-Grading Pipeline
The current V2 submit pipeline operates entirely in "no-grading" mode. It validates the payload, persists a dry-run copy, creates a debug submit record, and drafts finalization/closure ledgers. 
- **Action:** `EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING`
- **Result:** No grading is performed, attempt status is not changed to `SUBMITTED`, and `exam_results` are not written.

## 2. Boundaries Between No-Grading and Real Submit
The transition to real submit requires crossing a strict boundary where state changes become immutable:
- **No-Grading:** `v2_submit_dryrun_payloads`, `v2_submit_records` (with debug status), `v2_attempt_finalization_ledger`, `v2_attempt_closure_drafts`.
- **Real Submit:** `exam_results`, updating `exam_attempts.status = 'SUBMITTED'`, performing automated grading, and persisting final scores.

## 3. Conditions Required Before Enabling Real Submit
- All feature flags in `V2SubmitFeatureFlags` must be explicitly reviewed.
- The `EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING` route must be thoroughly tested on a VM environment with full E2E portable build.
- The legacy `EXAM_SUBMIT` must remain untouched and available as a fallback until V2 is 100% verified.

## 4. Feature Flags Rollout Order
1. `tse.v2.submitDryRunValidation.enabled`
2. `tse.v2.submitDryRunPersistence.enabled`
3. `tse.v2.submitRecord.enabled`
4. `tse.v2.attemptFinalizationDraft.enabled`
5. `tse.v2.attemptFinalizationLedger.enabled`
6. `tse.v2.attemptClosureDraft.enabled`
7. `tse.v2.serverNoGradingOrchestrator.enabled`
8. `tse.v2.clientServerNoGradingSubmit.enabled`

*Note: Real submit flags (e.g., `tse.v2.realSubmit.enabled`) will be introduced in Phase 7R+.*

## 5. Allowed Routes and Actions
- `EXAM_SUBMIT_V2_DRYRUN_VALIDATE`
- `EXAM_SUBMIT_V2_DRYRUN_PERSIST`
- `EXAM_SUBMIT_V2_RECORD_CREATE`
- `EXAM_SUBMIT_V2_FINALIZATION_DRAFT`
- `EXAM_SUBMIT_V2_FINALIZATION_LEDGER`
- `EXAM_SUBMIT_V2_CLOSURE_DRAFT`
- `EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING`

## 6. Forbidden Routes and Actions
- V2 payload must NOT be routed to legacy `EXAM_SUBMIT`.
- V2 client must NOT call legacy `EXAM_SUBMIT`.
- V2 submit must NOT trigger `submit_payload.enc` file generation.

## 7. Current DB Tables for V2
- `v2_submit_dryrun_payloads`: Stores dry-run payloads.
- `v2_submit_records`: Stores basic submit info without grading.
- `v2_attempt_finalization_ledger`: Idempotent ledger for finalization.
- `v2_attempt_closure_drafts`: Draft state before attempt closure.

## 8. When to Update `exam_attempts` to `SUBMITTED`
Only during the Real Submit phase (7R+) when the closure ledger explicitly authorizes a finalized state. Phase 7P+7Q does NOT update this status.

## 9. When to Write `exam_results`
Only when grading is explicitly enabled and validated in Phase 7R+. Phase 7P+7Q does NOT write to `exam_results`.

## 10. When to Perform Grading
Only in Phase 7R+ after the payload is persisted and validated as a Real Submit payload. Phase 7P+7Q does NOT perform grading.

## 11. VM / Manual Acceptance Gate
Before proceeding to Real Submit, the no-grading orchestrator must pass a VM-only acceptance test verifying:
- No file written (`submit_payload.enc`).
- `CLOSURE_DRAFTED_NO_GRADING` final status.
- No Rust/Java process hanging.

## 12. Rollback Strategy
If Real Submit causes issues, toggle off `tse.v2.realSubmit.enabled` and fall back to the legacy `EXAM_SUBMIT` route. Data written to V2 tables can be safely ignored as it is partitioned from legacy tables.

## 13. Audit and Security Checklist
- [x] No plaintext answers logged.
- [x] No `answerKey`, `isCorrect`, `correctOption` returned in responses.
- [x] `ClientHandler` strings replaced with `V2SubmitActions` constants.
- [x] All feature flags default to `false`.
- [x] Legacy `EXAM_SUBMIT` logic unchanged.
