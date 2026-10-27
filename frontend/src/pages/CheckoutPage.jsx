import React, { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import CartDemoPanel from '../components/CartDemoPanel'
import { orderApi, orderError } from '../services/orders'
import { formatVND } from '../utils/cart'
import '../styles/orders.css'

export default function CheckoutPage() {
  const { cartVersion, demo, ready, refresh } = useCart()
  const navigate = useNavigate()
  const [shipping, setShipping] = useState({ recipient: '', phone: '', province: '', ward: '', addressLine: '' })
  const [quote, setQuote] = useState(null)
  const [method, setMethod] = useState('COD')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [now, setNow] = useState(Date.now())
  const sending = useRef(false)
  useEffect(() => {
    let active = true
    setQuote(null)
    orderApi.account().then(({ data }) => {
      if (active) { setShipping(value => ({ ...value, recipient: data.name, phone: data.phone || '' })); setError('') }
    }).catch(error => { if (active) setError(orderError(error)) })
    return () => { active = false }
  }, [demo?.userId])
  useEffect(() => { const timer = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(timer) }, [])
  const remaining = quote ? Math.max(0, Math.ceil((new Date(quote.expiresAt).getTime() - now) / 1000)) : 0
  const preview = async event => {
    event.preventDefault()
    if (sending.current) return
    sending.current = true; setBusy(true); setError(''); setQuote(null)
    try { const { data } = await orderApi.quote({ cartVersion, shipping }); setQuote(data); setNow(Date.now()) }
    catch (error) { setError(orderError(error)); await refresh() }
    finally { sending.current = false; setBusy(false) }
  }
  const place = async () => {
    if (sending.current || !quote || !remaining) return
    sending.current = true; setBusy(true); setError('')
    // Keep the same key for retries, including when the first response is lost.
    const storageKey = `order-key:${quote.id}:${method}`
    const key = sessionStorage.getItem(storageKey) || crypto.randomUUID()
    sessionStorage.setItem(storageKey, key)
    try {
      const { data } = await orderApi.place({ quoteId: quote.id, paymentMethod: method }, key)
      await refresh()
      if (data.paymentUrl && data.status === 'PENDING_PAYMENT') window.location.assign(data.paymentUrl)
      else navigate('/orders', { state: { created: data.code } })
    } catch (error) { setError(orderError(error)) }
    finally { sending.current = false; setBusy(false) }
  }
  return <div className="orders-page">
    <CartDemoPanel />
    <nav className="order-actions"><Link to="/cart">← Giỏ hàng</Link><Link to="/orders">Đơn của tôi</Link></nav>
    <h1>Đặt hàng</h1>
    <p>Phí vận chuyển cố định 50.000đ. Số nhận hàng phải trùng số đã xác minh.</p>
    {error && <p className="order-error" role="alert">{error}</p>}
    <div className="checkout-grid">
      <form className="order-card" onSubmit={preview}>
        <h2>Thông tin nhận hàng</h2>
        <fieldset disabled={busy}>
          {[
            ['recipient', 'Tên người nhận', 100], ['phone', 'Số điện thoại đã xác minh', 16],
            ['province', 'Tỉnh / thành phố', 100], ['ward', 'Phường / xã', 100], ['addressLine', 'Số nhà, tên đường', 255],
          ].map(([key, label, maxLength]) => <label key={key}>{label}
            <input required maxLength={maxLength} minLength={key === 'addressLine' ? 5 : key === 'recipient' ? 2 : undefined}
              type={key === 'phone' ? 'tel' : 'text'} value={shipping[key]}
              onChange={event => { setShipping({ ...shipping, [key]: event.target.value }); setQuote(null) }} />
          </label>)}
          <button className="store-button" disabled={!ready} type="submit">{busy ? 'Đang xử lý…' : 'Xem trước đơn hàng'}</button>
        </fieldset>
      </form>
      <section className="order-card" aria-live="polite">
        <h2>Xác nhận đơn hàng</h2>
        {!quote ? <p>Nhập thông tin nhận hàng để lấy giá và tồn kho từ cửa hàng.</p> : <>
          <p>Hiệu lực còn {Math.floor(remaining / 60)}:{String(remaining % 60).padStart(2, '0')}</p>
          {quote.items.map(item => <div className="order-row" key={item.productId}><span>{item.name} × {item.quantity}</span><strong>{formatVND(Number(item.unitPrice) * item.quantity)}</strong></div>)}
          <div className="order-row"><span>Tạm tính</span><span>{formatVND(Number(quote.subtotal))}</span></div>
          <div className="order-row"><span>Vận chuyển</span><span>{formatVND(Number(quote.shippingFee))}</span></div>
          <div className="order-row"><strong>Tổng cộng</strong><strong>{formatVND(Number(quote.total))}</strong></div>
          <label>Phương thức thanh toán<select value={method} disabled={busy} onChange={event => setMethod(event.target.value)}>
            <option value="COD">COD — trả tiền khi giao thành công</option><option value="VNPAY">VNPAY sandbox</option>
          </select></label>
          <button className="store-button" onClick={place} disabled={busy || !remaining}>{busy ? 'Đang tạo đơn…' : remaining ? 'Đặt hàng' : 'Bản xem trước đã hết hạn'}</button>
        </>}
      </section>
    </div>
  </div>
}
