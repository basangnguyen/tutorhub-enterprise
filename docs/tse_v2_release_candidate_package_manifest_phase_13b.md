# Phase 13B: Release Candidate Package Manifest

## 1. Overview
This manifest records the exact file changes, build instructions, and verification steps necessary for packaging the Release Candidate.

## 2. Included Modules
- `ClientHandler.java`
- `V2SubmitActions.java`
- `V2SubmitFeatureFlags.java`
- Bridge implementations and adapter layers.
- Validation, Execution, Drafting, and Handoff Gate Services.

## 3. Excluded Elements
- Rust/native integration payloads.
- Production UI triggers.
- NetworkTSEExamService.java changes.
- application.properties changes.

## 4. Build Instructions
```bash
mvn clean install
build_portable.ps1
```
The output `TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar` is the definitive Release Candidate artifact.
