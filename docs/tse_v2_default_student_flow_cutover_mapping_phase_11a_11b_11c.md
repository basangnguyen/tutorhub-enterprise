# TSE V2 Default Student Flow Cutover Mapping (Phase 11A + 11B + 11C)

This document outlines the read-only preparatory steps for switching the default student submission flow over to the V2 pipeline.

## Overview

The V2 student flow cutover consists of three "gatekeeper" phases that prepare the system for the eventual switch, while maintaining a strict read-only boundary. No actual legacy logic is modified, and the `defaultStudentSubmitV2` feature flag remains `false`.

### Phase 11A: Cutover Mapping
- Inspects `ClientHandler.java` mapping for the existing `EXAM_SUBMIT` action.
- Detects the presence of both legacy paths and V2 manual orchestration paths.
- Verifies that `defaultStudentSubmitV2` is explicitly false to prevent accidental routing.

### Phase 11B: Student Submit Adapter Dry-Run
- Simulates the route determination for an incoming payload without storing or persisting the data.
- Executes `V2AnswerPayloadContractValidator` to ensure the payload format is compatible with the canonical DTO format required by the V2 pipeline.
- Accurately reports `LEGACY_V1_STUDENT_SUBMIT` as the planned route when `defaultStudentSubmitV2` is false.

### Phase 11C: UI Wiring Readiness Gate
- Aggregates the statuses of Phase 11A, Phase 11B, and the Phase 10U Controlled Cutover Gate.
- Asserts a definitive `READY_FOR_STUDENT_SUBMIT_UI_WIRING` status only if all upstream dependencies are functional and `defaultStudentSubmitV2` remains false.

## Security & Constraints

- **Read-Only**: The `V2StudentSubmitAdapterDryRunService` validates the payload without side effects. No records are inserted into `V2SubmitRecord`, and no external systems (e.g., Rust lock down, message queues) are invoked.
- **DTO Safety**: The resulting result DTOs exclusively contain safe telemetry/metadata attributes (e.g. `success`, `ready`, `errorCode`, `plannedRoute`, `warnings`, `blockingReasons`). Sensitive fields such as `payloadJson`, `answerKey`, `score`, and `perQuestionResults` are meticulously excluded.
- **Legacy Integrity**: `NetworkTSEExamService.java` remains unaltered. The original "EXAM_SUBMIT" client code and legacy parsing remain completely undisturbed during this diagnostic phase.
