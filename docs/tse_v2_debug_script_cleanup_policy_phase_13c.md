# Phase 13C: Debug Script Cleanup Policy

## 1. Overview
This policy defines the hygiene rules for handling utility and debug scripts generated during the Phase 7 to 13 implementation cycle. 

## 2. General Principles
- Scripts should not be committed to the master branch.
- They must remain untracked or added to `.gitignore` during the transitional period.
- No files should be hard deleted before a successful production release verification.

## 3. Categories

### Keep Temporarily (Do Not Delete)
Scripts related to `fix_`, `patch_`, or `recreate_` that may be required for quick rollback or diagnostic purposes during staging.

### Safe to Delete Manually After Backup
One-off generation scripts (e.g. `generate_*.py`) that have already successfully applied code changes.

### Exclude from Commit
All `.py`, `.ps1` and `.bat` debug scripts (except `build_portable.ps1`) must be excluded from git index.

## Appendix A — Full Debug Script Filename Inventory
- `fix_client.py`
- `fix_pom.py`
- `generate_tests.py`
- (All other utility scripts generated during development)
