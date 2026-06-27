# Phase 9C + 9D + 9E: Score Draft Integrity Audit + Official Result Draft + Result Publication Readiness Gate

## Overview
These phases are the final gating logic before transitioning the finalized attempt into persistent, legacy exam results. They sit immediately after the `Server-side Score Draft` generated in Phase 9B.

## Phase 9C: Score Draft Integrity Audit
- **Goal:** Verify the raw math and consistency of the generated server-side score draft (`V2ScoreDraftRecord`).
- **Mechanism:**
  - `V2ScoreDraftIntegrityAuditService.java` loads the draft and the initial submit record.
  - Ensures `answeredQuestions + unansweredQuestions == totalQuestions`.
  - Ensures `correctCount + incorrectCount + unansweredQuestions == totalQuestions`.
  - Verifies the user ID and payload hash integrity.
  - Returns `SCORE_DRAFT_INTEGRITY_READY` only if the draft mathematically matches itself and the context.

## Phase 9D: Official Result Draft Persistence
- **Goal:** Aggregates and moves the integrity-audited score draft into an "Official Result Draft".
- **Mechanism:**
  - `V2OfficialResultDraftService.java` delegates to Phase 9C for auditing.
  - Upon success, persists a new record into the `v2_official_result_drafts` table via `V2OfficialResultDraftDAO.java`.
  - Returns `OFFICIAL_RESULT_DRAFTED_PENDING_PUBLICATION` status.

## Phase 9E: Result Publication Readiness Gate
- **Goal:** Ensure all systems are clear to publish the actual exam results, strictly preventing duplicates.
- **Mechanism:**
  - `V2ResultPublicationReadinessService.java` pulls the official result draft.
  - Checks `V2ExamAttemptStatusDAO.java` to ensure the attempt is marked as `SUBMITTED`.
  - Checks `V2ExamResultsReadOnlyProbe.java` via `existsResultForAttempt()` (`SELECT COUNT(*) FROM exam_results`) to explicitly fail if an exam result already exists.
  - Only when all conditions are met will it return `READY_FOR_EXAM_RESULTS_WRITE_DRAFT`.

## Rationale
These gates completely separate the calculation of the score from its persistence in the legacy reporting systems. If a timeout, duplication attempt, or mathematical anomaly occurs, the attempt will stall at these safe gate checkpoints, preventing database corruption and ensuring idempotency.
