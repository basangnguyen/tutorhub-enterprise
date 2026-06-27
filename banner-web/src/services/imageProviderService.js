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
