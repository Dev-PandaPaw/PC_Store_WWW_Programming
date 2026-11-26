import http from './http'

const customer = '/v1/orders'
const admin = '/v1/admin/orders'

export const orderApi = {
  adminAccount: () => http.get(`${admin}/account`),
  account: () => http.get(`${customer}/account`),
  shippingOptions: (province = '', district = '') =>
    http.get(`${customer}/shipping-options`, { params: { province, district } }),
  vouchers: () => http.get(`${customer}/vouchers`),
  quote: body => http.post(`${customer}/quotes`, body),
  place: (body, key) => http.post(customer, body, { headers: { 'Idempotency-Key': key }, timeout: 20000 }),
  checkIdempotency: key => http.get(`${customer}/idempotency/${encodeURIComponent(key)}`, { timeout: 10000 }),
  get: (id, management = false) => http.get(`${management ? admin : customer}/${id}`),
  list: (management, page = 0) => http.get(management ? admin : customer, { params: { page } }),
  cancel: order => http.post(`${customer}/${order.id}/cancel`, { version: order.version, status: 'CANCELLED', reason: 'Khách hàng hủy đơn' }),
  createVnpayUrl: (orderId, amount, idempotencyKey) =>
    http.post('/v1/payments/vnpay/create-url', { orderId, amount, idempotencyKey }),
  transition: (order, status, reason) => http.patch(`${admin}/${order.id}/status`, { version: order.version, status, reason }),
  quantity: (order, productId, quantity, reason) => http.patch(`${admin}/${order.id}/items/${productId}`, { version: order.version, quantity, reason }),
}

export const orderError = error =>
  error?.response?.data?.detail ||
  'Không nhận được phản hồi từ máy chủ. Hệ thống sẽ kiểm tra trạng thái đơn trước khi thử lại.'

export const orderLabels = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  PENDING_CONFIRMATION: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang chuẩn bị hàng',
  SHIPPING: 'Đang giao hàng',
  DELIVERED: 'Đã giao hàng',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Hết hạn',
  UNPAID: 'Chưa thanh toán',
  PAID: 'Đã thanh toán',
  FAILED: 'Thanh toán thất bại',
  REVIEW_REQUIRED: 'Cần đối soát thanh toán',
}

export const shippingMethodLabels = {
  EXPRESS: 'Vận chuyển nhanh',
  INSTANT: 'Ship hỏa tốc 2h',
  STORE_PICKUP: 'Nhận tại cửa hàng',
}

export const paymentMethodLabels = {
  COD: 'Thanh toán khi nhận hàng (COD)',
  VNPAY: 'Thanh toán trực tuyến (VNPay)',
}
