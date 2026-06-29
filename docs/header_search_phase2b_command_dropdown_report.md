# Header Search Phase 2B - Command Dropdown Report

## 1. Files changed

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/GlobalSearchBar.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchDropdownPanel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/HeaderPanel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/ChatTab.java`

`ChatTab.java` was touched only to prevent its legacy header-search popup and `SEARCH_USER` request from running while the active card is not `Chat`.

## 2. SearchDropdownPanel integration

`SearchDropdownPanel` is now wired into `GlobalSearchBar`.

- Focus in the global header search opens the command dropdown.
- Typing refreshes the command list.
- Focus loss or outside click closes the dropdown.
- `Esc` closes the dropdown.

## 3. Supported commands

Phase 2B supports local command results only:

- `Mở Bảng tin` -> `Home`
- `Mở Tin nhắn` -> `Chat`
- `Mở Lớp học` -> `Saved`
- `Mở Lịch` -> `Schedule`
- `Mở QuizHub` -> `QuizHub`
- `Mở Tài liệu` -> `Docs`
- `Mở Hồ sơ` -> `Profile`
- `Mở Nâng cấp` -> `Upgrade`

When the user types a non-empty query, the dropdown also shows:

- `Tìm trong TutorHub: <query>`

That row is a safe no-op placeholder for future full internal search.

## 4. Keyboard navigation

- `Down`: select next result.
- `Up`: select previous result.
- `Enter`: run selected command.
- `Esc`: close dropdown.

Command execution uses `MainDashboard.switchToCard(...)` and then clears the global search field.

## 5. ChatTab impact

The global dropdown is disabled while the active card is `Chat`.

`ChatTab` also now receives an active-card predicate from `MainDashboard`, so its existing chat search popup only runs while the active card is `Chat`. This avoids popup overlap and prevents accidental backend `SEARCH_USER` calls from normal global command search.

## 6. Backend impact

No backend files were changed.

No server, database, protocol, `NetworkManager`, `ClientHandler`, `UserDAO`, `QuestionBank`, `Exam`, `TSE`, or `ProfileTab.java` changes were made.

## 7. Build and test result

Build command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
```

Result:

- Build success.
- Existing warnings remain in `ScheduleTab.java` and OpenJFX dependency metadata. They are unrelated to Phase 2B.

The first build attempt inside sandbox failed because Maven could not access Maven Central. It was rerun with approval and completed successfully.

## 8. update.jar

Copied:

```powershell
Copy-Item -Path ".\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" -Destination ".\HF_UPLOAD\update.jar" -Force
```

Result:

- `D:\Ban_sao_du_an\HF_UPLOAD\update.jar`
- Size: `263227219` bytes

## 9. Remaining risk

- This phase is command-only. It does not search real messages, classes, files, or remote providers.
- The `Tìm trong TutorHub` row is intentionally a placeholder.
- UI runtime interaction still needs manual visual verification in the desktop app: focus, typing `lich`, `quiz`, keyboard navigation, command switching, and ChatTab search behavior.
