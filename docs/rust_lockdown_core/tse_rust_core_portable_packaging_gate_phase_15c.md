# Phase 15C: Java/Rust IPC Portable Packaging Gate

## Objective
To outline the integration and distribution approach for shipping the compiled `rust-core.exe` binary alongside the final Java portable build without disturbing the existing legacy functionality or V2 Submit infrastructure.

## Packaging Decision
- **Status**: PENDING_PACKAGING_DECISION
- **Description**: The final copy directory within the `dist/` portable bundle is not yet permanently decided. We are holding the file in the `rust-core/target/release` output directory until the overarching packaging structure accommodates safe VM distribution guidelines. We do not automatically overwrite files in `dist/` at this stage.

## Java IPC Verification
The `V2RustLockdownCoreProbeService` has been structured to handle multiple potential resolutions for the binary path:
1. It handles resolving `rust-core.exe`.
2. It fails safely if missing (returns `RUST_CORE_NOT_FOUND`).
3. It aborts safely if the binary hangs (returns `RUST_CORE_TIMEOUT`).
4. Background processes do not leak, and Java's `ProcessBuilder` accurately consumes standard streams before destruction.

## Artifact State
Because packaging is delayed until the next phase grouping, the portable distribution remains unaffected by `rust-core.exe`. No production flags have been modified.
