# TutorHub Redeploy Build Report

Build date: 2026-06-04

## Java build

Commands run:

```powershell
mvn clean package
mvn package -P server -DskipTests
```

Result:

- Tests: 10 passed, 0 failed
- Client/update jar: `tutorhub-sync/update.jar`
- Core server jar: `tutorhub-core/TutorServer.jar`

Known warnings:

- OpenJFX effective model warnings
- `ScheduleTab.java` varargs warnings
- SLF4J no provider warning at runtime

These warnings did not fail the build.

## Jar verification

`tutorhub-core/TutorServer.jar`:

```text
Main-Class: com.mycompany.tutorhub_enterprise.server.TutorServer
```

Contains:

```text
com/mycompany/tutorhub_enterprise/server/ClientHandler.class
com/mycompany/tutorhub_enterprise/models/auth/AuthProtocol.class
com/mycompany/tutorhub_enterprise/models/auth/AuthResponse.class
```

`tutorhub-sync/update.jar`:

```text
Main-Class: com.mycompany.tutorhub_enterprise.client.LoginFrame
```

## Secret scan

The deploy folder was scanned for known hardcoded keys:

```text
AIza
gsk_
postgres://
APIXtb
zJXUV
0042097aa5ea
K004hlCV
neondb_owner
```

No matches were found.

## Upload recommendation

Create fresh Hugging Face Spaces with the same names to keep URLs unchanged:

```text
Hocba299-3/tutorhub-core
Hocba299-3/tutorhub-sync
```

Upload small files first, commit, then upload the large jar file.
