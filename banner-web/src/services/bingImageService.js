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
