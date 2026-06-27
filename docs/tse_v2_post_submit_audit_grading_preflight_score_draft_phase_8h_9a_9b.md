# Phase 8H + 9A + 9B: Post-Submit Integrity Audit + Grading Preflight + Server-side Score Draft

## Context
Implemented the core gating mechanisms before Final Scoring, validating the submission payload size against the answer key and drafting the server-side score securely.

## What was done
1. Created V2PostSubmitIntegrityAuditResult & V2PostSubmitIntegrityAuditService.
2. Created V2GradingPreflightResult & V2GradingPreflightService.
3. Created V2ScoreDraftRecord, V2ScoreDraftDAO, V2ScoreDraftResult & V2ScoreDraftService.
4. Created boundary interfaces V2AnswerKeyResolver & V2AnswerPayloadParser.
5. Created offline tests.
6. Handled secure action routing in ClientHandler.java.

## Validations
- Unit tests run fully offline without connecting to the Neon DB.
- Maven clean install passes (303 tests).
- Portable build completes successfully.
- No client-side exposure of exam_results, answerKey, or raw scoring data.