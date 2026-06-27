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
