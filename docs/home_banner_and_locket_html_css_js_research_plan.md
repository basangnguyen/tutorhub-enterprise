# Home Banner + Locket HTML/CSS/JS Research Plan

## 1. Executive Summary

Muc tieu phu hop nhat cho HomeTab hien tai la **giu Swing lam khung chinh**, chi chuyen hai khu vuc can do hoa va tuong tac cao sang HTML/CSS/JavaScript:

- Banner / Slide / Carousel dau trang.
- Locket Class.

Khong nen rewrite toan bo HomeTab trong giai doan nay. Cac phan nhu "Lop hoc noi bat", class cards, filter, sort, open class va accepted class dang gan voi UI/logic Swing hien tai; rewrite toan bo se tang rui ro regression, lam cham tien do va khong can thiet de dat muc tieu giao dien.

Kien truc de xuat: tao **mot WebView/JFXPanel chung** ten `HomeSocialWebPanel`, render ca Banner va Locket trong mot HTML bundle:

```text
src/main/resources/home-social/home-social.html
src/main/resources/home-social/home-social.css
src/main/resources/home-social/home-social.js
```

Ly do: it WebView hon, UI dong bo hon, dung chung CSS/animation/data bridge, giam chi phi lifecycle so voi hai WebView rieng. Java van la nguon dieu phoi data, permission va upload; WebView chi phu trach render va gui event ve Java.

## 2. Hien Trang HomeTab Hien Tai

`HomeTab.java` hien la mot Swing panel gom cac khu vuc chinh:

- Banner dau trang: duoc nhung bang `BannerSlider`.
- Locket Class: duoc nhung bang `VideoReelSection`, nhung UI label la "Locket Class".
- Filter / sort / class cards: van la Swing.
- Data locket duoc MainDashboard truyen vao HomeTab qua `loadReelsToVideoSection(List<String> data)`.

HomeTab da co bo cuc kha ro: sidebar/header nam ngoai MainDashboard, HomeTab chi quan ly noi dung bang tin. Diem yeu la cac khu vuc co tinh social/visual cao dang ve bang Java2D/Swing nen kho dat do muot va tinh te nhu ClassIn/Teams/Zoom.

## 3. Hien Trang Banner/Slide Hien Tai

Banner nam trong:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/BannerSlider.java
```

Va duoc add vao HomeTab trong:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/HomeTab.java
```

Hien trang:

- Load anh local tu `/images/slide1.png|jpg` den `/images/slide6.png|jpg`.
- Dung Swing `Timer` de auto slide khoang 4 giay.
- Co fade transition bang alpha.
- Co dots indicator.
- Chua co next/prev button.
- Chua co click action theo banner.
- Anh duoc draw truc tiep vao Graphics2D, chua co co che `object-fit: cover` dung nghia.
- HomeTab ep chieu cao banner khoang 160px.

Banner hien tai chay duoc, nhung do la custom Swing paint, kho polish chi tiet nhu hover, active state, responsive animation va layout hien dai.

## 4. Hien Trang Locket Class Hien Tai

Locket Class nam trong:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/VideoReelSection.java
```

Va duoc dieu phoi tu:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java
```

Hien trang:

- UI title la `Locket Class`.
- Dung `GridLayout(1, 5, 16, 0)`, chi hien toi da 5 card.
- Data dau vao la `List<String>`, moi item split theo `;;`.
- Format Locket hien tai: `id;;url;;title;;media_type;;author;;avatarBase64`.
- Card hien avatar/tac gia/title o muc co ban.
- Click card mo `NativeReelPlayer`.
- Upload Locket dang di qua `UploadLocketDialog` va `NativeReelPlayer`.
- Chua co class_id.
- Chua co like/comment rieng cho Locket.
- Chua co permission theo lop.
- Chua co pagination/cursor.
- Chua co empty/loading/error state theo chuan web.

Ten class `VideoReelSection` khong con dung nghia neu tiep tuc phat trien Locket; trong phase code sau nen tao component moi thay the, khong sua lan man class cu qua sau.

## 5. File/Code Lien Quan Da Doc

Da doc va audit cac file chinh:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/HomeTab.java
src/main/java/com/mycompany/tutorhub_enterprise/client/BannerSlider.java
src/main/java/com/mycompany/tutorhub_enterprise/client/VideoReelSection.java
src/main/java/com/mycompany/tutorhub_enterprise/client/ReelsTabPanel.java
src/main/java/com/mycompany/tutorhub_enterprise/client/UploadLocketDialog.java
src/main/java/com/mycompany/tutorhub_enterprise/client/UploadReelDialog.java
src/main/java/com/mycompany/tutorhub_enterprise/client/NativeReelPlayer.java
src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java
src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java
src/main/java/com/mycompany/tutorhub_enterprise/server/DatabaseManager.java
src/main/java/com/mycompany/tutorhub_enterprise/server/CloudStorageService.java
src/main/java/com/mycompany/tutorhub_enterprise/utils/B2Helper.java
src/main/java/com/mycompany/tutorhub_enterprise/models/Packet.java
src/main/java/com/mycompany/tutorhub_enterprise/utils/PacketDeserializer.java
src/main/java/com/mycompany/tutorhub_enterprise/server/dao/ClassroomDAO.java
src/main/java/com/mycompany/tutorhub_enterprise/models/ClassroomGroupModel.java
src/main/java/com/mycompany/tutorhub_enterprise/models/ClassroomMemberModel.java
pom.xml
```

Da kiem tra dau vet WebView/JFXPanel hien co:

- `MapPickerDialog.java` co WebView + `JSObject window.setMember("javaApp", ...)`.
- `TSEParentHtmlQuickSettingsPopup.java` co WebView + Java bridge.
- `QuizHubTab.java`, `DriveTab.java`, `ScheduleTab.java`, `ReelsTabPanel.java` da co JavaFX/JFXPanel.
- `pom.xml` da co `javafx-web`, `javafx-swing`, `javafx-controls`, `gson`, `jcefmaven`, `flatlaf`, PostgreSQL va AWS SDK.

## 6. Co Nen Rewrite Toan Bo HomeTab Khong

Khong nen.

Ly do:

- HomeTab hien da co nhieu logic gan voi Swing: filter, class list, sort, accepted class, open class.
- Rewrite toan bo se phai viet lai routing, state, event, refresh data va nhieu UI da chay.
- Muc tieu hien tai chi la lam dep Banner va Locket, khong phai thay nen toan bo tab.
- Swing + WebView hybrid da du de nang cap hai khu vuc can do hoa cao.

Chi nen rewrite HomeTab toan bo neu sau nay du an quyet dinh chuyen ca app sang JavaFX/WebView/Electron/Tauri. Do la quyet dinh kien truc lon, khong phu hop voi phase nay.

## 7. De Xuat Chi Chuyen Banner + Locket Sang HTML/CSS/JS

Nen chuyen rieng hai section nay sang WebView:

- Banner/Slide can animation, dots, next/prev, hover pause, responsive image.
- Locket Class can feed card, overlay, viewer, comment panel, reaction state, upload modal.

Nhung phan con lai giu Swing:

- Lop hoc noi bat.
- Class cards.
- Filter.
- Sort.
- Open class.
- Accepted class.

De xuat Java component:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/home/HomeSocialWebPanel.java
src/main/java/com/mycompany/tutorhub_enterprise/client/home/HomeSocialBridge.java
src/main/java/com/mycompany/tutorhub_enterprise/client/home/HomeSocialState.java
```

De xuat resource:

```text
src/main/resources/home-social/home-social.html
src/main/resources/home-social/home-social.css
src/main/resources/home-social/home-social.js
```

## 8. Kien Truc De Xuat: 1 WebView Hay Nhieu WebView

### Huong A: Tach Rieng WebView

```text
BannerCarouselWebPanel
LocketClassWebPanel
```

Uu diem:

- Loi JS cua section nay it anh huong section kia.
- De thay the tung section.
- De fallback tung khu vuc.

Nhuoc diem:

- Nhieu WebView ton RAM/CPU hon.
- Nhieu lifecycle JavaFX hon.
- Nhieu bridge hon.
- Nhieu CSS/JS bundle hon, de lech style.

### Huong B: Mot WebView Chung

```text
HomeSocialWebPanel
```

Uu diem:

- Chi mot WebView nen nhe hon.
- Banner va Locket dong bo spacing, typography, color, animation.
- Mot data bridge, mot lifecycle, mot loading state.
- De dieu chinh responsive theo width cua HomeTab.
- De build thanh mot "social strip" giong san pham that.

Nhuoc diem:

- JS crash co the anh huong ca Banner va Locket.
- Data contract lon hon mot chut.

### Khuyen Nghi

Dung **mot WebView chung** cho phase dau. De giam rui ro, Java side can co:

- Timeout/fallback neu WebView load fail.
- `window.TutorHubHomeSocial.setState(data)` duoc goi defensive.
- JS phai bat loi render tung component, khong de mot card loi lam blank toan section.
- Log bridge ro: `HOME_SOCIAL_READY`, `HOME_SOCIAL_ERROR`, `LOCKET_*`.

## 9. Reels Like/Comment Hien Hoat Dong The Nao

Reels hien co cac action trong `ClientHandler`:

```text
GET_REELS
LIKE_REEL
GET_REEL_COMMENTS
ADD_REEL_COMMENT
UPLOAD_REEL
```

Database hien co:

```text
reels
reel_comments
reel_likes
```

`DatabaseManager.getReels(currentUserId)` tra ve string:

```text
id;;videoUrl;;caption;;hashtags;;likes;;fullName;;avatarBase64;;isLiked;;commentCount;;location;;productLink
```

Like:

- Client toggle local state.
- Gui `LIKE_REEL`.
- Server toggle record trong `reel_likes`.
- Cap nhat `reels.likes`.

Comment:

- Client gui `GET_REEL_COMMENTS`.
- Server tra danh sach comment.
- Client gui `ADD_REEL_COMMENT` voi payload `reelId;;content`.
- Server insert vao `reel_comments`.

Diem manh:

- Da co pattern reaction/comment co ban.
- Da co avatar/fullName/comment count.
- Da co optimistic UI trong ReelsTab.

Diem yeu:

- Payload `;;` brittle, de loi neu caption/comment chua ky tu dac biet.
- Comment reaction hien co phan local UI, chua thay persistence ro.
- Chua co service layer rieng; logic nam nhieu trong `ClientHandler`/`DatabaseManager`.
- Chua co permission theo lop cho Locket.

## 10. Co The Tai Su Dung Gi Tu Reels

Nen tai su dung:

- Y tuong toggle like: unique `(post_id, user_id)`.
- Y tuong comment list/create.
- UI pattern avatar, fullName, count.
- Upload progress pattern cua `UploadReelDialog`.
- B2 upload helper va presigned URL pattern, neu duoc bao boi permission server.

Khong nen copy nguyen:

- String payload `;;`.
- UI Swing phuc tap trong `ReelsTabPanel`.
- Bang `reels` cho Locket.
- Global feed assumption cua Reels.

Locket nen co service/action rieng, vi Locket la noi dung theo lop, co privacy va permission khac Reels.

## 11. Upload/Media Hien Co Gi

Hien co hai huong upload:

```text
B2Helper
CloudStorageService
```

`B2Helper`:

- Doc cau hinh tu env/system properties `TUTORHUB_B2_*`.
- Co upload base64 image.
- Co lay presigned URL cho anh/video.
- Hien dang duoc client-side code su dung o mot so luong.

`CloudStorageService`:

- Server-side S3/MinIO-like service.
- Dung env `TUTORHUB_STORAGE_*`.
- Upload file va tra public URL.

`UploadLocketDialog`:

- Swing dialog ho tro drag/drop va file chooser.
- Chu yeu image upload.
- Filter dang noi JPG/PNG/WEBP nhung implementation can kiem lai vi filter hien chi chac JPG/PNG.
- Chua co camera.
- Preview con don gian.

De xuat Locket upload flow phase dau:

```text
1. User bam Add Locket.
2. Java mo modal upload image native/Swing hoac Web modal goi Java file chooser.
3. User chon anh.
4. Java preview va nen/resize thumbnail tren background worker.
5. Upload len storage.
6. Gui LOCKET_POST_CREATE voi imageUrl/thumbnailUrl/caption/classId.
7. Server verify membership.
8. Server luu record.
9. Client refresh feed bang LOCKET_POST_LIST.
```

Camera capture nen de phase sau, vi Java desktop + WebView camera permission co rui ro cao hon chon anh tu may.

## 12. Rui Ro Quyen Rieng Tu/Class Permission

Locket Class khong nen la feed public.

Rui ro hien tai:

- `locket_videos` chua co `class_id`.
- `GET_LOCKET_VIDEOS` hien lay tat ca Locket, khong filter theo lop.
- `deleteLocket(id,userId)` chi check owner, chua cho giao vien/admin xoa.
- Anh co the dang la URL public/presigned dai han.
- Chua co rate limit cho comment/reaction/upload.
- Chua co moderation/sanitize caption/comment.

Rule can co:

- Chi member da duyet cua lop moi duoc xem Locket cua lop.
- Chi member da duyet moi duoc react/comment.
- Owner bai dang, giao vien lop, chu lop hoac admin moi duoc xoa.
- Server khong tra URL anh cho user khong co quyen.
- Anh private nen dung presigned URL TTL ngan hoac endpoint proxy co auth.
- Strip EXIF/GPS truoc khi upload neu co the.
- Gioi han size/type: JPG, PNG, WEBP; phase sau moi video.

## 13. De Xuat Data Model

DTO phia client/WebView:

```json
{
  "currentUser": {
    "id": 12,
    "name": "Nguyen A",
    "avatarUrl": "..."
  },
  "banner": {
    "items": [
      {
        "id": "dream-platform",
        "imageUrl": "/images/slide1.png",
        "title": "Explore Dream Platform",
        "cta": "Join now",
        "target": null
      }
    ]
  },
  "locket": {
    "classId": 101,
    "canPost": true,
    "items": [
      {
        "id": 9001,
        "classId": 101,
        "authorId": 12,
        "authorName": "Nguyen A",
        "authorAvatar": "...",
        "imageUrl": "...",
        "thumbnailUrl": "...",
        "caption": "Buoi hoc hom nay that hieu qua!",
        "createdAt": "2026-06-24T08:30:00Z",
        "likedByMe": true,
        "likeCount": 128,
        "commentCount": 24,
        "canDelete": true
      }
    ],
    "nextCursor": null
  }
}
```

Java nen truyen data sang WebView bang Gson thay vi ghep chuoi `;;`.

## 14. De Xuat DB Schema

Khong chay migration trong phase nay. De xuat schema rieng:

```sql
CREATE TABLE locket_posts (
    id BIGSERIAL PRIMARY KEY,
    class_id INTEGER NOT NULL REFERENCES classroom_groups(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption TEXT,
    media_type VARCHAR(20) NOT NULL DEFAULT 'image',
    like_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_locket_posts_class_created
ON locket_posts(class_id, created_at DESC)
WHERE deleted_at IS NULL;

CREATE TABLE locket_reactions (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES locket_posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction_type VARCHAR(20) NOT NULL DEFAULT 'HEART',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id, reaction_type)
);

CREATE INDEX idx_locket_reactions_post
ON locket_reactions(post_id);

CREATE TABLE locket_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES locket_posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_locket_comments_post_created
ON locket_comments(post_id, created_at ASC)
WHERE deleted_at IS NULL;
```

Co the bo sung sau:

```text
image_width
image_height
file_size_bytes
moderation_status
report_count
```

Khong nen dung chung bang Reels, vi Reels la social/global video feed, con Locket la noi dung private theo class.

## 15. De Xuat Actions/Packet Contract

Nen dung JSON payload thay vi `;;`.

### `HOME_BANNER_LIST`

Dung neu banner can backend sau nay.

Client gui:

```json
{ "placement": "home_top" }
```

Server tra:

```json
{
  "items": [
    {
      "id": "banner_1",
      "imageUrl": "...",
      "title": "...",
      "targetType": "NONE",
      "targetId": null
    }
  ]
}
```

Phase dau co the chua can backend, chi dung local/static assets.

### `HOME_BANNER_CLICK`

Client gui khi user click banner:

```json
{ "bannerId": "banner_1" }
```

Server co the log analytics sau nay. Phase dau co the chi log client.

### `LOCKET_POST_LIST`

Client gui:

```json
{
  "classId": 101,
  "cursor": null,
  "limit": 20
}
```

Server tra `LOCKET_POST_LIST_RESPONSE`:

```json
{
  "success": true,
  "items": [],
  "nextCursor": null
}
```

Permission: user phai la member da duyet cua lop.

### `LOCKET_POST_CREATE`

Client gui:

```json
{
  "classId": 101,
  "imageUrl": "...",
  "thumbnailUrl": "...",
  "caption": "text",
  "mediaType": "image"
}
```

Server:

- Verify membership.
- Validate caption/type/URL.
- Insert post.
- Tra item moi.

### `LOCKET_POST_DELETE`

Client gui:

```json
{ "postId": 9001 }
```

Permission:

- Post owner.
- Class OWNER/TEACHER.
- Admin neu co.

Nen soft delete bang `deleted_at`.

### `LOCKET_POST_REACT`

Client gui:

```json
{
  "postId": 9001,
  "reactionType": "HEART"
}
```

Server toggle va tra:

```json
{
  "postId": 9001,
  "likedByMe": true,
  "likeCount": 129
}
```

### `LOCKET_COMMENT_LIST`

Client gui:

```json
{
  "postId": 9001,
  "cursor": null,
  "limit": 30
}
```

Server verify user co quyen xem post.

### `LOCKET_COMMENT_CREATE`

Client gui:

```json
{
  "postId": 9001,
  "content": "Bai hoc hay qua!"
}
```

Server sanitize/rate-limit/insert va tra comment moi.

### `LOCKET_COMMENT_DELETE`

Client gui:

```json
{ "commentId": 8001 }
```

Permission:

- Comment owner.
- Post owner.
- Class OWNER/TEACHER.
- Admin neu co.

### `LOCKET_UPLOAD_IMAGE`

Co hai huong:

1. Phase ngan han: Java client upload qua B2Helper/CloudStorageService roi goi `LOCKET_POST_CREATE`.
2. Phase production: client xin upload session tu server, server tao presigned upload URL, client upload, server confirm va scan/validate.

Khuyen nghi: phase dau dung huong 1 de nho gon, nhung phase production nen chuyen upload authority ve server.

## 16. De Xuat UI Banner/Slide

Banner HTML/CSS/JS:

- Full width trong khu noi dung HomeTab.
- Chieu cao desktop 160-190px, tuy width.
- Border radius 16-20px.
- Shadow nhe, khong neon/gradient qua tay.
- Anh `object-fit: cover`.
- Dots o giua duoi.
- Prev/next button tron nho o hai ben, hien ro khi hover.
- Auto slide 4-6 giay.
- Hover/focus pause auto slide.
- Support keyboard focus cho accessibility co ban.
- Neu `prefers-reduced-motion`, giam/bo animation.

Khong can dung Swiper ngay. Vanilla JS du de lam carousel nay va it rui ro voi JavaFX WebView hon.

## 17. De Xuat UI Locket Feed

Locket feed:

- Header: `Locket Class` + icon nho.
- Ben phai: `Xem tat ca`, arrow trai/phai.
- Horizontal rail, card rong 260-300px.
- Card co anh 16:9 hoac 4:3, radius 14-16px.
- Overlay gradient phia tren anh cho avatar/name/time.
- Caption dat trong anh hoac ngay ben duoi tuy thiet ke.
- Footer card co heart count va comment count.
- Khong co share button.
- Co card/nut add anh neu user `canPost`.
- Co loading skeleton.
- Co empty state: "Chua co khoanh khac nao trong lop".
- Co error state co nut thu lai.

Cam giac nen gan voi ClassIn/Teams/Zoom: gon, sang, thuc dung, khong qua nhieu decoration.

## 18. De Xuat UI Upload/Camera Modal

Phase dau nen de upload modal do Java mo, WebView chi gui event:

```text
LOCKET_CREATE_OPEN
```

UI mong muon:

- Modal giua man hinh.
- Dropzone chon anh.
- Preview anh lon.
- Caption ngan, gioi han 150 ky tu.
- Nut dang.
- Progress upload.
- Error message ro rang.

Camera capture:

- De Phase 7.
- Neu dung WebView camera, can kiem tra JavaFX WebView co ho tro `getUserMedia` tot khong.
- Neu dung JavaCV/OpenCV, can them dependency va permission phuc tap hon.

## 19. De Xuat UI Viewer/Comment

Viewer:

- Click card mo viewer lon.
- Nen render viewer trong cung WebView de co animation muot.
- Anh o giua, nen toi/translucent.
- Thong tin nguoi dang + caption.
- Prev/next.
- Heart button va count.
- Comment panel ben phai tren man hinh rong, duoi anh tren man hinh nho.
- Nut delete chi hien neu `canDelete`.

Comment:

- Load theo `LOCKET_COMMENT_LIST`.
- Optimistic append khi gui comment, nhung phai co rollback neu server fail.
- Gioi han do dai comment.
- Escape HTML bat buoc de tranh XSS trong WebView.

## 20. Roadmap Theo Phase

### Phase 1: Research & Design Plan

- Audit HomeTab/Banner/Locket.
- Audit Reels like/comment/upload.
- Audit DB/storage/permission.
- Tao report nay.
- Chua code UI/backend.

### Phase 2: Web UI Prototype Static

- Tao `home-social.html/css/js`.
- Tao `HomeSocialWebPanel`.
- Nhung WebView vao HomeTab thay cho `BannerSlider` + `VideoReelSection`.
- Dung mock data.
- Khong sua backend.
- Co fallback neu WebView load fail.

### Phase 3: Java-WebView Bridge + Data Adapter

- Java inject state bang Gson.
- JS gui events ve Java.
- Map du lieu Locket cu thanh DTO moi tam thoi.
- Banner dung local data.
- Chua migration DB.

### Phase 4: Backend Locket Core

- Tao DAO/service/action cho Locket moi.
- Them schema sau khi duyet.
- Permission theo class.
- List/create/delete/react/comment.

### Phase 5: Upload/Post Modal

- Chon anh tu may.
- Preview.
- Compress/resize thumbnail tren background worker.
- Upload.
- Post.
- Refresh feed.

### Phase 6: Viewer/Comments/Reaction Polish

- Viewer lon.
- Comment panel.
- Optimistic like/comment.
- Delete post/comment.
- Loading/error/empty states hoan chinh.

### Phase 7: Camera/Moderation/Production Hardening

- Camera capture neu kha thi.
- Moderation/report.
- Rate limit.
- Presigned URL TTL ngan/proxy media.
- Observability/logging.
- Performance test.

## 21. Quick Wins

- Giu HomeTab Swing, chi thay hai component visual.
- Dung mot WebView chung de giam RAM va dong bo style.
- Dung vanilla JS, chua them Swiper.
- Dung local banner assets truoc.
- Tao DTO bang Gson thay vi ghep chuoi.
- Reuse idea Reels like/comment, nhung tao action/schema Locket rieng.
- Upload anh tu file system truoc, camera de sau.
- Giu `UploadLocketDialog` lam fallback neu Web modal chua on dinh.

## 22. Rui Ro Ky Thuat

### JavaFX WebView compatibility

JavaFX WebView khong phai Chrome moi nhat. CSS/JS phai vua phai:

- Khong dung API web qua moi.
- Khong phu thuoc heavy animation.
- Test tren may target.

### Swing EDT + JavaFX Thread

Can tach ro:

- Swing UI update tren EDT.
- WebView update tren JavaFX Application Thread.
- Network/upload tren background worker.

### Data contract cu brittle

`List<String>` va `split(";;")` khong ben vung. Caption/comment co the pha parser. Phase moi nen dung JSON DTO.

### Privacy/storage

Neu URL anh public, user ngoai class co URL van xem duoc. Ban production nen dung presigned URL ngan han hoac backend media proxy.

### Permission gap

Locket moi phai check class membership tren server, khong tin client.

### Performance

Anh feed can thumbnail, lazy load va gioi han so card render. Khong render hang tram anh cung luc.

## 23. Nhung Phan Khong Nen Lam Ngay

- Khong rewrite toan bo HomeTab.
- Khong migrate class cards/filter/sort sang WebView.
- Khong sua Practice/Exam/TSE/Login/Reels/Profile trong phase nay.
- Khong them camera ngay.
- Khong dung chung bang Reels cho Locket khi chua co abstraction permission.
- Khong them thu vien JS ngoai nhu Swiper ngay neu vanilla JS dap ung du.
- Khong upload truc tiep secret storage tu HTML/JS.
- Khong chay DB migration khi chua duoc duyet.

## 24. Acceptance Criteria Cho Phase Code Tiep Theo

Phase code tiep theo nen duoc chap nhan khi:

- HomeTab van load duoc nhu cu.
- Banner + Locket duoc render trong `HomeSocialWebPanel`.
- Cac phan Swing con lai cua HomeTab khong bi doi hanh vi.
- Banner co dots, next/prev, auto slide va transition muot.
- Locket co card feed dep voi mock data.
- WebView co bridge event toi thieu:
  - `HOME_SOCIAL_READY`
  - `HOME_BANNER_CLICK`
  - `LOCKET_CREATE_OPEN`
  - `LOCKET_VIEW_OPEN`
  - `LOCKET_POST_REACT`
  - `LOCKET_COMMENT_OPEN`
- Neu WebView load fail, HomeTab khong blank va co fallback/thong bao nhe.
- Khong sua backend/schema trong Phase 2.
- Khong tac dong Reels/Exam/Login/Practice.
- Khong commit `target/`, `dist/`, runtime files hoac file secret.

