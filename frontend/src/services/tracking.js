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
 * Gui su kien tracking ve Backend.
 * @param {Object} payload
 * @param {string} payload.eventType - Loai su kien (VIEW_PRODUCT, DWELL_TIME_REACHED, SEARCH_KEYWORD, FILTER_CATEGORY)
 * @param {string} [payload.productId] - ID san pham (neu co)
 * @param {string} [payload.categorySlug] - Danh muc san pham (neu co)
 * @param {string} [payload.metadata] - Chuoi JSON chua thong tin bo sung
 */
export async function sendTrackingEvent({ eventType, productId = null, categorySlug = null, metadata = null }) {
  if (!eventType) return

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

