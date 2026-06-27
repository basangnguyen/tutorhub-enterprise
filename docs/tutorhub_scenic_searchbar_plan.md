# Kế Hoạch Chi Tiết: Tính Năng Ảnh Danh Lam Thắng Cảnh Việt Nam trên Thanh Tìm Kiếm TutorHub

> **Phiên bản:** 1.0  
> **Ngày soạn:** Tháng 6/2026  
> **Phạm vi:** Thanh tìm kiếm trang chủ TutorHub Enterprise  
> **Tham chiếu:** Windows 11 Spotlight, Bing HPImageArchive API, Unsplash API, Pexels API

---

## 1. Nghiên Cứu Cơ Chế Tham Chiếu

### 1.1. Windows 11 Spotlight — Cơ chế hoạt động thực sự

**Nguồn:** Wikipedia, Microsoft Learn, Pureinfotech

Windows Spotlight **không phải một API đơn giản** — đây là một hệ thống CDN phức tạp:

| Thành phần | Mô tả |
|---|---|
| **ContentDeliveryManager** | Service Windows tải ảnh ngầm từ CDN của Microsoft/Bing |
| **Kho lưu trữ local** | `%LocalAppData%\Packages\Microsoft.Windows.ContentDeliveryManager_cw5n1h2txyewy\LocalState\Assets` |
| **Định dạng** | JPEG, không có phần mở rộng file, kích thước > 300KB là ảnh nền |
| **Chu kỳ xoay** | Mỗi 1–2 ngày tự động thay ảnh mới |
| **Fallback** | Nếu không có ảnh mới → dùng ảnh mặc định đã cache |
| **Nguồn ảnh** | Bing Image Archive, các nhiếp ảnh gia được Microsoft chọn lọc |
| **Thông tin ảnh** | Từ 2017, mỗi ảnh có geolocation: tên địa điểm, quốc gia |
| **Tương tác** | Nút "Thích ảnh này?" / "Tìm hiểu thêm" → link Bing Search |

**Pattern học được từ Spotlight:**
- Cache ảnh local → không fetch mỗi lần load
- Xoay ảnh theo ngày, không phải theo session
- Hiện tên địa điểm + photographer credit
- Preload ảnh tiếp theo trong background
- Fallback an toàn nếu mất mạng

---

### 1.2. Bing HPImageArchive API — API chính thức, free, không cần key

**Endpoint chính thức (public, không cần auth):**
```
GET https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US
```

**Tham số:**
| Param | Giá trị | Ý nghĩa |
|---|---|---|
| `format` | `js` (JSON) hoặc `xml` hoặc `rss` | Định dạng response |
| `idx` | `0` = hôm nay, `1` = hôm qua, `2-6` = tối đa 7 ngày trước | Index ngày |
| `n` | `1–8` | Số ảnh trả về (max 8) |
| `mkt` | `vi-VN`, `en-US`, `zh-CN`, v.v. | Thị trường/ngôn ngữ |

**Response JSON:**
```json
{
  "images": [
    {
      "startdate": "20260625",
      "fullstartdate": "202606250700",
      "enddate": "20260626",
      "url": "/th?id=OHR.HaLongBay_VI-VN1234567890_1920x1080.jpg&rf=...",
      "urlbase": "/th?id=OHR.HaLongBay_VI-VN1234567890",
      "copyright": "Vịnh Hạ Long, Quảng Ninh, Việt Nam (© Photographer/Getty Images)",
      "copyrightlink": "https://www.bing.com/search?q=Vịnh+Hạ+Long",
      "title": "Vịnh Hạ Long",
      "hsh": "...",
      "drk": 1,
      "top": 1,
      "bot": 1
    }
  ]
}
```

**URL ảnh đầy đủ:**
```
https://www.bing.com + url_field
https://www.bing.com/th?id=OHR.HaLongBay_VI-VN1234567890_1920x1080.jpg
```

**Độ phân giải có thể thay thế suffix:**
- `_1920x1080.jpg` → Full HD
- `_1366x768.jpg` → HD
- `_UHD.jpg` → 4K
- `_800x480.jpg` → Mobile

**Giới hạn:** 7 ngày lịch sử, tối đa 8 ảnh/request. Đây là API không chính thức nhưng đã hoạt động ổn định từ nhiều năm.

---

### 1.3. Unsplash API — Photo search với Vietnam keyword

**Base URL:** `https://api.unsplash.com`  
**Auth:** `Authorization: Client-ID YOUR_ACCESS_KEY` header  
**Rate limit (demo):** 50 req/hour | **Production:** 5,000 req/hour

**Endpoints quan trọng nhất cho feature này:**

```
# Ảnh ngẫu nhiên theo keyword (phù hợp nhất)
GET /photos/random?query=vietnam+landscape&orientation=landscape&client_id=KEY

# Search theo keyword
GET /search/photos?query=ha+long+bay&orientation=landscape&per_page=10&client_id=KEY

# Ảnh từ collection cụ thể (tạo sẵn Vietnam collection)
GET /collections/{collection_id}/photos?per_page=10&client_id=KEY
```

**Response URLs:**
```json
{
  "id": "abc123",
  "urls": {
    "raw": "https://images.unsplash.com/photo-123?...",
    "full": "https://images.unsplash.com/photo-123?q=75&fm=jpg",
    "regular": "https://images.unsplash.com/photo-123?q=75&fm=jpg&w=1080&fit=max",
    "small": "https://images.unsplash.com/photo-123?q=75&fm=jpg&w=400&fit=max",
    "thumb": "https://images.unsplash.com/photo-123?q=75&fm=jpg&w=200"
  },
  "description": "Ha Long Bay sunrise",
  "location": { "name": "Hạ Long Bay, Vietnam" },
  "user": { "name": "Photographer Name", "links": { "html": "https://unsplash.com/@..." } }
}
```

**Quy tắc attribution bắt buộc của Unsplash:**
- Hiện "Photo by [Tên] on Unsplash" với hyperlink
- Không được dùng ảnh làm wallpaper app (cấm)

---

### 1.4. Pexels API

**Base URL:** `https://api.pexels.com/v1`  
**Auth:** `Authorization: YOUR_API_KEY` header  
**Rate limit:** 200 req/hour, 20,000 req/month

```
# Search ảnh landscape Việt Nam
GET /v1/search?query=vietnam+landscape&orientation=landscape&per_page=15

# Ảnh curated (trending, chất lượng cao)
GET /v1/curated?per_page=10
```

**Response:**
```json
{
  "photos": [
    {
      "id": 123,
      "photographer": "John Doe",
      "photographer_url": "https://www.pexels.com/@john",
      "url": "https://www.pexels.com/photo/123",
      "src": {
        "original": "...",
        "large2x": "...",
        "large": "...",
        "medium": "...",
        "small": "...",
        "landscape": "..."
      }
    }
  ]
}
```

---

### 1.5. Google Search Background — Pattern tham khảo UX

Google không có API công khai cho tính năng này. Google Doodles là do team nội bộ tạo. Tuy nhiên, pattern UX của Google rất quan trọng:
- Background image load **sau** khi search bar đã render
- Gradient overlay nhẹ để text vẫn đọc được
- Click vào ảnh hoặc credit link → mở tab mới
- Ảnh không làm chậm Core Web Vitals (lazy load)

---

## 2. Phân Tích UI Hiện Tại TutorHub

Từ screenshot, thanh tìm kiếm TutorHub hiện có:

```
┌─────────────────────────────────────────────────────────┐
│  [🔍 Tìm kiếm Biểu tượng        ] [⚙] [🔍]           │
└─────────────────────────────────────────────────────────┘
```

**Cấu trúc HTML hiện tại (ước lượng):**
```jsx
<div className="search-container">
  <input placeholder="Tìm kiếm Biểu tượng" />
  <button>⚙</button>
  <button>🔍</button>
</div>
```

**Sau khi implement:**
```
┌─────────────────────────────────────────────────────────┐
│  🌄 [Ảnh Vịnh Hạ Long làm background mờ nhẹ]          │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 🔍 Tìm kiếm Biểu tượng          [⚙] [🔍]       │  │
│  └──────────────────────────────────────────────────┘  │
│  📍 Vịnh Hạ Long, Quảng Ninh    [Tìm hiểu thêm →]    │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Kho Ảnh Việt Nam Curated — Danh Sách 30 Địa Điểm

Danh sách các danh lam thắng cảnh Việt Nam được chọn lọc kỹ, dùng làm **nguồn self-hosted** (không cần API):

### Miền Bắc
| # | Địa điểm | Keyword Unsplash/Pexels | Mô tả ngắn |
|---|---|---|---|
| 1 | Vịnh Hạ Long | `ha long bay vietnam` | UNESCO, vịnh đảo đá vôi huyền thoại |
| 2 | Ruộng bậc thang Mù Cang Chải | `mu cang chai terraced rice` | Mùa lúa chín tháng 9 |
| 3 | Ruộng bậc thang Sapa | `sapa rice terrace vietnam` | Fansipan, sương mù |
| 4 | Hồ Hoàn Kiếm, Hà Nội | `hoan kiem lake hanoi` | Tháp Rùa, trung tâm Hà Nội |
| 5 | Hang Sơn Đoòng | `son doong cave vietnam` | Hang động lớn nhất thế giới |
| 6 | Phong Nha - Kẻ Bàng | `phong nha cave quang binh` | UNESCO, hệ thống hang động |
| 7 | Thác Bản Giốc | `ban gioc waterfall cao bang` | Thác nước lớn nhất Đông Nam Á |
| 8 | Đồng lúa Ninh Bình | `ninh binh vietnam landscape` | Tam Cốc, Tràng An |
| 9 | Chùa Bái Đính | `bai dinh temple ninh binh` | Quần thể chùa lớn nhất VN |
| 10 | Bình nguyên Đồng Văn | `dong van plateau ha giang` | Núi đá tai mèo, biên giới |

### Miền Trung
| # | Địa điểm | Keyword | Mô tả |
|---|---|---|---|
| 11 | Phố cổ Hội An | `hoi an ancient town vietnam` | UNESCO, đèn lồng, phố cổ |
| 12 | Cầu Vàng Bà Nà Hills | `golden bridge ba na hills` | Cây cầu bàn tay khổng lồ |
| 13 | Đèo Hải Vân | `hai van pass vietnam` | Đèo đẹp nhất miền Trung |
| 14 | Bãi biển Đà Nẵng | `da nang beach vietnam` | Bãi cát trắng, cầu Rồng |
| 15 | Thánh địa Mỹ Sơn | `my son sanctuary champa` | UNESCO, đền Chăm |
| 16 | Lăng Cô, Thừa Thiên Huế | `lang co lagoon vietnam` | Đầm phá ven biển |
| 17 | Cố đô Huế | `hue citadel vietnam imperial` | Hoàng thành, lăng tẩm |
| 18 | Mũi Né, Bình Thuận | `mui ne sand dune vietnam` | Đồi cát đỏ, bình minh |
| 19 | Quần đảo Lý Sơn | `ly son island vietnam` | Đảo tỏi, địa hình núi lửa |
| 20 | Hồ Tuyền Lâm, Đà Lạt | `da lat lake vietnam` | Thành phố ngàn hoa |

### Miền Nam
| # | Địa điểm | Keyword | Mô tả |
|---|---|---|---|
| 21 | Đồng bằng sông Cửu Long | `mekong delta vietnam aerial` | Chợ nổi, kênh rạch |
| 22 | Đảo Phú Quốc | `phu quoc island vietnam` | Bãi biển nguyên sơ |
| 23 | Côn Đảo | `con dao island vietnam` | Đảo hoang sơ, đồi mồi |
| 24 | Vườn quốc gia Cát Tiên | `cat tien national park` | Rừng nhiệt đới |
| 25 | Chợ nổi Cái Răng | `cai rang floating market` | Chợ nổi miền Tây |
| 26 | Nhà thờ Đức Bà, TP.HCM | `notre dame cathedral saigon` | Biểu tượng Sài Gòn |
| 27 | Bến Nhà Rồng | `ben nha rong ho chi minh` | Cảng lịch sử Sài Gòn |
| 28 | Cao nguyên Buôn Ma Thuột | `buon ma thuot coffee highland` | Vùng cà phê nổi tiếng |
| 29 | Vùng biển Nha Trang | `nha trang beach coral vietnam` | Rạn san hô, lặn biển |
| 30 | Đảo Bình Ba, Khánh Hòa | `binh ba island khanh hoa` | Đảo tôm hùm xanh |

---

## 4. Kiến Trúc Giải Pháp Đề Xuất

### 4.1. So sánh 4 phương án nguồn ảnh

| Phương án | Ưu điểm | Nhược điểm | Khuyến nghị |
|---|---|---|---|
| **A. Self-curated JSON** | Không cần API key, offline-capable, kiểm soát hoàn toàn, load nhanh | Tốn công curation, cần CDN riêng | ⭐ **Recommended cho production** |
| **B. Bing HPImageArchive** | Miễn phí, không cần key, ảnh chất lượng cao, có metadata | Không kiểm soát được nội dung VN cụ thể, có thể bị rate limit | ✅ **Dùng làm fallback** |
| **C. Unsplash API** | Kho ảnh lớn, có search theo keyword, metadata phong phú | Cần API key, 50 req/h (demo), cần attribution | ✅ **Dùng khi muốn đa dạng** |
| **D. Pexels API** | Miễn phí, 200 req/h, không cần attribution riêng | Ít phổ biến hơn Unsplash | ⚠️ Backup option |

### 4.2. Kiến Trúc Hybrid Được Khuyến Nghị

```
┌─────────────────────────────────────────────────┐
│           TutorHub Search Banner                │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │  LAYER 1: Ảnh nền (ImageProvider)       │  │
│  │  LAYER 2: Gradient overlay CSS          │  │
│  │  LAYER 3: Search bar component          │  │
│  │  LAYER 4: Location info chip            │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘

ImageProvider (Priority order):
1. LocalCache (localStorage/IndexedDB) — load ngay lập tức
2. Self-curated JSON list (từ file static hoặc backend API)
3. Bing HPImageArchive (nếu mkt=vi-VN trả về ảnh VN)
4. Fallback image (ảnh default encode sẵn)
```

---

## 5. Kế Hoạch Triển Khai Chi Tiết

### Phase 1: Self-curated JSON + Local Rotation (Tuần 1-2)

**Mục tiêu:** Có tính năng chạy được ngay, không cần API key ngoài.

#### File 1: `vietnam_landmarks.json` (đặt tại `public/data/`)

```json
{
  "version": "1.0",
  "lastUpdated": "2026-06-01",
  "landmarks": [
    {
      "id": "ha-long-bay",
      "name_vi": "Vịnh Hạ Long",
      "name_en": "Ha Long Bay",
      "location_vi": "Quảng Ninh, Việt Nam",
      "location_en": "Quang Ninh Province, Vietnam",
      "description_vi": "Di sản Thiên nhiên Thế giới UNESCO với hơn 1.600 hòn đảo đá vôi",
      "description_en": "UNESCO World Natural Heritage with over 1,600 limestone islands",
      "image_url": "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=1200&q=80",
      "image_thumb": "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=400&q=60",
      "image_credit": "Photographer Name",
      "image_credit_url": "https://unsplash.com/@photographer",
      "image_source": "Unsplash",
      "dominant_color": "#1a6b8a",
      "text_color": "light",
      "region": "north",
      "tags": ["UNESCO", "biển", "đảo", "tự nhiên"],
      "bing_search_url": "https://www.bing.com/search?q=Vịnh+Hạ+Long",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Vịnh_Hạ_Long",
      "active": true
    },
    {
      "id": "hoi-an-ancient-town",
      "name_vi": "Phố Cổ Hội An",
      "name_en": "Hoi An Ancient Town",
      "location_vi": "Quảng Nam, Việt Nam",
      "location_en": "Quang Nam Province, Vietnam",
      "description_vi": "Di sản Văn hóa Thế giới UNESCO với kiến trúc cổ được bảo tồn nguyên vẹn",
      "description_en": "UNESCO World Cultural Heritage with well-preserved ancient architecture",
      "image_url": "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=1200&q=80",
      "image_thumb": "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=400&q=60",
      "image_credit": "Photographer Name",
      "image_credit_url": "https://unsplash.com/@photographer",
      "image_source": "Unsplash",
      "dominant_color": "#c4870a",
      "text_color": "dark",
      "region": "central",
      "tags": ["UNESCO", "phố cổ", "văn hóa", "đèn lồng"],
      "active": true
    }
  ]
}
```

---

#### File 2: `SearchBannerImage.jsx` — Component chính

```jsx
import React, { useState, useEffect, useRef } from 'react';
import vietnamLandmarks from '../../data/vietnam_landmarks.json';

/**
 * SearchBannerImage
 * Hiển thị ảnh danh lam thắng cảnh Việt Nam phía sau thanh tìm kiếm
 * Logic xoay ảnh: mỗi ngày 1 ảnh khác nhau (giống Windows Spotlight)
 */
const SearchBannerImage = ({ children, className = '' }) => {
  const [currentLandmark, setCurrentLandmark] = useState(null);
  const [imageLoaded, setImageLoaded] = useState(false);
  const [showInfo, setShowInfo] = useState(false);
  const [lang, setLang] = useState('vi'); // vi | en
  const cacheKey = 'tutorhub_banner_image_cache';
  const cacheDateKey = 'tutorhub_banner_image_date';

  useEffect(() => {
    loadDailyImage();
  }, []);

  /**
   * Chọn ảnh theo ngày (giống Bing/Spotlight)
   * Cùng 1 ngày luôn ra cùng 1 ảnh, hôm sau tự đổi
   */
  const loadDailyImage = () => {
    const today = new Date().toISOString().split('T')[0]; // "2026-06-25"
    const cachedDate = localStorage.getItem(cacheDateKey);
    const cachedData = localStorage.getItem(cacheKey);

    // Nếu cache còn hợp lệ (cùng ngày) → dùng luôn, không fetch
    if (cachedDate === today && cachedData) {
      try {
        setCurrentLandmark(JSON.parse(cachedData));
        return;
      } catch (e) {
        // Cache corrupted, ignore
      }
    }

    // Tính index theo ngày (xoay vòng qua danh sách)
    const activeLandmarks = vietnamLandmarks.landmarks.filter(l => l.active);
    const dayOfYear = getDayOfYear(new Date());
    const index = dayOfYear % activeLandmarks.length;
    const selected = activeLandmarks[index];

    // Cache vào localStorage
    localStorage.setItem(cacheDateKey, today);
    localStorage.setItem(cacheKey, JSON.stringify(selected));

    setCurrentLandmark(selected);
  };

  const getDayOfYear = (date) => {
    const start = new Date(date.getFullYear(), 0, 0);
    const diff = date - start;
    const oneDay = 1000 * 60 * 60 * 24;
    return Math.floor(diff / oneDay);
  };

  const handleImageLoad = () => {
    setImageLoaded(true);
  };

  if (!currentLandmark) return (
    <div className={`search-banner search-banner--loading ${className}`}>
      {children}
    </div>
  );

  const name = lang === 'vi' ? currentLandmark.name_vi : currentLandmark.name_en;
  const location = lang === 'vi' ? currentLandmark.location_vi : currentLandmark.location_en;
  const description = lang === 'vi' ? currentLandmark.description_vi : currentLandmark.description_en;

  return (
    <div className={`search-banner ${className}`}>
      {/* Layer 1: Ảnh nền */}
      <div
        className={`search-banner__bg ${imageLoaded ? 'search-banner__bg--loaded' : ''}`}
        style={{
          backgroundImage: `url(${currentLandmark.image_url})`,
        }}
      />

      {/* Preload image (không hiện trực tiếp, dùng để trigger onLoad) */}
      <img
        src={currentLandmark.image_url}
        alt=""
        style={{ display: 'none' }}
        onLoad={handleImageLoad}
      />

      {/* Layer 2: Gradient overlay cho readability */}
      <div className="search-banner__overlay" />

      {/* Layer 3: Search bar (children) */}
      <div className="search-banner__content">
        {children}
      </div>

      {/* Layer 4: Thông tin địa điểm */}
      <div className="search-banner__info-bar">
        <div className="search-banner__location" onClick={() => setShowInfo(!showInfo)}>
          <span className="search-banner__location-icon">📍</span>
          <span className="search-banner__location-name">{name}</span>
          <span className="search-banner__location-sub">, {location}</span>
        </div>

        <div className="search-banner__actions">
          <a
            href={currentLandmark.bing_search_url}
            target="_blank"
            rel="noopener noreferrer"
            className="search-banner__action-btn"
          >
            Tìm hiểu thêm →
          </a>
          <span className="search-banner__credit">
            {lang === 'vi' ? 'Ảnh: ' : 'Photo: '}
            <a href={currentLandmark.image_credit_url} target="_blank" rel="noopener noreferrer">
              {currentLandmark.image_credit}
            </a>
            {' on '}{currentLandmark.image_source}
          </span>
        </div>
      </div>

      {/* Tooltip/popup thông tin chi tiết */}
      {showInfo && (
        <div className="search-banner__tooltip">
          <p>{description}</p>
          {currentLandmark.tags.map(tag => (
            <span key={tag} className="search-banner__tag">#{tag}</span>
          ))}
          <a href={currentLandmark.wikipedia_url} target="_blank" rel="noopener noreferrer">
            Xem trên Wikipedia →
          </a>
        </div>
      )}
    </div>
  );
};

export default SearchBannerImage;
```

---

#### File 3: `SearchBannerImage.css` — Styles

```css
/* ============================================================
   SearchBannerImage — Ảnh danh lam thắng cảnh Việt Nam
   Cơ chế: giống Windows Spotlight / Bing Search
   ============================================================ */

.search-banner {
  position: relative;
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  min-height: 80px;          /* Chiều cao tối thiểu */
  padding: 12px 16px 28px;  /* Padding: trên, trái-phải, dưới (cho info bar) */
}

/* ── Layer 1: Ảnh nền ── */
.search-banner__bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center 30%;  /* Focus vào phần trên ảnh */
  background-repeat: no-repeat;
  opacity: 0;
  transition: opacity 0.8s ease-in-out;  /* Fade-in như Spotlight */
  will-change: opacity;
}

.search-banner__bg--loaded {
  opacity: 1;
}

.search-banner--loading .search-banner__bg {
  background-color: #e8edf2;  /* Placeholder màu xám nhẹ */
}

/* ── Layer 2: Gradient overlay ── */
/* Tối phần dưới để info bar đọc được, phần trên trong hơn */
.search-banner__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.10) 0%,
    rgba(0, 0, 0, 0.15) 40%,
    rgba(0, 0, 0, 0.40) 70%,
    rgba(0, 0, 0, 0.60) 100%
  );
  pointer-events: none;
}

/* ── Layer 3: Search content ── */
.search-banner__content {
  position: relative;
  z-index: 2;
  /* Input trong content cần background riêng để đọc được */
}

/* Đảm bảo search input có nền đủ tương phản */
.search-banner__content input,
.search-banner__content .search-input {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

/* ── Layer 4: Info bar ── */
.search-banner__info-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 16px 8px;
  gap: 8px;
}

.search-banner__location {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.95);
  font-size: 12px;
  font-weight: 600;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
  transition: color 0.2s;
}

.search-banner__location:hover {
  color: #fff;
}

.search-banner__location-icon {
  font-size: 11px;
}

.search-banner__location-name {
  font-weight: 700;
}

.search-banner__location-sub {
  font-weight: 400;
  opacity: 0.85;
}

.search-banner__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-banner__action-btn {
  color: rgba(255, 255, 255, 0.9);
  font-size: 11px;
  font-weight: 600;
  text-decoration: none;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 20px;
  padding: 3px 10px;
  backdrop-filter: blur(4px);
  transition: background 0.2s;
}

.search-banner__action-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.search-banner__credit {
  color: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.search-banner__credit a {
  color: rgba(255, 255, 255, 0.75);
  text-decoration: none;
}

.search-banner__credit a:hover {
  color: #fff;
  text-decoration: underline;
}

/* ── Tooltip thông tin chi tiết ── */
.search-banner__tooltip {
  position: absolute;
  bottom: 36px;
  left: 16px;
  z-index: 10;
  background: rgba(0, 0, 0, 0.82);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  padding: 12px 16px;
  max-width: 320px;
  color: #fff;
  font-size: 12px;
  line-height: 1.5;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.search-banner__tooltip p {
  margin: 0 0 8px;
  opacity: 0.9;
}

.search-banner__tag {
  display: inline-block;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 2px 8px;
  font-size: 10px;
  margin: 0 4px 4px 0;
  color: rgba(255, 255, 255, 0.85);
}

.search-banner__tooltip a {
  color: #7eb6ff;
  font-size: 11px;
  text-decoration: none;
  display: block;
  margin-top: 8px;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .search-banner__info-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    padding: 4px 12px 8px;
  }

  .search-banner__actions {
    gap: 8px;
  }

  .search-banner__credit {
    display: none;  /* Ẩn credit trên mobile để tiết kiệm không gian */
  }
}
```

---

#### File 4: Tích hợp vào trang chủ

```jsx
// Trong HomePage.jsx hoặc trang chủ TutorHub

import SearchBannerImage from './components/SearchBannerImage';
import SearchBar from './components/SearchBar'; // Component tìm kiếm hiện tại

const HomePage = () => {
  return (
    <div className="home-page">
      {/* Bọc search bar hiện tại trong SearchBannerImage */}
      <SearchBannerImage className="home-search-banner">
        <SearchBar placeholder="Tìm kiếm Biểu tượng" />
      </SearchBannerImage>

      {/* Phần còn lại của trang */}
      {/* ... Locket, Lớp học nổi bật, v.v. */}
    </div>
  );
};
```

---

### Phase 2: Bing API Integration (Tuần 2-3)

**Mục tiêu:** Thêm ảnh Bing làm nguồn phụ, tự động cập nhật hàng ngày từ internet.

#### `bingImageService.js`

```javascript
/**
 * BingImageService
 * Lấy ảnh của ngày từ Bing HPImageArchive API
 * Endpoint: https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=vi-VN
 * Không cần API key, public endpoint
 */
class BingImageService {
  static BASE_URL = 'https://www.bing.com';
  static API_ENDPOINT = '/HPImageArchive.aspx';
  static CACHE_KEY = 'tutorhub_bing_image';
  static CACHE_DATE_KEY = 'tutorhub_bing_image_date';

  /**
   * Lấy ảnh của ngày từ Bing
   * @param {string} market - 'vi-VN' | 'en-US' | ...
   * @returns {Promise<Object>} - { url, title, copyright, copyrightlink }
   */
  static async getDailyImage(market = 'vi-VN') {
    const today = new Date().toISOString().split('T')[0];
    const cacheDate = localStorage.getItem(this.CACHE_DATE_KEY);
    const cachedData = localStorage.getItem(this.CACHE_KEY);

    // Dùng cache nếu cùng ngày
    if (cacheDate === today && cachedData) {
      try { return JSON.parse(cachedData); } catch (_) {}
    }

    try {
      const params = new URLSearchParams({
        format: 'js',
        idx: '0',
        n: '1',
        mkt: market
      });

      const response = await fetch(
        `${this.BASE_URL}${this.API_ENDPOINT}?${params}`,
        {
          headers: {
            'User-Agent': 'Mozilla/5.0 (compatible; TutorHub/1.0)'
          }
        }
      );

      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const data = await response.json();
      const image = data.images?.[0];

      if (!image) throw new Error('No image in response');

      // Xây dựng URL ảnh đầy đủ
      const imageData = {
        image_url: `${this.BASE_URL}${image.url.replace('_1920x1080', '_1920x1080')}`,
        image_thumb: `${this.BASE_URL}${image.urlbase}_800x480.jpg`,
        name_vi: image.title || 'Danh lam thắng cảnh',
        name_en: image.title || 'Scenic spot',
        location_vi: this.extractLocationFromCopyright(image.copyright),
        copyright: image.copyright || '',
        copyrightlink: image.copyrightlink || '#',
        source: 'Bing'
      };

      // Cache lại
      localStorage.setItem(this.CACHE_DATE_KEY, today);
      localStorage.setItem(this.CACHE_KEY, JSON.stringify(imageData));

      return imageData;

    } catch (error) {
      console.warn('[TutorHub] Bing image fetch failed:', error);
      return null;
    }
  }

  // Parse copyright string: "(© Photographer/Agency)" → tên địa điểm
  static extractLocationFromCopyright(copyright) {
    if (!copyright) return 'Việt Nam';
    // Bing format: "Tên địa điểm, Tỉnh (© Photographer)"
    const parts = copyright.split('(©')[0].trim();
    return parts || 'Việt Nam';
  }
}

export default BingImageService;
```

---

### Phase 3: Unsplash API Integration (Tuần 3-4)

**Mục tiêu:** Tích hợp Unsplash để có kho ảnh Vietnam đa dạng, fetch theo keyword.

```javascript
// unsplashVietnamService.js

const VIETNAM_KEYWORDS = [
  'ha long bay', 'hoi an vietnam', 'sapa rice terrace',
  'mekong delta vietnam', 'da nang vietnam beach',
  'phu quoc island', 'ninh binh vietnam', 'hue citadel',
  'ban gioc waterfall', 'son doong cave', 'mu cang chai',
  'golden bridge vietnam', 'hai van pass', 'nha trang beach vietnam'
];

class UnsplashVietnamService {
  static BASE_URL = 'https://api.unsplash.com';
  // Access key lấy từ https://unsplash.com/developers
  // Đặt trong .env: REACT_APP_UNSPLASH_ACCESS_KEY=...
  static ACCESS_KEY = process.env.REACT_APP_UNSPLASH_ACCESS_KEY;

  /**
   * Lấy 1 ảnh ngẫu nhiên Vietnam theo keyword của ngày
   */
  static async getDailyVietnamPhoto() {
    const dayOfYear = Math.floor(
      (Date.now() - new Date(new Date().getFullYear(), 0, 0)) / 86400000
    );
    const keyword = VIETNAM_KEYWORDS[dayOfYear % VIETNAM_KEYWORDS.length];

    try {
      const response = await fetch(
        `${this.BASE_URL}/photos/random?query=${encodeURIComponent(keyword)}&orientation=landscape&content_filter=high`,
        {
          headers: {
            'Authorization': `Client-ID ${this.ACCESS_KEY}`
          }
        }
      );

      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const photo = await response.json();

      return {
        image_url: photo.urls.regular,
        image_thumb: photo.urls.small,
        name_vi: photo.description || photo.alt_description || 'Việt Nam',
        location_vi: photo.location?.name || 'Việt Nam',
        image_credit: photo.user?.name || 'Unknown',
        image_credit_url: `${photo.user?.links?.html}?utm_source=tutorhub&utm_medium=referral`,
        image_source: 'Unsplash',
        unsplash_download_url: photo.links?.download_location // Track download theo guidelines
      };

    } catch (error) {
      console.warn('[TutorHub] Unsplash fetch failed:', error);
      return null;
    }
  }
}
```

---

### Phase 4: Unified Image Provider với Priority Queue

```javascript
// imageProviderService.js — Orchestrator chọn nguồn ảnh

import localLandmarks from '../data/vietnam_landmarks.json';
import BingImageService from './bingImageService';
import UnsplashVietnamService from './unsplashVietnamService';

class ImageProviderService {

  /**
   * Lấy ảnh theo priority:
   * 1. Local cache (nếu còn hợp lệ)
   * 2. Self-curated JSON list
   * 3. Bing (nếu online)
   * 4. Unsplash (nếu có API key)
   * 5. Fallback default image
   */
  static async getDailyImage(options = {}) {
    const { preferSource = 'curated' } = options;

    // Priority 1: Curated local list (always works, offline-capable)
    if (preferSource === 'curated' || preferSource === 'auto') {
      const curated = this.getFromCuratedList();
      if (curated) return { ...curated, source: 'curated' };
    }

    // Priority 2: Bing (no API key needed)
    try {
      const bingImage = await BingImageService.getDailyImage('vi-VN');
      if (bingImage) return { ...bingImage, source: 'bing' };
    } catch (_) {}

    // Priority 3: Unsplash (needs API key)
    if (process.env.REACT_APP_UNSPLASH_ACCESS_KEY) {
      try {
        const unsplashImage = await UnsplashVietnamService.getDailyVietnamPhoto();
        if (unsplashImage) return { ...unsplashImage, source: 'unsplash' };
      } catch (_) {}
    }

    // Priority 4: Absolute fallback
    return this.getFallbackImage();
  }

  static getFromCuratedList() {
    const active = localLandmarks.landmarks.filter(l => l.active);
    if (!active.length) return null;

    const today = new Date();
    const dayOfYear = Math.floor(
      (today - new Date(today.getFullYear(), 0, 0)) / 86400000
    );
    return active[dayOfYear % active.length];
  }

  static getFallbackImage() {
    return {
      image_url: '/images/default-vietnam.jpg', // Ảnh default bundle vào app
      name_vi: 'Việt Nam',
      location_vi: 'Việt Nam',
      source: 'fallback'
    };
  }
}

export default ImageProviderService;
```

---

## 6. Vận Hành & Caching Logic

### Sơ đồ logic tải ảnh (giống Windows Spotlight)

```
App khởi động
     │
     ▼
Kiểm tra localStorage cache
     │
     ├─ Cache tồn tại & cùng ngày hôm nay?
     │    YES → Dùng ảnh từ cache → Render ngay lập tức ✅
     │    NO  ↓
     │
     ▼
Tải ảnh theo priority:
  1. Curated JSON (sync, luôn available)
  2. Bing API (async, no key)
  3. Unsplash (async, cần key)
     │
     ▼
Lưu vào localStorage (key + ngày)
     │
     ▼
Set state → Re-render với ảnh mới
     │
     ▼
Preload ảnh tiếp theo (background, không block)
```

### Cache strategy
```
Key: "tutorhub_banner_image_cache" → JSON(landmark data)
Key: "tutorhub_banner_image_date" → "2026-06-25"
TTL: Hết hạn khi sang ngày mới (compare date string)
```

---

## 7. Kho Ảnh Tự Hosting (Khuyến Nghị Cho Production)

### Cấu trúc thư mục

```
public/
  images/
    vietnam-landmarks/
      ha-long-bay/
        full.webp         ← 1920x1080, ~150KB (WebP)
        thumb.webp        ← 400x225, ~20KB
        metadata.json     ← { credit, license, location }
      hoi-an/
        full.webp
        thumb.webp
      sapa-rice-terrace/
        ...
      [28 địa điểm còn lại]/
```

### Quy trình tối ưu ảnh

```bash
# Dùng cwebp để convert PNG/JPG → WebP
cwebp -q 80 ha-long-bay.jpg -o full.webp

# Resize về 1920x1080 với ImageMagick
convert ha-long-bay.jpg -resize 1920x1080^ -gravity center -extent 1920x1080 ha-long-bay-1920.jpg

# Tạo thumbnail 400x225
cwebp -q 60 ha-long-bay.jpg -resize 400 225 -o thumb.webp

# Script batch cho tất cả ảnh
for f in *.jpg; do
  cwebp -q 80 "$f" -resize 1920 1080 -o "${f%.jpg}-full.webp"
  cwebp -q 60 "$f" -resize 400 225 -o "${f%.jpg}-thumb.webp"
done
```

### Nguồn ảnh hợp pháp để download

| Nguồn | License | Attribution | Link |
|---|---|---|---|
| Unsplash | Unsplash License (free commercial) | Có (tên tác giả) | unsplash.com/s/photos/vietnam |
| Pexels | Pexels License (free) | Có thể bỏ qua | pexels.com/search/vietnam |
| Wikimedia Commons | CC BY-SA / Public Domain | Cần check từng ảnh | commons.wikimedia.org |
| VNExpress Photo | Bản quyền VNExpress | ❌ Không dùng | - |
| Unsplash Vietnam Collection | Unsplash License | Có | Tạo collection riêng |

---

## 8. Kế Hoạch Mở Rộng (Phase 5+)

### 8.1. Tính năng "Ảnh của bạn" (User Contribution)
- Người dùng upload ảnh địa điểm → backend review → vào kho ảnh community
- Badge "Ảnh của cộng đồng TutorHub"

### 8.2. Seasonal rotation
- Mùa xuân (T1-T3): Ảnh lễ hội, hoa đào, hoa mai
- Mùa hè (T4-T8): Biển, đảo, resort
- Mùa thu (T9-T11): Ruộng bậc thang chín vàng, Mù Cang Chải
- Mùa đông (T12): Sapa tuyết, Hà Giang đá tai mèo

### 8.3. Tương tác "Thích ảnh này"
```jsx
// Như Windows 11 "Do you like what you see?"
<button onClick={handleLike}>👍 Đẹp quá!</button>
<button onClick={handleSkip}>→ Ảnh tiếp</button>
```
- Like → persist vào user preferences, không show ảnh đã dislike
- Analytics: track địa điểm nào được like nhiều nhất

### 8.4. "Biết thêm về địa điểm này"
- Click vào tên địa điểm → Modal với:
  - Mô tả chi tiết (lấy từ Wikipedia API tiếng Việt)
  - Google Maps embed
  - Liên kết đặt tour/vé
  - Thời tiết hiện tại (OpenWeather API)

### 8.5. Backend API riêng
```
GET /api/v1/banner-image
  → { landmark, imageUrl, metadata }
  → Server tự rotate theo ngày, cache Redis 24h
  → Không expose API keys ra frontend
```

---

## 9. Acceptance Criteria

Sau khi hoàn thành Phase 1-2, chạy checklist:

```
✅ Thanh tìm kiếm hiện ảnh danh lam thắng cảnh Việt Nam mỗi ngày
✅ Ảnh xoay theo ngày (hôm nay khác hôm qua)
✅ Cùng ngày, mọi user đều thấy cùng ảnh (deterministic)
✅ Ảnh không làm chậm tải trang (lazy load, async)
✅ Nếu mất mạng → vẫn hiện ảnh từ cache hoặc fallback
✅ Tên địa điểm hiển thị bên dưới (VD: "📍 Vịnh Hạ Long, Quảng Ninh")
✅ Nút "Tìm hiểu thêm →" mở tab mới, không rời trang
✅ Credit ảnh hiển thị (theo yêu cầu Unsplash/Pexels)
✅ Search bar vẫn có màu nền đủ tương phản, đọc được
✅ Responsive: hoạt động tốt trên mobile
✅ Không ảnh hưởng tính năng tìm kiếm hiện tại
✅ Không có lỗi Console khi ảnh không load được
```

---

## 10. Files Cần Tạo/Chỉnh Sửa

| File | Hành động | Mô tả |
|---|---|---|
| `public/data/vietnam_landmarks.json` | TẠO MỚI | Kho ảnh curated 30 địa điểm |
| `public/images/vietnam-landmarks/` | TẠO MỚI | Thư mục ảnh self-hosted |
| `public/images/default-vietnam.jpg` | TẠO MỚI | Ảnh fallback mặc định |
| `src/components/SearchBannerImage/index.jsx` | TẠO MỚI | Component chính |
| `src/components/SearchBannerImage/SearchBannerImage.css` | TẠO MỚI | Styles |
| `src/services/imageProviderService.js` | TẠO MỚI | Orchestrator nguồn ảnh |
| `src/services/bingImageService.js` | TẠO MỚI | Bing API client |
| `src/services/unsplashVietnamService.js` | TẠO MỚI | Unsplash API client |
| `src/pages/HomePage.jsx` | CẬP NHẬT | Bọc SearchBar trong SearchBannerImage |
| `.env` | CẬP NHẬT | Thêm `REACT_APP_UNSPLASH_ACCESS_KEY` |
| `.env.example` | CẬP NHẬT | Document biến môi trường |

---

## 11. Rủi Ro và Cách Xử Lý

| Rủi ro | Xác suất | Giải pháp |
|---|---|---|
| Bing API ngừng hoạt động | Thấp | Fallback về curated list |
| Unsplash rate limit | Trung bình | Cache 24h, chỉ fetch 1 lần/ngày |
| Ảnh Unsplash không phải VN | Cao | Self-curated list là primary source |
| Ảnh load chậm → UX xấu | Trung bình | Skeleton loader + thumb trước, full sau |
| User xóa localStorage | Thấp | Graceful fallback, không crash |
| Copyright violation | Thấp nếu dùng Unsplash/Pexels | Chỉ dùng ảnh CC0 hoặc license rõ ràng |
| Search bar không đọc được | Trung bình | Glass morphism + min-contrast đảm bảo |

---

## 12. Câu Hỏi Cần Xác Nhận Trước Khi Code

1. **Framework frontend** của TutorHub là gì? React/Vue/Angular/Next.js?
2. **Ảnh có được tự host** trên CDN của TutorHub không, hay phải dùng Unsplash URL trực tiếp?
3. **Chiều cao** của search banner mong muốn là bao nhiêu pixel? (Desktop / Mobile)
4. **Ngôn ngữ mặc định** của info chip: Tiếng Việt, tiếng Anh, hay theo setting user?
5. **Tần suất xoay ảnh**: Mỗi ngày (khuyến nghị), mỗi phiên, hay ngẫu nhiên mỗi lần load?
6. **Vị trí chính xác** của SearchBanner: Chỉ phần search bar hay toàn bộ hero section?
7. Có cần **backend API** riêng không, hay chỉ frontend với file JSON tĩnh là đủ?
8. **API key Unsplash** đã có chưa, hay cần tôi hướng dẫn đăng ký?

---

*Tài liệu này tổng hợp từ: Windows Spotlight docs (Microsoft Learn), Bing HPImageArchive API (public endpoint), Unsplash API docs (unsplash.com/documentation), Pexels API docs (pexels.com/api/documentation)*
