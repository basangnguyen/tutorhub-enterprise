# Phase 7N + 7O: Client-side No-Grading Server Submit Bridge + Full Regression Gate

## 1. Phase goal
Phase 7N/7O connects the V2 debug client path to the server-side no-grading submit orchestrator. The client prepares a safe V2 submit payload from in-memory answer selection state and sends it to the server without invoking legacy submit, grading, Final Submit, Rust, or any file-based submit artifact.

## 2. Canonical socket action
The canonical action for this phase is:

```text
EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING
```

The bridge no longer waits for a generic `_RESPONSE` suffix. It accepts only the canonical `_OK` and `_ERROR` response actions for this route.

## 3. Client bridge design
- `TSEV2ServerSubmitTransport` isolates the network transport so tests can use a fake transport.
- `TSEV2ServerNoGradingSubmitBridgeService` owns the feature flag check, canonical action dispatch, response parsing, and unsafe response validation.
- `TSEV2ServerNoGradingSubmitBridgeResult` carries only safe metadata: status, closure draft id, ledger id, submit record id, payload hash, final status, idempotency flag, and error code/message.
- The bridge rejects server responses containing unsafe markers such as answer keys, correctness, grading, score, tokens, encryption keys, or plaintext.

## 4. Payload prepare flow
- `TSEV2ClientSubmitPayloadPrepareService` converts the current `TSEV2AnswerSelectionState` into a draft snapshot.
- The snapshot is converted into `TSEV2SubmitPayload` through the existing V2 submit payload service.
- The phase does not write files, does not create encrypted submit payload artifacts, and does not log raw answers.

## 5. Debug-only UI behavior
- `TSEV2ReadOnlyExamPanel` exposes a debug button named `Server Submit Dry-run`.
- The button appears only when `tse.v2.clientServerNoGradingSubmit.enabled=true`.
- The UI displays only safe status fields: closure draft id, ledger id, submit record id, payload hash, and final status.
- Forbidden final-submit labels are not used: `Final Submit`, `Submit Now`, `Finish Exam`, and `Nop bai that`.

## 6. Server orchestrator route audit
- `ClientHandler` now routes `EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING` to `V2ServerSubmitNoGradingOrchestratorService`.
- The canonical action returns `EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING_OK` or `EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING_ERROR`.
- A previous alias remains compatibility-only and does not replace the canonical route.
- The server serializes the orchestrator result to safe JSON in `Packet.data`, avoiding direct serialization of service result objects.

## 7. Feature flags
Client flag:

```text
tse.v2.clientServerNoGradingSubmit.enabled
```

Default: `false`.

When the flag is off, the bridge returns `ERROR_FEATURE_DISABLED` and does not call the transport.

## 8. Why no Final Submit and no grading
This phase is a bridge/regression gate only. It validates the client-to-server path for closure draft creation without committing a real exam submission. It intentionally does not grade, does not update the attempt to `SUBMITTED`, does not write `exam_results`, and does not create legacy `submit_payload.enc`.

## 9. Security validation
Narrow security scan covered the bridge, payload prepare flow, UI panel, `ClientHandler`, orchestrator service, and related tests.

Accepted hits:
- Unsafe marker names in blacklist checks.
- Negative test assertions.
- Existing legacy route code in `ClientHandler`, not called by the new bridge.

No new file-write path, Final Submit path, Rust path, Quick Settings path, grading path, or answer-key exposure was introduced.

## 10. Unit test result
Targeted Maven test command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" "-Dtest=TSEV2ServerNoGradingSubmitBridgeServiceTest,TSEV2ClientSubmitPayloadPrepareServiceTest,TSEV2SelectionPanelTest,V2ServerSubmitNoGradingOrchestratorServiceTest,V2AttemptClosureDraftServiceTest" test
```

Result:

```text
28 tests, 0 failures, 0 errors, 0 skipped
BUILD SUCCESS
```

## 11. Maven build result
Full Maven command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install
```

Result:

```text
214 tests, 0 failures, 0 errors, 0 skipped
BUILD SUCCESS
```

## 12. Portable build result
Portable build command:

```powershell
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
```

Result:

```text
BUILD SUCCESS
Portable output: D:\Ban_sao_du_an\dist\TutorHubSecureExam
```

## 13. run_input_test status

```text
PENDING - VM-only / skipped by fast-track rule.
```

Lockdown GUI was not run on the physical machine.

## 14. Remaining risks
- VM-only runtime regression still needs to be executed for the legacy portable flow.
- The V2 debug UI is still Swing-based and should remain debug-only until the full secure exam UX is finalized.
- The canonical server route is validated by unit/regression tests, but a real server socket run should be covered in a later VM test phase.

## 15. Next phase proposal
Proceed to the next V2 phase only after VM regression confirms the portable legacy flow still starts, submits, exits cleanly, and leaves no hanging Java/Rust/PowerShell process.
