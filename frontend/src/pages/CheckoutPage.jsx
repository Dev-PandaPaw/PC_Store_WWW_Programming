import React, { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Check, CheckCircle2, CreditCard, MapPin, Package, ShieldCheck, ShoppingBag, Truck, UserRound } from 'lucide-react'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'
import CartDemoPanel from '../components/CartDemoPanel'
import OrderReference from '../components/OrderReference'
import AddressSelect from '../components/AddressSelect'
import provinces from '../data/vietnamDistricts.json'
import { selectedAddress, changeProvince, changeDistrict } from '../utils/addresses'
import { orderApi, orderError } from '../services/orders'
import { formatVND } from '../utils/cart'
import { shippingFromCustomer, mergeCustomerShipping } from '../utils/checkout'
import '../styles/orders.css'

const fields = [
  ['recipient', 'Họ và tên người nhận', 'Nhập họ và tên', 100, 'name'],
  ['phone', 'Số điện thoại', 'Số điện thoại đã xác minh', 16, 'tel'],
  ['province', 'Tỉnh / thành phố', 'Ví dụ: TP. Hồ Chí Minh', 100, 'address-level1'],
  ['district', 'Quận / huyện', 'Chọn tỉnh / thành phố trước', 100, 'address-level2'],
  ['ward', 'Phường / xã', 'Nhập phường hoặc xã', 100, 'address-level3'],
  ['addressLine', 'Địa chỉ nhận hàng', 'Số nhà, tên đường, tòa nhà…', 255, 'street-address'],
]

function CheckoutSteps({ step }) {
  return <ol className="checkout-steps" aria-label="Tiến trình đặt hàng">
    {['Thông tin nhận hàng', 'Kiểm tra & đặt hàng', 'Hoàn tất'].map((label, index) => <li key={label}
      className={index < step ? 'is-complete' : index === step ? 'is-current' : ''} aria-current={index === step ? 'step' : undefined}>
      <span>{index < step ? <Check size={15} /> : String(index + 1).padStart(2, '0')}</span><strong>{label}</strong>
    </li>)}
  </ol>
}

export default function CheckoutPage() {
  const { cartVersion, demo, ready, syncing, refresh, items, totalAmount, totalCount } = useCart()
  const { user, loading: authLoading } = useAuth()
  const navigate = useNavigate()
  const customerIdentity = demo?.userId || user?.id || ''
  const [shipping, setShipping] = useState(() => shippingFromCustomer(user))
  const [quote, setQuote] = useState(null)
  const [created, setCreated] = useState(null)
  const [method, setMethod] = useState('COD')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [accountLoading, setAccountLoading] = useState(false)
  const [now, setNow] = useState(Date.now())
  const sending = useRef(false)
  const edited = useRef(new Set())
  const identityRevision = useRef(0)
  const heading = useRef(null)
  const province = selectedAddress(provinces, shipping.province)
  const districts = province?.districts || []
  const handleError = error => {
    if (error?.response?.status === 401) navigate('/login', { replace: true, state: { from: '/checkout' } })
    else setError(orderError(error))
  }
  useEffect(() => {
    let active = true
    edited.current = new Set()
    setQuote(null); setCreated(null); setError('')
    // Render the authenticated customer's information immediately; revalidate in the background.
    setShipping(shippingFromCustomer(user))
    if (authLoading || !user) return
    setAccountLoading(true)
    orderApi.account().then(({ data }) => {
      if (active) setShipping(value => mergeCustomerShipping(value, data, edited.current))
    }).catch(error => { if (active) handleError(error) })
      .finally(() => { if (active) setAccountLoading(false) })
    return () => { active = false; identityRevision.current++ }
  }, [customerIdentity, user, authLoading])
  useEffect(() => {
    if (!quote) return
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [quote?.id])
  useEffect(() => { setQuote(null) }, [cartVersion])
  useEffect(() => { heading.current?.focus() }, [quote?.id, created?.id])
  const remaining = quote ? Math.max(0, Math.ceil((new Date(quote.expiresAt).getTime() - now) / 1000)) : 0
  const preview = async event => {
    event.preventDefault()
    if (sending.current || syncing || !ready || !items.length) return
    if (!province || !selectedAddress(districts, shipping.district)) {
      setError('Vui lòng chọn tỉnh / thành phố và quận / huyện từ danh sách.'); return
    }
    sending.current = true; setBusy(true); setError('')
    const revision = identityRevision.current
    try {
      const { data } = await orderApi.quote({ cartVersion, shipping })
      if (revision === identityRevision.current) { setQuote(data); setNow(Date.now()); window.scrollTo(0, 0) }
    } catch (error) {
      if (revision === identityRevision.current) { handleError(error); if (error?.response?.status !== 401) void refresh() }
    } finally { sending.current = false; setBusy(false) }
  }
  const place = async () => {
    if (sending.current || !quote || !remaining || quote.cartVersion !== cartVersion) return
    sending.current = true; setBusy(true); setError('')
    const revision = identityRevision.current
    try {
      // Retain the same key if a response is lost and the customer retries.
      const storageKey = `order-key:${quote.id}:${method}`
      const key = sessionStorage.getItem(storageKey) || crypto.randomUUID()
      sessionStorage.setItem(storageKey, key)
      const { data } = await orderApi.place({ quoteId: quote.id, paymentMethod: method }, key)
      if (revision !== identityRevision.current) return
      setCreated(data); void refresh(); window.scrollTo(0, 0)
    } catch (error) { if (revision === identityRevision.current) handleError(error) }
    finally { sending.current = false; setBusy(false) }
  }
  const step = created ? 2 : quote ? 1 : 0
  return <div className="orders-page checkout-page">
    <CartDemoPanel />
    <nav className="checkout-nav"><Link to="/cart"><ArrowLeft size={16} /> Giỏ hàng</Link><span><ShieldCheck size={16} /> Mua sắm an tâm</span></nav>
    <header className="checkout-hero">
      <div><p className="checkout-eyebrow">PIXELCORE / CHECKOUT</p><h1>Hoàn thiện góc máy của bạn.</h1><p>Chỉ vài bước nữa, đơn hàng sẽ sẵn sàng.</p></div>
      <ShoppingBag size={48} strokeWidth={1.2} aria-hidden="true" />
    </header>
    <CheckoutSteps step={step} />
    {error && <p className="order-error" role="alert">{error}</p>}
    {created ? <section className="checkout-panel order-success" aria-labelledby="order-success-title">
      <div className="order-success__icon"><CheckCircle2 size={34} aria-hidden="true" /></div>
      <p className="checkout-eyebrow">CẢM ƠN BẠN ĐÃ CHỌN PIXELCORE</p>
      <h2 id="order-success-title" ref={heading} tabIndex={-1}>Đặt hàng thành công!</h2>
      <p>Chúng mình đã nhận được đơn hàng của bạn.</p>
      <OrderReference code={created.code} />
      <div className="checkout-delivery"><MapPin size={21} aria-hidden="true" /><div><strong>{created.recipient}</strong><span>{created.phone}</span><p>{created.address}</p></div></div>
      <div className="checkout-total"><span>Tổng thanh toán</span><strong>{formatVND(Number(created.total))}</strong></div>
      <p className="checkout-note">{created.status === 'PENDING_PAYMENT' ? 'Đơn hàng đang chờ thanh toán. Tiếp tục với VNPAY để hoàn tất.' : 'Cửa hàng sẽ liên hệ xác nhận và chuẩn bị giao hàng cho bạn.'}</p>
      <div className="checkout-footer">
        {created.status === 'PENDING_PAYMENT' && created.paymentUrl && <a className="store-button" href={created.paymentUrl}>Thanh toán VNPAY <ArrowRight size={16} /></a>}
        <Link className="store-button" to="/orders">Theo dõi đơn hàng <ArrowRight size={16} /></Link>
        <Link className="checkout-back" to="/catalog">Tiếp tục mua sắm</Link>
      </div>
    </section> : ready && !items.length ? <section className="checkout-panel checkout-empty"><Package size={40} /><h2>Giỏ hàng đang trống</h2><Link className="store-button" to="/catalog">Khám phá sản phẩm</Link></section> : !quote ?
    <form className="checkout-panel" onSubmit={preview} aria-busy={busy}>
      <div className="checkout-section-title"><span><UserRound size={21} /></span><div><p>BƯỚC 01</p><h2 ref={heading} tabIndex={-1}>Thông tin nhận hàng</h2></div></div>
      <p className="checkout-note">Kiểm tra thông tin của bạn và bổ sung địa chỉ giao hàng.</p>
      {accountLoading && !user && <p className="checkout-status" role="status">Đang lấy thông tin tài khoản…</p>}
      <fieldset disabled={busy || authLoading} className="checkout-fields">
        {fields.map(([key, label, placeholder, maxLength, autoComplete]) => key === 'province' ?
          <AddressSelect key={`province:${customerIdentity}`} label={label} placeholder="Gõ để tìm tỉnh / thành phố" options={provinces}
            value={shipping.province} disabled={busy || authLoading} onChange={value => setShipping(previous => changeProvince(previous, value))} /> : key === 'district' ?
          <AddressSelect key={`district:${customerIdentity}:${shipping.province}`} label={label}
            placeholder={province ? 'Gõ để tìm quận / huyện' : placeholder} options={districts} value={shipping.district} disabled={!province || busy || authLoading}
            onChange={value => setShipping(previous => changeDistrict(previous, value))} /> :
          <label key={key} className={key === 'addressLine' ? 'checkout-field--wide' : ''}>
          {label}<span aria-hidden="true"> *</span>
          <input required maxLength={maxLength} minLength={key === 'addressLine' ? 5 : key === 'recipient' ? 2 : undefined}
            type={key === 'phone' ? 'tel' : 'text'} autoComplete={autoComplete} placeholder={placeholder} value={shipping[key]}
            onChange={event => { edited.current.add(key); setShipping(value => ({ ...value, [key]: event.target.value })) }} />
        </label>)}
      </fieldset>
      <p className="checkout-help">Danh sách tỉnh và quận/huyện theo địa giới trước tháng 7/2025.</p>
      <p className="checkout-help"><ShieldCheck size={15} /> Sử dụng số điện thoại đã xác minh của tài khoản.</p>
      <div className="checkout-mini-summary"><span><Package size={19} /> {totalCount} sản phẩm</span><div><span>Tạm tính</span><strong>{formatVND(Number(totalAmount))}</strong></div></div>
      <div className="checkout-footer"><Link className="checkout-back" to="/cart"><ArrowLeft size={16} /> Quay lại giỏ hàng</Link>
        <button className="store-button" disabled={busy || syncing || authLoading || !ready || !items.length} type="submit">Xác nhận <ArrowRight size={17} /></button></div>
      {syncing && !busy && <p className="checkout-status" role="status">Đang lưu giỏ hàng…</p>}
      {busy && <p className="checkout-status" role="status">Đang xác nhận thông tin và tồn kho…</p>}
    </form> :
    <section className="checkout-panel" aria-labelledby="confirmation-title" aria-busy={busy}>
      <div className="checkout-section-title"><span><ShieldCheck size={21} /></span><div><p>BƯỚC 02</p><h2 id="confirmation-title" ref={heading} tabIndex={-1}>Kiểm tra đơn hàng</h2></div></div>
      <p className="checkout-note">Mọi thứ đã sẵn sàng. Kiểm tra lần cuối trước khi đặt hàng.</p>
      <div className="checkout-review-grid">
        <div className="checkout-delivery"><MapPin size={21} aria-hidden="true" /><div><small>GIAO ĐẾN</small><strong>{quote.recipient}</strong><span>{quote.phone}</span><p>{quote.address}</p></div></div>
        <fieldset disabled={busy} className="checkout-payment"><legend><CreditCard size={17} /> Thanh toán</legend>
          {[['COD', 'Thanh toán khi nhận hàng', 'Trả tiền khi giao thành công'], ['VNPAY', 'VNPAY', 'Thanh toán trực tuyến · Sandbox']].map(([value, label, note]) =>
            <label className={method === value ? 'is-selected' : ''} key={value}><input type="radio" name="payment" value={value} checked={method === value} onChange={() => setMethod(value)} /><span><strong>{label}</strong><small>{note}</small></span></label>)}
        </fieldset>
      </div>
      <div className="checkout-items"><div className="checkout-items__heading"><h3>Sản phẩm của bạn</h3><span>{quote.items.reduce((sum, item) => sum + item.quantity, 0)} sản phẩm</span></div>
        {quote.items.map(item => <div className="checkout-item" key={item.productId}>
          {items.find(product => product.id === item.productId)?.image ? <img src={items.find(product => product.id === item.productId).image} alt="" width="56" height="56" /> : <span className="checkout-item__placeholder"><Package size={24} /></span>}
          <div><strong>{item.name}</strong><span>Số lượng: {item.quantity}</span></div><strong>{formatVND(Number(item.unitPrice) * item.quantity)}</strong>
        </div>)}
      </div>
      <div className="checkout-totals"><div><span>Tạm tính</span><span>{formatVND(Number(quote.subtotal))}</span></div><div><span><Truck size={16} /> Phí vận chuyển</span><span>{formatVND(Number(quote.shippingFee))}</span></div></div>
      <div className="checkout-total"><span>Tổng thanh toán</span><strong>{formatVND(Number(quote.total))}</strong></div>
      <p className="checkout-help">{remaining ? 'Thông tin đã được xác nhận. Bạn có thể đặt hàng.' : 'Thông tin đã hết hạn. Vui lòng quay lại và xác nhận lại.'}</p>
      <div className="checkout-footer"><button className="checkout-back" type="button" disabled={busy} onClick={() => { setQuote(null); setError('') }}><ArrowLeft size={16} /> Chỉnh sửa thông tin</button>
        <button className="store-button" type="button" onClick={place} disabled={busy || !remaining || quote.cartVersion !== cartVersion}>Đặt hàng <ArrowRight size={17} /></button></div>
      {busy && <p className="checkout-status" role="status">Đang tạo đơn hàng…</p>}
    </section>}
    <p className="checkout-assurance"><ShieldCheck size={15} /> Thông tin của bạn chỉ được sử dụng để xử lý và giao đơn hàng.</p>
  </div>
}
