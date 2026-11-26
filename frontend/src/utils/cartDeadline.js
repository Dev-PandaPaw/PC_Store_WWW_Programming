export const CART_SYNC_DEADLINE_MS = 5000

// Includes queueing, CSRF, refresh and retry time, not just the final HTTP request.
export async function runWithCartDeadline(work, timeoutMs = CART_SYNC_DEADLINE_MS) {
  const controller = new AbortController()
  let timer
  const expired = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const error = Object.assign(new Error('Cart synchronization timed out'), { code: 'ETIMEDOUT' })
      reject(error)
      controller.abort(error)
    }, timeoutMs)
  })
  try {
    return await Promise.race([Promise.resolve().then(() => work(controller.signal)), expired])
  } finally {
    clearTimeout(timer)
  }
}
