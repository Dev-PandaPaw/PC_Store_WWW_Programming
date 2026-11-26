import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useCart } from '../contexts/cartContextStore'
import CartDemoPanel from '../components/CartDemoPanel'
import { orderApi, orderError, orderLabels, paymentMethodLabels, shippingMethodLabels } from '../services/orders'
import { formatVND } from '../utils/cart'
import '../styles/orders.css'
import useAdminAccess from '../hooks/useAdminAccess'
import OrderReference from '../components/OrderReference'
import { shortOrderCode } from '../utils/checkout'
import { CalendarDays, MapPin, Package, RefreshCw, X } from 'lucide-react'

const NEXT_ADMIN_STATUS = {
  PENDING_CONFIRMATION: 'CONFIRMED',
  CONFIRMED: 'PROCESSING',
  PROCESSING: 'SHIPPING',
  SHIPPING: 'DELIVERED',
  DELIVERED: 'COMPLETED',
}

const ORDER_STATUS_TONE = {
  PENDING_PAYMENT: 'pending',
  PENDING_CONFIRMATION: 'pending',
  CONFIRMED: 'progress',
  PROCESSING: 'progress',
  SHIPPING: 'progress',
  DELIVERED: 'positive',
  COMPLETED: 'positive',
  CANCELLED: 'negative',
  EXPIRED: 'negative',
}

function OrderCard({ order, management, busy, run }) {
  const [reason, setReason] = useState('')
  const [quantities, setQuantities] = useState({})
  const next = NEXT_ADMIN_STATUS[order.status]
  const cancellable = management
    ? ['PENDING_PAYMENT', 'PENDING_CONFIRMATION', 'CONFIRMED', 'PROCESSING'].includes(order.status) && order.paymentStatus !== 'PAID'
    : ['PENDING_PAYMENT', 'PENDING_CONFIRMATION'].includes(order.status) && order.paymentStatus !== 'PAID'

  return (
    <article className="order-card order-list-card" aria-label={`Đơn hàng của ${order.recipient}`}>
      <div className="order-list-card__header">
        <div className="order-list-card__identity">
          <span className="order-list-card__icon"><Package size={22} /></span>
          <div>
            <span className="order-list-card__label">ĐƠN HÀNG</span>
            <OrderReference code={order.code} />
          </div>
        </div>
        <span className={`order-status order-status--${order.status.toLowerCase()}`}>
          {orderLabels[order.status] || order.status}
        </span>
      </div>
      <div className="order-list-card__meta">
        <span><CalendarDays size={14} />{new Date(order.createdAt).toLocaleString('vi-VN')}</span>
        <span>
          {paymentMethodLabels[order.paymentMethod] || order.paymentMethod} · {orderLabels[order.paymentStatus] || order.paymentStatus}
        </span>
      </div>
      <div className="order-list-card__address">
        <MapPin size={17} />
        <div>
          <strong>{order.recipient}</strong>
          <span> · {order.phone} · {shippingMethodLabels[order.shippingMethod] || 'Vận chuyển nhanh'}</span>
          <p>{order.address}</p>
        </div>
      </div>
      {order.items.map(item => (
        <div className="order-row" key={item.productId}>
          <span>{item.name} ({item.sku}) × {item.quantity} · {formatVND(Number(item.unitPrice))}</span>
          <strong>{formatVND(Number(item.unitPrice) * item.quantity)}</strong>
          {management && order.paymentMethod === 'COD' && order.status === 'PENDING_CONFIRMATION' && (
            <div>
              <input
                aria-label={`Số lượng ${item.name}`}
                type="number"
                min="1"
                max="99"
                value={quantities[item.productId] ?? item.quantity}
                onChange={event => setQuantities({ ...quantities, [item.productId]: event.target.value })}
              />
              <button
                type="button"
                className="admin-list__button admin-list__button--small"
                disabled={busy || !reason.trim() || !Number.isInteger(Number(quantities[item.productId])) || Number(quantities[item.productId]) < 1 || Number(quantities[item.productId]) > 99}
                onClick={() => run(() => orderApi.quantity(order, item.productId, Number(quantities[item.productId]), reason))}
              >
                Lưu lượng
              </button>
            </div>
          )}
        </div>
      ))}
      <div className="order-row">
        <span>Tạm tính</span>
        <span>{formatVND(Number(order.subtotal ?? order.total))}</span>
      </div>
      <div className="order-row">
        <span>Phí vận chuyển ({shippingMethodLabels[order.shippingMethod] || 'Vận chuyển nhanh'})</span>
        <strong>{Number(order.shippingFee) === 0 ? 'Miễn phí' : formatVND(Number(order.shippingFee))}</strong>
      </div>
      {Number(order.discountAmount) > 0 && (
        <div className="order-row checkout-discount-row">
          <span>Giảm giá ({order.voucherCode})</span>
          <strong>−{formatVND(Number(order.discountAmount))}</strong>
        </div>
      )}
      <div className="order-row">
        <strong>Tổng thanh toán</strong>
        <strong>{formatVND(Number(order.total))}</strong>
      </div>
      {management && (
        <label>
          Lý do thao tác
          <input maxLength={500} value={reason} onChange={event => setReason(event.target.value)} />
        </label>
      )}
      <div className="order-actions">
        {cancellable && (
          <button
            type="button"
            className={management ? 'admin-list__button admin-list__button--negative' : undefined}
            disabled={busy || (management && !reason.trim())}
            onClick={() => run(() => management
              ? orderApi.transition(order, 'CANCELLED', reason)
              : orderApi.cancel(order))}
          >
            Hủy đơn
          </button>
        )}
        {management && next && (
          <button
            type="button"
            className="admin-list__button admin-list__button--primary"
            disabled={busy || !reason.trim()}
            onClick={() => run(() => orderApi.transition(order, next, reason))}
          >
            {orderLabels[next] || next}
          </button>
        )}
      </div>
      <details>
        <summary>Lịch sử trạng thái ({order.history?.length || 0})</summary>
        <ol>
          {order.history?.map((entry, index) => (
            <li key={index}>
              {new Date(entry.createdAt).toLocaleString('vi-VN')} · {orderLabels[entry.toStatus] || entry.toStatus} · {entry.reason}
            </li>
          ))}
        </ol>
      </details>
    </article>
  )
}

function AdminOrderDetails({ order, busy, error, run, onClose }) {
  const dialog = useRef(null)

  useEffect(() => {
    if (!dialog.current.open) dialog.current.showModal()
  }, [])

  return (
    <dialog ref={dialog} className="admin-list__dialog admin-orders__dialog" aria-labelledby="admin-order-details-title" onClose={onClose}>
      <header className="admin-list__dialog-heading">
        <h2 id="admin-order-details-title">Chi tiết đơn hàng</h2>
        <button
          type="button" className="admin-list__button admin-list__button--small" aria-label="Đóng chi tiết đơn hàng"
          onClick={() => dialog.current.close()}
        >
          <X size={17} />
        </button>
      </header>
      {error && <p className="admin-list__notice admin-list__notice--error" role="alert">{error}</p>}
      <OrderCard key={`${order.id}:${order.version}`} order={order} management busy={busy} run={run} />
    </dialog>
  )
}

function AdminOrdersList({ orders, loading, busy, error, load, run, page, setPage }) {
  const [selectedId, setSelectedId] = useState(null)
  const selectedOrder = orders.find(order => order.id === selectedId)

  return (
    <div className="admin-list">
      <CartDemoPanel />
      <header className="admin-list__heading">
        <div>
          <h1>Quản lý đơn hàng</h1>
          {!loading && !error && <p>Hiển thị {orders.length} đơn hàng trên trang này.</p>}
        </div>
        <button type="button" className="admin-list__button" disabled={busy || loading} onClick={load}>
          <RefreshCw size={15} /> Tải lại
        </button>
      </header>
      {error && !selectedOrder && <p className="admin-list__notice admin-list__notice--error" role="alert">{error}</p>}
      <div className="admin-list__table-wrap">
        <table className="admin-list__table" aria-label="Danh sách đơn hàng" aria-busy={loading}>
          <thead><tr>
            <th scope="col">Mã đơn</th><th scope="col">Người nhận</th><th scope="col">Thanh toán</th>
            <th scope="col">Trạng thái</th><th scope="col">Tổng tiền</th><th scope="col">Ngày tạo</th>
            <th scope="col" className="admin-list__end">Thao tác</th>
          </tr></thead>
          <tbody>
            {loading || orders.length === 0 ? (
              <tr><td colSpan={7} className="admin-list__empty">
                <span role="status">{loading ? 'Đang tải đơn hàng…' : error ? 'Không tải được danh sách đơn hàng.' : 'Chưa có đơn hàng ở trang này.'}</span>
              </td></tr>
            ) : orders.map(order => {
              const tone = ORDER_STATUS_TONE[order.status]
              return (
                <tr key={order.id}>
                  <td className="admin-list__name admin-list__nowrap">{shortOrderCode(order.code)}</td>
                  <td>
                    <span className="admin-list__name">{order.recipient}</span>
                    {order.phone && <span className="admin-list__subline">{order.phone}</span>}
                  </td>
                  <td>
                    {paymentMethodLabels[order.paymentMethod] || order.paymentMethod}
                    <span className="admin-list__subline">{orderLabels[order.paymentStatus] || order.paymentStatus}</span>
                  </td>
                  <td><span className={`admin-list__pill${tone ? ` admin-list__pill--${tone}` : ''}`}>{orderLabels[order.status] || order.status}</span></td>
                  <td className="admin-list__nowrap">{formatVND(Number(order.total))}</td>
                  <td className="admin-list__nowrap">{new Date(order.createdAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })}</td>
                  <td className="admin-list__end">
                    <button
                      type="button" className="admin-list__button admin-list__button--small"
                      aria-label={`Chi tiết đơn ${shortOrderCode(order.code)}`} onClick={() => setSelectedId(order.id)}
                    >
                      Chi tiết
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      <div className="admin-list__pagination">
        <span>Trang {page + 1}</span>
        <div className="admin-list__actions">
          <button type="button" className="admin-list__button admin-list__button--small" disabled={!page || loading || busy} onClick={() => setPage(page - 1)}>Trang trước</button>
          <button type="button" className="admin-list__button admin-list__button--small" disabled={orders.length < 20 || loading || busy} onClick={() => setPage(page + 1)}>Trang sau</button>
        </div>
      </div>
      {selectedOrder && <AdminOrderDetails order={selectedOrder} busy={busy || loading} error={error} run={run} onClose={() => setSelectedId(null)} />}
    </div>
  )
}

export default function OrdersPage({ management = false }) {
  const adminAccess = useAdminAccess(!management)
  const { demo } = useCart()
  const location = useLocation()
  const [orders, setOrders] = useState([])
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const generation = useRef(0)

  const load = useCallback(async () => {
    const current = ++generation.current
    setLoading(true)
    try {
      const { data } = await orderApi.list(management, page)
      if (current === generation.current) {
        setOrders(data)
        setError('')
      }
    } catch (err) {
      if (current === generation.current) {
        setOrders([])
        setError(orderError(err))
      }
    } finally {
      if (current === generation.current) setLoading(false)
    }
  }, [management, page, demo?.userId])

  useEffect(() => {
    setOrders([])
    load()
    return () => { generation.current++ }
  }, [load])

  const run = async action => {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await action()
      await load()
    } catch (err) {
      const message = orderError(err)
      await load()
      setError(message)
    } finally {
      setBusy(false)
    }
  }

  if (management) {
    return <AdminOrdersList
      orders={orders} loading={loading} busy={busy} error={error}
      load={load} run={run} page={page} setPage={setPage}
    />
  }

  return (
    <div className="orders-page">
      <CartDemoPanel />
      <nav className="order-actions">
        <Link to="/cart">Giỏ hàng</Link>
        {adminAccess.account && <Link to="/admin/orders">Quản trị đơn</Link>}
        <button disabled={busy || loading} onClick={load}>Tải lại</button>
      </nav>
      <h1>Đơn hàng của tôi</h1>
      {location.state?.created && (
        <div className="order-created-notice">
          <p role="status">Đơn hàng đã được tạo thành công.</p>
          <OrderReference code={location.state.created} />
        </div>
      )}
      {error && <p className="order-error" role="alert">{error}</p>}
      {loading ? (
        <p role="status">Đang tải đơn hàng…</p>
      ) : orders.length === 0 && !error ? (
        <p>Chưa có đơn hàng ở trang này.</p>
      ) : (
        orders.map(order => (
          <OrderCard key={`${order.id}:${order.version}`} order={order} busy={busy} run={run} />
        ))
      )}
      <div className="order-actions">
        <button disabled={!page || loading || busy} onClick={() => setPage(page - 1)}>Trang trước</button>
        <span>Trang {page + 1}</span>
        <button disabled={orders.length < 20 || loading || busy} onClick={() => setPage(page + 1)}>Trang sau</button>
      </div>
    </div>
  )
}
