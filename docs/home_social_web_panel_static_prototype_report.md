# Home Social Web Panel Static Prototype Report

## 1. Da Tao File Nao

Da tao moi:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/home/HomeSocialWebPanel.java
src/main/resources/home-social/home-social.html
src/main/resources/home-social/home-social.css
src/main/resources/home-social/home-social.js
docs/home_social_web_panel_static_prototype_report.md
```

## 2. Da Sua HomeTab O Dau

Da sua:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/HomeTab.java
```

Thay phan render cu:

```text
BannerSlider
VideoReelSection
```

bang:

```text
HomeSocialWebPanel
```

Cac phan con lai cua HomeTab nhu class header, filter, sort, class cards, list/grid view va logic class list duoc giu nguyen.

Method legacy:

```text
loadReelsToVideoSection(List<String> data)
```

van duoc giu de MainDashboard khong bi vo. Trong Phase 2 static prototype, method nay chi log so item legacy va khong bind backend data vao UI moi.

## 3. Banner Moi Hoat Dong The Nao

Banner moi nam trong:

```text
src/main/resources/home-social/home-social.html
src/main/resources/home-social/home-social.css
src/main/resources/home-social/home-social.js
```

Tinh nang:

- Render bang HTML/CSS/JS trong JavaFX WebView.
- Dung mock/static data trong `home-social.js`.
- Co 3 slide.
- Co dots indicator.
- Co nut previous/next.
- Co auto slide 5.2 giay.
- Co hover pause.
- Click banner gui bridge event `HOME_BANNER_CLICK`.
- Anh lay tu resources hien co:
  - `images/slide1.png`
  - `images/slide2.png`
  - `images/slide3.png`

Da kiem tra cac file anh mock ton tai trong `src/main/resources/images`.

## 4. Locket Mock Feed Hoat Dong The Nao

Locket Class moi duoc render trong cung WebView voi Banner.

Tinh nang:

- Header `Locket Class`.
- Nut `Xem tat ca`.
- Nut scroll trai/phai.
- Horizontal feed.
- Card anh co overlay gradient.
- Card co avatar initials, ten, thoi gian, caption.
- Co tim va so tim.
- Co binh luan va so binh luan.
- Khong co nut share.
- Co card `+` de mo luong dang anh sau nay.
- Click card gui `LOCKET_VIEW_OPEN`.
- Click card `+` gui `LOCKET_CREATE_OPEN`.
- Click tim toggle local UI mock va gui `LOCKET_POST_REACT`.
- Click comment gui `LOCKET_COMMENT_OPEN`.

Data hien la mock trong `home-social.js`, chua noi backend.

## 5. Bridge Event Hien Chi Log Gi

`HomeSocialWebPanel` expose Java bridge:

```java
public void onEvent(String type, String payloadJson)
public void log(String message)
```

Log bat buoc da co:

```text
[HOME_SOCIAL] HomeSocialWebPanel initialized
[HOME_SOCIAL] Loading /home-social/home-social.html
[HOME_SOCIAL] Loaded home-social.html
[HOME_SOCIAL] Bridge event: ...
```

Smoke test tu `HF_UPLOAD/update.jar` da ghi nhan:

```text
[HOME_SOCIAL] HomeSocialWebPanel initialized
[HOME_SOCIAL] Loading /home-social/home-social.html
[HOME_SOCIAL] Bridge event: HOME_SOCIAL_READY payload={"bannerCount":3,"locketCount":5}
[HOME_SOCIAL] Loaded home-social.html
```

## 6. Co Dung Backend Khong

Khong.

Phase nay khong sua:

```text
ClientHandler.java
DatabaseManager.java
AuthProtocol.java
Packet contract
DAO/service backend
```

Khong tao action packet moi, khong tao DB schema moi, khong migration.

## 7. Co Dung Practice Khong

Khong.

Khong sua module Practice/On tap, Exam/TSE, Question Bank, Login, Schedule, Chat, Reels, Profile.

## 8. Build Result

Da chay:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
```

Ket qua:

```text
BUILD SUCCESS
```

Co warning san co lien quan JavaFX effective model va mot so warning compile o `ScheduleTab.java`, khong phai do phase nay.

## 9. Manual Test Result

Da copy jar:

```powershell
Copy-Item ".\target\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar" ".\HF_UPLOAD\update.jar" -Force
```

Da kiem tra resource trong JAR:

```powershell
jar tf ".\HF_UPLOAD\update.jar" | findstr /I "home-social"
```

Ket qua:

```text
home-social/
home-social/home-social.css
home-social/home-social.html
home-social/home-social.js
```

Da chay smoke test instantiate HomeTab tu chinh `HF_UPLOAD/update.jar` bang JShell.

Ket qua:

```text
HOME_TAB_SMOKE componentCount=1
[HOME_SOCIAL] Bridge event: HOME_SOCIAL_READY payload={"bannerCount":3,"locketCount":5}
HOME_TAB_SMOKE done
```

Chua thuc hien nghiem thu visual bang mat tren dashboard dang nhap that trong phien nay. Buoc do can mo app, dang nhap va vao tab Bang tin lop de kiem tra truc quan banner/locket.

## 10. Rui Ro Con Lai

- JavaFX WebView co the khac Chrome moi, nen can test visual truc tiep tren may target.
- Phase nay dung mock data, chua bind Locket backend that.
- `loadReelsToVideoSection` hien chi log du lieu legacy, chua render data that.
- Height cua `HomeSocialWebPanel` dang co dinh 560px de on dinh trong Swing BoxLayout; co the can tinh chinh sau khi xem tren dashboard that.
- Anh trong WebView la local resource, neu sau nay dung URL cloud can them loading/error/fallback anh.

