import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import CartDemoPanel from '../components/CartDemoPanel'
import { orderApi, orderError, orderLabels } from '../services/orders'
import { formatVND } from '../utils/cart'
import '../styles/orders.css'
import useAdminAccess from '../hooks/useAdminAccess'
import OrderReference from '../components/OrderReference'
import { MapPin, Package, CalendarDays } from 'lucide-react'

function OrderCard({ order, management, busy, run }) {
  const [reason, setReason] = useState('')
  const [quantities, setQuantities] = useState({})
  const next = { PENDING_CONFIRMATION: 'CONFIRMED', CONFIRMED: 'SHIPPING', SHIPPING: 'COMPLETED' }[order.status]
  const cancellable = ['PENDING_PAYMENT', 'PENDING_CONFIRMATION'].includes(order.status) && order.paymentStatus !== 'PAID'
  return <article className="order-card order-list-card" aria-label={`Đơn hàng của ${order.recipient}`}>
    <div className="order-list-card__header"><div className="order-list-card__identity"><span className="order-list-card__icon"><Package size={22} /></span><div><span className="order-list-card__label">ĐƠN HÀNG</span><OrderReference code={order.code} /></div></div>
      <span className={`order-status order-status--${order.status.toLowerCase()}`}>{orderLabels[order.status]}</span></div>
    <div className="order-list-card__meta"><span><CalendarDays size={14} />{new Date(order.createdAt).toLocaleString('vi-VN')}</span><span>{order.paymentMethod} · {orderLabels[order.paymentStatus] || order.paymentStatus}</span></div>
    <div className="order-list-card__address"><MapPin size={17} /><div><strong>{order.recipient}</strong><span> · {order.phone}</span><p>{order.address}</p></div></div>
    {order.items.map(item => <div className="order-row" key={item.productId}>
      <span>{item.name} × {item.quantity} · {formatVND(Number(item.unitPrice))}</span>
      {management && order.paymentMethod === 'COD' && order.status === 'PENDING_CONFIRMATION' && <div>
        <input aria-label={`Số lượng ${item.name}`} type="number" min="1" max="99" value={quantities[item.productId] ?? item.quantity}
          onChange={event => setQuantities({ ...quantities, [item.productId]: event.target.value })} />
        <button disabled={busy || !reason.trim() || !Number.isInteger(Number(quantities[item.productId])) || Number(quantities[item.productId]) < 1 || Number(quantities[item.productId]) > 99}
          onClick={() => run(() => orderApi.quantity(order, item.productId, Number(quantities[item.productId]), reason))}>Lưu lượng</button>
      </div>}
    </div>)}
    <div className="order-row"><span>Vận chuyển</span><strong>{formatVND(Number(order.shippingFee))}</strong></div>
    <div className="order-row"><strong>Tổng thanh toán</strong><strong>{formatVND(Number(order.total))}</strong></div>
    {management && <label>Lý do thao tác<input maxLength={500} value={reason} onChange={event => setReason(event.target.value)} /></label>}
    <div className="order-actions">
      {cancellable && <button disabled={busy || (management && !reason.trim())} onClick={() => run(() => management
        ? orderApi.transition(order, 'CANCELLED', reason) : orderApi.cancel(order))}>Hủy đơn</button>}
      {management && next && <button className="store-button" disabled={busy || !reason.trim()}
        onClick={() => run(() => orderApi.transition(order, next, reason))}>{orderLabels[next]}</button>}
      {!management && order.status === 'PENDING_PAYMENT' && order.paymentUrl && new Date(order.expiresAt) > new Date() && <a className="store-button" href={order.paymentUrl}>Tiếp tục thanh toán</a>}
    </div>
    <details><summary>Lịch sử đơn hàng</summary><ol>{order.history.map((entry, index) => <li key={index}>
      {new Date(entry.createdAt).toLocaleString('vi-VN')} · {orderLabels[entry.toStatus]} · {entry.reason}
    </li>)}</ol></details>
  </article>
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
    try { const { data } = await orderApi.list(management, page); if (current === generation.current) { setOrders(data); setError('') } }
    catch (error) { if (current === generation.current) { setOrders([]); setError(orderError(error)) } }
    finally { if (current === generation.current) setLoading(false) }
  }, [management, page, demo?.userId])
  useEffect(() => { setOrders([]); load(); return () => { generation.current++ } }, [load])
  const run = async action => {
    if (busy) return
    setBusy(true); setError('')
    try { await action(); await load() }
    catch (error) { const message = orderError(error); await load(); setError(message) }
    finally { setBusy(false) }
  }
  return <div className="orders-page">
    <CartDemoPanel />
    <nav className="order-actions"><Link to="/cart">Giỏ hàng</Link>
      {management ? <Link to="/orders">Đơn của tôi</Link> : adminAccess.account && <Link to="/admin/orders">Quản trị đơn</Link>}
      <button disabled={busy || loading} onClick={load}>Tải lại</button></nav>
    <h1>{management ? 'Quản trị đơn hàng' : 'Đơn hàng của tôi'}</h1>
    {location.state?.created && <div className="order-created-notice"><p role="status">Đơn hàng đã được tạo thành công.</p><OrderReference code={location.state.created} /></div>}
    {location.search.includes('vnp_') && <p role="status">Đã trở về từ VNPAY. Trạng thái sẽ cập nhật khi cửa hàng nhận IPN. Hãy tải lại để kiểm tra.</p>}
    {error && <p className="order-error" role="alert">{error}</p>}
    {loading ? <p role="status">Đang tải đơn hàng…</p> : orders.length === 0 && !error ? <p>Chưa có đơn hàng ở trang này.</p> :
      orders.map(order => <OrderCard key={`${order.id}:${order.version}`} order={order} management={management} busy={busy} run={run} />)}
    <div className="order-actions"><button disabled={!page || loading || busy} onClick={() => setPage(page - 1)}>Trang trước</button>
      <span>Trang {page + 1}</span><button disabled={orders.length < 20 || loading || busy} onClick={() => setPage(page + 1)}>Trang sau</button></div>
  </div>
}
