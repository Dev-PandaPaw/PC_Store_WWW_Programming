import React from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowDownWideNarrow, ChevronLeft, ChevronRight, Info, Package, Search, ShoppingBag } from 'lucide-react'
import { mockOrders } from '../data/mockOrders'
import '../styles/order-history.css'

// Nhãn và tone badge theo UI_SCREEN_SPEC §4.2 — Order và Payment là hai domain riêng.
const ORDER_STATUSES = {
  AWAITING_PAYMENT:      { label: 'Chờ thanh toán',  tone: 'warning' },
  AWAITING_CONFIRMATION: { label: 'Chờ xác nhận',    tone: 'warning' },
  CONFIRMED:             { label: 'Đã xác nhận',     tone: 'info'    },
  SHIPPING:              { label: 'Đang giao',        tone: 'info'    },
  DELIVERED:             { label: 'Đã giao',          tone: 'success' },
  CANCELLED:             { label: 'Đã hủy',           tone: 'danger'  },
  EXPIRED:               { label: 'Hết hạn',          tone: 'neutral' },
}
const PAYMENT_STATUSES = {
  PENDING:         { label: 'Chờ thanh toán', tone: 'warning' },
  PAID:            { label: 'Đã thanh toán',  tone: 'success' },
  FAILED:          { label: 'Thất bại',       tone: 'danger'  },
  REVIEW_REQUIRED: { label: 'Cần kiểm tra',   tone: 'warning' },
}
const PAYMENT_METHOD_LABELS = {
  COD:   'Thanh toán khi nhận hàng (COD)',
  VNPAY: 'VNPAY sandbox',
}

const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })
const dateFormatter = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh',
})
const PAGE_SIZES = [5, 10, 20]

function StatusBadge({ status }) {
  if (!status) return null
  return <span className={`oh-badge oh-badge--${status.tone}`}>{status.label}</span>
}

function formatDate(isoString) {
  return dateFormatter.format(new Date(isoString))
}

/**
 * C04 — Đơn hàng của tôi
 *
 * Trang này sử dụng dữ liệu mẫu nội bộ (mockOrders) để minh họa giao diện.
 * Prop `orders` cho phép thay bằng dữ liệu từ API khi module Auth và Order sẵn sàng.
 *
 * Không có xác thực thật: trang này không xác nhận quyền sở hữu đơn hàng
 * và không gắn với phiên người dùng nào.
 */
export default function OrderHistoryPage({ orders = mockOrders }) {
  const [params, setParams] = useSearchParams()

  const query      = params.get('q') || ''
  const rawStatus  = params.get('status') || ''
  const status     = Object.hasOwn(ORDER_STATUSES, rawStatus) ? rawStatus : ''
  const sort       = params.get('sort') === 'oldest' ? 'oldest' : 'newest'
  const rawSize    = Number(params.get('size'))
  const size       = PAGE_SIZES.includes(rawSize) ? rawSize : 5
  const rawPage    = Number(params.get('page'))

  const filtered = orders
    .filter((o) =>
      o.code.toUpperCase().includes(query.trim().toUpperCase()) &&
      (!status || o.status === status)
    )
    .sort((a, b) => {
      const diff = new Date(b.placedAt) - new Date(a.placedAt)
      return sort === 'oldest' ? -diff || a.code.localeCompare(b.code)
                               :  diff || a.code.localeCompare(b.code)
    })

  const pageCount = Math.max(1, Math.ceil(filtered.length / size))
  const page = Number.isSafeInteger(rawPage) && rawPage >= 0
    ? Math.min(rawPage, pageCount - 1)
    : 0
  const visibleOrders = filtered.slice(page * size, (page + 1) * size)
  const hasFilters = Boolean(query || status)

  function update(key, value) {
    setParams((prev) => {
      const next = new URLSearchParams(prev)
      next.delete('page')
      value !== '' ? next.set(key, String(value)) : next.delete(key)
      return next
    }, { replace: key === 'q' })
  }

  function clearFilters() {
    setParams((prev) => {
      const next = new URLSearchParams(prev)
      for (const k of ['q', 'status', 'page']) next.delete(k)
      return next
    })
  }

  const firstItem = page * size + 1
  const lastItem  = Math.min((page + 1) * size, filtered.length)

  return (
    <section className="order-history" aria-labelledby="oh-title">
      {/* Breadcrumb */}
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span aria-hidden="true">/</span>
        <span aria-current="page">Đơn hàng của tôi</span>
      </nav>

      {/* Tiêu đề trang */}
      <header className="oh-heading">
        <div className="oh-heading__icon" aria-hidden="true">
          <ShoppingBag size={28} />
        </div>
        <div>
          <p className="oh-eyebrow">TÀI KHOẢN</p>
          <h1 id="oh-title">Đơn hàng của tôi</h1>
          <p>Xem sản phẩm đã đặt, trạng thái xử lý và lịch sử thanh toán.</p>
        </div>
      </header>

      {/* Thông báo dữ liệu mẫu */}
      <aside className="oh-demo" role="note" aria-label="Thông tin bản xem thử">
        <Info size={20} aria-hidden="true" />
        <div>
          <strong>Bản xem thử · Dữ liệu mẫu</strong>
          <p>
            Các đơn bên dưới chỉ dùng để minh họa giao diện, không phải đơn hàng thật
            và không gắn với tài khoản của bạn. Chức năng xem chi tiết, hủy đơn
            và thanh toán chưa được kích hoạt ở phiên bản này.
          </p>
        </div>
      </aside>

      {/* Bộ lọc và tìm kiếm */}
      <div className="oh-filters" role="search" aria-label="Tìm và lọc đơn hàng mẫu">
        <label className="oh-field oh-field--search" htmlFor="oh-search">
          <span>Tìm theo mã đơn hàng</span>
          <span className="oh-search-control">
            <Search size={16} aria-hidden="true" />
            <input
              id="oh-search"
              type="search"
              placeholder="Ví dụ: PC260926-001"
              value={query}
              autoComplete="off"
              onChange={(e) => update('q', e.target.value)}
            />
          </span>
        </label>

        <label className="oh-field" htmlFor="oh-status">
          <span>Trạng thái đơn hàng</span>
          <select id="oh-status" value={status} onChange={(e) => update('status', e.target.value)}>
            <option value="">Tất cả trạng thái</option>
            {Object.entries(ORDER_STATUSES).map(([code, { label }]) => (
              <option key={code} value={code}>{label}</option>
            ))}
          </select>
        </label>

        <label className="oh-field" htmlFor="oh-sort">
          <span>Sắp xếp ngày đặt</span>
          <select id="oh-sort" value={sort} onChange={(e) => update('sort', e.target.value)}>
            <option value="newest">Mới nhất trước</option>
            <option value="oldest">Cũ nhất trước</option>
          </select>
        </label>

        <button
          className="oh-button"
          type="button"
          onClick={clearFilters}
          disabled={!hasFilters}
        >
          Xóa bộ lọc
        </button>
      </div>

      {/* Thanh kết quả */}
      <div className="oh-results-bar">
        <p role="status" aria-live="polite" aria-atomic="true">
          {filtered.length > 0
            ? <>Hiển thị <strong>{firstItem}–{lastItem}</strong> trong <strong>{filtered.length}</strong> đơn hàng mẫu</>
            : 'Không có đơn hàng phù hợp với bộ lọc hiện tại'
          }
        </p>
        <span className="oh-sort-note" aria-hidden="true">
          <ArrowDownWideNarrow size={15} />
          {sort === 'newest' ? 'Ngày đặt mới nhất' : 'Ngày đặt cũ nhất'}
        </span>
      </div>

      {/* Danh sách đơn hàng hoặc empty state */}
      {visibleOrders.length > 0 ? (
        <div className="oh-list" role="list">
          {visibleOrders.map((order) => {
            const orderStatus   = ORDER_STATUSES[order.status]
            const paymentStatus = PAYMENT_STATUSES[order.paymentStatus]
            const totalItems    = order.items.reduce((s, i) => s + i.quantity, 0)

            return (
              <article
                className="oh-order"
                key={order.code}
                role="listitem"
                aria-labelledby={`oh-code-${order.code}`}
              >
                {/* Header: mã đơn + ngày + trạng thái đơn */}
                <div className="oh-order__header">
                  <div className="oh-order__meta">
                    <h2 id={`oh-code-${order.code}`} className="oh-order__code">
                      {order.code}
                    </h2>
                    <p className="oh-order__date">
                      Ngày đặt:&nbsp;
                      <time dateTime={order.placedAt}>{formatDate(order.placedAt)}</time>
                    </p>
                  </div>
                  <div className="oh-order__status-col">
                    <span className="oh-label-sm">Trạng thái đơn</span>
                    <StatusBadge status={orderStatus} />
                  </div>
                </div>

                {/* Body: sản phẩm | thanh toán | tổng tiền */}
                <div className="oh-order__body">
                  {/* Cột sản phẩm */}
                  <div className="oh-products">
                    <span className="oh-label">Sản phẩm đã đặt</span>
                    <ul>
                      {order.items.map((item, idx) => (
                        <li key={idx}>
                          <span className="oh-qty">{item.quantity}&thinsp;×</span>
                          {item.name}
                          <span className="oh-sku">{item.sku}</span>
                        </li>
                      ))}
                    </ul>
                    <p className="oh-item-count">{totalItems} sản phẩm</p>
                  </div>

                  {/* Cột thanh toán */}
                  <dl className="oh-payment">
                    <div>
                      <dt>Phương thức thanh toán</dt>
                      <dd>{PAYMENT_METHOD_LABELS[order.paymentMethod] ?? order.paymentMethod}</dd>
                    </div>
                    <div>
                      <dt>Trạng thái thanh toán</dt>
                      <dd><StatusBadge status={paymentStatus} /></dd>
                    </div>
                  </dl>

                  {/* Cột tổng tiền */}
                  <div className="oh-total">
                    <span className="oh-label">Tổng tiền đơn hàng</span>
                    <div className="oh-total__rows">
                      <div className="oh-total__row">
                        <span>Tạm tính</span>
                        <span>{money.format(order.subtotal ?? order.total - order.shippingFee)}</span>
                      </div>
                      <div className="oh-total__row">
                        <span>Phí giao hàng</span>
                        <span>{money.format(order.shippingFee)}</span>
                      </div>
                      <div className="oh-total__row oh-total__row--grand">
                        <span>Tổng cộng</span>
                        <strong>{money.format(order.total)}</strong>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Footer: link xem chi tiết (C05 — Task 2) */}
                <div className="oh-order__footer">
                  <span className="oh-detail-note">
                    Xem chi tiết đơn hàng sẽ có ở phiên bản tiếp theo.
                  </span>
                </div>
              </article>
            )
          })}
        </div>
      ) : (
        <div className="oh-empty" role="status">
          <Package size={48} aria-hidden="true" />
          <h2>
            {hasFilters ? 'Không tìm thấy đơn hàng' : 'Chưa có đơn hàng mẫu'}
          </h2>
          <p>
            {hasFilters
              ? 'Thử nhập mã đơn khác hoặc xóa bộ lọc để xem lại danh sách mẫu.'
              : 'Danh sách mẫu hiện đang trống. Bạn có thể tiếp tục khám phá sản phẩm.'
            }
          </p>
          {hasFilters
            ? <button className="oh-button oh-button--primary" type="button" onClick={clearFilters}>Xóa bộ lọc</button>
            : <Link className="oh-button oh-button--primary" to="/catalog">Khám phá sản phẩm</Link>
          }
        </div>
      )}

      {/* Phân trang */}
      {filtered.length > 0 && (
        <footer className="oh-pagination">
          <label className="oh-page-size" htmlFor="oh-size">
            Số đơn mỗi trang
            <select
              id="oh-size"
              value={size}
              onChange={(e) => update('size', e.target.value)}
            >
              {PAGE_SIZES.map((v) => <option key={v} value={v}>{v}</option>)}
            </select>
          </label>

          <nav className="oh-page-controls" aria-label="Phân trang đơn hàng">
            <button
              className="oh-button"
              type="button"
              aria-label="Trang trước"
              disabled={page === 0}
              onClick={() => update('page', page - 1)}
            >
              <ChevronLeft size={18} aria-hidden="true" />
            </button>
            <span aria-current="page">
              Trang <strong>{page + 1}</strong> / {pageCount}
            </span>
            <button
              className="oh-button"
              type="button"
              aria-label="Trang sau"
              disabled={page >= pageCount - 1}
              onClick={() => update('page', page + 1)}
            >
              <ChevronRight size={18} aria-hidden="true" />
            </button>
          </nav>
        </footer>
      )}
    </section>
  )
}

