import http from './http'

/**
 * Service gui nhat ky hanh vi nguoi dung ve Backend phuc vu cho Spring AI Recommendation.
 * Co che: Asynchronous Non-blocking, khong gay anh huong den trai nghiem nguoi dung.
 */

const IS_DEV = Boolean(import.meta.env.DEV)

/**
 * In log kiem tra trong moi truong Dev (khong dung icon/emoji, dinh dang ro rang, de doc).
 */
function logTracker(eventType, details) {
  if (IS_DEV) {
    console.log(`[UserTracker] Event: ${eventType}`, details)
  }
}

/**
 * Lưu nhật ký hành vi vào Local Storage để Frontend có thể phản hồi gợi ý tức thì (Real-time Adaptive UI)
 * ngay cả khi Backend chưa bật profile vector hoặc đang chạy client-side.
 */
function recordLocalTracking(categorySlug, productId, eventType) {
  try {
    if (typeof localStorage === 'undefined') return
    if (!categorySlug && !productId) return
    const key = 'pc_recent_interests'
    const raw = localStorage.getItem(key)
    const list = raw ? JSON.parse(raw) : []
    const entry = {
      category: categorySlug ? String(categorySlug).toLowerCase().trim() : null,
      productId,
      eventType,
      time: Date.now(),
    }
    // Đẩy lên đầu danh sách, loại bỏ sự kiện trùng danh mục/sản phẩm vừa diễn ra
    const updated = [
      entry,
      ...list.filter(item => !(item.productId === productId && item.category === entry.category))
    ].slice(0, 20)
    localStorage.setItem(key, JSON.stringify(updated))
  } catch {}
}

/**
 * Gui su kien tracking ve Backend.
 * @param {Object} payload
 * @param {string} payload.eventType - Loai su kien (VIEW_PRODUCT, DWELL_TIME_REACHED, SEARCH_KEYWORD, FILTER_CATEGORY)
 * @param {string} [payload.productId] - ID san pham (neu co)
 * @param {string} [payload.categorySlug] - Danh muc san pham (neu co)
 * @param {string} [payload.metadata] - Chuoi JSON chua thong tin bo sung
 */
export async function sendTrackingEvent({ eventType, productId = null, categorySlug = null, metadata = null }) {
  if (!eventType) return

  // Lưu ngay vết hành vi vào local tracking để UI gợi ý phản hồi tức thì
  recordLocalTracking(categorySlug, productId, eventType)

  const payload = {
    eventType,
    productId,
    categorySlug,
    metadata: typeof metadata === 'object' && metadata !== null ? JSON.stringify(metadata) : metadata,
    clientTimestamp: new Date().toISOString(),
  }

  logTracker(eventType, payload)

  try {
    // Goi API POST /api/tracking/events
    await http.post('/tracking/events', payload, {
      timeout: 5000,
    })
  } catch (error) {
    // Bo qua loi ngam de khong bao gio lam gian doan trai nghiem mua hang cua khach
    if (IS_DEV) {
      console.warn(`[UserTracker] Server chua san sang hoac loi endpoint /api/tracking/events: ${error.message}`)
    }
  }
}

