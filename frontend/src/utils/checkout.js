import { ADMIN_HOME, hasAdminRole } from './authorization.js'

export const SELECTED_CART_STORAGE_KEY = 'pcstore:selectedCartIds'

export function shippingFromCustomer(customer) {
  return {
    recipient: customer?.fullName || customer?.name || '',
    phone: customer?.phone || '',
    province: '',
    district: '',
    ward: '',
    addressLine: '',
  }
}

export function loginDestination(from, user) {
  if (from && typeof from === 'object') from = `${from.pathname || '/'}${from.search || ''}`
  if (typeof from === 'string' && /^\/(?!\/|\\)/.test(from) && !from.startsWith('/login')) return from
  return hasAdminRole(user) ? ADMIN_HOME : '/'
}

// A late account response must never replace what the customer is typing.
export function mergeCustomerShipping(shipping, customer, edited) {
  const latest = shippingFromCustomer(customer)
  return {
    ...shipping,
    recipient: edited.has('recipient') ? shipping.recipient : latest.recipient,
    phone: edited.has('phone') ? shipping.phone : latest.phone,
  }
}

export function shortOrderCode(code) {
  if (!code || code.length <= 16) return code || ''
  const reference = code.replace(/^PC-/i, '').replace(/-/g, '')
  return `PC-${reference.slice(0, 4).toUpperCase()}-${reference.slice(-4).toUpperCase()}`
}

export function resolveSelectedCartItems(items, selectedIds) {
  if (!Array.isArray(items) || items.length === 0) return []
  if (!Array.isArray(selectedIds)) return items
  const selectedSet = new Set(selectedIds)
  return items.filter(item => selectedSet.has(item.id || item.productId))
}

export function calculateSelectedPreview(items, selectedIds) {
  const chosen = resolveSelectedCartItems(items, selectedIds)
  return {
    selectedItems: chosen,
    selectedSkuCount: chosen.length,
    selectedQuantity: chosen.reduce((sum, item) => sum + item.quantity, 0),
    selectedSubtotal: chosen.reduce((sum, item) => sum + Number(item.price ?? item.unitPrice ?? 0) * item.quantity, 0),
  }
}

export function isReconfirmationError(error) {
  const data = error?.response?.data
  if (!data) return false
  if (data.reconfirmationRequired === true) return true
  const code = data.errorCode || data.code
  return [
    'PRICE_CHANGED',
    'VOUCHER_RECONFIRMATION_REQUIRED',
    'SHIPPING_FEE_CHANGED',
    'CART_SELECTION_CHANGED',
  ].includes(code)
}

/** Errors after which the current quote can no longer be used and the customer must re-quote. */
export function invalidatesQuote(error) {
  const code = error?.response?.data?.errorCode || error?.response?.data?.code
  return [
    'QUOTE_EXPIRED',
    'QUOTE_USED',
    'QUOTE_NOT_FOUND',
    'INSUFFICIENT_STOCK',
    'PRODUCT_DELETED',
    'PRODUCT_UNAVAILABLE',
    'CART_ITEM_NOT_IN_CART',
    'CART_VERSION_CONFLICT',
    'SHIPPING_METHOD_UNSUPPORTED',
    'SHIPPING_ADDRESS_UNSUPPORTED',
    'INVALID_SHIPPING_ADDRESS',
  ].includes(code)
}

/** Validates the 3-level administrative address chosen from the official lists. */
export function administrativeAddressError({ province, district, ward, wards }) {
  if (!province) return 'Vui lòng chọn tỉnh / thành phố hợp lệ từ danh sách.'
  if (!district) return 'Vui lòng chọn quận / huyện thuộc tỉnh / thành phố đã chọn.'
  if (Array.isArray(wards) && wards.length > 0 && !ward) {
    return 'Vui lòng chọn phường / xã thuộc quận / huyện đã chọn.'
  }
  return ''
}

export function isTimeoutOrNetworkError(error) {
  return !error?.response || error.code === 'ECONNABORTED'
}

/**
 * Places an order with an Idempotency-Key. If the request times out or loses network connection,
 * checks the operation result on the backend BEFORE allowing a retry so an already-created order
 * is immediately recovered instead of re-submitted blindly.
 */
export async function placeOrderWithTimeoutCheck({
  quoteId,
  paymentMethod,
  idempotencyKey,
  hadPriorTimeout = false,
  placeRequest,
  checkIdempotency,
}) {
  // A previous attempt with this key timed out: ask the backend first so an order that was
  // already committed is recovered without sending another POST.
  if (hadPriorTimeout) {
    try {
      const prior = await checkIdempotency(idempotencyKey)
      if (prior?.status === 200 && prior?.data?.id) {
        return { order: prior.data, recoveredFromTimeout: true }
      }
    } catch {
      // 404 / network: no committed order yet, continue with the same key
    }
  }
  try {
    const response = await placeRequest({ quoteId, paymentMethod }, idempotencyKey)
    return { order: response.data, recoveredFromTimeout: false }
  } catch (error) {
    if (!isTimeoutOrNetworkError(error)) {
      throw error
    }
    try {
      const checkResponse = await checkIdempotency(idempotencyKey)
      if (checkResponse?.status === 200 && checkResponse?.data?.id) {
        return { order: checkResponse.data, recoveredFromTimeout: true }
      }
    } catch {
      // Fall through so caller can safely retry using the exact same idempotencyKey
    }
    error.canRetrySameKey = true
    error.idempotencyKey = idempotencyKey
    throw error
  }
}
