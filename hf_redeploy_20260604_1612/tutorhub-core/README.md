---
title: TutorHub Core
colorFrom: indigo
colorTo: blue
sdk: docker
pinned: false
license: mit
---

# TutorHub Core Server

Java WebSocket server for TutorHub Enterprise.

## Required files

- `.gitattributes`
- `Dockerfile`
- `README.md`
- `TutorServer.jar`

## Required secrets

Configure these in Hugging Face Space Settings:

- `TUTORHUB_DB_URL`
- `TUTORHUB_DB_USER`
- `TUTORHUB_DB_PASSWORD`

Optional:

- `TUTORHUB_SPEEDSMS_TOKEN`
- `TUTORHUB_B2_ACCESS_KEY`
- `TUTORHUB_B2_SECRET_KEY`
- `TUTORHUB_B2_BUCKET`
- `TUTORHUB_B2_PUBLIC_BASE_URL`
