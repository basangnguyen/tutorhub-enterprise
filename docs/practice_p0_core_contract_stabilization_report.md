# Practice P0 Core Contract Stabilization Report

Date: 2026-06-24

## 1. Scope

This P0 pass stabilizes the core Practice client/server contract. It does not implement gamification, large UI polish, reports, leaderboards, or advanced assignment policies.

## 2. Files touched in this pass

- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/resources/tse/quiz-practice-template.html`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/QuestionAnalyticsService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAttemptService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/AuthClient.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`

Note: several of these files are currently untracked in this worktree from earlier phases. They were used by Maven and compiled successfully.

## 3. Packet constructor fix

`PracticeTab` no longer sends Practice requests with `new Packet(false, "...")`.

The fixed request pattern is:

```java
new Packet(action, payloadJson)
```

This ensures `Packet.action` is set correctly before the packet reaches `ClientHandler`.

## 4. Canonical Practice actions

Client requests now use canonical `AuthProtocol` constants:

- `PRACTICE_LIST`
- `PRACTICE_START`
- `PRACTICE_SAVE_PROGRESS`
- `PRACTICE_SUBMIT_ATTEMPT`
- `PRACTICE_ATTEMPT_HISTORY`
- `PRACTICE_RESUME_ATTEMPT`
- `PRACTICE_WRONG_QUESTIONS`
- `PRACTICE_ASSIGNMENT_CREATE`
- `PRACTICE_ASSIGNMENT_LIST`
- `PRACTICE_ASSIGNMENT_DETAIL`
- `PRACTICE_ASSIGNMENT_START`

Legacy request names `PRACTICE_SAVE`, `PRACTICE_SUBMIT`, and `PRACTICE_ASSIGN` are no longer emitted by `PracticeTab`.

## 5. Client autosave behavior

`quiz-practice-template.html` now calls autosave when:

- a single-choice answer changes
- a multiple-choice checkbox changes
- a question is flagged/unflagged

Autosave is sent through:

```text
JavaBridge.saveAnswer -> PracticeTab -> PRACTICE_SAVE_PROGRESS -> ExamController.handlePracticeSaveProgress
```

## 6. Submit snapshot behavior

Submit now sends an `answersSnapshot` array with the current visible state of all questions.

Before final scoring, `ExamController.handlePracticeSubmitAttempt` saves the snapshot entries into `practice_answers`, then calls `PracticeAttemptService.submitAttempt`.

This reduces the race where the UI submits before the last autosave response returns.

## 7. Server-side snapshot validation

Each snapshot item is validated through the existing `PracticeAttemptService.saveAnswer` path, which checks:

- attempt exists
- attempt belongs to authenticated user
- attempt is still `IN_PROGRESS`
- question belongs to the paper
- selected option belongs to the question

Invalid snapshot entries fail the submit instead of being silently trusted.

## 8. Wrong questions flow

`PRACTICE_WRONG_QUESTIONS` is now routed end-to-end:

```text
quiz-practice-template.html
-> PracticeTab.startWrongQuestions
-> ClientHandler
-> ExamController.handlePracticeWrongQuestions
-> QuestionAnalyticsService.getWrongQuestionsHtml
-> MainDashboard
-> PracticeTab.onPracticeStartReceived
-> TutorHubPractice.loadQuiz
```

The wrong-questions response now includes the same shape expected by the modern Practice player:

- `attemptId`
- `paperId`
- `mode`
- `title`
- `questionCount`
- `totalQuestions`
- `showAnswersPolicy`
- `htmlContent`
- `quizData`

## 9. Template stale references

Stale demo references were removed from `quiz-practice-template.html`:

- `DECKS`
- `TITLES`
- `QUIZ_*`

Functions such as retry wrong questions, flashcards, and capture flagged questions now use the current server-loaded quiz state.

## 10. Resume attempt behavior

`PracticeAttemptService.resumeAttempt` now reads assignment answer policy when the attempt belongs to an assignment.

If policy is not `IMMEDIATELY`, it does not include answers in `quizData` and does not include legacy answer-bearing HTML content.

Current assignment creation still rejects unsupported hidden-answer policies, so production behavior remains conservative.

## 11. Assignment policy handling

`PracticeAssignmentService` already rejects unsupported policies such as `AFTER_SUBMIT` and `NEVER`.

This pass preserves that behavior. The UI may display those options in the debug assign dialog, but the server rejects them. This avoids claiming hidden-answer support before the player and persistence model fully support it.

## 12. Multiple-choice limitation

The client snapshot carries `selectedOptionIds`, but the current database/service path persists only one `selectedOptionId`.

For P0, the server uses the first selected option when saving. Full multi-select grading requires a schema/service change and should be handled in a later phase.

## 13. Dashboard routing

`MainDashboard` now routes save ACKs:

- `PRACTICE_SAVE_PROGRESS_SUCCESS` -> `PracticeTab.onPracticeSaveAck`
- `PRACTICE_SAVE_PROGRESS_ERROR` -> `PracticeTab.onPracticeSubmitError`

Submit success/error and start/wrong-questions routing were already present and remain in use.

## 14. Build and test results

Commands run:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" -Dtest=PracticeHotfixSmokeTest test
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" test
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
Copy-Item ".\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" ".\HF_UPLOAD\update.jar" -Force
& "C:\Program Files\Apache NetBeans\jdk\bin\jar.exe" tf ".\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar"
```

Results:

- `PracticeHotfixSmokeTest`: pass, 2 tests, 0 failures.
- Full `mvn test`: exit code 0. Some older tests printed expected/failure-path DB logs, including a PostgreSQL `AUTO_INCREMENT` syntax error from an existing V2 result draft test path, but Maven did not fail.
- `clean compile assembly:single -DskipTests`: BUILD SUCCESS.
- `HF_UPLOAD/update.jar`: created, 222,149,440 bytes.
- Jar contains `tse/quiz-practice-template.html`.

## 15. Remaining risks and next phase gate

P0 contract stabilization is complete enough to move to manual E2E verification and then UX polish, with these known limits:

- Full multi-choice persistence/grading is not complete.
- Assignment hidden-answer policies remain intentionally disabled/rejected.
- Manual E2E should verify login -> Practice list -> start -> autosave -> submit -> resume -> wrong questions against a real DB.
- The worktree is very dirty and contains many unrelated modified/untracked files from earlier phases. Do not commit blindly.

