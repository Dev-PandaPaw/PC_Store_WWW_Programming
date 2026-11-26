import http from './http'

const IS_DEV = Boolean(import.meta.env.DEV)

function logTracker(eventType, details) {
  if (IS_DEV) {
    console.log(`[UserTracker] Event: ${eventType}`, details)
  }
}

/**
 * Luu lich su quan tam gan nhat vao localStorage.
 * Phuc vu render phan goi y san pham tuc thi o client truoc khi dong bo voi server.
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
    const updated = [
      entry,
      ...list.filter(item => !(item.productId === productId && item.category === entry.category))
    ].slice(0, 20)
    localStorage.setItem(key, JSON.stringify(updated))
  } catch { }
}

/**
 * Gui su kien tracking nguoi dung ve backend.
 * Hoat dong non-blocking, khong lam gian doan luong mua hang neu gap su co mang.
 */
export async function sendTrackingEvent({ eventType, productId = null, categorySlug = null, metadata = null }) {
  if (!eventType) return

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
    await http.post('/tracking/events', payload, {
      timeout: 5000,
    })
  } catch (error) {
    if (IS_DEV) {
      console.warn(`[UserTracker] Server chua san sang hoac loi endpoint /api/tracking/events: ${error.message}`)
    }
  }
}

