# HomeSocial Web Panel Data Bridge Report

## 1. DTO created

Created in `src/main/java/com/mycompany/tutorhub_enterprise/client/home/`:

- `HomeBannerItem`
- `HomeLocketItem`
- `HomeSocialState`

The DTOs are intentionally small and public-field based because this phase only needs a lightweight Java to WebView JSON contract.

## 2. HomeSocialWebPanel API

Added public API:

- `setBannerItems(List<HomeBannerItem> banners)`
- `setLocketItems(List<HomeLocketItem> items)`
- `setHomeSocialState(HomeSocialState state)`
- `refreshState()`

Implementation details:

- Uses Gson with `disableHtmlEscaping()` to serialize state.
- Caches state if WebView is not ready.
- Flushes cached state after `home-social.html` loads successfully.
- Pushes state through:

```javascript
window.TutorHubHomeSocial.setState(...)
```

Required logs are present:

- `[HOME_SOCIAL] State queued`
- `[HOME_SOCIAL] State pushed to WebView`
- `[HOME_SOCIAL][ERROR] State push failed: ...`

Also fixed the bridge lifetime by keeping a strong Java reference to `HomeSocialBridge`, so WebView click events continue to reach Java after page load.

## 3. JavaScript setState behavior

`src/main/resources/home-social/home-social.js` now exposes:

```javascript
window.TutorHubHomeSocial = {
  setState: function (state) { ... }
};
```

Behavior:

- Validates state defensively.
- Renders banners from `state.banners`.
- Renders locket cards from `state.locketItems`.
- Falls back to polished local banners when banner data is empty or invalid.
- Shows empty state when locket data is empty.
- Keeps mock data only as a fallback, not as the primary source after Java pushes state.
- Avoids modern unsupported syntax such as `??`, optional chaining, modules, and top-level await.

## 4. HomeTab banner data

`HomeTab` now initializes `HomeSocialWebPanel` with Java-provided default banner data:

- `../images/slide1.png`
- `../images/slide2.png`
- `../images/slide3.png`

This keeps current static visual behavior while moving ownership of the data contract to Java.

## 5. HomeTab and MainDashboard locket data

Backward-compatible method retained:

```java
public void loadReelsToVideoSection(java.util.List<String> data)
```

The method now maps old locket rows to `HomeLocketItem`:

```text
id;;url;;title;;media_type;;author;;avatarBase64
```

Mapping:

- `id` -> `HomeLocketItem.id`
- `url` -> `imageUrl` and `thumbnailUrl`
- `title` -> `caption`
- `author` -> `authorName`
- `avatarBase64` -> `authorAvatar` data URL when present
- `timeText` -> `Vua xong`
- `likeCount/commentCount` -> `0`
- `likedByMe/canDelete` -> `false`

`MainDashboard` can keep calling:

```java
homeTab.loadReelsToVideoSection(locketVideos);
```

No packet/action or backend flow was changed.

## 6. Backward compatibility

`loadReelsToVideoSection(...)` remains available in `HomeTab`.

Legacy smoke test passed:

```text
[HOME_SOCIAL] Legacy locket data available, count=2
[HOME_SOCIAL] Bridge event: HOME_SOCIAL_STATE_APPLIED payload={"bannerCount":3,"locketCount":2,"locketCanPost":true}
[HOME_SOCIAL] State pushed to WebView
```

## 7. Bridge event logs

Bridge events still log on Java side:

- `HOME_SOCIAL_READY`
- `HOME_SOCIAL_STATE_APPLIED`
- `HOME_BANNER_CLICK`
- `LOCKET_CREATE_OPEN`
- `LOCKET_VIEW_OPEN`
- `LOCKET_POST_REACT`
- `LOCKET_COMMENT_OPEN`

Smoke test verified:

```text
[HOME_SOCIAL] Bridge event: HOME_BANNER_CLICK payload=...
[HOME_SOCIAL] Bridge event: LOCKET_POST_REACT payload=...
[HOME_SOCIAL] Bridge event: LOCKET_COMMENT_OPEN payload=...
[HOME_SOCIAL] Bridge event: LOCKET_CREATE_OPEN payload=...
```

## 8. Backend touched?

No backend code was changed.

Not changed:

- `ClientHandler`
- `AuthProtocol`
- `DatabaseManager`
- DB schema
- Reels backend
- Exam/TSE/Question Bank
- Practice/On tap
- new packet/action

## 9. Build and test result

JavaScript syntax:

```powershell
node --check ".\src\main\resources\home-social\home-social.js"
```

Result: passed.

Maven build:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
```

Result: `BUILD SUCCESS`.

Existing warnings remain in JavaFX dependencies and `ScheduleTab.java` varargs calls. They are unrelated to this phase.

Packaging:

```powershell
Copy-Item ".\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" ".\HF_UPLOAD\update.jar" -Force
```

Result: `D:\Ban_sao_du_an\HF_UPLOAD\update.jar` updated.

Jar resource check:

```text
home-social/
com/mycompany/tutorhub_enterprise/client/home/HomeBannerItem.class
com/mycompany/tutorhub_enterprise/client/home/HomeLocketItem.class
com/mycompany/tutorhub_enterprise/client/home/HomeSocialState.class
home-social/home-social.css
home-social/home-social.html
home-social/home-social.js
```

## 10. Remaining risks

- This phase still uses bootstrap/sample locket items in Java until real legacy data arrives.
- Legacy image URLs that point to non-resource server paths may rely on WebView fallback gradients unless the URL is reachable.
- Click events are logged only; no real upload, like, comment, or backend mutation is implemented in this phase.
- Full visual acceptance should still be checked in the running dashboard window.
