import { cartStockError } from './cartStock.js'

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

export function createAddOperationId() {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID()
  }
  const bytes = new Uint8Array(16)
  if (typeof globalThis.crypto?.getRandomValues === 'function') {
    globalThis.crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < 16; i += 1) bytes[i] = Math.floor(Math.random() * 256)
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function isValidOperationId(value) {
  return typeof value === 'string' && UUID_PATTERN.test(value.trim())
}

/**
 * Strictly parses an integer quantity without coercing decimals (e.g. "1.5", 1.5, "2.0") to integers.
 */
export function parseStrictIntegerQuantity(raw, max = 99) {
  const effectiveMax = Math.max(1, Math.min(99, max ?? 99))
  if (typeof raw === 'number') {
    if (!Number.isFinite(raw) || !Number.isInteger(raw)) {
      return { valid: false, value: null, message: 'Số lượng phải là số nguyên, không chấp nhận số thập phân.' }
    }
    if (raw < 1 || raw > effectiveMax) {
      return { valid: false, value: null, message: `Số lượng phải là số nguyên từ 1 đến ${effectiveMax}.` }
    }
    return { valid: true, value: raw, message: '' }
  }
  const text = String(raw ?? '').trim()
  if (!text) {
    return { valid: false, value: null, message: `Nhập số nguyên từ 1 đến ${effectiveMax}.` }
  }
  if (/^[+-]?\d+[.,]\d+$/.test(text)) {
    return { valid: false, value: null, message: 'Số lượng phải là số nguyên, không chấp nhận số thập phân.' }
  }
  if (!/^\d+$/.test(text)) {
    return { valid: false, value: null, message: `Nhập số nguyên từ 1 đến ${effectiveMax}.` }
  }
  const parsed = Number(text)
  if (!Number.isSafeInteger(parsed) || parsed < 1 || parsed > effectiveMax) {
    return { valid: false, value: null, message: `Nhập số nguyên từ 1 đến ${effectiveMax}.` }
  }
  return { valid: true, value: parsed, message: '' }
}

/**
 * Distinguishes creating a NEW add operation from retrying an EXISTING unconfirmed operation.
 * - New operation: validates integer bounds AND checks additive stock against current projected cart,
 *   then mints a new operationId.
 * - Retry operation: validates integer bounds AND existing operationId, but SKIPS additive client stock
 *   preflight because the refreshed cart may already include the committed effect of this very operationId.
 */
export function prepareAddAttempt({ items, product, quantity, isRetry = false, operationId = null }) {
  if (typeof quantity !== 'number' || !Number.isInteger(quantity)) {
    return {
      allowed: false,
      message: 'Số lượng phải là số nguyên từ 1 đến 99, không chấp nhận số thập phân.',
    }
  }
  if (quantity < 1 || quantity > 99) {
    return {
      allowed: false,
      message: 'Số lượng phải là số nguyên từ 1 đến 99.',
    }
  }
  if (isRetry) {
    if (!isValidOperationId(operationId)) {
      return {
        allowed: false,
        message: 'Mã thao tác thử lại không hợp lệ.',
      }
    }
    return {
      allowed: true,
      isRetry: true,
      operationId: operationId.trim(),
      message: '',
    }
  }

  const stockMessage = cartStockError(items, product, quantity, true)
  if (stockMessage) {
    return {
      allowed: false,
      message: stockMessage,
    }
  }
  return {
    allowed: true,
    isRetry: false,
    operationId: createAddOperationId(),
    message: '',
  }
}

export function isRetryableAddError(error) {
  if (!error?.response) return true
  const status = error.response.status
  const code = error.response.data?.errorCode || error.response.data?.code
  return status >= 500 || code === 'CART_VERSION_CONFLICT'
}

/**
 * Executes an add-to-cart operation with a stable operationId.
 * Refreshes and retries version conflicts and immediate network failures once using the SAME ID.
 * Timeouts and server errors return to the UI immediately for a manual retry instead of
 * keeping the mutation queue blocked through another GET + POST timeout cycle.
 */
export async function executeIdempotentAdd({
  productId,
  quantity,
  getVersion,
  addRequest,
  getCart,
  onCartLoaded,
  operationId,
}) {
  const opId = isValidOperationId(operationId) ? operationId.trim() : createAddOperationId()
  try {
    const response = await addRequest(productId, quantity, getVersion(), opId)
    return { data: response.data, operationId: opId, retried: false }
  } catch (firstError) {
    const timedOut = ['ECONNABORTED', 'ETIMEDOUT', 'ERR_CANCELED'].includes(firstError.code)
    const code = firstError.response?.data?.errorCode || firstError.response?.data?.code
    const canAutoRetry = code === 'CART_VERSION_CONFLICT' || (!firstError.response && !timedOut)
    if (!canAutoRetry) {
      firstError.operationId = opId
      throw firstError
    }
    let refreshedCart
    try {
      const latest = await getCart()
      refreshedCart = latest.data
      onCartLoaded?.(refreshedCart)
    } catch {
      firstError.operationId = opId
      throw firstError
    }
    try {
      const retryResponse = await addRequest(productId, quantity, refreshedCart.cartVersion, opId)
      return { data: retryResponse.data, operationId: opId, retried: true }
    } catch (retryError) {
      retryError.operationId = opId
      throw retryError
    }
  }
}
