import http from './http'

const customer = '/v1/orders'
const admin = '/v1/admin/orders'
export const orderApi = {
  account: () => http.get(`${customer}/account`),
  quote: body => http.post(`${customer}/quotes`, body),
  place: (body, key) => http.post(customer, body, { headers: { 'Idempotency-Key': key }, timeout: 20000 }),
  list: (management, page = 0) => http.get(management ? admin : customer, { params: { page } }),
  cancel: order => http.post(`${customer}/${order.id}/cancel`, { version: order.version, status: 'CANCELLED', reason: 'Khách hàng hủy đơn' }),
  transition: (order, status, reason) => http.patch(`${admin}/${order.id}/status`, { version: order.version, status, reason }),
  quantity: (order, productId, quantity, reason) => http.patch(`${admin}/${order.id}/items/${productId}`, { version: order.version, quantity, reason }),
}
export const orderError = error => error?.response?.data?.detail || 'Không nhận được kết quả. Hãy thử lại cùng yêu cầu hoặc kiểm tra lịch sử đơn.'
export const orderLabels = {
  PENDING_PAYMENT: 'Chờ thanh toán', PENDING_CONFIRMATION: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận',
  SHIPPING: 'Đang giao', COMPLETED: 'Hoàn tất', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn',
  UNPAID: 'Chưa thanh toán', PAID: 'Đã thanh toán', FAILED: 'Thanh toán thất bại', REVIEW_REQUIRED: 'Cần đối soát thanh toán',
}
