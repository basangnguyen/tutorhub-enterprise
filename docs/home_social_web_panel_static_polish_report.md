# HomeSocial Web Panel Static Polish Report

## 1. Files edited

- `src/main/java/com/mycompany/tutorhub_enterprise/client/home/HomeSocialWebPanel.java`
- `src/main/resources/home-social/home-social.css`
- `src/main/resources/home-social/home-social.js`

Existing Phase 2A integration remains in:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/HomeTab.java`
- `src/main/resources/home-social/home-social.html`

## 2. Height change

- Reduced `HomeSocialWebPanel` preferred/min/max height from 560px to 520px.
- Matched `.home-social-shell` height to 520px.
- Reduced vertical padding and section gaps so the next Swing section, `Lop hoc noi bat`, starts closer to the web panel.

## 3. Banner polish

- Reduced banner height to 156px.
- Reduced banner title scale and tightened text hierarchy.
- Softened overlay and CTA styling.
- Kept rounded corners, dots, arrows, auto slide, and image cover behavior.
- Added layered fallback backgrounds so missing images do not render as flat gray.

## 4. Locket feed polish

- Reduced card width to 268px and image height to 148px.
- Lightened image overlays and removed the heavy dark-gradient feel.
- Balanced avatar, text, metrics, and footer spacing.
- Removed the bookmark action from the card footer for a cleaner feed.
- Added loading skeleton, empty state, and error state markup/styles.
- Added nicer fallback image gradients for cards with missing image assets.

## 5. Carousel and scroll changes

- Locket carousel now scrolls by one card step instead of a fixed wide jump.
- Rail has left/right padding so the first and last cards are not cut harshly.
- Browser scrollbar is hidden for WebView polish.
- Arrow buttons are disabled at rail edges and hidden when the list does not overflow.
- Resize and scroll listeners keep arrow state in sync.

## 6. Backend touched?

No backend code was changed.

No database, server packet/action, login, exam, practice, reels backend, or network flow was modified.

## 7. Build result

Command:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
```

Result:

- `BUILD SUCCESS`
- Existing warnings remain in JavaFX dependency model and `ScheduleTab.java` varargs calls.
- No new compile error from HomeSocial Phase 2B.

## 8. Packaging and smoke test result

Copied:

```powershell
.\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar -> .\HF_UPLOAD\update.jar
```

Resource check:

```text
home-social/
home-social/home-social.css
home-social/home-social.html
home-social/home-social.js
```

JavaScript syntax check:

```powershell
node --check src/main/resources/home-social/home-social.js
```

Result: passed.

JShell smoke test:

```text
[HOME_SOCIAL] HomeSocialWebPanel initialized
HOME_TAB_SMOKE componentCount=1
[HOME_SOCIAL] Loading /home-social/home-social.html
[HOME_SOCIAL] Bridge event: HOME_SOCIAL_READY payload={"bannerCount":3,"locketCount":5}
[HOME_SOCIAL] Loaded home-social.html
HOME_TAB_SMOKE done
```

Result: HomeTab initializes and the HomeSocial bridge reports ready.

## 9. Remaining risks

- This phase verifies static resource packaging and WebView bridge smoke only.
- Final visual acceptance should still be checked in the real dashboard window, especially carousel card cropping on the user's actual screen width.
- The data remains static mock data by design for Phase 2B.
