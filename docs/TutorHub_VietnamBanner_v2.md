# 🇻🇳 TutorHub Vietnam Landmark Banner — Kế Hoạch Hoàn Chỉnh v2.0

> **Copy-paste ready · Production-grade · AI Agent Compatible**  
> Phiên bản: 2.0 | Tháng 6/2026 | Không cần dependency mới | Framework: React

---

## ⚡ Quick Start — 5 Bước Để Chạy Ngay

```
Bước 1: Copy toàn bộ files theo cấu trúc bên dưới
Bước 2: Thêm REACT_APP_UNSPLASH_ACCESS_KEY vào .env (tuỳ chọn)
Bước 3: Bọc <SearchBar> trong <SearchBannerImage> ở HomePage.jsx
Bước 4: npm start
Bước 5: Done. Ảnh tự rotate mỗi ngày, không cần làm gì thêm.
```

---

## 📁 Cấu Trúc File Đầy Đủ (14 files)

```
project-root/
│
├── .env.example                              ← Biến môi trường (bước setup)
├── scripts/
│   └── populate-images.js                   ← Script tự động lấy URL ảnh thật từ Unsplash
│
├── public/
│   └── data/
│       └── vietnam_landmarks.json            ← 30 địa điểm, dữ liệu đầy đủ
│
└── src/
    ├── utils/
    │   ├── dailyRotation.js                  ← Nguồn DUY NHẤT cho logic ngày + cache
    │   └── fetchWithTimeout.js               ← Wrapper fetch có timeout, dùng mọi nơi
    │
    ├── services/
    │   ├── imagePreloadService.js            ← Preload ảnh ngày mai khi browser rảnh
    │   ├── bingImageService.js               ← Bing HPImageArchive (miễn phí, không cần key)
    │   ├── unsplashVietnamService.js          ← Unsplash API (cần key, tùy chọn)
    │   └── imageProviderService.js           ← Orchestrator: priority queue + in-flight lock
    │
    └── components/
        └── SearchBannerImage/
            ├── ErrorBoundary.jsx             ← Bắt lỗi, giữ app chạy
            ├── LoadingSkeleton.jsx           ← UI trong khi chờ ảnh
            ├── useImageBanner.js             ← Custom hook, toàn bộ logic state
            ├── index.jsx                     ← Component chính (UI thuần)
            └── SearchBannerImage.css         ← Styles đầy đủ
```

---

## 🔧 Setup Môi Trường

### .env.example
```env
# Tùy chọn — Không có key thì tự động dùng Bing (miễn phí) + JSON curated
# Đăng ký miễn phí tại: https://unsplash.com/developers
REACT_APP_UNSPLASH_ACCESS_KEY=your_unsplash_access_key_here
```

> **Quan trọng:** Tính năng này KHÔNG cần thêm npm package nào.  
> Chỉ dùng React, CSS, fetch API, và localStorage — tất cả đã có sẵn.

---

## PHASE 0 — SHARED UTILITIES

### File 1/14: `src/utils/dailyRotation.js`

```javascript
/**
 * dailyRotation.js
 * =================
 * NGUỒN DUY NHẤT cho mọi logic liên quan đến ngày và cache banner ảnh.
 *
 * QUY TẮC: Không component hoặc service nào được tự viết lại getDayOfYear()
 * hay gọi localStorage trực tiếp. Luôn import từ file này để đảm bảo:
 * - Mọi user thấy cùng ảnh trong cùng ngày (deterministic)
 * - Cache không crash khi Safari Private Mode disable storage
 * - Không có race condition giữa 2 component cùng gọi cùng lúc
 */

/**
 * Trả về ngày hôm nay dạng "YYYY-MM-DD" (giờ địa phương).
 * Dùng làm key so sánh "đã sang ngày mới chưa".
 * @param {Date} [date=new Date()] - Cho phép inject date khi test
 * @returns {string} VD: "2026-06-25"
 */
export function getTodayKey(date = new Date()) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * Trả về index ổn định trong [0, length) dựa trên ngày UTC.
 * Dùng UTC để tránh lệch ảnh giữa user ở múi giờ khác nhau
 * (ưu tiên "cùng giờ UTC = cùng ảnh" hơn "đổi đúng nửa đêm địa phương").
 *
 * @param {number} length - Số lượng phần tử cần xoay vòng
 * @param {Date} [date=new Date()] - Inject date để test hoặc preload ngày mai
 * @returns {number} Index từ 0 đến length-1
 */
export function getRotationIndex(length, date = new Date()) {
  if (!length || length <= 0) return 0;
  const startOfYear = Date.UTC(date.getUTCFullYear(), 0, 1);
  const today = Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
  const dayOfYear = Math.floor((today - startOfYear) / 86_400_000); // 0-indexed
  return ((dayOfYear % length) + length) % length; // guard against negative modulo
}

/**
 * Wrapper an toàn cho localStorage.
 * Không throw khi storage bị disable (Safari Private Mode, quota đầy).
 * Mọi service đọc/ghi localStorage phải dùng object này, không dùng trực tiếp.
 */
export const safeStorage = {
  get(key) {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  },
  set(key, value) {
    try {
      localStorage.setItem(key, value);
      return true;
    } catch {
      return false; // Caller tiếp tục chạy bằng in-memory state, không crash
    }
  },
  remove(key) {
    try {
      localStorage.removeItem(key);
      return true;
    } catch {
      return false;
    }
  },
};

/**
 * Đọc cache theo ngày. Trả về null nếu cache không tồn tại,
 * lỗi parse, hoặc đã qua ngày mới — để caller tự fetch lại.
 *
 * @param {string} dataKey - Key lưu JSON data
 * @param {string} dateKey - Key lưu ngày tạo cache
 * @param {string} [today=getTodayKey()] - Inject để test
 * @returns {any|null}
 */
export function readDailyCache(dataKey, dateKey, today = getTodayKey()) {
  const cachedDate = safeStorage.get(dateKey);
  if (cachedDate !== today) return null;
  const raw = safeStorage.get(dataKey);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null; // Cache hỏng — coi như không có, không crash app
  }
}

/**
 * Ghi cache kèm ngày tạo. Trả về true/false để caller
 * biết có ghi được không (false = private mode, đã hết quota).
 *
 * @param {string} dataKey
 * @param {string} dateKey
 * @param {any} value - Sẽ được JSON.stringify
 * @param {string} [today=getTodayKey()]
 * @returns {boolean}
 */
export function writeDailyCache(dataKey, dateKey, value, today = getTodayKey()) {
  const ok1 = safeStorage.set(dateKey, today);
  const ok2 = safeStorage.set(dataKey, JSON.stringify(value));
  return ok1 && ok2;
}

/**
 * Xoá toàn bộ cache banner (dùng khi cần force refresh).
 * Gọi từ DevTools console: import { clearBannerCache } from './utils/dailyRotation'
 */
export function clearBannerCache() {
  const keys = [
    'tutorhub_banner_image_cache', 'tutorhub_banner_image_date',
    'tutorhub_bing_image', 'tutorhub_bing_image_date',
    'tutorhub_resolved_image', 'tutorhub_resolved_image_date',
  ];
  keys.forEach(k => safeStorage.remove(k));
  console.log('[TutorHub] Banner cache cleared');
}
```

---

### File 2/14: `src/utils/fetchWithTimeout.js`

```javascript
/**
 * fetchWithTimeout.js
 * ====================
 * Wrapper quanh fetch() với AbortController để đảm bảo
 * chuỗi fallback trong imageProviderService luôn resolve đúng lúc.
 *
 * QUY TẮC: Mọi service gọi API ngoài (Bing, Unsplash, Pexels)
 * PHẢI dùng hàm này thay vì fetch() trực tiếp.
 * Nếu không có timeout, promise treo vô hạn khi API chậm → UI trống mãi.
 */

/**
 * @param {string} url
 * @param {RequestInit} [options={}]
 * @param {number} [timeoutMs=5000] - Default 5s cho API calls thông thường
 * @returns {Promise<Response>} - Throw AbortError nếu timeout
 */
export async function fetchWithTimeout(url, options = {}, timeoutMs = 5000) {
  const controller = new AbortController();
  const timerId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
    });
    return response;
  } finally {
    clearTimeout(timerId); // Luôn clear timer dù thành công hay thất bại
  }
}

/**
 * Phiên bản retry: thử lại tối đa `retries` lần nếu gặp lỗi network.
 * Dùng khi cần độ tin cậy cao hơn (VD: fetch JSON landmarks lần đầu).
 *
 * @param {string} url
 * @param {RequestInit} [options={}]
 * @param {number} [timeoutMs=5000]
 * @param {number} [retries=2] - Số lần thử lại sau lần đầu thất bại
 * @param {number} [delayMs=500] - Thời gian chờ giữa các lần retry
 * @returns {Promise<Response>}
 */
export async function fetchWithRetry(url, options = {}, timeoutMs = 5000, retries = 2, delayMs = 500) {
  let lastError;
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      return await fetchWithTimeout(url, options, timeoutMs);
    } catch (err) {
      lastError = err;
      if (attempt < retries) {
        await new Promise(resolve => setTimeout(resolve, delayMs * (attempt + 1)));
      }
    }
  }
  throw lastError;
}
```

---

## PHASE 1 — DATA LAYER

### File 3/14: `public/data/vietnam_landmarks.json`

> **Lưu ý:** `image_url` hiện dùng [Picsum Photos](https://picsum.photos) làm placeholder.
> Chạy `node scripts/populate-images.js YOUR_UNSPLASH_KEY` để thay bằng ảnh thật.
> Hoặc tự thay từng URL bằng ảnh Unsplash/Pexels đúng địa điểm.

```json
{
  "version": "2.0",
  "lastUpdated": "2026-06-01",
  "note": "image_url dùng Picsum placeholder. Chạy scripts/populate-images.js để lấy ảnh thật từ Unsplash.",
  "landmarks": [
    {
      "id": "ha-long-bay",
      "name_vi": "Vịnh Hạ Long",
      "name_en": "Ha Long Bay",
      "location_vi": "Quảng Ninh, Việt Nam",
      "location_en": "Quang Ninh Province, Vietnam",
      "description_vi": "Di sản Thiên nhiên Thế giới UNESCO với hơn 1.600 hòn đảo đá vôi kỳ vĩ trên vịnh Bắc Bộ.",
      "description_en": "UNESCO Natural World Heritage site featuring over 1,600 limestone islands and islets in the Gulf of Tonkin.",
      "image_url": "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=1920&q=85&fit=crop&auto=format",
      "image_thumb": "https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=800&q=70&fit=crop&auto=format",
      "image_credit": "Unsplash Community",
      "image_credit_url": "https://unsplash.com/s/photos/ha-long-bay",
      "image_source": "Unsplash",
      "dominant_color": "#1a6b8a",
      "text_color": "light",
      "region": "north",
      "tags": ["UNESCO", "biển", "đảo", "thiên nhiên", "đá vôi"],
      "bing_search_url": "https://www.bing.com/search?q=V%E1%BB%8Bnh+H%E1%BA%A1+Long",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/V%E1%BB%8Bnh_H%E1%BA%A1_Long",
      "active": true
    },
    {
      "id": "mu-cang-chai",
      "name_vi": "Ruộng Bậc Thang Mù Cang Chải",
      "name_en": "Mu Cang Chai Rice Terraces",
      "location_vi": "Yên Bái, Việt Nam",
      "location_en": "Yen Bai Province, Vietnam",
      "description_vi": "Ruộng bậc thang rộng 2.200 ha được UNESCO công nhận là di sản văn hóa, đẹp nhất vào tháng 9–10 khi lúa chín vàng.",
      "description_en": "2,200-hectare rice terraces recognized by UNESCO, most spectacular in September–October harvest season.",
      "image_url": "https://picsum.photos/seed/mucangchai/1920/1080",
      "image_thumb": "https://picsum.photos/seed/mucangchai/800/450",
      "image_credit": "Cần cập nhật ảnh thật (chạy populate-images.js)",
      "image_credit_url": "https://unsplash.com/s/photos/mu-cang-chai",
      "image_source": "Picsum",
      "dominant_color": "#4a7c3f",
      "text_color": "light",
      "region": "north",
      "tags": ["ruộng bậc thang", "thiên nhiên", "núi", "mùa gặt", "Tây Bắc"],
      "bing_search_url": "https://www.bing.com/search?q=M%C3%B9+C%C4%83ng+Ch%E1%BA%A3i",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/M%C3%B9_C%C4%83ng_Ch%E1%BA%A3i",
      "active": true
    },
    {
      "id": "sapa-rice-terrace",
      "name_vi": "Ruộng Bậc Thang Sa Pa",
      "name_en": "Sapa Rice Terraces",
      "location_vi": "Lào Cai, Việt Nam",
      "location_en": "Lao Cai Province, Vietnam",
      "description_vi": "Thị trấn núi với ruộng bậc thang huyền ảo trong sương mù, nơi sinh sống của người Hmông và Dao Đỏ.",
      "description_en": "Mountain town with misty rice terraces, home to H'Mong and Red Dao ethnic minorities near Fansipan peak.",
      "image_url": "https://picsum.photos/seed/saparice/1920/1080",
      "image_thumb": "https://picsum.photos/seed/saparice/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/sapa-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#4d7a3e",
      "text_color": "light",
      "region": "north",
      "tags": ["ruộng bậc thang", "sương mù", "núi", "dân tộc", "Fansipan"],
      "bing_search_url": "https://www.bing.com/search?q=Sa+Pa+Vi%E1%BB%87t+Nam",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Sa_Pa",
      "active": true
    },
    {
      "id": "hoan-kiem-lake",
      "name_vi": "Hồ Hoàn Kiếm",
      "name_en": "Hoan Kiem Lake",
      "location_vi": "Hà Nội, Việt Nam",
      "location_en": "Hanoi, Vietnam",
      "description_vi": "\"Hồ Gươm\" huyền thoại giữa lòng Hà Nội với Tháp Rùa, gắn liền với truyền thuyết vua Lê trả gươm thần.",
      "description_en": "Legendary \"Sword Lake\" at the heart of Hanoi, home to Turtle Tower and the myth of King Le returning his magical sword.",
      "image_url": "https://picsum.photos/seed/hoankiem/1920/1080",
      "image_thumb": "https://picsum.photos/seed/hoankiem/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/hoan-kiem-lake",
      "image_source": "Picsum",
      "dominant_color": "#1a5276",
      "text_color": "light",
      "region": "north",
      "tags": ["đô thị", "hồ", "lịch sử", "Hà Nội", "biểu tượng"],
      "bing_search_url": "https://www.bing.com/search?q=H%E1%BB%93+Ho%C3%A0n+Ki%E1%BA%BFm",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/H%E1%BB%93_Ho%C3%A0n_Ki%E1%BA%BFm",
      "active": true
    },
    {
      "id": "son-doong-cave",
      "name_vi": "Hang Sơn Đoòng",
      "name_en": "Son Doong Cave",
      "location_vi": "Quảng Bình, Việt Nam",
      "location_en": "Quang Binh Province, Vietnam",
      "description_vi": "Hang động lớn nhất thế giới, dài 9km, cao 200m, phát hiện năm 2009 với rừng cây và mây bên trong.",
      "description_en": "World's largest cave, 9km long and 200m tall, discovered in 2009 with its own jungle, clouds, and river inside.",
      "image_url": "https://picsum.photos/seed/sondoong/1920/1080",
      "image_thumb": "https://picsum.photos/seed/sondoong/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/son-doong-cave",
      "image_source": "Picsum",
      "dominant_color": "#1c2833",
      "text_color": "light",
      "region": "north",
      "tags": ["hang động", "kỷ lục thế giới", "UNESCO", "khám phá", "Quảng Bình"],
      "bing_search_url": "https://www.bing.com/search?q=Hang+S%C6%A1n+%C4%90o%C3%B2ng",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/H%C3%A0ng_%C4%90%E1%BB%99ng_S%C6%A1n_%C4%90o%C3%B2ng",
      "active": true
    },
    {
      "id": "phong-nha-ke-bang",
      "name_vi": "Phong Nha – Kẻ Bàng",
      "name_en": "Phong Nha – Ke Bang",
      "location_vi": "Quảng Bình, Việt Nam",
      "location_en": "Quang Binh Province, Vietnam",
      "description_vi": "Di sản UNESCO với hệ thống hơn 300 hang động, sông ngầm và rừng nguyên sinh hàng trăm triệu năm tuổi.",
      "description_en": "UNESCO Heritage site with 300+ caves, underground rivers, and one of the world's oldest karst landscapes.",
      "image_url": "https://picsum.photos/seed/phongnha/1920/1080",
      "image_thumb": "https://picsum.photos/seed/phongnha/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/phong-nha-cave",
      "image_source": "Picsum",
      "dominant_color": "#154360",
      "text_color": "light",
      "region": "north",
      "tags": ["UNESCO", "hang động", "sông ngầm", "rừng", "địa chất"],
      "bing_search_url": "https://www.bing.com/search?q=Phong+Nha+K%E1%BA%BB+B%C3%A0ng",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/V%C6%B0%E1%BB%9Dn_qu%E1%BB%91c_gia_Phong_Nha-K%E1%BA%BB_B%C3%A0ng",
      "active": true
    },
    {
      "id": "ban-gioc-waterfall",
      "name_vi": "Thác Bản Giốc",
      "name_en": "Ban Gioc Waterfall",
      "location_vi": "Cao Bằng, Việt Nam",
      "location_en": "Cao Bang Province, Vietnam",
      "description_vi": "Một trong những thác nước lớn nhất Đông Nam Á, nằm trên biên giới Việt–Trung, rộng 300m và cao 70m.",
      "description_en": "One of Southeast Asia's largest waterfalls on the Vietnam–China border, 300m wide and 70m tall.",
      "image_url": "https://picsum.photos/seed/bangioc/1920/1080",
      "image_thumb": "https://picsum.photos/seed/bangioc/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/ban-gioc-waterfall",
      "image_source": "Picsum",
      "dominant_color": "#0369a1",
      "text_color": "light",
      "region": "north",
      "tags": ["thác nước", "biên giới", "thiên nhiên", "Cao Bằng", "kỳ quan"],
      "bing_search_url": "https://www.bing.com/search?q=Th%C3%A1c+B%E1%BA%A3n+Gi%E1%BB%91c",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Th%C3%A1c_B%E1%BA%A3n_Gi%E1%BB%91c",
      "active": true
    },
    {
      "id": "ninh-binh",
      "name_vi": "Tràng An – Ninh Bình",
      "name_en": "Trang An – Ninh Binh",
      "location_vi": "Ninh Bình, Việt Nam",
      "location_en": "Ninh Binh Province, Vietnam",
      "description_vi": "\"Hạ Long trên cạn\" với danh thắng Tràng An được UNESCO vinh danh, núi đá vôi và sông nước hữu tình.",
      "description_en": "\"Ha Long Bay on land\" — UNESCO-listed Trang An scenic area with limestone karsts, grottos, and waterways.",
      "image_url": "https://picsum.photos/seed/ninhbinh/1920/1080",
      "image_thumb": "https://picsum.photos/seed/ninhbinh/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/ninh-binh",
      "image_source": "Picsum",
      "dominant_color": "#1e5631",
      "text_color": "light",
      "region": "north",
      "tags": ["UNESCO", "karst", "thuyền", "thiên nhiên", "Tràng An"],
      "bing_search_url": "https://www.bing.com/search?q=Tr%C3%A0ng+An+Ninh+B%C3%ACnh",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Qu%E1%BA%A7n_th%E1%BB%83_danh_th%E1%BA%AFng_Tr%C3%A0ng_An",
      "active": true
    },
    {
      "id": "bai-dinh-temple",
      "name_vi": "Chùa Bái Đính",
      "name_en": "Bai Dinh Pagoda",
      "location_vi": "Ninh Bình, Việt Nam",
      "location_en": "Ninh Binh Province, Vietnam",
      "description_vi": "Quần thể chùa lớn nhất Việt Nam với 500 tượng La Hán đá, chuông đồng 36 tấn và nhiều kỷ lục quốc gia.",
      "description_en": "Vietnam's largest Buddhist complex featuring 500 stone Arhat statues, a 36-ton bronze bell, and multiple national records.",
      "image_url": "https://picsum.photos/seed/baidinhtamcoc/1920/1080",
      "image_thumb": "https://picsum.photos/seed/baidinhtamcoc/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/bai-dinh-pagoda",
      "image_source": "Picsum",
      "dominant_color": "#78350f",
      "text_color": "light",
      "region": "north",
      "tags": ["chùa", "Phật giáo", "tâm linh", "kiến trúc", "kỷ lục"],
      "bing_search_url": "https://www.bing.com/search?q=Ch%C3%B9a+B%C3%A1i+%C4%90%C3%ADnh",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Ch%C3%B9a_B%C3%A1i_%C4%90%C3%ADnh",
      "active": true
    },
    {
      "id": "dong-van-plateau",
      "name_vi": "Cao Nguyên Đá Đồng Văn",
      "name_en": "Dong Van Karst Plateau",
      "location_vi": "Hà Giang, Việt Nam",
      "location_en": "Ha Giang Province, Vietnam",
      "description_vi": "Công viên địa chất toàn cầu UNESCO với cao nguyên đá tai mèo ấn tượng và phố cổ Đồng Văn 200 năm tuổi.",
      "description_en": "UNESCO Global Geopark with striking jagged limestone terrain and the 200-year-old Dong Van ancient quarter.",
      "image_url": "https://picsum.photos/seed/dongvan/1920/1080",
      "image_thumb": "https://picsum.photos/seed/dongvan/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/ha-giang-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#374151",
      "text_color": "light",
      "region": "north",
      "tags": ["UNESCO", "cao nguyên đá", "biên giới", "dân tộc", "Hà Giang"],
      "bing_search_url": "https://www.bing.com/search?q=%C4%90%E1%BB%93ng+V%C4%83n+H%C3%A0+Giang",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/C%C3%B4ng_vi%C3%AAn_%C4%91%E1%BB%8Ba_ch%E1%BA%A5t_to%C3%A0n_c%E1%BA%A7u_Cao_nguy%C3%AAn_%C4%91%C3%A1_%C4%90%E1%BB%93ng_V%C4%83n",
      "active": true
    },
    {
      "id": "hoi-an-ancient-town",
      "name_vi": "Phố Cổ Hội An",
      "name_en": "Hoi An Ancient Town",
      "location_vi": "Quảng Nam, Việt Nam",
      "location_en": "Quang Nam Province, Vietnam",
      "description_vi": "Di sản Văn hóa Thế giới UNESCO, phố thương cảng cổ thế kỷ 15–19 nổi tiếng với đèn lồng và kiến trúc đa văn hóa.",
      "description_en": "UNESCO World Cultural Heritage — a 15th–19th century trading port renowned for lanterns, ancient temples, and multicultural architecture.",
      "image_url": "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=1920&q=85&fit=crop&auto=format",
      "image_thumb": "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=800&q=70&fit=crop&auto=format",
      "image_credit": "Unsplash Community",
      "image_credit_url": "https://unsplash.com/s/photos/hoi-an",
      "image_source": "Unsplash",
      "dominant_color": "#b45309",
      "text_color": "light",
      "region": "central",
      "tags": ["UNESCO", "phố cổ", "đèn lồng", "văn hóa", "kiến trúc"],
      "bing_search_url": "https://www.bing.com/search?q=Ph%E1%BB%91+C%E1%BB%95+H%E1%BB%99i+An",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Ph%E1%BB%91_c%E1%BB%95_H%E1%BB%99i_An",
      "active": true
    },
    {
      "id": "golden-bridge-ba-na",
      "name_vi": "Cầu Vàng Bà Nà Hills",
      "name_en": "Golden Bridge Ba Na Hills",
      "location_vi": "Đà Nẵng, Việt Nam",
      "location_en": "Da Nang, Vietnam",
      "description_vi": "Cây cầu 150m độc đáo được nâng đỡ bởi hai bàn tay khổng lồ, khai trương 2018, trở thành biểu tượng mới của Đà Nẵng.",
      "description_en": "Iconic 150m bridge held by two giant stone hands, opened in 2018 and quickly became one of Vietnam's most photographed spots.",
      "image_url": "https://picsum.photos/seed/goldenbridge/1920/1080",
      "image_thumb": "https://picsum.photos/seed/goldenbridge/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/golden-bridge-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#92400e",
      "text_color": "light",
      "region": "central",
      "tags": ["cầu", "kiến trúc", "du lịch", "Đà Nẵng", "Bà Nà Hills"],
      "bing_search_url": "https://www.bing.com/search?q=C%E1%BA%A7u+V%C3%A0ng+B%C3%A0+N%C3%A0+Hills",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/C%E1%BA%A7u_V%C3%A0ng_(B%C3%A0_N%C3%A0_Hills)",
      "active": true
    },
    {
      "id": "hai-van-pass",
      "name_vi": "Đèo Hải Vân",
      "name_en": "Hai Van Pass",
      "location_vi": "Thừa Thiên Huế – Đà Nẵng, Việt Nam",
      "location_en": "Thua Thien Hue – Da Nang, Vietnam",
      "description_vi": "\"Đèo Mây\" dài 20km nối Đà Nẵng với Huế, một trong những cung đường ven biển đẹp nhất Đông Nam Á.",
      "description_en": "\"Ocean Cloud Pass\" — a 20km mountain road connecting Da Nang to Hue, named one of Southeast Asia's most scenic coastal drives.",
      "image_url": "https://picsum.photos/seed/haivanpass/1920/1080",
      "image_thumb": "https://picsum.photos/seed/haivanpass/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/hai-van-pass",
      "image_source": "Picsum",
      "dominant_color": "#1e3a5f",
      "text_color": "light",
      "region": "central",
      "tags": ["đèo", "ven biển", "phong cảnh", "đường đèo", "miền Trung"],
      "bing_search_url": "https://www.bing.com/search?q=%C4%90%C3%A8o+H%E1%BA%A3i+V%C3%A2n",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/%C4%90%C3%A8o_H%E1%BA%A3i_V%C3%A2n",
      "active": true
    },
    {
      "id": "da-nang-beach",
      "name_vi": "Bãi Biển Đà Nẵng",
      "name_en": "Da Nang Beach",
      "location_vi": "Đà Nẵng, Việt Nam",
      "location_en": "Da Nang, Vietnam",
      "description_vi": "Bãi biển Mỹ Khê và Non Nước với cát trắng trải dài, được Forbes bình chọn là một trong 6 bãi biển đẹp nhất hành tinh.",
      "description_en": "My Khe and Non Nuoc beaches with pristine white sand, ranked by Forbes as one of the world's 6 most beautiful beaches.",
      "image_url": "https://picsum.photos/seed/danangbeach/1920/1080",
      "image_thumb": "https://picsum.photos/seed/danangbeach/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/da-nang-beach",
      "image_source": "Picsum",
      "dominant_color": "#0891b2",
      "text_color": "light",
      "region": "central",
      "tags": ["biển", "bãi cát", "resort", "Đà Nẵng", "kỳ nghỉ"],
      "bing_search_url": "https://www.bing.com/search?q=B%C3%A3i+bi%E1%BB%83n+%C4%90%C3%A0+N%E1%BA%B5ng",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/%C4%90%C3%A0_N%E1%BA%B5ng",
      "active": true
    },
    {
      "id": "my-son-sanctuary",
      "name_vi": "Thánh Địa Mỹ Sơn",
      "name_en": "My Son Sanctuary",
      "location_vi": "Quảng Nam, Việt Nam",
      "location_en": "Quang Nam Province, Vietnam",
      "description_vi": "Di sản UNESCO, quần thể đền tháp Chăm Pa từ thế kỷ 4–13, hơn 70 công trình kiến trúc Hindu tuyệt mỹ.",
      "description_en": "UNESCO Heritage site — over 70 Hindu temples and towers built by the Champa kingdom between the 4th and 13th centuries.",
      "image_url": "https://picsum.photos/seed/myson/1920/1080",
      "image_thumb": "https://picsum.photos/seed/myson/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/my-son-sanctuary",
      "image_source": "Picsum",
      "dominant_color": "#78350f",
      "text_color": "light",
      "region": "central",
      "tags": ["UNESCO", "Chăm Pa", "đền tháp", "lịch sử", "khảo cổ"],
      "bing_search_url": "https://www.bing.com/search?q=Th%C3%A1nh+%C4%91%E1%BB%8Ba+M%E1%BB%B9+S%C6%A1n",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Th%C3%A1nh_%C4%91%E1%BB%8Ba_M%E1%BB%B9_S%C6%A1n",
      "active": true
    },
    {
      "id": "lang-co-lagoon",
      "name_vi": "Đầm Lăng Cô",
      "name_en": "Lang Co Lagoon",
      "location_vi": "Thừa Thiên Huế, Việt Nam",
      "location_en": "Thua Thien Hue Province, Vietnam",
      "description_vi": "Một trong 30 vịnh đẹp nhất thế giới (Club of Most Beautiful Bays), nổi tiếng với đầm phá trong xanh và rừng dương liễu.",
      "description_en": "One of the world's 30 most beautiful bays (Club of Most Beautiful Bays) with turquoise lagoons and casuarina forests.",
      "image_url": "https://picsum.photos/seed/langco/1920/1080",
      "image_thumb": "https://picsum.photos/seed/langco/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/lang-co-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#0f766e",
      "text_color": "light",
      "region": "central",
      "tags": ["đầm phá", "biển", "thiên nhiên", "Huế", "yên tĩnh"],
      "bing_search_url": "https://www.bing.com/search?q=L%C4%83ng+C%C3%B4+Hu%E1%BA%BF",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/L%C4%83ng_C%C3%B4",
      "active": true
    },
    {
      "id": "hue-imperial-city",
      "name_vi": "Cố Đô Huế",
      "name_en": "Hue Imperial City",
      "location_vi": "Thừa Thiên Huế, Việt Nam",
      "location_en": "Thua Thien Hue Province, Vietnam",
      "description_vi": "Di sản UNESCO, cố đô triều Nguyễn (1802–1945) với Hoàng Thành, lăng tẩm vua chúa và chùa Thiên Mụ.",
      "description_en": "UNESCO Heritage site — former imperial capital of the Nguyen dynasty (1802–1945) with its citadel, royal tombs, and Thien Mu Pagoda.",
      "image_url": "https://picsum.photos/seed/hueimperial/1920/1080",
      "image_thumb": "https://picsum.photos/seed/hueimperial/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/hue-citadel-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#6b21a8",
      "text_color": "light",
      "region": "central",
      "tags": ["UNESCO", "hoàng thành", "lịch sử", "triều Nguyễn", "lăng tẩm"],
      "bing_search_url": "https://www.bing.com/search?q=C%E1%BB%91+%C4%90%C3%B4+Hu%E1%BA%BF",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Kinh_th%C3%A0nh_Hu%E1%BA%BF",
      "active": true
    },
    {
      "id": "mui-ne-sand-dune",
      "name_vi": "Đồi Cát Đỏ Mũi Né",
      "name_en": "Mui Ne Red Sand Dunes",
      "location_vi": "Bình Thuận, Việt Nam",
      "location_en": "Binh Thuan Province, Vietnam",
      "description_vi": "Đồi cát đỏ và trắng huyền ảo, nổi tiếng với cảnh bình minh rực rỡ và thiên đường lướt diều nổi tiếng Đông Nam Á.",
      "description_en": "Red and white sand dunes, famous for spectacular sunrises and as Southeast Asia's top kitesurfing destination.",
      "image_url": "https://picsum.photos/seed/muinesand/1920/1080",
      "image_thumb": "https://picsum.photos/seed/muinesand/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/mui-ne-sand-dunes",
      "image_source": "Picsum",
      "dominant_color": "#c2410c",
      "text_color": "light",
      "region": "central",
      "tags": ["đồi cát", "sa mạc", "bình minh", "lướt diều", "Bình Thuận"],
      "bing_search_url": "https://www.bing.com/search?q=M%C5%A9i+N%C3%A9+B%C3%ACnh+Thu%E1%BA%ADn",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/M%C5%A9i_N%C3%A9",
      "active": true
    },
    {
      "id": "ly-son-island",
      "name_vi": "Đảo Lý Sơn",
      "name_en": "Ly Son Island",
      "location_vi": "Quảng Ngãi, Việt Nam",
      "location_en": "Quang Ngai Province, Vietnam",
      "description_vi": "\"Vương quốc tỏi\" với địa hình núi lửa độc đáo, rạn san hô đa dạng và bãi biển hoang sơ chưa bị khai thác.",
      "description_en": "The \"Garlic Kingdom\" — a volcanic island with unique crater terrain, rich coral reefs, and pristine beaches.",
      "image_url": "https://picsum.photos/seed/lysonisland/1920/1080",
      "image_thumb": "https://picsum.photos/seed/lysonisland/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/ly-son-island-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#1e40af",
      "text_color": "light",
      "region": "central",
      "tags": ["đảo", "núi lửa", "san hô", "hoang sơ", "Quảng Ngãi"],
      "bing_search_url": "https://www.bing.com/search?q=%C4%90%E1%BA%A3o+L%C3%BD+S%C6%A1n",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Huy%E1%BB%87n_%C4%91%E1%BA%A3o_L%C3%BD_S%C6%A1n",
      "active": true
    },
    {
      "id": "da-lat-lake",
      "name_vi": "Đà Lạt – Hồ Tuyền Lâm",
      "name_en": "Da Lat – Tuyen Lam Lake",
      "location_vi": "Lâm Đồng, Việt Nam",
      "location_en": "Lam Dong Province, Vietnam",
      "description_vi": "\"Thành phố ngàn hoa\" với kiến trúc Pháp cổ kính, đồi thông xanh mướt và khí hậu mát mẻ quanh năm.",
      "description_en": "\"City of Eternal Spring\" — French colonial architecture, pine forests, flower gardens, and a mild mountain climate year-round.",
      "image_url": "https://picsum.photos/seed/dalatlake/1920/1080",
      "image_thumb": "https://picsum.photos/seed/dalatlake/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/da-lat-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#166534",
      "text_color": "light",
      "region": "central",
      "tags": ["hồ", "rừng thông", "Pháp thuộc", "hoa", "khí hậu mát"],
      "bing_search_url": "https://www.bing.com/search?q=%C4%90%C3%A0+L%E1%BA%A1t+L%C3%A2m+%C4%90%E1%BB%93ng",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/%C4%90%C3%A0_L%E1%BA%A1t",
      "active": true
    },
    {
      "id": "mekong-delta",
      "name_vi": "Đồng Bằng Sông Cửu Long",
      "name_en": "Mekong Delta",
      "location_vi": "Đồng bằng Sông Cửu Long, Việt Nam",
      "location_en": "Mekong Delta, Vietnam",
      "description_vi": "\"Vựa lúa\" của Việt Nam với mạng lưới kênh rạch chằng chịt, chợ nổi, vườn trái cây nhiệt đới và đời sống sông nước.",
      "description_en": "Vietnam's \"rice bowl\" — a vast network of rivers, floating markets, fruit orchards, and unique waterway culture.",
      "image_url": "https://picsum.photos/seed/mekongdelta/1920/1080",
      "image_thumb": "https://picsum.photos/seed/mekongdelta/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/mekong-delta-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#3f6212",
      "text_color": "light",
      "region": "south",
      "tags": ["sông nước", "chợ nổi", "lúa gạo", "miền Tây", "kênh rạch"],
      "bing_search_url": "https://www.bing.com/search?q=%C4%90%E1%BB%93ng+B%E1%BA%B1ng+S%C3%B4ng+C%E1%BB%ADu+Long",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/%C4%90%E1%BB%93ng_b%E1%BA%B1ng_s%C3%B4ng_C%E1%BB%ADu_Long",
      "active": true
    },
    {
      "id": "phu-quoc-island",
      "name_vi": "Đảo Phú Quốc",
      "name_en": "Phu Quoc Island",
      "location_vi": "Kiên Giang, Việt Nam",
      "location_en": "Kien Giang Province, Vietnam",
      "description_vi": "Đảo lớn nhất Việt Nam, khu sinh quyển UNESCO với bãi cát trắng, nước biển trong xanh và hệ sinh thái rừng nguyên sinh.",
      "description_en": "Vietnam's largest island, a UNESCO Biosphere Reserve with white sandy beaches, crystal waters, and pristine forest.",
      "image_url": "https://picsum.photos/seed/phuquoc/1920/1080",
      "image_thumb": "https://picsum.photos/seed/phuquoc/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/phu-quoc-island",
      "image_source": "Picsum",
      "dominant_color": "#0e7490",
      "text_color": "light",
      "region": "south",
      "tags": ["đảo", "biển", "UNESCO", "resort", "nghỉ dưỡng"],
      "bing_search_url": "https://www.bing.com/search?q=%C4%90%E1%BA%A3o+Ph%C3%BA+Qu%E1%BB%91c",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Ph%C3%BA_Qu%E1%BB%91c",
      "active": true
    },
    {
      "id": "con-dao-island",
      "name_vi": "Côn Đảo",
      "name_en": "Con Dao Archipelago",
      "location_vi": "Bà Rịa – Vũng Tàu, Việt Nam",
      "location_en": "Ba Ria – Vung Tau Province, Vietnam",
      "description_vi": "Quần đảo 16 đảo hoang sơ với bãi biển nguyên vẹn, rùa biển đẻ trứng và vườn quốc gia đa dạng sinh học bậc nhất.",
      "description_en": "A pristine 16-island archipelago known for sea turtle nesting beaches, crystal-clear diving spots, and remarkable biodiversity.",
      "image_url": "https://picsum.photos/seed/condao/1920/1080",
      "image_thumb": "https://picsum.photos/seed/condao/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/con-dao-vietnam",
      "image_source": "Picsum",
      "dominant_color": "#164e63",
      "text_color": "light",
      "region": "south",
      "tags": ["đảo", "rùa biển", "lặn biển", "hoang sơ", "thiên nhiên"],
      "bing_search_url": "https://www.bing.com/search?q=C%C3%B4n+%C4%90%E1%BA%A3o+Vi%E1%BB%87t+Nam",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/C%C3%B4n_%C4%90%E1%BA%A3o",
      "active": true
    },
    {
      "id": "cat-tien-national-park",
      "name_vi": "Vườn Quốc Gia Cát Tiên",
      "name_en": "Cat Tien National Park",
      "location_vi": "Đồng Nai – Lâm Đồng, Việt Nam",
      "location_en": "Dong Nai – Lam Dong Province, Vietnam",
      "description_vi": "Khu Dự trữ Sinh quyển UNESCO với rừng nhiệt đới hơn 70.000 ha, nơi sống của 1.600+ loài thực vật và 400+ loài chim.",
      "description_en": "UNESCO Biosphere Reserve with 70,000+ hectares of tropical rainforest, home to 1,600+ plant species and 400+ bird species.",
      "image_url": "https://picsum.photos/seed/cattien/1920/1080",
      "image_thumb": "https://picsum.photos/seed/cattien/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/cat-tien-rainforest",
      "image_source": "Picsum",
      "dominant_color": "#14532d",
      "text_color": "light",
      "region": "south",
      "tags": ["UNESCO", "rừng nhiệt đới", "động vật", "chim", "sinh thái"],
      "bing_search_url": "https://www.bing.com/search?q=V%C6%B0%E1%BB%9Dn+Qu%E1%BB%91c+Gia+C%C3%A1t+Ti%C3%AAn",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/V%C6%B0%E1%BB%9Dn_qu%E1%BB%91c_gia_C%C3%A1t_Ti%C3%AAn",
      "active": true
    },
    {
      "id": "cai-rang-floating-market",
      "name_vi": "Chợ Nổi Cái Răng",
      "name_en": "Cai Rang Floating Market",
      "location_vi": "Cần Thơ, Việt Nam",
      "location_en": "Can Tho City, Vietnam",
      "description_vi": "Chợ nổi lớn nhất miền Tây với hàng trăm ghe thuyền chở đầy trái cây và nông sản, họp từ 5 giờ sáng mỗi ngày.",
      "description_en": "The Mekong Delta's largest floating market — hundreds of boats laden with tropical fruits, opening daily at 5 AM.",
      "image_url": "https://picsum.photos/seed/cairang/1920/1080",
      "image_thumb": "https://picsum.photos/seed/cairang/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/cai-rang-floating-market",
      "image_source": "Picsum",
      "dominant_color": "#92400e",
      "text_color": "light",
      "region": "south",
      "tags": ["chợ nổi", "miền Tây", "văn hóa", "ghe thuyền", "Cần Thơ"],
      "bing_search_url": "https://www.bing.com/search?q=Ch%E1%BB%A3+N%E1%BB%95i+C%C3%A1i+R%C4%83ng",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Ch%E1%BB%A3_n%E1%BB%95i_C%C3%A1i_R%C4%83ng",
      "active": true
    },
    {
      "id": "notre-dame-saigon",
      "name_vi": "Nhà Thờ Đức Bà Sài Gòn",
      "name_en": "Saigon Notre-Dame Cathedral",
      "location_vi": "TP. Hồ Chí Minh, Việt Nam",
      "location_en": "Ho Chi Minh City, Vietnam",
      "description_vi": "Nhà thờ Đức Bà Sài Gòn xây năm 1880, công trình kiến trúc Pháp biểu tượng của thành phố, nằm ngay trung tâm Quận 1.",
      "description_en": "1880 French-built cathedral, an iconic landmark of Ho Chi Minh City in the heart of District 1.",
      "image_url": "https://picsum.photos/seed/notredamesaigon/1920/1080",
      "image_thumb": "https://picsum.photos/seed/notredamesaigon/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/notre-dame-cathedral-saigon",
      "image_source": "Picsum",
      "dominant_color": "#7f1d1d",
      "text_color": "light",
      "region": "south",
      "tags": ["kiến trúc Pháp", "nhà thờ", "lịch sử", "Sài Gòn", "biểu tượng"],
      "bing_search_url": "https://www.bing.com/search?q=Nh%C3%A0+Th%E1%BB%9D+%C4%90%E1%BB%A9c+B%C3%A0+S%C3%A0i+G%C3%B2n",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Nh%C3%A0_th%E1%BB%9D_ch%C3%ADnh_t%C3%B2a_S%C3%A0i_G%C3%B2n",
      "active": true
    },
    {
      "id": "ben-nha-rong",
      "name_vi": "Bến Nhà Rồng",
      "name_en": "Ben Nha Rong Wharf",
      "location_vi": "TP. Hồ Chí Minh, Việt Nam",
      "location_en": "Ho Chi Minh City, Vietnam",
      "description_vi": "\"Cảng Nhà Rồng\" lịch sử xây năm 1899, nơi ngày 5/6/1911 Chủ tịch Hồ Chí Minh xuất dương tìm đường cứu nước.",
      "description_en": "Historic 1899 wharf from which Ho Chi Minh departed on June 5, 1911, to seek independence for Vietnam.",
      "image_url": "https://picsum.photos/seed/bennharong/1920/1080",
      "image_thumb": "https://picsum.photos/seed/bennharong/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/ho-chi-minh-city-river",
      "image_source": "Picsum",
      "dominant_color": "#1e3a5f",
      "text_color": "light",
      "region": "south",
      "tags": ["lịch sử", "Hồ Chí Minh", "bảo tàng", "bến cảng", "Sài Gòn"],
      "bing_search_url": "https://www.bing.com/search?q=B%E1%BA%BFn+Nh%C3%A0+R%E1%BB%93ng",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/B%E1%BA%BFn_Nh%C3%A0_R%E1%BB%93ng",
      "active": true
    },
    {
      "id": "buon-ma-thuot-highland",
      "name_vi": "Buôn Ma Thuột – Cao Nguyên Cà Phê",
      "name_en": "Buon Ma Thuot – Coffee Highland",
      "location_vi": "Đắk Lắk, Việt Nam",
      "location_en": "Dak Lak Province, Vietnam",
      "description_vi": "Thủ phủ cà phê Việt Nam trên cao nguyên Tây Nguyên, nơi sản xuất hơn 40% sản lượng cà phê xuất khẩu cả nước.",
      "description_en": "Vietnam's coffee capital on the Central Highlands, producing over 40% of the country's coffee export volume.",
      "image_url": "https://picsum.photos/seed/buonmathuot/1920/1080",
      "image_thumb": "https://picsum.photos/seed/buonmathuot/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/buon-ma-thuot-coffee",
      "image_source": "Picsum",
      "dominant_color": "#431407",
      "text_color": "light",
      "region": "south",
      "tags": ["cà phê", "cao nguyên", "Tây Nguyên", "dân tộc", "nông nghiệp"],
      "bing_search_url": "https://www.bing.com/search?q=Bu%C3%B4n+Ma+Thu%E1%BB%99t+%C4%90%E1%BA%AFk+L%E1%BA%AFk",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Bu%C3%B4n_Ma_Thu%E1%BB%99t",
      "active": true
    },
    {
      "id": "nha-trang-beach",
      "name_vi": "Vịnh Nha Trang",
      "name_en": "Nha Trang Bay",
      "location_vi": "Khánh Hòa, Việt Nam",
      "location_en": "Khanh Hoa Province, Vietnam",
      "description_vi": "Một trong 29 vịnh đẹp nhất thế giới với rạn san hô đa dạng, tháp Chăm Po Nagar và khu nghỉ dưỡng đẳng cấp.",
      "description_en": "One of the world's 29 most beautiful bays — vibrant coral reefs, the ancient Po Nagar Cham towers, and world-class resorts.",
      "image_url": "https://picsum.photos/seed/nhatrang/1920/1080",
      "image_thumb": "https://picsum.photos/seed/nhatrang/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/nha-trang-beach",
      "image_source": "Picsum",
      "dominant_color": "#0369a1",
      "text_color": "light",
      "region": "south",
      "tags": ["vịnh", "san hô", "lặn biển", "resort", "Khánh Hòa"],
      "bing_search_url": "https://www.bing.com/search?q=V%E1%BB%8Bnh+Nha+Trang",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/Nha_Trang",
      "active": true
    },
    {
      "id": "binh-ba-island",
      "name_vi": "Đảo Bình Ba",
      "name_en": "Binh Ba Island",
      "location_vi": "Khánh Hòa, Việt Nam",
      "location_en": "Khanh Hoa Province, Vietnam",
      "description_vi": "\"Đảo tôm hùm xanh\" hoang sơ với nước biển trong vắt, san hô còn nguyên sơ và hải sản tươi ngon giá bình dân.",
      "description_en": "The pristine \"Blue Lobster Island\" with crystal-clear waters, intact coral reefs, and fresh seafood at local prices.",
      "image_url": "https://picsum.photos/seed/binhba/1920/1080",
      "image_thumb": "https://picsum.photos/seed/binhba/800/450",
      "image_credit": "Cần cập nhật ảnh thật",
      "image_credit_url": "https://unsplash.com/s/photos/binh-ba-island",
      "image_source": "Picsum",
      "dominant_color": "#134e4a",
      "text_color": "light",
      "region": "south",
      "tags": ["đảo", "hoang sơ", "hải sản", "lặn biển", "tôm hùm"],
      "bing_search_url": "https://www.bing.com/search?q=%C4%90%E1%BA%A3o+B%C3%ACnh+Ba+Kh%C3%A1nh+H%C3%B2a",
      "wikipedia_url": "https://vi.wikipedia.org/wiki/B%C3%ACnh_Ba",
      "active": true
    }
  ]
}
```

---

### File 4/14: `scripts/populate-images.js`

> Chạy: `node scripts/populate-images.js YOUR_UNSPLASH_ACCESS_KEY`  
> Script này tự tìm ảnh thật từ Unsplash cho từng địa điểm và cập nhật JSON.

```javascript
/**
 * populate-images.js
 * ==================
 * Script Node.js tự động lấy URL ảnh thật từ Unsplash API
 * và cập nhật vào vietnam_landmarks.json.
 *
 * Chạy 1 lần sau khi có Unsplash API key:
 *   node scripts/populate-images.js ACCESS_KEY_CUA_BAN
 *
 * Sau khi chạy: commit lại file vietnam_landmarks.json đã được cập nhật.
 */

const fs = require('fs');
const path = require('path');
const https = require('https');

const ACCESS_KEY = process.argv[2];
if (!ACCESS_KEY) {
  console.error('❌ Thiếu Unsplash Access Key!');
  console.error('   Cách dùng: node scripts/populate-images.js YOUR_KEY');
  console.error('   Đăng ký miễn phí: https://unsplash.com/developers');
  process.exit(1);
}

const JSON_PATH = path.join(__dirname, '..', 'public', 'data', 'vietnam_landmarks.json');

/** Mapping từ landmark ID sang query Unsplash tốt nhất */
const UNSPLASH_QUERIES = {
  'ha-long-bay':              'ha long bay vietnam',
  'mu-cang-chai':             'mu cang chai rice terrace vietnam',
  'sapa-rice-terrace':        'sapa vietnam rice terraces misty',
  'hoan-kiem-lake':           'hoan kiem lake hanoi vietnam',
  'son-doong-cave':           'son doong cave vietnam',
  'phong-nha-ke-bang':        'phong nha cave vietnam',
  'ban-gioc-waterfall':       'ban gioc waterfall vietnam',
  'ninh-binh':                'ninh binh tam coc vietnam',
  'bai-dinh-temple':          'pagoda vietnam temple',
  'dong-van-plateau':         'ha giang vietnam mountain pass',
  'hoi-an-ancient-town':      'hoi an vietnam lanterns',
  'golden-bridge-ba-na':      'golden bridge ba na hills vietnam',
  'hai-van-pass':             'hai van pass vietnam coast',
  'da-nang-beach':            'da nang beach vietnam',
  'my-son-sanctuary':         'my son champa temple vietnam',
  'lang-co-lagoon':           'lang co vietnam lagoon',
  'hue-imperial-city':        'hue citadel imperial vietnam',
  'mui-ne-sand-dune':         'mui ne sand dunes vietnam',
  'ly-son-island':            'vietnam island tropical aerial',
  'da-lat-lake':              'da lat vietnam lake pine forest',
  'mekong-delta':             'mekong delta vietnam aerial river',
  'phu-quoc-island':          'phu quoc island vietnam beach',
  'con-dao-island':           'con dao island vietnam pristine',
  'cat-tien-national-park':   'vietnam tropical rainforest',
  'cai-rang-floating-market': 'mekong floating market vietnam',
  'notre-dame-saigon':        'notre dame cathedral saigon',
  'ben-nha-rong':             'ho chi minh city river saigon',
  'buon-ma-thuot-highland':   'vietnam coffee highland plantation',
  'nha-trang-beach':          'nha trang bay vietnam turquoise',
  'binh-ba-island':           'vietnam tropical island clear water',
};

function httpsGet(url) {
  return new Promise((resolve, reject) => {
    const options = {
      headers: {
        'Authorization': `Client-ID ${ACCESS_KEY}`,
        'Accept-Version': 'v1',
      },
    };
    https.get(url, options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) resolve(JSON.parse(data));
        else reject(new Error(`HTTP ${res.statusCode}: ${data}`));
      });
    }).on('error', reject);
  });
}

async function fetchUnsplashPhoto(query) {
  const url = `https://api.unsplash.com/search/photos?query=${encodeURIComponent(query)}&orientation=landscape&per_page=3&content_filter=high&order_by=relevant`;
  const data = await httpsGet(url);
  const photo = data.results?.[0];
  if (!photo) throw new Error(`No results for: ${query}`);
  return {
    image_url:      `${photo.urls.raw}&w=1920&q=85&fit=crop&auto=format`,
    image_thumb:    `${photo.urls.raw}&w=800&q=70&fit=crop&auto=format`,
    image_credit:   photo.user.name,
    image_credit_url: `${photo.user.links.html}?utm_source=tutorhub&utm_medium=referral`,
    image_source:   'Unsplash',
    unsplash_id:    photo.id,
    download_url:   photo.links.download_location,
  };
}

async function main() {
  console.log('🚀 Bắt đầu populate ảnh thật từ Unsplash...\n');
  const json = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
  let updated = 0, skipped = 0, failed = 0;

  for (const landmark of json.landmarks) {
    // Bỏ qua nếu đã có ảnh thật (không phải Picsum)
    if (landmark.image_source !== 'Picsum' && landmark.unsplash_id) {
      console.log(`  ⏭️  ${landmark.id} — đã có ảnh thật, bỏ qua`);
      skipped++;
      continue;
    }

    const query = UNSPLASH_QUERIES[landmark.id];
    if (!query) {
      console.warn(`  ⚠️  ${landmark.id} — không có query mapping, bỏ qua`);
      skipped++;
      continue;
    }

    try {
      console.log(`  🔍 ${landmark.id} — query: "${query}"`);
      const photo = await fetchUnsplashPhoto(query);
      Object.assign(landmark, photo);
      console.log(`  ✅ ${landmark.id} — OK (by ${photo.image_credit})`);
      updated++;
      // Chờ 250ms giữa các request để không vượt rate limit
      await new Promise(r => setTimeout(r, 250));
    } catch (err) {
      console.error(`  ❌ ${landmark.id} — FAIL: ${err.message}`);
      failed++;
    }
  }

  json.lastUpdated = new Date().toISOString().split('T')[0];
  json.note = 'Ảnh được lấy từ Unsplash API. Đã có ảnh thật.';
  fs.writeFileSync(JSON_PATH, JSON.stringify(json, null, 2), 'utf8');

  console.log(`\n📊 Kết quả:`);
  console.log(`   ✅ Cập nhật: ${updated}`);
  console.log(`   ⏭️  Bỏ qua:  ${skipped}`);
  console.log(`   ❌ Thất bại: ${failed}`);
  console.log('\n✨ Xong! Commit lại vietnam_landmarks.json vào git.');
}

main().catch(err => {
  console.error('💥 Script crash:', err);
  process.exit(1);
});
```

---

## PHASE 2 — SERVICES

### File 5/14: `src/services/imagePreloadService.js`

```javascript
/**
 * imagePreloadService.js
 * =======================
 * Preload ảnh ngày MAI vào browser cache khi browser đang rảnh.
 * Khi user mở app ngày hôm sau, ảnh đã có sẵn → hiện ngay, không có
 * khoảng trống. Đây là kỹ thuật của Windows Spotlight và Bing.
 *
 * QUY TẮC: Fire-and-forget, không throw lỗi ra ngoài, không block render.
 */
import { getRotationIndex } from '../utils/dailyRotation';

/** Trả về Date object của ngày mai */
const getTomorrow = () => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d;
};

/**
 * Preload ảnh ngày mai vào browser HTTP cache.
 * Gọi sau khi ảnh hôm nay đã render xong (trong useEffect cleanup phase).
 *
 * @param {Array<Object>} activeLandmarks - Array landmark đang active
 */
export function preloadNextDailyImage(activeLandmarks) {
  if (!activeLandmarks?.length) return;

  // requestIdleCallback: chạy khi browser không bận render/JS khác
  // Fallback sang setTimeout 200ms cho browser không hỗ trợ (IE, cũ)
  const schedule = window.requestIdleCallback
    ? (cb) => window.requestIdleCallback(cb, { timeout: 2000 })
    : (cb) => setTimeout(cb, 200);

  schedule(() => {
    try {
      const tomorrowIndex = getRotationIndex(activeLandmarks.length, getTomorrow());
      const tomorrow = activeLandmarks[tomorrowIndex];
      if (!tomorrow?.image_url) return;

      // Tạo Image object — browser tự cache theo HTTP response headers
      const img = new Image();
      img.src = tomorrow.image_url;
      // Không cần onload — fire-and-forget là đủ

      // Cũng preload thumbnail để skeleton load nhanh hơn
      if (tomorrow.image_thumb) {
        const thumb = new Image();
        thumb.src = tomorrow.image_thumb;
      }
    } catch {
      // Preload optional — im lặng nếu fail
    }
  });
}
```

---

### File 6/14: `src/services/bingImageService.js`

```javascript
/**
 * bingImageService.js
 * ====================
 * Lấy ảnh của ngày từ Bing HPImageArchive API (không cần key, miễn phí).
 * Endpoint này đã hoạt động ổn định nhiều năm và là nguồn ảnh của
 * Windows Spotlight / Bing Search.
 *
 * Endpoint: https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=vi-VN
 */
import { fetchWithTimeout } from '../utils/fetchWithTimeout';
import { getTodayKey, readDailyCache, writeDailyCache } from '../utils/dailyRotation';

const BASE_URL    = 'https://www.bing.com';
const API_ENDPOINT = '/HPImageArchive.aspx';
const CACHE_KEY   = 'tutorhub_bing_image';
const CACHE_DATE  = 'tutorhub_bing_image_date';

/**
 * Các độ phân giải Bing hỗ trợ.
 * Thay TARGET_RESOLUTION thành '_UHD.jpg' cho màn hình 4K.
 */
const RESOLUTIONS = {
  UHD:     '_UHD.jpg',       // 4K — ~800KB
  FHD:     '_1920x1080.jpg', // 1080p — ~300KB (DEFAULT)
  HD:      '_1366x768.jpg',  // HD — ~150KB
  MOBILE:  '_800x480.jpg',   // Mobile — ~80KB
};
const TARGET_RESOLUTION = RESOLUTIONS.FHD;
const THUMB_RESOLUTION  = RESOLUTIONS.MOBILE;

/**
 * Parse chuỗi copyright Bing: "Tên địa điểm, Tỉnh (© Photographer/Agency)"
 * Trả về tên địa điểm hoặc fallback về "Việt Nam".
 */
function extractLocation(copyright = '') {
  const parts = copyright.split(' (©')[0].trim();
  return parts || 'Việt Nam';
}

/**
 * Parse tên nhiếp ảnh gia từ copyright: "(© Tên/Getty Images)"
 */
function extractPhotographer(copyright = '') {
  const match = copyright.match(/\(©\s*([^)]+)\)/);
  if (!match) return null;
  const raw = match[1].trim();
  // Bỏ phần tên agency: "Photographer/Getty Images" → "Photographer"
  return raw.split('/')[0].trim() || null;
}

/**
 * Lấy ảnh của ngày từ Bing HPImageArchive API.
 * Cache 1 ngày trong localStorage.
 *
 * @param {string} [market='vi-VN'] - 'vi-VN' ưu tiên ảnh Việt Nam
 * @returns {Promise<Object|null>} - Landmark-compatible object hoặc null
 */
export async function getBingDailyImage(market = 'vi-VN') {
  const today = getTodayKey();

  // Cache hit: trả về ngay
  const cached = readDailyCache(CACHE_KEY, CACHE_DATE, today);
  if (cached) return cached;

  try {
    const params = new URLSearchParams({
      format: 'js',
      idx:    '0',     // 0 = hôm nay, 1 = hôm qua, 2–6 = 7 ngày trước
      n:      '1',     // Số ảnh: 1–8
      mkt:    market,
    });

    const response = await fetchWithTimeout(
      `${BASE_URL}${API_ENDPOINT}?${params}`,
      { headers: { 'Accept': 'application/json' } },
      5000 // 5s timeout — đủ ngắn để không kẹt chuỗi fallback
    );

    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    const image = data.images?.[0];
    if (!image) throw new Error('Bing returned no images');

    // Bing trả về urlbase không có suffix → tự thêm độ phân giải
    const imageData = {
      id:               `bing-${image.startdate}`,
      name_vi:          image.title || 'Danh lam thắng cảnh',
      name_en:          image.title || 'Scenic Landmark',
      location_vi:      extractLocation(image.copyright),
      location_en:      extractLocation(image.copyright),
      description_vi:   image.copyright || '',
      description_en:   image.copyright || '',
      image_url:        `${BASE_URL}${image.urlbase}${TARGET_RESOLUTION}`,
      image_thumb:      `${BASE_URL}${image.urlbase}${THUMB_RESOLUTION}`,
      image_credit:     extractPhotographer(image.copyright) || 'Bing / Microsoft',
      image_credit_url: image.copyrightlink || `${BASE_URL}/search?q=${encodeURIComponent(image.title || '')}`,
      image_source:     'Bing',
      bing_search_url:  image.copyrightlink || '#',
      dominant_color:   '#1a3a5c',
      text_color:       'light',
      tags:             ['Bing'],
      active:           true,
      region:           'auto',
    };

    writeDailyCache(CACHE_KEY, CACHE_DATE, imageData, today);
    return imageData;

  } catch (err) {
    // Không warn trong production để tránh log noise
    if (process.env.NODE_ENV === 'development') {
      console.warn('[TutorHub] Bing image fetch failed:', err.message);
    }
    return null;
  }
}
```

---

### File 7/14: `src/services/unsplashVietnamService.js`

```javascript
/**
 * unsplashVietnamService.js
 * ==========================
 * Lấy ảnh Vietnam từ Unsplash API theo keyword của ngày.
 * Cần REACT_APP_UNSPLASH_ACCESS_KEY trong .env để hoạt động.
 * Nếu không có key → service trả về null ngay, không có lỗi.
 *
 * Lưu ý attribution bắt buộc của Unsplash:
 * "Photo by [Tên] on Unsplash" với hyperlink đến profile.
 */
import { fetchWithTimeout } from '../utils/fetchWithTimeout';
import { getRotationIndex, getTodayKey, readDailyCache, writeDailyCache } from '../utils/dailyRotation';

const BASE_URL   = 'https://api.unsplash.com';
const CACHE_KEY  = 'tutorhub_unsplash_image';
const CACHE_DATE = 'tutorhub_unsplash_date';

/** Keywords xoay vòng theo ngày để đa dạng ảnh Vietnam */
const VIETNAM_KEYWORDS = [
  'ha long bay vietnam',
  'hoi an vietnam lanterns',
  'sapa rice terrace vietnam',
  'mekong delta vietnam',
  'da nang beach vietnam',
  'phu quoc island vietnam',
  'ninh binh vietnam karst',
  'hue citadel vietnam',
  'ban gioc waterfall vietnam',
  'da lat vietnam pine forest',
  'mu cang chai vietnam',
  'golden bridge vietnam hands',
  'hai van pass vietnam coast',
  'nha trang beach vietnam',
  'vietnam floating market mekong',
];

/**
 * Lấy 1 ảnh Vietnam từ Unsplash theo keyword của ngày.
 * Cache 1 ngày.
 *
 * @returns {Promise<Object|null>}
 */
export async function getUnsplashVietnamPhoto() {
  const accessKey = process.env.REACT_APP_UNSPLASH_ACCESS_KEY;
  if (!accessKey) return null; // Key chưa cấu hình → skip, không warn

  const today = getTodayKey();
  const cached = readDailyCache(CACHE_KEY, CACHE_DATE, today);
  if (cached) return cached;

  // Chọn keyword theo ngày (không phải random — deterministic)
  const keywordIndex = getRotationIndex(VIETNAM_KEYWORDS.length);
  const keyword = VIETNAM_KEYWORDS[keywordIndex];

  try {
    const params = new URLSearchParams({
      query:          keyword,
      orientation:    'landscape',
      content_filter: 'high',   // Lọc nội dung phù hợp
      order_by:       'relevant',
    });

    const response = await fetchWithTimeout(
      `${BASE_URL}/photos/random?${params}`,
      { headers: { 'Authorization': `Client-ID ${accessKey}` } },
      5000
    );

    if (response.status === 403) {
      console.warn('[TutorHub] Unsplash: Đã hết quota (50 req/h). Đang dùng fallback.');
      return null;
    }
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const photo = await response.json();

    // Trigger Unsplash download tracking (bắt buộc theo guidelines)
    trackUnsplashDownload(photo.links?.download_location, accessKey);

    const photoData = {
      id:               `unsplash-${photo.id}`,
      name_vi:          photo.description || photo.alt_description || `${keyword} — Việt Nam`,
      name_en:          photo.description || photo.alt_description || `${keyword} — Vietnam`,
      location_vi:      photo.location?.name || 'Việt Nam',
      location_en:      photo.location?.name || 'Vietnam',
      description_vi:   photo.description || '',
      description_en:   photo.description || '',
      image_url:        `${photo.urls.raw}&w=1920&q=85&fit=crop&auto=format`,
      image_thumb:      `${photo.urls.raw}&w=800&q=70&fit=crop&auto=format`,
      image_credit:     photo.user?.name || 'Unknown',
      image_credit_url: `${photo.user?.links?.html}?utm_source=tutorhub&utm_medium=referral`,
      image_source:     'Unsplash',
      dominant_color:   '#1a5276',
      text_color:       'light',
      tags:             ['Unsplash', 'Vietnam'],
      active:           true,
      region:           'auto',
    };

    writeDailyCache(CACHE_KEY, CACHE_DATE, photoData, today);
    return photoData;

  } catch (err) {
    if (process.env.NODE_ENV === 'development') {
      console.warn('[TutorHub] Unsplash fetch failed:', err.message);
    }
    return null;
  }
}

/** Gọi download endpoint để track theo Unsplash API guidelines */
function trackUnsplashDownload(downloadUrl, accessKey) {
  if (!downloadUrl || !accessKey) return;
  fetchWithTimeout(downloadUrl, {
    headers: { 'Authorization': `Client-ID ${accessKey}` },
  }, 3000).catch(() => {}); // Fire-and-forget
}
```

---

### File 8/14: `src/services/imageProviderService.js`

```javascript
/**
 * imageProviderService.js — Orchestrator
 * =======================================
 * Tổng hợp tất cả nguồn ảnh theo priority queue.
 * Là entry point DUY NHẤT mà component gọi.
 *
 * Priority order:
 *   1. Orchestrator-level daily cache  (instant load)
 *   2. Self-curated JSON (offline-capable, deterministic)
 *   3. Bing HPImageArchive (free, no key, auto-updates daily)
 *   4. Unsplash API (needs key, diverse images)
 *   5. Absolute fallback (never fails)
 *
 * Race-condition protection:
 * - In-flight promise lock: nếu 2+ component gọi cùng lúc,
 *   chỉ 1 lần fetch thật, phần còn lại "đu theo" cùng promise.
 */
import localData from '../data/vietnam_landmarks.json'; // Webpack bundle trực tiếp
import { getBingDailyImage }        from './bingImageService';
import { getUnsplashVietnamPhoto }  from './unsplashVietnamService';
import { preloadNextDailyImage }    from './imagePreloadService';
import {
  getTodayKey, getRotationIndex,
  readDailyCache, writeDailyCache,
} from '../utils/dailyRotation';

// Import JSON trực tiếp nếu dùng Webpack/Vite
// Nếu dùng Next.js, thay bằng dynamic import hoặc API route
const CACHE_KEY  = 'tutorhub_resolved_image';
const CACHE_DATE = 'tutorhub_resolved_date';

/** In-flight promise lock — ngăn fetch trùng lặp khi nhiều component mount cùng lúc */
let _inFlight = null;

/**
 * Lấy landmark của ngày theo priority queue.
 *
 * @param {Object} options
 * @param {'curated'|'bing'|'unsplash'|'auto'} [options.preferSource='curated']
 *   - 'curated': Ưu tiên JSON local (khuyến nghị cho production)
 *   - 'bing':    Bỏ qua JSON, thử Bing trước
 *   - 'auto':    Curated trước, fallback Bing → Unsplash
 * @returns {Promise<Object>} - Luôn trả về object (không bao giờ null)
 */
export async function getDailyImage(options = {}) {
  const { preferSource = 'curated' } = options;
  const today = getTodayKey();

  // Priority 0: Cache ở tầng orchestrator
  const cached = readDailyCache(CACHE_KEY, CACHE_DATE, today);
  if (cached) return cached;

  // In-flight lock: nếu đã đang fetch → đợi kết quả chung
  if (_inFlight) return _inFlight;

  _inFlight = _resolve(preferSource, today).finally(() => {
    _inFlight = null;
  });

  return _inFlight;
}

async function _resolve(preferSource, today) {
  let result = null;
  const active = localData.landmarks.filter(l => l.active);

  // Priority 1: Curated local JSON (không cần network)
  if (preferSource !== 'bing' && preferSource !== 'unsplash') {
    result = _getFromCurated(active);
  }

  // Priority 2: Bing (nếu curated không được dùng hoặc thất bại)
  if (!result || preferSource === 'bing') {
    const bing = await getBingDailyImage('vi-VN');
    if (bing) result = bing;
  }

  // Priority 3: Unsplash (nếu cả 2 trên thất bại + có API key)
  if (!result) {
    const unsplash = await getUnsplashVietnamPhoto();
    if (unsplash) result = unsplash;
  }

  // Priority 4: Fallback tuyệt đối — không bao giờ fail
  if (!result) result = _getFallback(active);

  // Lưu cache ở tầng orchestrator
  writeDailyCache(CACHE_KEY, CACHE_DATE, result, today);

  // Preload ảnh ngày mai trong background (không block render)
  if (active.length > 0) {
    setTimeout(() => preloadNextDailyImage(active), 3000);
  }

  return result;
}

function _getFromCurated(active) {
  if (!active.length) return null;
  const index = getRotationIndex(active.length);
  return { ...active[index], source: 'curated' };
}

function _getFallback(active) {
  // Lấy mục đầu tiên trong JSON hoặc object tối giản
  const first = active[0];
  if (first) return { ...first, source: 'fallback' };
  return {
    id:           'fallback',
    name_vi:      'Việt Nam',
    name_en:      'Vietnam',
    location_vi:  'Việt Nam',
    location_en:  'Vietnam',
    description_vi: 'Đất nước hình chữ S tươi đẹp.',
    description_en: 'A beautiful S-shaped country.',
    image_url:    '/images/default-vietnam.jpg',
    image_thumb:  '/images/default-vietnam.jpg',
    image_credit: 'TutorHub',
    image_credit_url: '/',
    image_source: 'fallback',
    dominant_color: '#1a5276',
    text_color:   'light',
    tags:         [],
    active:       true,
    source:       'fallback',
  };
}

/**
 * Force refresh: xoá cache và fetch lại (dùng cho nút "Ảnh tiếp")
 */
export async function refreshDailyImage() {
  const { clearBannerCache } = await import('../utils/dailyRotation');
  clearBannerCache();
  return getDailyImage();
}
```

---

## PHASE 3 — COMPONENTS

### File 9/14: `src/components/SearchBannerImage/ErrorBoundary.jsx`

```jsx
/**
 * ErrorBoundary.jsx
 * ==================
 * Bắt mọi lỗi runtime trong SearchBannerImage và con của nó.
 * Nếu lỗi xảy ra → hiện children bình thường (search bar), không crash app.
 *
 * Dùng class component vì React Error Boundaries phải là class.
 */
import React from 'react';

class SearchBannerErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    // Log lỗi (có thể gửi lên Sentry/Datadog)
    if (process.env.NODE_ENV === 'development') {
      console.error('[SearchBannerImage] Error caught:', error, info);
    }
  }

  render() {
    if (this.state.hasError) {
      // Fallback: hiện children (search bar) không có ảnh nền
      return (
        <div className="search-banner search-banner--error">
          {this.props.fallbackStyle && (
            <style>{`.search-banner--error { background: ${this.props.fallbackColor || '#1a3a5c'}; }`}</style>
          )}
          {this.props.children}
        </div>
      );
    }
    return this.props.children;
  }
}

export default SearchBannerErrorBoundary;
```

---

### File 10/14: `src/components/SearchBannerImage/LoadingSkeleton.jsx`

```jsx
/**
 * LoadingSkeleton.jsx
 * ====================
 * Skeleton UI hiển thị khi đang tải ảnh, tránh layout shift (CLS).
 * Dùng shimmer animation giống Facebook/LinkedIn skeleton.
 */
import React from 'react';

const LoadingSkeleton = ({ children }) => {
  return (
    <div
      className="search-banner search-banner--skeleton"
      role="progressbar"
      aria-label="Đang tải ảnh danh lam thắng cảnh..."
      aria-busy="true"
    >
      {/* Shimmer overlay */}
      <div className="search-banner__shimmer" aria-hidden="true" />

      {/* Search bar vẫn hiện bình thường, không phải đợi ảnh */}
      <div className="search-banner__content">
        {children}
      </div>

      {/* Skeleton info bar */}
      <div className="search-banner__info-bar search-banner__info-bar--skeleton" aria-hidden="true">
        <div className="skeleton-line skeleton-line--location" />
        <div className="skeleton-line skeleton-line--action" />
      </div>
    </div>
  );
};

export default LoadingSkeleton;
```

---

### File 11/14: `src/components/SearchBannerImage/useImageBanner.js`

```javascript
/**
 * useImageBanner.js — Custom Hook
 * =================================
 * Tách toàn bộ logic khỏi UI component.
 * Component chỉ render, hook xử lý state và side effects.
 *
 * Returns: { landmark, state flags, text helpers, event handlers }
 */
import { useReducer, useEffect, useCallback, useMemo } from 'react';
import { getDailyImage } from '../../services/imageProviderService';
import { safeStorage }   from '../../utils/dailyRotation';

// ─── State ───────────────────────────────────────────────────────────────────
const INITIAL_STATE = {
  landmark:       null,   // Object địa điểm hiện tại
  isLoading:      true,   // Đang fetch từ service
  isImageLoaded:  false,  // Ảnh đã load xong trong DOM
  showInfo:       false,  // Tooltip thông tin chi tiết
  lang:           'vi',   // 'vi' | 'en'
  liked:          null,   // true | false | null
  hasError:       false,
};

const A = {
  SET_LANDMARK:  'SET_LANDMARK',
  IMAGE_LOADED:  'IMAGE_LOADED',
  TOGGLE_INFO:   'TOGGLE_INFO',
  TOGGLE_LANG:   'TOGGLE_LANG',
  SET_LIKED:     'SET_LIKED',
  SET_ERROR:     'SET_ERROR',
};

function reducer(state, action) {
  switch (action.type) {
    case A.SET_LANDMARK:
      return { ...state, landmark: action.payload, isLoading: false, hasError: false };
    case A.IMAGE_LOADED:
      return { ...state, isImageLoaded: true };
    case A.TOGGLE_INFO:
      return { ...state, showInfo: !state.showInfo };
    case A.TOGGLE_LANG:
      return { ...state, lang: state.lang === 'vi' ? 'en' : 'vi' };
    case A.SET_LIKED:
      return { ...state, liked: action.payload };
    case A.SET_ERROR:
      return { ...state, hasError: true, isLoading: false };
    default:
      return state;
  }
}

// ─── Hook ────────────────────────────────────────────────────────────────────
/**
 * @param {Object} [options]
 * @param {'curated'|'bing'|'auto'} [options.preferSource='curated']
 */
export function useImageBanner(options = {}) {
  const { preferSource = 'curated' } = options;
  const [state, dispatch] = useReducer(reducer, INITIAL_STATE);

  // Load initial image + restore liked state from localStorage
  useEffect(() => {
    let cancelled = false;

    // Restore liked preference từ lần trước
    const savedLiked = safeStorage.get('tutorhub_banner_liked');
    if (savedLiked !== null) {
      try { dispatch({ type: A.SET_LIKED, payload: JSON.parse(savedLiked) }); }
      catch { /* ignore */ }
    }

    getDailyImage({ preferSource })
      .then(landmark => {
        if (!cancelled) dispatch({ type: A.SET_LANDMARK, payload: landmark });
      })
      .catch(() => {
        if (!cancelled) dispatch({ type: A.SET_ERROR });
      });

    return () => { cancelled = true; };
  }, [preferSource]);

  // Đóng tooltip khi nhấn ESC
  useEffect(() => {
    if (!state.showInfo) return;
    const onKeyDown = (e) => {
      if (e.key === 'Escape') dispatch({ type: A.TOGGLE_INFO });
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [state.showInfo]);

  // ─── Handlers ──────────────────────────────────────────────────────────────
  const handleImageLoad = useCallback(() => {
    dispatch({ type: A.IMAGE_LOADED });
  }, []);

  const handleToggleInfo = useCallback(() => {
    dispatch({ type: A.TOGGLE_INFO });
  }, []);

  const handleToggleLang = useCallback(() => {
    dispatch({ type: A.TOGGLE_LANG });
  }, []);

  const handleLike = useCallback((liked) => {
    dispatch({ type: A.SET_LIKED, payload: liked });
    safeStorage.set('tutorhub_banner_liked', JSON.stringify(liked));
  }, []);

  // ─── Derived text (memoized, chỉ tính khi landmark hoặc lang thay đổi) ──
  const { currentName, currentLocation, currentDescription } = useMemo(() => {
    if (!state.landmark) return { currentName: '', currentLocation: '', currentDescription: '' };
    const { lang, landmark: l } = state;
    return {
      currentName:        lang === 'vi' ? l.name_vi        : l.name_en,
      currentLocation:    lang === 'vi' ? l.location_vi    : l.location_en,
      currentDescription: lang === 'vi' ? l.description_vi : l.description_en,
    };
  }, [state.landmark, state.lang]);

  return {
    // State
    landmark:       state.landmark,
    isLoading:      state.isLoading,
    isImageLoaded:  state.isImageLoaded,
    showInfo:       state.showInfo,
    lang:           state.lang,
    liked:          state.liked,
    hasError:       state.hasError,
    // Derived
    currentName,
    currentLocation,
    currentDescription,
    // Handlers
    handleImageLoad,
    handleToggleInfo,
    handleToggleLang,
    handleLike,
  };
}
```

---

### File 12/14: `src/components/SearchBannerImage/index.jsx`

```jsx
/**
 * SearchBannerImage — Component Chính
 * =====================================
 * Bọc search bar hiện tại trong một banner có ảnh danh lam thắng cảnh
 * Việt Nam, xoay mỗi ngày 1 ảnh (giống Windows Spotlight / Bing Search).
 *
 * CÁCH DÙNG:
 *   <SearchBannerImage>
 *     <SearchBar placeholder="Tìm kiếm..." />
 *   </SearchBannerImage>
 *
 * PROPS:
 *   children      — Search bar hoặc bất kỳ UI nào cần đặt lên ảnh nền
 *   className     — CSS class bổ sung
 *   preferSource  — 'curated' | 'bing' | 'auto' (default: 'curated')
 *   showCredit    — Hiện credit ảnh (default: true, bắt buộc nếu dùng Unsplash)
 *   showLikeBtn   — Hiện nút 👍 / → (default: false)
 */
import React from 'react';
import { useImageBanner }           from './useImageBanner';
import LoadingSkeleton               from './LoadingSkeleton';
import SearchBannerErrorBoundary     from './ErrorBoundary';
import './SearchBannerImage.css';

// ─── Inner Component (không export trực tiếp — được bọc bởi ErrorBoundary) ──
function SearchBannerInner({
  children,
  className = '',
  preferSource = 'curated',
  showCredit = true,
  showLikeBtn = false,
}) {
  const {
    landmark, isLoading, isImageLoaded,
    showInfo, lang, liked,
    currentName, currentLocation, currentDescription,
    handleImageLoad, handleToggleInfo, handleToggleLang, handleLike,
  } = useImageBanner({ preferSource });

  // Loading state — search bar vẫn dùng được
  if (isLoading) {
    return <LoadingSkeleton>{children}</LoadingSkeleton>;
  }

  // Error state — hiện children không có ảnh nền
  if (!landmark) {
    return (
      <div className={`search-banner search-banner--no-image ${className}`}>
        <div className="search-banner__content">{children}</div>
      </div>
    );
  }

  const langLabel = lang === 'vi' ? 'EN' : 'VI';
  const regionLabel = {
    north:   lang === 'vi' ? 'Miền Bắc' : 'Northern',
    central: lang === 'vi' ? 'Miền Trung' : 'Central',
    south:   lang === 'vi' ? 'Miền Nam' : 'Southern',
  }[landmark.region] || '';

  return (
    <div
      className={`search-banner ${className}`}
      role="region"
      aria-label={currentName || 'Vietnam Landmark Banner'}
      style={{ '--banner-dominant-color': landmark.dominant_color }}
    >
      {/* ── Layer 1: Ảnh nền ── */}
      <div
        className={`search-banner__bg ${isImageLoaded ? 'search-banner__bg--visible' : ''}`}
        style={{ backgroundImage: `url(${landmark.image_url})` }}
        aria-hidden="true"
      />

      {/* Preload trigger — ẩn hoàn toàn, chỉ để kích hoạt browser load */}
      <img
        src={landmark.image_url}
        alt=""
        aria-hidden="true"
        style={{ display: 'none' }}
        onLoad={handleImageLoad}
        onError={handleImageLoad} // Không block UI nếu ảnh lỗi
      />

      {/* ── Layer 2: Gradient overlay (readability) ── */}
      <div className="search-banner__overlay" aria-hidden="true" />

      {/* ── Layer 3: Nội dung chính (search bar) ── */}
      <div className="search-banner__content">
        {children}
      </div>

      {/* ── Layer 4: Info bar phía dưới ── */}
      <div className="search-banner__info-bar">
        {/* Trái: Tên địa điểm (click để xem mô tả) */}
        <button
          className="search-banner__location-btn"
          onClick={handleToggleInfo}
          aria-expanded={showInfo}
          aria-controls="banner-info-tooltip"
          type="button"
        >
          <span className="search-banner__pin-icon" aria-hidden="true">📍</span>
          <span className="search-banner__location-name">{currentName}</span>
          {currentLocation && (
            <span className="search-banner__location-sub">, {currentLocation}</span>
          )}
          {regionLabel && (
            <span className="search-banner__region-badge">{regionLabel}</span>
          )}
        </button>

        {/* Phải: Actions */}
        <div className="search-banner__actions">
          {/* Nút Like (tuỳ chọn) */}
          {showLikeBtn && (
            <div className="search-banner__like-group" role="group" aria-label="Đánh giá ảnh này">
              <button
                className={`search-banner__like-btn ${liked === true ? 'search-banner__like-btn--active' : ''}`}
                onClick={() => handleLike(true)}
                aria-pressed={liked === true}
                title={lang === 'vi' ? 'Đẹp quá!' : 'Love it!'}
                type="button"
              >👍</button>
              <button
                className={`search-banner__like-btn ${liked === false ? 'search-banner__like-btn--active' : ''}`}
                onClick={() => handleLike(false)}
                aria-pressed={liked === false}
                title={lang === 'vi' ? 'Không thích' : 'Not for me'}
                type="button"
              >👎</button>
            </div>
          )}

          {/* Nút đổi ngôn ngữ */}
          <button
            className="search-banner__lang-btn"
            onClick={handleToggleLang}
            aria-label={`Switch to ${langLabel}`}
            title={`Chuyển sang ${langLabel}`}
            type="button"
          >
            {langLabel}
          </button>

          {/* Link "Tìm hiểu thêm" */}
          {landmark.bing_search_url && (
            <a
              href={landmark.bing_search_url}
              target="_blank"
              rel="noopener noreferrer"
              className="search-banner__learn-btn"
              aria-label={`Tìm hiểu thêm về ${currentName}`}
            >
              {lang === 'vi' ? 'Tìm hiểu thêm' : 'Learn more'}
              <span aria-hidden="true"> →</span>
            </a>
          )}

          {/* Credit ảnh */}
          {showCredit && landmark.image_credit && (
            <span className="search-banner__credit" aria-label={`Photo by ${landmark.image_credit}`}>
              {lang === 'vi' ? 'Ảnh: ' : 'Photo: '}
              <a
                href={landmark.image_credit_url}
                target="_blank"
                rel="noopener noreferrer"
                tabIndex={-1}
              >
                {landmark.image_credit}
              </a>
              {landmark.image_source && ` on ${landmark.image_source}`}
            </span>
          )}
        </div>
      </div>

      {/* ── Tooltip thông tin chi tiết ── */}
      {showInfo && (
        <div
          id="banner-info-tooltip"
          className="search-banner__tooltip"
          role="dialog"
          aria-label={`Thông tin về ${currentName}`}
          aria-modal="false"
        >
          <button
            className="search-banner__tooltip-close"
            onClick={handleToggleInfo}
            aria-label="Đóng"
            type="button"
          >
            ✕
          </button>
          {currentDescription && (
            <p className="search-banner__tooltip-desc">{currentDescription}</p>
          )}
          {landmark.tags?.length > 0 && (
            <div className="search-banner__tags" aria-label="Tags">
              {landmark.tags.map(tag => (
                <span key={tag} className="search-banner__tag">#{tag}</span>
              ))}
            </div>
          )}
          {landmark.wikipedia_url && (
            <a
              href={landmark.wikipedia_url}
              target="_blank"
              rel="noopener noreferrer"
              className="search-banner__wiki-link"
            >
              {lang === 'vi' ? '📖 Xem trên Wikipedia' : '📖 View on Wikipedia'}
            </a>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Export: Component được bọc bởi Error Boundary ───────────────────────────
export default function SearchBannerImage(props) {
  return (
    <SearchBannerErrorBoundary fallbackStyle fallbackColor="#1a3a5c">
      <SearchBannerInner {...props} />
    </SearchBannerErrorBoundary>
  );
}
```

---

### File 13/14: `src/components/SearchBannerImage/SearchBannerImage.css`

```css
/* ===========================================================================
   SearchBannerImage.css
   Ảnh danh lam thắng cảnh Việt Nam cho thanh tìm kiếm TutorHub.
   Kiến trúc 4 lớp: bg → overlay → content → info-bar
   =========================================================================== */

/* ─── Custom Properties (tokens) ─────────────────────────────────────────── */
.search-banner {
  --banner-radius:        16px;
  --banner-padding-h:     16px;
  --banner-min-height:    96px;
  --banner-transition:    0.7s ease-in-out;
  --overlay-top:          rgba(0, 0, 0, 0.08);
  --overlay-bottom:       rgba(0, 0, 0, 0.65);
  --text-shadow:          0 1px 4px rgba(0, 0, 0, 0.6);
  --glass-bg:             rgba(255, 255, 255, 0.92);
  --glass-border:         rgba(255, 255, 255, 0.5);
  --info-color:           rgba(255, 255, 255, 0.95);
  --credit-color:         rgba(255, 255, 255, 0.60);
  --tag-bg:               rgba(255, 255, 255, 0.18);
  --dominant-color:       var(--banner-dominant-color, #1a5276);
}

/* ─── Base Container ──────────────────────────────────────────────────────── */
.search-banner {
  position:      relative;
  width:         100%;
  border-radius: var(--banner-radius);
  overflow:      hidden;
  min-height:    var(--banner-min-height);
  padding:       12px var(--banner-padding-h) 36px;
  box-sizing:    border-box;
  contain:       layout style; /* Performance: isolate repaints */
}

/* Trạng thái không có ảnh — dùng màu dominant */
.search-banner--no-image {
  background: var(--dominant-color);
}

/* ─── Layer 1: Ảnh nền ────────────────────────────────────────────────────── */
.search-banner__bg {
  position:            absolute;
  inset:               0;
  background-size:     cover;
  background-position: center 30%; /* Focus vào 1/3 trên ảnh — thường đẹp nhất */
  background-repeat:   no-repeat;
  background-color:    var(--dominant-color); /* Placeholder màu trong khi ảnh load */
  opacity:             0;
  transition:          opacity var(--banner-transition);
  will-change:         opacity;
  transform:           translateZ(0); /* GPU compositing layer */
}

.search-banner__bg--visible {
  opacity: 1;
}

/* ─── Skeleton State ──────────────────────────────────────────────────────── */
.search-banner--skeleton {
  background: #e2e8f0;
}

.search-banner__shimmer {
  position: absolute;
  inset:    0;
  background: linear-gradient(
    90deg,
    transparent    0%,
    rgba(255,255,255,0.25) 50%,
    transparent    100%
  );
  background-size: 200% 100%;
  animation:       shimmer 1.5s ease-in-out infinite;
}

@keyframes shimmer {
  0%   { background-position: 200% center; }
  100% { background-position: -200% center; }
}

.search-banner__info-bar--skeleton {
  padding: 6px var(--banner-padding-h) 8px;
}

.skeleton-line {
  height:        10px;
  border-radius: 6px;
  background:    rgba(0,0,0,0.12);
}

.skeleton-line--location {
  width:  45%;
  margin-bottom: 0;
}

.skeleton-line--action {
  width: 25%;
}

/* ─── Layer 2: Gradient Overlay ──────────────────────────────────────────── */
/* Sáng trên (ảnh thở được), tối dần xuống (info bar đọc được) */
.search-banner__overlay {
  position:       absolute;
  inset:          0;
  background:     linear-gradient(
    to bottom,
    var(--overlay-top)    0%,
    rgba(0,0,0,0.12)     40%,
    rgba(0,0,0,0.45)     70%,
    var(--overlay-bottom) 100%
  );
  pointer-events: none;
  z-index:        1;
}

/* ─── Layer 3: Content (Search Bar) ──────────────────────────────────────── */
.search-banner__content {
  position: relative;
  z-index:  2;
}

/* Input/search bar bên trong — glassmorphism */
.search-banner__content input,
.search-banner__content .search-input,
.search-banner__content [class*="search-bar"],
.search-banner__content [class*="search-input"] {
  background:        var(--glass-bg) !important;
  backdrop-filter:   blur(12px) !important;
  -webkit-backdrop-filter: blur(12px) !important;
  border:            1px solid var(--glass-border) !important;
  box-shadow:        0 2px 16px rgba(0,0,0,0.18),
                     0 0 0 1px rgba(255,255,255,0.3) inset !important;
  transition:        box-shadow 0.2s, background 0.2s !important;
}

.search-banner__content input:focus,
.search-banner__content [class*="search-input"]:focus {
  background:  rgba(255,255,255,0.97) !important;
  box-shadow:  0 4px 24px rgba(0,0,0,0.22),
               0 0 0 2px rgba(255,255,255,0.5) inset !important;
}

/* Button trong search bar */
.search-banner__content button:not(.search-banner__lang-btn):not(.search-banner__learn-btn) {
  background: rgba(255,255,255,0.85);
  backdrop-filter: blur(8px);
}

/* ─── Layer 4: Info Bar ───────────────────────────────────────────────────── */
.search-banner__info-bar {
  position:        absolute;
  bottom:          0;
  left:            0;
  right:           0;
  z-index:         3;
  display:         flex;
  align-items:     center;
  justify-content: space-between;
  padding:         5px var(--banner-padding-h) 7px;
  gap:             8px;
  flex-wrap:       nowrap;
}

/* ── Nút địa điểm ── */
.search-banner__location-btn {
  display:     flex;
  align-items: center;
  gap:         4px;
  background:  none;
  border:      none;
  padding:     2px 4px;
  cursor:      pointer;
  color:       var(--info-color);
  font-size:   12px;
  font-weight: 600;
  text-shadow: var(--text-shadow);
  border-radius: 4px;
  transition:  background 0.15s, color 0.15s;
  white-space: nowrap;
  max-width:   55%;
  overflow:    hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

.search-banner__location-btn:hover,
.search-banner__location-btn:focus-visible {
  background: rgba(255,255,255,0.12);
  color:      #fff;
  outline:    none;
}

.search-banner__pin-icon { font-size: 11px; flex-shrink: 0; }

.search-banner__location-name {
  font-weight:    700;
  overflow:       hidden;
  text-overflow:  ellipsis;
  white-space:    nowrap;
}

.search-banner__location-sub {
  opacity:     0.80;
  font-weight: 400;
  flex-shrink: 2;
  overflow:    hidden;
  text-overflow: ellipsis;
  white-space:   nowrap;
}

.search-banner__region-badge {
  background:    rgba(255,255,255,0.22);
  border-radius: 20px;
  padding:       1px 7px;
  font-size:     10px;
  font-weight:   600;
  flex-shrink:   0;
  letter-spacing: 0.3px;
}

/* ── Actions (phải) ── */
.search-banner__actions {
  display:     flex;
  align-items: center;
  gap:         8px;
  flex-shrink: 0;
}

/* ── Nút Like ── */
.search-banner__like-group {
  display: flex;
  gap:     4px;
}

.search-banner__like-btn {
  background:    rgba(255,255,255,0.15);
  border:        1px solid rgba(255,255,255,0.25);
  border-radius: 50%;
  width:         26px;
  height:        26px;
  cursor:        pointer;
  font-size:     13px;
  display:       flex;
  align-items:   center;
  justify-content: center;
  transition:    background 0.2s, transform 0.15s;
  backdrop-filter: blur(4px);
}

.search-banner__like-btn:hover {
  background: rgba(255,255,255,0.28);
  transform:  scale(1.12);
}

.search-banner__like-btn--active {
  background: rgba(255,255,255,0.35);
  border-color: rgba(255,255,255,0.5);
}

/* ── Nút đổi ngôn ngữ ── */
.search-banner__lang-btn {
  background:    rgba(255,255,255,0.12);
  border:        1px solid rgba(255,255,255,0.22);
  border-radius: 4px;
  padding:       2px 7px;
  font-size:     10px;
  font-weight:   700;
  color:         rgba(255,255,255,0.85);
  cursor:        pointer;
  letter-spacing: 0.5px;
  text-shadow:   var(--text-shadow);
  transition:    background 0.2s;
  backdrop-filter: blur(4px);
}

.search-banner__lang-btn:hover,
.search-banner__lang-btn:focus-visible {
  background: rgba(255,255,255,0.22);
  color:      #fff;
  outline:    none;
}

/* ── Nút "Tìm hiểu thêm" ── */
.search-banner__learn-btn {
  color:         rgba(255,255,255,0.92);
  font-size:     11px;
  font-weight:   600;
  text-decoration: none;
  text-shadow:   var(--text-shadow);
  background:    rgba(255,255,255,0.14);
  border:        1px solid rgba(255,255,255,0.25);
  border-radius: 20px;
  padding:       3px 10px;
  backdrop-filter: blur(4px);
  transition:    background 0.2s, color 0.2s;
  white-space:   nowrap;
}

.search-banner__learn-btn:hover,
.search-banner__learn-btn:focus-visible {
  background: rgba(255,255,255,0.26);
  color:      #fff;
  outline:    none;
}

/* ── Credit ảnh ── */
.search-banner__credit {
  color:       var(--credit-color);
  font-size:   10px;
  text-shadow: var(--text-shadow);
  white-space: nowrap;
}

.search-banner__credit a {
  color:           rgba(255,255,255,0.72);
  text-decoration: none;
}

.search-banner__credit a:hover {
  color:           #fff;
  text-decoration: underline;
}

/* ─── Tooltip ────────────────────────────────────────────────────────────── */
.search-banner__tooltip {
  position:         absolute;
  bottom:           44px;
  left:             var(--banner-padding-h);
  z-index:          10;
  background:       rgba(10, 15, 30, 0.88);
  backdrop-filter:  blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  border-radius:    14px;
  padding:          14px 16px;
  max-width:        340px;
  width:            calc(100% - 32px);
  color:            #fff;
  font-size:        13px;
  line-height:      1.55;
  box-shadow:       0 8px 32px rgba(0,0,0,0.4),
                    0 0 0 1px rgba(255,255,255,0.08) inset;
  animation:        tooltip-in 0.2s ease-out;
}

@keyframes tooltip-in {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0);   }
}

.search-banner__tooltip-close {
  position:      absolute;
  top:           8px;
  right:         10px;
  background:    none;
  border:        none;
  color:         rgba(255,255,255,0.5);
  font-size:     14px;
  cursor:        pointer;
  padding:       2px 6px;
  border-radius: 4px;
  line-height:   1;
  transition:    color 0.15s;
}

.search-banner__tooltip-close:hover {
  color: #fff;
}

.search-banner__tooltip-desc {
  margin:   0 0 10px;
  opacity:  0.88;
  font-size: 12px;
}

.search-banner__tags {
  display:   flex;
  flex-wrap: wrap;
  gap:       5px;
  margin-bottom: 10px;
}

.search-banner__tag {
  background:    rgba(255,255,255,0.14);
  border-radius: 20px;
  padding:       2px 9px;
  font-size:     10px;
  color:         rgba(255,255,255,0.80);
  letter-spacing: 0.2px;
}

.search-banner__wiki-link {
  color:           #7eb6ff;
  font-size:       12px;
  text-decoration: none;
  display:         inline-flex;
  align-items:     center;
  gap:             4px;
  transition:      color 0.2s;
}

.search-banner__wiki-link:hover {
  color: #a5cfff;
  text-decoration: underline;
}

/* ─── Responsive ─────────────────────────────────────────────────────────── */
@media (max-width: 640px) {
  .search-banner {
    border-radius: 12px;
    padding:       10px 12px 34px;
  }

  .search-banner__info-bar {
    padding: 4px 12px 6px;
    gap:     6px;
  }

  .search-banner__location-btn {
    font-size: 11px;
    max-width: 50%;
  }

  .search-banner__region-badge,
  .search-banner__credit {
    display: none; /* Ẩn trên mobile nhỏ */
  }

  .search-banner__learn-btn {
    font-size: 10px;
    padding:   2px 8px;
  }

  .search-banner__tooltip {
    left:      12px;
    right:     12px;
    max-width: none;
    width:     calc(100% - 24px);
  }
}

/* ─── Dark Mode ───────────────────────────────────────────────────────────── */
@media (prefers-color-scheme: dark) {
  .search-banner--no-image {
    background: color-mix(in srgb, var(--dominant-color) 80%, #000 20%);
  }

  .search-banner__content input,
  .search-banner__content .search-input {
    background: rgba(20, 28, 48, 0.92) !important;
    color:      #e2e8f0 !important;
    border-color: rgba(255,255,255,0.25) !important;
  }

  .search-banner--skeleton {
    background: #1e2533;
  }
}

/* ─── Reduced Motion (accessibility) ────────────────────────────────────── */
@media (prefers-reduced-motion: reduce) {
  .search-banner__bg {
    transition: none;
  }
  .search-banner__shimmer {
    animation: none;
    background: rgba(255,255,255,0.15);
  }
  .search-banner__tooltip {
    animation: none;
  }
  .search-banner__like-btn:hover {
    transform: none;
  }
}
```

---

## PHASE 4 — TÍCH HỢP VÀO APP

### File 14/14: Cập nhật `src/pages/HomePage.jsx`

```jsx
/**
 * HomePage.jsx — Ví dụ tích hợp
 * ================================
 * Chỉ cần bọc <SearchBar> hiện tại trong <SearchBannerImage>.
 * Không cần sửa gì bên trong SearchBar.
 */
import React from 'react';
import SearchBannerImage from '../components/SearchBannerImage';
import SearchBar         from '../components/SearchBar'; // Component tìm kiếm của bạn

const HomePage = () => {
  return (
    <div className="home-page">

      {/* ── TRƯỚC: ── */}
      {/* <SearchBar placeholder="Tìm kiếm Biểu tượng" /> */}

      {/* ── SAU: Chỉ bọc thêm 1 component ── */}
      <SearchBannerImage
        preferSource="curated"  // 'curated' | 'bing' | 'auto'
        showCredit={true}       // Bắt buộc true nếu dùng Unsplash
        showLikeBtn={false}     // Bật nếu muốn thu thập feedback
      >
        <SearchBar placeholder="Tìm kiếm Biểu tượng" />
      </SearchBannerImage>

      {/* Phần còn lại của trang giữ nguyên */}
      {/* ... Locket, lớp học, v.v. */}
    </div>
  );
};

export default HomePage;
```

---

## ✅ Checklist Kiểm Tra (Chạy sau Phase 1 + 2)

### Chức năng cốt lõi
```
□ Ảnh xuất hiện phía sau thanh tìm kiếm
□ Ảnh fade in mượt mà sau khi load xong
□ Mỗi ngày ảnh khác nhau (kiểm tra bằng cách đổi ngày máy tính)
□ Cùng ngày → cùng ảnh khi mở nhiều tab cùng lúc (deterministic)
□ Reload trang → ảnh cũ load ngay từ cache (không flicker)
□ Xoá localStorage → vẫn hoạt động, fetch lại ảnh
□ Offline → hiện ảnh từ cache hoặc dominant_color fallback
```

### UI / Accessibility
```
□ Thanh tìm kiếm vẫn nổi bật, đọc được trên nền ảnh (glassmorphism)
□ Tên địa điểm hiện đúng bên dưới: "📍 Vịnh Hạ Long, Quảng Ninh"
□ Nút "Tìm hiểu thêm →" mở tab mới (không rời trang hiện tại)
□ Click tên địa điểm → tooltip mở, ESC → đóng tooltip
□ Nút đổi ngôn ngữ VI/EN hoạt động
□ Credit ảnh hiển thị với hyperlink đúng
□ Keyboard navigation: Tab → Focus visible trên mọi button
□ Screen reader: aria-label, role đặt đúng
□ Mobile: layout không bị vỡ ở 375px
□ Dark mode: search bar vẫn đọc được
□ prefers-reduced-motion: không animation nếu user bật
```

### Performance
```
□ LCP (Largest Contentful Paint) < 2.5s — ảnh không phải là LCP element
□ CLS (Cumulative Layout Shift) = 0 — skeleton giữ chiều cao cố định
□ Không có console.error khi ảnh không load
□ Preload ngày mai chạy sau 3 giây, không tranh bandwidth
□ Bing API chỉ gọi 1 lần/ngày (kiểm tra Network tab)
```

### Debug Commands (DevTools Console)
```javascript
// Force xoá cache để test ảnh mới:
import('/src/utils/dailyRotation').then(m => m.clearBannerCache())

// Xem ảnh đang dùng:
JSON.parse(localStorage.getItem('tutorhub_resolved_image'))

// Xem ngày cache:
localStorage.getItem('tutorhub_resolved_date')

// Simulate ngày mai:
getRotationIndex(30, new Date(Date.now() + 86400000))
```

---

## 🚀 Roadmap Mở Rộng (Phase 5+)

### Phase 5 — Seasonal Rotation
```javascript
// Thêm vào vietnam_landmarks.json:
"seasons": ["summer"],  // "spring" | "summer" | "autumn" | "winter"

// Trong getFromCurated(): lọc thêm theo mùa
const season = getSeason(); // Tháng 3-5: spring, 6-8: summer, v.v.
const seasonal = active.filter(l => !l.seasons || l.seasons.includes(season));
```

### Phase 6 — Backend API
```
GET /api/v1/banner-image
  Response: { landmark, imageUrl, metadata }
  Cache:    Redis 24h
  Lợi ích: Không expose API keys ra frontend, cập nhật kho ảnh không cần redeploy
```

### Phase 7 — Analytics
```javascript
// Track địa điểm được xem nhiều nhất
analytics.track('banner_view', { landmarkId: landmark.id, region: landmark.region });
analytics.track('banner_learn_more', { landmarkId: landmark.id });
analytics.track('banner_liked', { landmarkId: landmark.id, liked });
```

### Phase 8 — User Contribution
```
- User upload ảnh địa điểm → moderation queue → vào kho community
- Badge "📸 Ảnh của cộng đồng TutorHub"
- Leaderboard nhiếp ảnh gia đóng góp nhiều nhất
```

---

## 📚 Tài Liệu Tham Khảo

| Nguồn | URL |
|---|---|
| Bing HPImageArchive API | `https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=vi-VN` |
| Unsplash API Docs | https://unsplash.com/documentation |
| Unsplash Access Key | https://unsplash.com/developers |
| Pexels API (backup) | https://www.pexels.com/api/ |
| Picsum Photos (demo) | https://picsum.photos |
| Web Accessibility (WCAG) | https://www.w3.org/WAI/WCAG21/quickref/ |
| CSS prefers-reduced-motion | https://developer.mozilla.org/en-US/docs/Web/CSS/@media/prefers-reduced-motion |

---

*Tài liệu v2.0 — TutorHub Vietnam Landmark Banner — Tháng 6/2026*
