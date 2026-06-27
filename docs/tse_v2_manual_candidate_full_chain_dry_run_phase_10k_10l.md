# TutorHub Secure Exam V2 - Manual Candidate Full-chain Dry-run Gate + In-memory Pipeline Simulation (Phase 10K + 10L)

## 1. Overview
This document outlines the Phase 10K + 10L implementation of the TutorHub Secure Exam V2 rollout. In this phase, we implemented a full-chain dry-run gate and an in-memory pipeline simulation for manual candidate submissions. This serves as the final safe checkpoint before proceeding to actual partial or full submissions that persist state to the database.

**Crucial Constraints Applied:**
- **No Side Effects:** Absolutely zero database writes (`INSERT`, `UPDATE`, `DELETE`) are performed in this phase.
- **Data Privacy:** Simulation results are strictly sanitized. Sensitive internal data such as raw scores, percentages, answer keys, correctly matched options, or the canonical payload JSON are **NOT** exposed to the client DTOs.
- **In-memory Execution:** The `V2InMemoryScoreDraftCalculator` computes scoring purely in RAM and produces internal models that are discarded after deriving basic simulation metrics.

## 2. Key Components

### 2.1 Feature Flags and Actions
- **Flags (`V2SubmitFeatureFlags.java`)**: 
  - `tse.v2.manualCandidateFullChainDryRun.enabled`
  - `tse.v2.inMemoryPipelineSimulation.enabled`
- **Actions (`V2SubmitActions.java`)**:
  - `EXAM_SUBMIT_V2_MANUAL_CANDIDATE_FULL_CHAIN_DRY_RUN_GATE`
  - `EXAM_SUBMIT_V2_IN_MEMORY_PIPELINE_SIMULATION`

### 2.2 DTOs (Data Transfer Objects)
- `V2ManualCandidateFullChainDryRunGateResult`: Evaluates readiness for full-chain dry run. Returns metrics such as `examId`, `paperId`, and `answerCount`.
- `V2InMemoryPipelineSimulationResult`: Evaluates in-memory execution. Returns planned pipeline steps (e.g., `PAYLOAD_PARSE`, `ANSWER_KEY_RESOLVE`, `SCORE_DRAFT_COMPUTE_IN_MEMORY`, `SUBMIT_RECORD_CREATE_PLANNED`), step counts, and question counts without emitting scores.

### 2.3 Services
- **`V2InMemoryScoreDraftCalculator`**: Stateless utility for performing mock grading strictly in RAM, preventing premature execution against the actual `V2ScoreDraftService`.
- **`V2ManualCandidateFullChainDryRunGateService`**: The gatekeeper. Ensures all preceding services (Manual Candidate Check, Orchestrator Gate, Payload Contract Validation, and Dependency Health) are `ready=true`. It also checks that no existing `exam_results` record exists for the given attempt.
- **`V2InMemoryPipelineSimulationService`**: Coordinates the internal parse, answer key resolution, and score drafting. It constructs a list of planned actions to provide visibility into what the final submission execution will entail.

## 3. Integration into `ClientHandler`
The two actions are routed via `ClientHandler.java`. The services are instantiated internally by `ClientHandler` utilizing default constructors to streamline complex dependency injection chaining for read-only layers.

## 4. Verification Checklists
- [x] Compilation successful (`mvn clean install` passes with 467 tests).
- [x] Strict security sweep utilizing `findstr` on the specific Phase 10K+10L artifacts confirming the absence of `UPDATE`, `INSERT`, `rawScore`, `percentage`, etc., in the exposed attributes.
- [x] Zero invocations to `EXAM_SUBMIT` or Final Rust submit procedures.
- [x] The `payloadJson` from the client is internally parsed and discarded, not echoed back.

## 5. Next Phase Transition
This phase proves the pipeline logic up to the final commit boundary. The next steps involve transitioning into formal state persistence (Partial Execution or Full Execution logic) now that we know the payload successfully traverses the entire validation matrix.
