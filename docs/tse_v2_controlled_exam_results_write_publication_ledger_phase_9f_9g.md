# Phase 9F + 9G: Controlled `exam_results` Write + Result Publication Ledger

This document outlines the design and boundaries for writing official exam results to the legacy `exam_results` table securely during the TutorHub Secure Exam V2 flow.

## 1. Schema Interpretations

### `total_score`
After inspecting `ExamDatabaseManager.java` and `ExamPaperDAO.java`, it was determined that the `total_score` column in the database (across `exam_sessions`, `exam_papers`, and `exam_results`) represents the sum of points (raw score), **not** a percentage. 
- *Proof*: `ExamPaperDAO` performs `UPDATE exam_papers SET total_score = (SELECT COALESCE(SUM(points), 0) FROM exam_paper_questions WHERE paper_id = ?)`.
- *Mapping Decision*: `exam_results.total_score = officialResultDraft.rawScore`.

### `graded_by`
The `graded_by` column is defined as `INT` without `NOT NULL` constraints, making it naturally nullable.
- *Mapping Decision*: Since this is a server-side automated grading flow, `graded_by` is set to `null` to indicate a system operation.

## 2. Strict Transactional Boundary
To prevent split-brain issues, the insertion of the legacy record and the V2 idempotency ledger are tightly coupled within a single JDBC transaction.
```java
conn.setAutoCommit(false);
insert exam_results;
insert v2_result_publication_ledger;
conn.commit(); // rollback if either fails
```
This guarantees we never reach a state where an `exam_results` row exists without a corresponding ledger entry natively created by V2. 

## 3. Safe Fallbacks & Idempotency
- If **both** `exam_results` and the ledger exist: Treated as an idempotent success.
- If **only one** exists: The transaction is rejected as an unsafe state (`ERROR_V2_RESULT_PUBLICATION_INCONSISTENT_EXISTING_RESULT`).

## 4. Preservation of Legacy Constraints
- No updates, deletes, or merges were implemented for `exam_results`.
- `GRADED` and `COMPLETED` attempt statuses are not updated in this phase.
- Answer keys and `perQuestionResults` remain strictly protected and are never passed to the DTO payload.
