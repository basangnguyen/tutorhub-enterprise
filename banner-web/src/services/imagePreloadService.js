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
