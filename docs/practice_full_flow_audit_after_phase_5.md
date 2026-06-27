# Practice Full Flow Audit After Phase 5

Date: 2026-06-23

Scope: audit-only review of TutorHub Enterprise / TutorHub_Maven Practice tab after Phase 5. No Java, HTML, XML, schema, protocol, or runtime code was changed in this audit.

## 1. Executive Summary

The Practice tab has a real foundation now: it can list practice papers, start a practice session, render a JavaFX WebView player, save per-question progress, submit attempts, resume normal in-progress attempts, calculate wrong/mastery stats, and has a first version of assignment tables and services.

However, it is not ready to be treated as a production-grade Phase 5 release yet. The biggest issue is that assignment actions are implemented in `ExamController` and UI, but `ClientHandler` does not route `PRACTICE_ASSIGNMENT_*` actions, so the assignment flow is likely not end-to-end functional. There are also important security and data-integrity gaps around paper visibility, role/ownership checks, assignment attempt resume, and server/client result synchronization.

Build and full Maven test suite passed, but there are no dedicated Practice end-to-end tests. The test pass only proves the repository currently compiles and existing unrelated suites pass; it does not prove the Practice assignment flow works.

## 2. Files And Docs Reviewed

Reviewed docs:

- `docs/practice_tab_research_and_roadmap_v2.md`
- `docs/practice_phase_0_cleanup_report.md`
- `docs/practice_phase_1_dashboard_start_report.md`
- `docs/practice_phase_2_player_v1_report.md`
- `docs/auth_session_architecture_plan.md`
- `docs/google_apps_script_email_relay_setup.md`
- `docs/tse_question_bank_backend_phase_2_3.md`
- `docs/tse_exam_paper_builder_ui_phase_4e_2.md`
- `docs/tse_exam_operation_architecture_research.md`

Expected but not found in `docs`:

- `docs/practice_phase_3_attempt_persistence_report.md`
- `docs/practice_phase_3b_history_resume_report.md`
- `docs/practice_phase_4_wrong_question_mastery_report.md`
- `docs/practice_phase_5_assignment_report.md`

Reviewed source:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/resources/tse/practice-template.html`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAttemptService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAssignmentService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/QuestionAnalyticsService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamHtmlTemplateRenderer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAttemptDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAssignmentDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/UserQuestionStatsDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/db/ExamDatabaseManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/auth/AuthProtocol.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/AuthClient.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`

## 3. Build And Test Result

Build command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
```

Result: passed. Jar built at:

```text
D:\Ban_sao_du_an\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Copied to:

```text
D:\Ban_sao_du_an\HF_UPLOAD\update.jar
```

Size:

```text
222,088,307 bytes
```

Full test command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" test
```

Result from surefire reports:

- Test suites: 118
- Tests: 746
- Failures: 0
- Errors: 0
- Skipped: 5

Notes:

- `mvn test` printed PostgreSQL stack traces around `AUTO_INCREMENT` in V2 official result draft code, but surefire still reported 0 failures and 0 errors. Treat this as an existing noisy V2 test/runtime issue, not a Practice-specific failure.
- No dedicated automated Practice flow test was found.

## 4. Current Flow Status

| Flow | Status | Notes |
|---|---:|---|
| Practice list | Partially works | Server returns latest 50 `exam_papers`, but without publication/ownership filtering. |
| Start practice | Mostly connected | `PRACTICE_START` routes through `ClientHandler` to `ExamController` and renders HTML. |
| Player UI | Works as prototype | Uses JavaFX WebView and custom URL bridge. |
| Save progress | Mostly connected | Per-question save routes to server, verifies owner/status, and computes correctness server-side. |
| Submit attempt | Partially connected | DB update has double-submit guard, but UI does not surface server success/error clearly. |
| Resume normal attempt | Partially works | History/resume service exists; UI can resume in-progress normal practice attempts. |
| Wrong-question retry | Partially works | Server builds weak-question HTML from mastery stats, but still exposes `isCorrect`. |
| Mastery stats | Partially works | DB stats and summary exist, but never-attempted questions are not counted as weak. |
| Assignment create/list/start | Blocked | `ExamController` and UI support it, but `ClientHandler` does not route `PRACTICE_ASSIGNMENT_*`. |
| Assignment policy | Prototype only | Creation currently only allows `IMMEDIATELY`; start path still renders answer-exposing practice HTML. |

## 5. Strengths

| Area | Positive finding |
|---|---|
| Architecture direction | Practice has separate service/DAO layers for attempts, assignments, and mastery stats. |
| Attempt ownership | `PracticeAttemptService.saveAnswer`, `submitAttempt`, and `resumeAttempt` verify `attempt.userId == userId`. |
| Double-submit guard | `PracticeAttemptDAO.updateAttemptSummary` only updates attempts with `status = 'IN_PROGRESS'`. |
| Server-side grading | Save-progress recomputes correctness from DB options instead of trusting a client-sent `isCorrect`. |
| Incremental schema | Practice tables are additive and do not directly break existing exam tables. |
| UX prototype | One-question-at-a-time player, feedback, retry wrong questions, mark/skip, and resume are already present. |

## 6. High-Priority Issues

| Severity | Issue | Evidence | Impact |
|---|---|---|---|
| P0 | Assignment actions are not routed by `ClientHandler` | `ClientHandler` routes only `PRACTICE_LIST`, `PRACTICE_START`, `PRACTICE_SAVE_PROGRESS`, `PRACTICE_SUBMIT_ATTEMPT`, `PRACTICE_ATTEMPT_HISTORY`, `PRACTICE_RESUME_ATTEMPT`, `PRACTICE_WRONG_QUESTIONS`, `PRACTICE_MASTERY_STATS`. No `PRACTICE_ASSIGNMENT_*` cases. | Phase 5 assignment create/list/start can silently fail from the desktop client. |
| P0 | No role guard for assignment create | `handlePracticeAssignmentCreate` only checks `userId > 0`; `PracticeAssignmentService.createAssignment` does not validate teacher role or paper ownership. | Any logged-in user may attempt to assign any paper to users if routed. |
| P0 | Practice list exposes all papers | `handlePracticeList` selects latest 50 from `exam_papers` without published status, recipient assignment, role, or ownership filter. | Students may see papers they should not see. |
| P1 | Assignment resume is broken/incomplete | DAO selects `r.last_attempt_id` but does not put it into the map; UI looks for `last_attempt_id`. | In-progress assignments may start a new attempt instead of resuming. |
| P1 | Save-answer does not verify question belongs to attempt paper | `saveAnswer` verifies owner/status, then validates selected option for `questionId`; it does not verify `questionId` is inside `attempt.paperId`. | A crafted payload can write unrelated question stats into the attempt. |
| P1 | UI calculates result before server confirmation | `practice-template.html` shows result immediately and sends `PRACTICE_SUBMIT_ATTEMPT`; `MainDashboard` has no handler for submit success/error. | User may think submit succeeded even if server rejected it. |
| P1 | Save/submit race risk | Save is fire-and-forget through URL bridge; submit reads answers already persisted in DB. | Last answer can be lost if submit arrives before the final save is committed. |
| P1 | Answer key is exposed to client | `PracticeOptionViewDTO.isCorrect` is rendered into HTML and JS uses `opt.isCorrect`. | Fine for immediate practice feedback, unsafe for any assignment policy that should hide answers. |
| P2 | Search box is UI-only | `searchField` is added but has no filtering listener. | Users can type but nothing changes. |
| P2 | Local HTML import remains visible | `Nhập HTML` debug button is present in Practice toolbar. | Production UI still exposes a debug path. |

## 7. Security And Data Integrity Review

Answer leakage:

- Practice renderer intentionally exposes `isCorrect` for immediate feedback.
- This is acceptable for pure practice mode if the product intentionally gives instant feedback.
- It is not acceptable for assignments with `AFTER_SUBMIT`, `NEVER`, graded mode, or any anti-cheat scenario.
- Current assignment creation only permits `IMMEDIATELY`, which reduces immediate leakage risk. But the DB default is `AFTER_SUBMIT`, and assignment start does not harden against externally inserted or future policies before calling `renderPractice`.

Ownership and authorization:

- Attempt ownership is handled reasonably.
- Paper visibility and assignment creation are not sufficiently guarded.
- Assignment recipients are checked in `getAssignmentDetailForStudent`, but assignment packet routing is currently missing from `ClientHandler`.

Persistence:

- `practice_attempts`, `practice_answers`, `user_question_stats`, `practice_assignments`, and `practice_assignment_recipients` exist.
- Double-submit is guarded by DB status.
- Submit summary is computed from saved answer rows. If an unanswered question was never saved as skipped, the server summary can diverge from the client result.

Mastery:

- Mastery is simple and workable for MVP: attempts, correct, wrong, skipped, last attempt, mastery level.
- It does not yet implement spaced repetition, decay, or per-skill/topic analytics.
- Never-attempted questions reduce mastery percent through `totalQuestions`, but are not counted as weak questions.

## 8. Comparison With Target Product Behavior

| Product-quality expectation | Current state | Gap |
|---|---|---|
| Student sees only assigned/published practice sets | All latest papers are listed | Need visibility/role model. |
| Teacher can assign safely | UI exists, service exists | Missing route and role/ownership guard. |
| Student submit is reliable | Client shows local result immediately | Need server ACK-driven final state. |
| Hidden-answer assignment policy | DB has policy column | Renderer still exposes answers; only `IMMEDIATELY` is allowed for now. |
| Resume assignment | DB has `last_attempt_id` | DAO/UI key mismatch prevents reliable resume. |
| Auditability | Attempts and answers are stored | Need event/audit log for assignment start/save/submit. |
| Automated regression | Many V2 tests exist | No Practice-specific tests. |

## 9. Recommended Fix Order Before Phase 6

1. Route assignment actions in `ClientHandler`.
   Add `PRACTICE_ASSIGNMENT_CREATE`, `PRACTICE_ASSIGNMENT_LIST`, `PRACTICE_ASSIGNMENT_DETAIL`, and `PRACTICE_ASSIGNMENT_START` to the same controller route as other practice actions.

2. Add role and ownership checks.
   `PRACTICE_ASSIGNMENT_CREATE` should require teacher/admin role and should only allow assignable papers owned by or visible to that teacher.

3. Fix assignment resume payload.
   Map `last_attempt_id` from DAO to response and align the key used by `PracticeTab`.

4. Make submit ACK-driven.
   Handle `PRACTICE_SUBMIT_ATTEMPT_SUCCESS/ERROR` in `MainDashboard` or `PracticeTab`. The UI should show "submitted" only after server confirmation.

5. Persist all unanswered questions at submit.
   Either have client send a complete answer snapshot and server save it transactionally before scoring, or server fill missing paper questions as skipped before summary.

6. Validate question-paper membership in `saveAnswer`.
   Reject any `questionId` that is not part of the attempt's `paperId`.

7. Hide debug UI in production.
   Gate `Nhập HTML` behind a debug system property or build flag.

8. Add Practice tests.
   Start with service-level tests for start/save/submit/double-submit/resume, then assignment route tests.

## 10. Suggested Phase 6 Scope

Phase 6 should not start with new visual polish. It should be a hardening phase:

- Phase 6A: Practice routing and assignment unblock.
- Phase 6B: Role/ownership/visibility guard.
- Phase 6C: Submit transaction and ACK-driven UI.
- Phase 6D: Assignment resume and max-attempt correctness.
- Phase 6E: Automated tests for Practice core.
- Phase 6F: Policy-aware renderer: immediate practice renderer vs hidden-answer assignment renderer.

After that, UI improvements and more advanced Quizizz/Kahoot-style flows will be safer to build.

## 11. Manual Test Checklist Still Needed

These were not executed as interactive UI tests in this audit:

- Login as teacher, open Practice tab, create assignment.
- Login as student, list assigned practice.
- Start assignment, answer question, close app, resume assignment.
- Submit assignment and verify DB status becomes `COMPLETED`.
- Try double-submit and verify second submit is rejected without duplicate stats.
- Try crafted save payload with unrelated `questionId` and verify rejection after fix.
- Verify hidden-answer policy once a non-immediate renderer exists.

## 12. Upload Recommendation

`HF_UPLOAD/update.jar` was generated successfully and is ready as a file artifact.

Recommendation:

- Do not upload it as a production/stable Practice Phase 5 release.
- It is acceptable to upload only as an internal/dev build if the goal is to deploy the latest current app state and you accept the known Practice assignment blockers.

Reason: build and tests pass, but the Practice Phase 5 assignment flow has a routing blocker and missing authorization/visibility guards.

## 13. Final Status

| Question | Answer |
|---|---|
| Is Practice fully stable after Phase 5? | No. Core practice is partially usable; assignment is blocked/incomplete. |
| Are phases 0-5 connected cleanly? | Mostly for normal practice; not for assignment. |
| Does build pass? | Yes. |
| Does full Maven test pass? | Yes, 746 tests, 0 failures/errors, 5 skipped. |
| Is there answer leakage? | Yes, intentionally in practice renderer; unsafe for hidden-answer assignment policies. |
| Is double-submit guarded? | Mostly yes at DB update level, but UI handling is incomplete. |
| Is mastery usable? | Yes for MVP, not yet robust enough for adaptive learning. |
| Should Phase 6 proceed directly? | Only after fixing the routing/auth/submit blockers above. |

