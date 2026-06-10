---
title: TutorHub Sync
colorFrom: green
colorTo: indigo
sdk: docker
pinned: false
license: mit
---

# TutorHub Sync Server

Node.js server for LiveKit token issuing, upload helpers, and client update metadata.

## Required files

- `.gitattributes`
- `Dockerfile`
- `README.md`
- `package.json`
- `package-lock.json`
- `server.js`
- `update.jar`
- `version.json`

## Required secrets

Configure these in Hugging Face Space Settings if the related features are enabled:

- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`
- `B2_ENDPOINT`
- `B2_KEY_ID`
- `B2_APPLICATION_KEY`
- `B2_BUCKET`
