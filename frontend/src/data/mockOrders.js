/**
 * LOCAL UI FIXTURES — C04 Đơn hàng của tôi (OrderHistoryPage)
 *
 * Đây là dữ liệu mẫu UI độc lập, KHÔNG gắn với tài khoản thật, không phản ánh
 * trạng thái live và không có giá trị thẩm quyền. Tên sản phẩm và giá là
 * snapshot thời điểm đặt hàng giả định, độc lập với Catalog hiện tại.
 *
 * Thay thế bằng kết quả API khi module Auth và Order hoàn thành:
 *   const { data } = await api.get('/api/orders', { params: { page, size, q, status } })
 *
 * Nguồn fixture: UI_SCREEN_SPEC §10
 *   PC-OFF-01: PC Học tập Essential  — 9.990.000 ₫
 *   PC-DEV-01: PC Lập trình Studio   — 18.490.000 ₫
 *   PC-GAM-01: PC Gaming Balance     — 22.990.000 ₫
 *   PC-CRE-01: PC Sáng tạo Pro       — 32.990.000 ₫
 *   Customer demo: "Nguyễn Minh An", email an.demo@example.com
 */

// Snapshot sản phẩm tại thời điểm đặt hàng — không lấy từ Catalog live.
const snapshots = {
  essential: { sku: 'PC-OFF-01', name: 'PC Học tập Essential', unitPrice: 9990000 },
  studio:    { sku: 'PC-DEV-01', name: 'PC Lập trình Studio',  unitPrice: 18490000 },
  gaming:    { sku: 'PC-GAM-01', name: 'PC Gaming Balance',    unitPrice: 22990000 },
  creative:  { sku: 'PC-CRE-01', name: 'PC Sáng tạo Pro',      unitPrice: 32990000 },
}

/** Tạo object đơn hàng từ các tham số ngắn gọn. lines = [[productKey, qty], …] */
function order(code, placedAt, status, paymentMethod, paymentStatus, lines) {
  const SHIPPING_FEE = 50000
  const items = lines.map(([key, qty]) => ({ ...snapshots[key], quantity: qty }))
  const subtotal = items.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
  return {
    code,
    placedAt,
    status,
    paymentMethod,
    paymentStatus,
    items,
    subtotal,
    shippingFee: SHIPPING_FEE,
    total: subtotal + SHIPPING_FEE,
  }
}

/**
 * Mỗi bản ghi là một kịch bản trạng thái riêng biệt.
 * Trạng thái được giữ cố định — browser không tự suy payment hay hết hạn từ đồng hồ hệ thống.
 * Danh sách chưa sắp xếp để trang luôn phải áp dụng logic sort.
 *
 * Bao gồm đủ 7 trạng thái đơn: AWAITING_PAYMENT, AWAITING_CONFIRMATION,
 * CONFIRMED, SHIPPING, DELIVERED, CANCELLED, EXPIRED
 * Và 4 trạng thái thanh toán: PENDING, PAID, FAILED, REVIEW_REQUIRED
 */
export const mockOrders = [
  // ── Fixture chuẩn từ UI_SCREEN_SPEC §10 ─────────────────────────────────
  // PC260926-001: COD, 2×PC-DEV-01, phí 50.000 ₫, tổng 37.030.000 ₫
  order('PC260926-001', '2026-09-26T09:30:00+07:00', 'DELIVERED',             'COD',   'PAID',            [['studio',   2]]),
  // PC260926-002: VNPAY sandbox, 1×PC-GAM-01, phí 50.000 ₫, tổng 23.040.000 ₫
  order('PC260926-002', '2026-09-26T14:30:00+07:00', 'AWAITING_PAYMENT',      'VNPAY', 'PENDING',         [['gaming',   1]]),

  // ── Đơn bổ sung — đủ kịch bản cho mọi trạng thái ───────────────────────
  order('PC261026-001', '2026-10-26T10:15:00+07:00', 'AWAITING_CONFIRMATION', 'COD',   'PENDING',         [['essential', 1]]),
  order('PC261025-001', '2026-10-25T15:45:00+07:00', 'CONFIRMED',             'VNPAY', 'PAID',            [['studio',   1], ['essential', 1]]),
  order('PC261024-001', '2026-10-24T08:00:00+07:00', 'SHIPPING',              'COD',   'PENDING',         [['gaming',   1]]),
  order('PC261023-001', '2026-10-23T11:20:00+07:00', 'DELIVERED',             'VNPAY', 'PAID',            [['creative', 1]]),
  order('PC261022-001', '2026-10-22T16:10:00+07:00', 'CANCELLED',             'COD',   'PENDING',         [['essential', 2]]),
  order('PC261021-001', '2026-10-21T09:05:00+07:00', 'EXPIRED',               'VNPAY', 'FAILED',          [['studio',   1]]),
  order('PC261020-001', '2026-10-20T13:40:00+07:00', 'EXPIRED',               'VNPAY', 'REVIEW_REQUIRED', [['gaming',   1]]),
  order('PC261019-001', '2026-10-19T10:30:00+07:00', 'AWAITING_PAYMENT',      'VNPAY', 'FAILED',          [['essential', 1]]),
  order('PC261018-001', '2026-10-18T14:00:00+07:00', 'AWAITING_CONFIRMATION', 'VNPAY', 'PAID',            [['creative', 1]]),
  order('PC261017-001', '2026-10-17T08:45:00+07:00', 'CONFIRMED',             'COD',   'PENDING',         [['studio',   2]]),
  order('PC261016-001', '2026-10-16T16:15:00+07:00', 'SHIPPING',              'VNPAY', 'PAID',            [['gaming',   1], ['essential', 1]]),
  order('PC261015-001', '2026-10-15T11:50:00+07:00', 'DELIVERED',             'COD',   'PAID',            [['essential', 1]]),
  order('PC261014-001', '2026-10-14T09:20:00+07:00', 'CANCELLED',             'VNPAY', 'FAILED',          [['creative', 1]]),
  order('PC261013-001', '2026-10-13T15:00:00+07:00', 'EXPIRED',               'VNPAY', 'PENDING',         [['gaming',   1]]),
  order('PC261012-001', '2026-10-12T10:10:00+07:00', 'DELIVERED',             'COD',   'PAID',            [['studio',   1]]),
  order('PC261011-001', '2026-10-11T13:25:00+07:00', 'DELIVERED',             'VNPAY', 'PAID',            [['essential', 3]]),
  order('PC261010-001', '2026-10-10T08:35:00+07:00', 'CANCELLED',             'COD',   'PENDING',         [['studio',   1]]),
  order('PC261009-001', '2026-10-09T17:00:00+07:00', 'DELIVERED',             'VNPAY', 'PAID',            [['creative', 1], ['studio', 1]]),
  order('PC261008-001', '2026-10-08T12:40:00+07:00', 'DELIVERED',             'COD',   'PAID',            [['gaming',   2]]),
  order('PC261007-001', '2026-10-07T09:15:00+07:00', 'EXPIRED',               'VNPAY', 'REVIEW_REQUIRED', [['essential', 1]]),
  order('PC261006-001', '2026-10-06T14:50:00+07:00', 'DELIVERED',             'VNPAY', 'PAID',            [['studio',   1]]),
  order('PC261005-001', '2026-10-05T10:00:00+07:00', 'DELIVERED',             'COD',   'PAID',            [['essential', 1], ['gaming', 1]]),
]
