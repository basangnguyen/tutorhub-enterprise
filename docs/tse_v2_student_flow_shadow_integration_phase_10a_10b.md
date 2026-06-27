# Phase 10A + 10B: V2 Shadow Integration into Student Exam Flow + Primary Flow Cutover Readiness Gate

## Overview

This phase implements the final checks needed to integrate the V2 Submit mechanism securely into the Student Exam Flow. It introduces:
1. **Phase 10A**: A **Shadow Integration Check** to evaluate if the current environment and session are capable of entering the V2 submission flow instead of the legacy flow.
2. **Phase 10B**: A **Cutover Readiness Gate** to verify all downstream prerequisites (finalization, score draft, publication verification, etc.) are globally enabled and ready.

## Constraints Followed
- No database write operations were performed.
- Valid status candidates strictly checked for "IN_PROGRESS" or similar actively testing statuses. `SUBMITTED`, `COMPLETED`, and `GRADED` immediately return `ready=false`.
- The final verification gate explicitly warns if downstream processes are not fully enabled globally.
- Payload responses for these checks do not contain sensitive data like grading results, correct answers, scores, etc.

## Architecture

### 1. Feature Flags
Located in `V2SubmitFeatureFlags.java`:
- `tse.v2.studentFlowShadowIntegration.enabled` (default `false`)
- `tse.v2.studentFlowCutoverReadiness.enabled` (default `false`)

### 2. Actions
Located in `V2SubmitActions.java`:
- `EXAM_SUBMIT_V2_STUDENT_FLOW_SHADOW_CHECK`
- `EXAM_SUBMIT_V2_STUDENT_FLOW_CUTOVER_READINESS`

### 3. Service Layer
- **V2StudentFlowShadowCheckService**: Verifies attempt mapping, user matching, and attempt status validity.
- **V2StudentFlowCutoverReadinessService**: Evaluates the `ShadowCheckService` and asserts that all other necessary features for the end-to-end V2 submit flow are functional.

## Results
- 388 unit tests passed successfully, ensuring no regression.
- Code matches secure coding requirements, ensuring robust integration potential for V2 Submit.
