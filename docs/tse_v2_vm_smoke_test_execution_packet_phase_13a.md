# Phase 13A: VM Smoke Test Execution Packet

## 1. Overview
This document outlines the procedure to execute the VM Smoke Test for the TSE V2 pipeline before making it the default student submit mode.

## 2. Test Environment setup
- Windows 10/11 VM configured.
- `TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar` built successfully.
- Production environment configurations.
- `defaultStudentSubmitV2=false` (Production is untouched, we only run in isolated tests).

## 3. Test Steps
- Step 1: Start the VM and load the TutorHub portable runtime.
- Step 2: Establish the Client Handler Connection.
- Step 3: Trigger `EXAM_SUBMIT` with default legacy configuration to ensure legacy flow stability.
- Step 4: Override configuration via the test harnessing module (`EXAM_SUBMIT_V2_STUDENT_SUBMIT_E2E_HARNESS_CHECK`) with valid test payloads.
- Step 5: Monitor the 7-stage pipeline (Preflight -> Materialize -> Submit Status -> Drafts -> Publication -> Final Status -> Handoff).
- Step 6: Verify `V2_SUBMIT_READY_FOR_VM_SIGNOFF` / `V2_SUBMIT_READY_FOR_FINAL_REVIEW` is reached.

## 4. Expected Outcomes
- The backend parses the payload successfully.
- No real data is permanently written (Mock payloads are isolated).
- Legacy flow acts as an active fallback.
- No `NullPointerException` or unhandled exceptions.
