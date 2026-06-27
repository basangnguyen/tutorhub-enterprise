# Practice Hotfix / Hardening After Phase 5 Report

Date: 2026-06-23

## 1. Files and documents read

- `AGENTS.md`
- `docs/practice_full_flow_audit_after_phase_5.md`
- `docs/practice_tab_research_and_roadmap_v2.md`
- `docs/secure_exam_tasks_v2.md`
- `docs/secure_exam_rust_and_seb_learning_sources_ONLY_3_DOCS.md`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAssignmentService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAttemptService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAssignmentDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAttemptDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/AuthClient.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/auth/AuthProtocol.java`
- `src/main/resources/tse/practice-template.html`

Note: `docs/MASTER_SECURE_EXAM_BLUEPRINT_v4.md` was referenced by local agent instructions but is not present in the workspace.

## 2. Files changed or created

- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAssignmentService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAttemptService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAssignmentDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAttemptDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/resources/tse/practice-template.html`
- `src/test/java/com/mycompany/tutorhub_enterprise/PracticeHotfixSmokeTest.java`
- `docs/practice_hotfix_after_phase_5_report.md`

## 3. Assignment route fix

`ClientHandler` already routes the four assignment actions to `ExamController`:

- `PRACTICE_ASSIGNMENT_CREATE`
- `PRACTICE_ASSIGNMENT_LIST`
- `PRACTICE_ASSIGNMENT_DETAIL`
- `PRACTICE_ASSIGNMENT_START`

No extra protocol action was added. The hotfix completed the missing server-side detail handler and fixed error action handling in the assignment create guard.

## 4. Role and ownership guard

Server-side role validation now treats these roles as assignment-capable:

- `ADMIN`
- `TUTOR`
- `TEACHER`
- `INSTRUCTOR`
- `GIA_SU`
- `GIASU`

Hardening applied:

- Student/direct packet create is rejected server-side.
- Teacher assignment creation requires paper ownership unless role is `ADMIN`.
- Student assignment start still uses `getAssignmentDetailForStudent`, so an unassigned user cannot start the assignment.
- Teacher/admin assignment detail uses owner/admin-aware lookup.
- Non-admin teacher detail lookup is constrained by `a.teacher_id = currentUserId`.

Limit: the current paper-access model still has only owner/admin logic. Shared paper ACL is not implemented in this hotfix.

## 5. Practice list visibility

`PRACTICE_LIST` is now role-filtered:

- Admin: sees all papers.
- Teacher/TUTOR-like role: sees papers where `exam_papers.creator_id = currentUserId`.
- Student: sees only papers attached to active assignments assigned to that student.

Limit: because the schema does not yet expose a clear public practice flag, this hotfix intentionally does not show all published/internal papers to students.

## 6. Assignment resume payload

Fixed:

- `PracticeAssignmentDAO` now maps both `lastAttemptId` and `last_attempt_id`.
- `PracticeTab` reads both key styles.
- `PracticeAssignmentService.startAssignmentAttempt` checks `last_attempt_id` for an existing `IN_PROGRESS` assignment attempt and returns it with `resumed=true`.
- New assignment attempts use `createAssignmentAttempt(...)` with `assignment_id` and `mode='ASSIGNMENT'`.

Expected behavior: start assignment, leave in progress, return later, click continue, and resume the same attempt id instead of creating a duplicate.

## 7. Submit ACK-driven UI

Fixed:

- `MainDashboard` now forwards `PRACTICE_SUBMIT_ATTEMPT_SUCCESS` server result data to `PracticeTab`.
- `PracticeTab` calls `window.tutorhubPracticeSubmitAck(...)` inside the WebView when available.
- `practice-template.html` now supports:
  - submitting state
  - server-result rendering
  - submit error rollback

Important behavior change:

- The template no longer destroys the player DOM when submitting. It hides the player and shows a pending result state, so a server reject can safely restore the player.

## 8. Unanswered questions at submit

Server-side handling is implemented in `PracticeAttemptService.submitAttempt`:

- It loads all questions in the attempt paper.
- It finds saved answers.
- It inserts missing questions as skipped before scoring.
- It rejects already submitted/non-in-progress attempts before doing the fill.

Expected behavior: if a paper has 10 questions and only 3 were answered, the server counts 7 as skipped.

## 9. Question-paper membership validation

Fixed in `PracticeAttemptService.saveAnswer`:

- `questionId` must belong to `attempt.paperId`.
- `selectedOptionId`, when present, must belong to that `questionId`.
- Crafted payloads with a question outside the paper or an option outside the question are rejected before DB write.

## 10. Local HTML debug gate

Existing behavior confirmed:

- The local `Nhập HTML` debug action is gated by `-Dtutorhub.practice.localHtmlDebug=true`.
- Default production/dev run does not add the debug button.

No additional change was required for this item.

## 11. Tests and build result

Commands run:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" test
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
Copy-Item ".\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" ".\HF_UPLOAD\update.jar" -Force
Get-Item ".\HF_UPLOAD\update.jar"
```

Results:

- `mvn test`: passed with exit code 0.
- New `PracticeHotfixSmokeTest`: 2 tests, 0 failures, 0 errors.
- `mvn clean compile assembly:single -DskipTests`: BUILD SUCCESS.
- `HF_UPLOAD/update.jar` copied successfully.
- `HF_UPLOAD/update.jar` size: `222,094,767` bytes.

Observed noise:

- Maven still reports OpenJFX effective model warnings.
- Some existing V2 tests print PostgreSQL `AUTO_INCREMENT` stack traces, but Surefire reports no test failures/errors.

## 12. Remaining risks

- Full assignment E2E manual test still needs real tutor/student accounts and server DB state.
- Teacher/shared paper ACL is still not modeled. Current rule is owner/admin only.
- `show_answers_policy` is intentionally limited to `IMMEDIATELY`; `AFTER_SUBMIT` and `NEVER` are rejected until a safe renderer/DTO split exists.
- The repository worktree contains many pre-existing unrelated changes, especially TSE/V2 files. This hotfix did not attempt to clean or revert them.
- `MainDashboard.java` already contains large previous Practice/Exam UI edits. This hotfix only adjusted submit ACK forwarding in that existing block.

## 13. Hugging Face upload recommendation

The jar can be uploaded to a staging/test Hugging Face Space because build and automated tests pass.

Do not treat it as production-ready until the manual assignment E2E checklist passes:

- Tutor creates assignment.
- Student sees assigned item.
- Student starts/resumes/submits.
- Status becomes `COMPLETED`.
- Max attempts/deadline/unauthorized start are verified.

## 14. Phase 6 readiness

Recommended decision: conditionally ready for Phase 6 Report work only after manual E2E confirms the assignment flow in a real server environment.

Code-level hotfix gates are now in place, but production sign-off should wait for manual test evidence.
