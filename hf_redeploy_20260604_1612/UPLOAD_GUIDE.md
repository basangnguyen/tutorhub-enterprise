# TutorHub Fresh Hugging Face Redeploy

Use this folder to recreate the Hugging Face Spaces from scratch.

## 1. Core server

Space name:

```text
Hocba299-3/tutorhub-core
```

Upload everything inside:

```text
tutorhub-core/
```

Important file:

```text
TutorServer.jar
```

The jar manifest must be:

```text
Main-Class: com.mycompany.tutorhub_enterprise.server.TutorServer
```

## 2. Sync/update server

Space name:

```text
Hocba299-3/tutorhub-sync
```

Upload everything inside:

```text
tutorhub-sync/
```

Important file:

```text
update.jar
```

The update jar manifest must be:

```text
Main-Class: com.mycompany.tutorhub_enterprise.client.LoginFrame
```

## 3. Optional AI server

Create only if you use the Lavie AI backend as a separate Space.

Upload everything inside:

```text
tutorhub-ai-optional/
```

## Upload order

For each Space, upload small files first, commit, then upload the large `.jar` file. This avoids confusing Hugging Face's web uploader.

## After deploy

Open each Space's Logs tab and verify it is Running before testing the Java client.
