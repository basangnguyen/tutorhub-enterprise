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
