# Phase 10G + 10H: Payload/AnswerKey Resolver Schema Implementation + Score Draft Dependency Hardening

## Overview
This document records the implementations and hardening measures taken to eliminate `NullPointerException` (NPE) risks in `V2ScoreDraftService` due to missing schemas for the payload and answer keys.

## Phase 10G: Schema Validations and Resolvers

### Answer Key Resolver
- **Schema Validation**: Verified `exam_paper_questions` and `question_options` tables via `ExamDatabaseManager.java`. The `is_correct` column dictates the answer key.
- **Implementation**: Created `V2DatabaseAnswerKeyResolver` using `com.mycompany.tutorhub_enterprise.server.DatabaseManager`. It utilizes a read-only `SELECT` statement joining the questions and options tables.
- **Constraints**: 
  - Strictly read-only (`SELECT`).
  - No `INSERT/UPDATE/DELETE/MERGE`.
  - Bounded by the `tse.v2.databaseAnswerKeyResolver.enabled` feature flag.

### JSON Payload Parser
- **Schema Validation**: The actual format of `payloadJson` remains unverified (whether it uses `{"1": 2}` or `{"answers": [{"questionId": 1, ...}]}`).
- **Implementation**: Created `V2JsonAnswerPayloadParser`.
- **Constraints**:
  - Implemented as an unavailable-safe sentinel.
  - Returns `null` as instructed because the schema is not reliably known, avoiding blind parsing and preventing fatal errors down the chain.

## Phase 10H: Dependency Hardening
- Replaced `answerKeyResolver = null` and `payloadParser = null` with concrete objects (guarded by their own availability states).
- Upgraded `V2ScoreDraftService` to gracefully return `ERROR_V2_SCORE_DRAFT_PAYLOAD_PARSER_UNAVAILABLE` or `ERROR_V2_SCORE_DRAFT_ANSWER_KEY_RESOLVER_UNAVAILABLE` instead of throwing `NullPointerException`.
- Implemented `V2ScoreDraftDependencyHealthService` to expose health metrics.
- Rejected payloads or schemas safely return `NOT_READY`.

## Security Notes
- `payloadJson` and `answerKey` objects are not logged, exposed, or written back to the client interface.
- Unit tests use mock DAO layers without touching Neon DB.
