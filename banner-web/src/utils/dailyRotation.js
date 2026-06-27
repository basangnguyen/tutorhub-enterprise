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
