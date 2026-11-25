import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  ArrowLeft, ArrowRight, Banknote, Check, CheckCircle2, CreditCard,
  MapPin, Package, ShieldCheck, ShoppingBag, Tag, Truck, UserRound, AlertTriangle,
} from 'lucide-react'
import { useCart } from '../contexts/cartContextStore'
import { useAuth } from '../contexts/authContextStore'
import CartDemoPanel from '../components/CartDemoPanel'
import OrderReference from '../components/OrderReference'
import AddressSelect from '../components/AddressSelect'
import provinces from '../data/vietnamDistricts.json'
import wardsByDistrict from '../data/vietnamWards.json'
import { selectedAddress, changeProvince, changeDistrict } from '../utils/addresses'
import { orderApi, orderError, paymentMethodLabels, shippingMethodLabels } from '../services/orders'
import { formatVND } from '../utils/cart'
import {
  SELECTED_CART_STORAGE_KEY,
  administrativeAddressError,
  calculateSelectedPreview,
  invalidatesQuote,
  isReconfirmationError,
  mergeCustomerShipping,
  placeOrderWithTimeoutCheck,
  shippingFromCustomer,
} from '../utils/checkout'
import '../styles/orders.css'

const fields = [
  ['recipient', 'Họ và tên người nhận', 'Nhập họ và tên', 100, 'name'],
  ['phone', 'Số điện thoại nhận hàng', 'Ví dụ: 0912345678', 16, 'tel'],
  ['province', 'Tỉnh / thành phố', 'Ví dụ: TP. Hồ Chí Minh', 100, 'address-level1'],
  ['district', 'Quận / huyện', 'Chọn tỉnh / thành phố trước', 100, 'address-level2'],
  ['ward', 'Phường / xã', 'Nhập phường hoặc xã', 100, 'address-level3'],
  ['addressLine', 'Địa chỉ nhận hàng', 'Số nhà, tên đường, tòa nhà…', 255, 'street-address'],
]

const DEFAULT_SHIPPING_OPTIONS = [
  { method: 'EXPRESS', label: 'Vận chuyển nhanh', description: 'Giao hàng tiêu chuẩn toàn quốc (2–4 ngày)', fee: 50000, available: true },
  { method: 'INSTANT', label: 'Ship hỏa tốc 2h', description: 'Áp dụng nội thành TP.HCM & Hà Nội', fee: 80000, available: false, unavailableReason: 'Chọn TP. Hồ Chí Minh hoặc Hà Nội để dùng Ship hỏa tốc.' },
  { method: 'STORE_PICKUP', label: 'Nhận tại cửa hàng', description: 'Giữ hàng và nhận trực tiếp tại showroom PixelCore', fee: 0, available: true },
]

function readInitialSelectedIds(locationState, items) {
  if (Array.isArray(locationState?.selectedProductIds) && locationState.selectedProductIds.length > 0) {
    return locationState.selectedProductIds
  }
  try {
    const raw = sessionStorage.getItem(SELECTED_CART_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) return parsed
    }
  } catch { /* ignore */ }
  return items.filter(item => item.availableQuantity !== 0).map(item => item.id)
}

function CheckoutSteps({ step }) {
  return (
    <ol className="checkout-steps" aria-label="Tiến trình đặt hàng">
      {['Chọn món & giao hàng', 'Kiểm tra & xác nhận', 'Hoàn tất'].map((label, index) => (
        <li
          key={label}
          className={index < step ? 'is-complete' : index === step ? 'is-current' : ''}
          aria-current={index === step ? 'step' : undefined}
        >
          <span>{index < step ? <Check size={15} /> : String(index + 1).padStart(2, '0')}</span>
          <strong>{label}</strong>
        </li>
      ))}
    </ol>
  )
}

export default function CheckoutPage() {
  const { cartVersion, demo, ready, syncing, refresh, items } = useCart()
  const { user, loading: authLoading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const customerIdentity = demo?.userId || user?.id || ''

  const [selectedIds, setSelectedIds] = useState(() => readInitialSelectedIds(location.state, items))
  const [shipping, setShipping] = useState(() => shippingFromCustomer(user))
  const [shippingMethod, setShippingMethod] = useState('EXPRESS')
  const [shippingOptions, setShippingOptions] = useState(DEFAULT_SHIPPING_OPTIONS)
  const [voucherCode, setVoucherCode] = useState('')
  const [availableVouchers, setAvailableVouchers] = useState([])
  const [paymentMethod, setPaymentMethod] = useState('COD')
  const [quote, setQuote] = useState(null)
  const [created, setCreated] = useState(null)
  const [error, setError] = useState('')
  const [reconfirmNotice, setReconfirmNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [accountLoading, setAccountLoading] = useState(false)
  const [now, setNow] = useState(Date.now())

  const sending = useRef(false)
  const edited = useRef(new Set())
  const identityRevision = useRef(0)
  const heading = useRef(null)
  const initializedSelection = useRef(false)
  const timedOutKeys = useRef(new Set())

  const province = selectedAddress(provinces, shipping.province)
  const districts = province?.districts || []
  const selectedDistrict = province ? selectedAddress(districts, shipping.district) : undefined
  const wards = selectedDistrict ? (wardsByDistrict[selectedDistrict.code] || []) : []
  const selectedWard = wards.length ? selectedAddress(wards, shipping.ward) : undefined

  useEffect(() => {
    if (!ready) return
    const validIds = new Set(items.map(item => item.id))
    setSelectedIds(prev => {
      if (!initializedSelection.current && (!prev || prev.length === 0)) {
        initializedSelection.current = true
        return readInitialSelectedIds(location.state, items).filter(id => validIds.has(id))
      }
      initializedSelection.current = true
      return prev.filter(id => validIds.has(id))
    })
  }, [ready, items, location.state])

  const handleError = useCallback(err => {
    if (err?.response?.status === 401) {
      navigate('/login', { replace: true, state: { from: '/checkout' } })
    } else {
      setError(orderError(err))
    }
  }, [navigate])

  useEffect(() => {
    let active = true
    edited.current = new Set()
    setQuote(null)
    setCreated(null)
    setError('')
    setReconfirmNotice('')
    setShipping(shippingFromCustomer(user))
    if (authLoading || (!user && !demo?.userId)) return
    setAccountLoading(true)
    orderApi.account()
      .then(({ data }) => {
        if (active) setShipping(value => mergeCustomerShipping(value, data, edited.current))
      })
      .catch(err => { if (active) handleError(err) })
      .finally(() => { if (active) setAccountLoading(false) })

    orderApi.vouchers()
      .then(({ data }) => { if (active && Array.isArray(data)) setAvailableVouchers(data) })
      .catch(() => { /* vouchers optional */ })

    return () => { active = false; identityRevision.current++ }
  }, [customerIdentity, user, demo?.userId, authLoading, handleError])

  // Fetch server-authoritative shipping options; zones are resolved by official administrative codes
  useEffect(() => {
    let active = true
    if (!user && !demo?.userId) return undefined
    orderApi.shippingOptions(shipping.province, shipping.district)
      .then(({ data }) => {
        if (!active || !Array.isArray(data) || !data.length) return
        setShippingOptions(data)
        const currentOpt = data.find(opt => opt.method === shippingMethod)
        if (currentOpt && !currentOpt.available) {
          const fallback = data.find(opt => opt.available)
          if (fallback) setShippingMethod(fallback.method)
        }
      })
      .catch(() => { /* keep fallback options */ })
    return () => { active = false }
  }, [shipping.province, shipping.district, user, demo?.userId, shippingMethod])

  useEffect(() => {
    if (!quote) return undefined
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [quote?.id])

  useEffect(() => {
    heading.current?.focus()
  }, [quote?.id, created?.id])

  const previewSelection = useMemo(
    () => calculateSelectedPreview(items, selectedIds),
    [items, selectedIds],
  )

  const toggleSelectedProduct = id => {
    setError('')
    setSelectedIds(prev => {
      const next = prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
      try { sessionStorage.setItem(SELECTED_CART_STORAGE_KEY, JSON.stringify(next)) } catch { /* ignore */ }
      return next
    })
  }

  const remaining = quote ? Math.max(0, Math.ceil((new Date(quote.expiresAt).getTime() - now) / 1000)) : 0

  const requestServerQuote = useCallback(async ({ overrideVoucherCode } = {}) => {
    const effectiveVoucher = overrideVoucherCode !== undefined ? overrideVoucherCode : voucherCode
    const payload = {
      cartVersion,
      selectedProductIds: selectedIds,
      shipping,
      shippingMethod,
      voucherCode: effectiveVoucher.trim() || null,
      paymentMethod,
    }
    const { data } = await orderApi.quote(payload)
    return data
  }, [cartVersion, selectedIds, shipping, shippingMethod, voucherCode, paymentMethod])

  const preview = async event => {
    event.preventDefault()
    if (sending.current || syncing || !ready) return
    if (previewSelection.selectedItems.length === 0) {
      setError('Vui lòng chọn ít nhất một sản phẩm cần mua từ giỏ hàng.')
      return
    }
    const addressError = administrativeAddressError({
      province, district: selectedDistrict, ward: selectedWard, wards,
    })
    if (addressError) {
      setError(addressError)
      return
    }
    sending.current = true
    setBusy(true)
    setError('')
    setReconfirmNotice('')
    const revision = identityRevision.current
    try {
      const data = await requestServerQuote()
      if (revision === identityRevision.current) {
        setQuote(data)
        setNow(Date.now())
        window.scrollTo(0, 0)
      }
    } catch (err) {
      if (revision === identityRevision.current) {
        handleError(err)
        if (err?.response?.status !== 401) void refresh()
      }
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  const place = async () => {
    if (sending.current || !quote || !remaining) return
    sending.current = true
    setBusy(true)
    setError('')
    const revision = identityRevision.current
    const storageKey = `order-key:${quote.id}:${paymentMethod}`
    const idempotencyKey = sessionStorage.getItem(storageKey) || crypto.randomUUID()
    sessionStorage.setItem(storageKey, idempotencyKey)

    try {
      const { order, recoveredFromTimeout } = await placeOrderWithTimeoutCheck({
        quoteId: quote.id,
        paymentMethod,
        idempotencyKey,
        hadPriorTimeout: timedOutKeys.current.has(idempotencyKey),
        placeRequest: orderApi.place,
        checkIdempotency: orderApi.checkIdempotency,
      })
      timedOutKeys.current.delete(idempotencyKey)
      try { sessionStorage.removeItem(storageKey) } catch { /* ignore */ }
      if (revision !== identityRevision.current) return
      setCreated(order)
      setReconfirmNotice(recoveredFromTimeout
        ? 'Kết nối bị gián đoạn nhưng đơn hàng đã được tạo thành công trước đó. Hệ thống không tạo thêm đơn trùng.'
        : '')
      // Remove only purchased IDs from stored selection, keeping unselected cart IDs
      const orderedSet = new Set(order.items.map(item => item.productId))
      const remainingSelected = selectedIds.filter(id => !orderedSet.has(id))
      setSelectedIds(remainingSelected)
      try { sessionStorage.setItem(SELECTED_CART_STORAGE_KEY, JSON.stringify(remainingSelected)) } catch { /* ignore */ }
      await refresh()
      window.scrollTo(0, 0)
    } catch (err) {
      if (revision !== identityRevision.current) return
      if (err?.canRetrySameKey) {
        // Keep the same key: the next click checks the backend result before re-sending.
        timedOutKeys.current.add(idempotencyKey)
        setError('Không nhận được phản hồi từ máy chủ. Bấm "Xác nhận đặt hàng" lần nữa: hệ thống sẽ kiểm tra đơn đã tạo trước khi gửi lại, không tạo đơn trùng.')
      } else if (isReconfirmationError(err)) {
        const detailMessage = orderError(err)
        const errData = err?.response?.data || {}
        const invalidVoucher = Boolean(errData.invalidVoucherCode)
        if (invalidVoucher) {
          setVoucherCode('')
        }
        try { sessionStorage.removeItem(storageKey) } catch { /* ignore */ }
        // Use the freshly loaded cart version instead of the stale render closure.
        const freshCart = await refresh()
        try {
          const { data } = await orderApi.quote({
            cartVersion: freshCart?.cartVersion ?? errData.cartVersion ?? cartVersion,
            selectedProductIds: selectedIds,
            shipping,
            shippingMethod,
            voucherCode: invalidVoucher ? null : (voucherCode.trim() || null),
            paymentMethod,
          })
          setQuote(data)
          setNow(Date.now())
          setReconfirmNotice(detailMessage)
        } catch (reQuoteErr) {
          // Re-quote failed (e.g. stock ran out): go back to step 1 and show the real reason.
          setQuote(null)
          setReconfirmNotice(detailMessage)
          handleError(reQuoteErr)
        }
      } else {
        handleError(err)
        if (invalidatesQuote(err)) {
          try { sessionStorage.removeItem(storageKey) } catch { /* ignore */ }
          setQuote(null)
          void refresh()
        }
      }
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  const step = created ? 2 : quote ? 1 : 0
  const unselectedCount = Math.max(0, items.length - previewSelection.selectedSkuCount)

  return (
    <div className="orders-page checkout-page">
      <CartDemoPanel />
      <nav className="checkout-nav">
        <Link to="/cart"><ArrowLeft size={16} /> Giỏ hàng</Link>
        <span><ShieldCheck size={16} /> Mua sắm an tâm</span>
      </nav>
      <header className="checkout-hero">
        <div>
          <p className="checkout-eyebrow">PIXELCORE / CHECKOUT</p>
          <h1>Hoàn thiện đơn hàng của bạn.</h1>
          <p>Chọn sản phẩm cần mua, phương thức vận chuyển và ưu đãi phù hợp.</p>
        </div>
        <ShoppingBag size={48} strokeWidth={1.2} aria-hidden="true" />
      </header>
      <CheckoutSteps step={step} />

      {reconfirmNotice && (
        <div className="checkout-reconfirm-banner" role="alert">
          <AlertTriangle size={20} aria-hidden="true" />
          <div>
            <strong>Cần xác nhận lại thông tin đơn hàng</strong>
            <p>{reconfirmNotice}</p>
          </div>
        </div>
      )}
      {error && <p className="order-error" role="alert">{error}</p>}

      {created ? (
        <section className="checkout-panel order-success" aria-labelledby="order-success-title">
          <div className="order-success__icon"><CheckCircle2 size={34} aria-hidden="true" /></div>
          <p className="checkout-eyebrow">CẢM ƠN BẠN ĐÃ CHỌN PIXELCORE</p>
          <h2 id="order-success-title" ref={heading} tabIndex={-1}>Đặt hàng thành công!</h2>
          <p>
            {created.paymentMethod === 'VNPAY'
              ? 'Đơn hàng đã được khởi tạo ở trạng thái Chờ thanh toán và giữ tồn kho cho bạn.'
              : 'Chúng mình đã ghi nhận đơn hàng và giữ tồn kho cho các sản phẩm bạn đặt.'}
          </p>
          <OrderReference code={created.code} />
          <div className="checkout-delivery">
            <MapPin size={21} aria-hidden="true" />
            <div>
              <strong>{created.recipient}</strong>
              <span>{created.phone} · {shippingMethodLabels[created.shippingMethod] || created.shippingMethod}</span>
              <p>{created.address}</p>
            </div>
          </div>
          <div className="checkout-totals">
            <div><span>Tạm tính ({created.items.reduce((s, i) => s + i.quantity, 0)} sản phẩm)</span><span>{formatVND(Number(created.subtotal))}</span></div>
            <div><span>Phí vận chuyển</span><span>{formatVND(Number(created.shippingFee))}</span></div>
            {Number(created.discountAmount) > 0 && (
              <div className="checkout-discount-row">
                <span>Giảm giá ({created.voucherCode})</span>
                <strong>−{formatVND(Number(created.discountAmount))}</strong>
              </div>
            )}
            <div><span>Phương thức thanh toán</span><span>{paymentMethodLabels[created.paymentMethod] || created.paymentMethod}</span></div>
          </div>
          <div className="checkout-total">
            <span>Tổng thanh toán</span>
            <strong>{formatVND(Number(created.total))}</strong>
          </div>
          {items.length > 0 && (
            <p className="checkout-note">
              {items.length} mặt hàng chưa chọn mua vẫn được giữ nguyên trong giỏ hàng của bạn.
            </p>
          )}
          <div className="checkout-footer">
            <Link className="store-button" to="/orders">Theo dõi đơn hàng <ArrowRight size={16} /></Link>
            <Link className="checkout-back" to="/cart">Về giỏ hàng</Link>
          </div>
        </section>
      ) : ready && !items.length ? (
        <section className="checkout-panel checkout-empty">
          <Package size={40} />
          <h2>Giỏ hàng đang trống</h2>
          <Link className="store-button" to="/catalog">Khám phá sản phẩm</Link>
        </section>
      ) : !quote ? (
        <form className="checkout-panel" onSubmit={preview} aria-busy={busy}>
          <div className="checkout-section-title">
            <span><ShoppingBag size={21} /></span>
            <div><p>BƯỚC 01</p><h2 ref={heading} tabIndex={-1}>Sản phẩm chọn mua & Giao hàng</h2></div>
          </div>
          <p className="checkout-note">
            Chọn những sản phẩm bạn muốn đặt trong lần này. Các sản phẩm bỏ chọn sẽ được giữ lại trong giỏ hàng.
          </p>

          <div className="checkout-selection-list" role="group" aria-label="Sản phẩm trong giỏ hàng">
            {items.map(item => {
              const checked = selectedIds.includes(item.id)
              const soldOut = item.availableQuantity === 0
              const overStock = !soldOut && item.availableQuantity != null && item.quantity > item.availableQuantity
              return (
                <label key={item.id} className={`checkout-select-item${checked ? ' is-selected' : ''}${soldOut ? ' is-disabled' : ''}`}>
                  <input
                    type="checkbox"
                    checked={checked}
                    disabled={busy || soldOut}
                    onChange={() => toggleSelectedProduct(item.id)}
                  />
                  {item.image ? (
                    <img src={item.image} alt="" width="48" height="48" />
                  ) : (
                    <span className="checkout-item__placeholder"><Package size={20} /></span>
                  )}
                  <div className="checkout-select-item__info">
                    <strong>{item.name}</strong>
                    <span>
                      Số lượng: {item.quantity} · Đơn giá: {formatVND(item.price)}
                      {soldOut ? ' · Hết hàng' : overStock ? ` · Chỉ còn ${item.availableQuantity} sp` : ''}
                    </span>
                  </div>
                  <strong>{formatVND(item.price * item.quantity)}</strong>
                </label>
              )
            })}
          </div>

          <div className="checkout-subsection">
            <h3><UserRound size={17} /> Thông tin người nhận & Địa chỉ</h3>
            {accountLoading && !user && <p className="checkout-status" role="status">Đang lấy thông tin tài khoản…</p>}
            <fieldset disabled={busy || authLoading} className="checkout-fields">
              {fields.map(([key, label, placeholder, maxLength, autoComplete]) => key === 'province' ? (
                <AddressSelect
                  key={`province:${customerIdentity}`}
                  label={label}
                  placeholder="Gõ để tìm tỉnh / thành phố"
                  options={provinces}
                  value={shipping.province}
                  disabled={busy || authLoading}
                  onChange={value => setShipping(previous => changeProvince(previous, value))}
                />
              ) : key === 'district' ? (
                <AddressSelect
                  key={`district:${customerIdentity}:${shipping.province}`}
                  label={label}
                  placeholder={province ? 'Gõ để tìm quận / huyện' : placeholder}
                  options={districts}
                  value={shipping.district}
                  disabled={!province || busy || authLoading}
                  onChange={value => setShipping(previous => changeDistrict(previous, value))}
                />
              ) : key === 'ward' && wards.length > 0 ? (
                <AddressSelect
                  key={`ward:${customerIdentity}:${shipping.province}:${shipping.district}`}
                  label={label}
                  placeholder="Gõ để tìm phường / xã"
                  options={wards}
                  value={shipping.ward}
                  disabled={!selectedDistrict || busy || authLoading}
                  onChange={value => setShipping(previous => ({ ...previous, ward: value }))}
                />
              ) : key === 'ward' && !selectedDistrict ? (
                <AddressSelect
                  key={`ward-empty:${customerIdentity}:${shipping.province}`}
                  label={label}
                  placeholder="Chọn quận / huyện trước"
                  options={[]}
                  value=""
                  disabled
                  onChange={() => {}}
                />
              ) : (
                <label key={key} className={key === 'addressLine' ? 'checkout-field--wide' : ''}>
                  {label}<span aria-hidden="true"> *</span>
                  <input
                    required
                    maxLength={maxLength}
                    minLength={key === 'addressLine' ? 5 : key === 'recipient' ? 2 : undefined}
                    type={key === 'phone' ? 'tel' : 'text'}
                    autoComplete={autoComplete}
                    placeholder={placeholder}
                    value={shipping[key]}
                    onChange={event => {
                      edited.current.add(key)
                      setShipping(value => ({ ...value, [key]: event.target.value }))
                    }}
                  />
                </label>
              ))}
            </fieldset>
          </div>

          <div className="checkout-subsection">
            <h3><Truck size={17} /> Phương thức vận chuyển</h3>
            <div className="checkout-option-grid" role="radiogroup" aria-label="Phương thức vận chuyển">
              {shippingOptions.map(option => (
                <label
                  key={option.method}
                  className={`checkout-option-card${shippingMethod === option.method ? ' is-selected' : ''}${!option.available ? ' is-disabled' : ''}`}
                >
                  <input
                    type="radio"
                    name="shippingMethod"
                    value={option.method}
                    checked={shippingMethod === option.method}
                    disabled={busy || !option.available}
                    onChange={() => setShippingMethod(option.method)}
                  />
                  <div>
                    <strong>{option.label} — {option.fee === 0 ? 'Miễn phí' : formatVND(Number(option.fee))}</strong>
                    <small>{option.available ? option.description : option.unavailableReason}</small>
                  </div>
                </label>
              ))}
            </div>
          </div>

          <div className="checkout-subsection">
            <h3><Tag size={17} /> Mã giảm giá (Voucher)</h3>
            <div className="checkout-voucher-box">
              <input
                type="text"
                maxLength={50}
                placeholder="Nhập mã giảm giá (VD: PIXEL50K, PIXEL5)"
                value={voucherCode}
                disabled={busy}
                onChange={event => setVoucherCode(event.target.value.toUpperCase())}
              />
              {voucherCode && (
                <button type="button" className="checkout-voucher-clear" onClick={() => setVoucherCode('')}>
                  Bỏ mã
                </button>
              )}
            </div>
            {availableVouchers.length > 0 && (
              <div className="checkout-voucher-chips">
                {availableVouchers.map(v => {
                  const eligible = previewSelection.selectedSubtotal >= Number(v.minOrderSubtotal)
                  return (
                    <button
                      type="button"
                      key={v.code}
                      disabled={busy || !eligible}
                      className={`checkout-voucher-chip${voucherCode === v.code ? ' is-active' : ''}`}
                      onClick={() => setVoucherCode(voucherCode === v.code ? '' : v.code)}
                      title={v.description}
                    >
                      <strong>{v.code}</strong> · {v.description}
                    </button>
                  )
                })}
              </div>
            )}
          </div>

          <div className="checkout-subsection">
            <h3><CreditCard size={17} /> Phương thức thanh toán</h3>
            <div className="checkout-option-grid" role="radiogroup" aria-label="Phương thức thanh toán">
              <label className={`checkout-option-card${paymentMethod === 'COD' ? ' is-selected' : ''}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value="COD"
                  checked={paymentMethod === 'COD'}
                  disabled={busy}
                  onChange={() => setPaymentMethod('COD')}
                />
                <div>
                  <strong>Thanh toán khi nhận hàng (COD)</strong>
                  <small>Kiểm tra và thanh toán tiền mặt khi nhận hàng.</small>
                </div>
              </label>
              <label className={`checkout-option-card${paymentMethod === 'VNPAY' ? ' is-selected' : ''}`}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value="VNPAY"
                  checked={paymentMethod === 'VNPAY'}
                  disabled={busy}
                  onChange={() => setPaymentMethod('VNPAY')}
                />
                <div>
                  <strong>Thanh toán trực tuyến (VNPay)</strong>
                  <small>Tạo đơn giữ hàng và chuyển sang bước thanh toán trực tuyến.</small>
                </div>
              </label>
            </div>
          </div>

          <div className="checkout-mini-summary">
            <span>
              <Package size={19} /> Đã chọn {previewSelection.selectedQuantity} sản phẩm
              {unselectedCount > 0 ? ` (giữ lại ${unselectedCount} mặt hàng trong giỏ)` : ''}
            </span>
            <div>
              <span>Tạm tính</span>
              <strong>{formatVND(previewSelection.selectedSubtotal)}</strong>
            </div>
          </div>

          <div className="checkout-footer">
            <Link className="checkout-back" to="/cart"><ArrowLeft size={16} /> Quay lại giỏ hàng</Link>
            <button
              className="store-button"
              disabled={busy || syncing || authLoading || !ready || previewSelection.selectedItems.length === 0}
              type="submit"
            >
              Tính tổng tiền & Tiếp tục <ArrowRight size={17} />
            </button>
          </div>
          {syncing && !busy && <p className="checkout-status" role="status">Đang đồng bộ giỏ hàng…</p>}
          {busy && <p className="checkout-status" role="status">Backend đang kiểm tra tồn kho, phí vận chuyển và tính tổng tiền…</p>}
        </form>
      ) : (
        <section className="checkout-panel" aria-labelledby="confirmation-title" aria-busy={busy}>
          <div className="checkout-section-title">
            <span><ShieldCheck size={21} /></span>
            <div><p>BƯỚC 02</p><h2 id="confirmation-title" ref={heading} tabIndex={-1}>Kiểm tra & Xác nhận đặt hàng</h2></div>
          </div>
          <p className="checkout-note">
            Tổng tiền dưới đây được xác nhận trực tiếp từ hệ thống. Khi bấm Đặt hàng, hệ thống sẽ kiểm tra lại giá, tồn kho và voucher lần cuối.
          </p>

          <div className="checkout-review-grid">
            <div className="checkout-delivery">
              <MapPin size={21} aria-hidden="true" />
              <div>
                <small>GIAO ĐẾN · {shippingMethodLabels[quote.shippingMethod] || quote.shippingMethod}</small>
                <strong>{quote.recipient}</strong>
                <span>{quote.phone}</span>
                <p>{quote.address}</p>
              </div>
            </div>
            <section className="checkout-payment" aria-label="Phương thức thanh toán">
              <h3 className="checkout-payment__title"><Banknote size={17} /> Phương thức thanh toán</h3>
              <div className="checkout-payment__summary">
                <CheckCircle2 size={20} aria-hidden="true" />
                <div>
                  <strong>{paymentMethodLabels[paymentMethod] || paymentMethod}</strong>
                  <small>
                    {paymentMethod === 'VNPAY'
                      ? 'Sau khi tạo đơn, đơn hàng sẽ ở trạng thái Chờ thanh toán để tiến hành thanh toán.'
                      : 'Nhận hàng và thanh toán trực tiếp cho đơn vị vận chuyển.'}
                  </small>
                </div>
              </div>
            </section>
          </div>

          <div className="checkout-items">
            <div className="checkout-items__heading">
              <h3>Sản phẩm đặt mua</h3>
              <span>{quote.items.reduce((sum, item) => sum + item.quantity, 0)} sản phẩm</span>
            </div>
            {quote.items.map(item => (
              <div className="checkout-item" key={item.productId}>
                {items.find(product => product.id === item.productId)?.image ? (
                  <img src={items.find(product => product.id === item.productId).image} alt="" width="56" height="56" />
                ) : (
                  <span className="checkout-item__placeholder"><Package size={24} /></span>
                )}
                <div>
                  <strong>{item.name}</strong>
                  <span>Mã SKU: {item.sku} · Đơn giá: {formatVND(Number(item.unitPrice))} × {item.quantity}</span>
                </div>
                <strong>{formatVND(Number(item.unitPrice) * item.quantity)}</strong>
              </div>
            ))}
          </div>

          <div className="checkout-totals">
            <div>
              <span>Tạm tính</span>
              <span>{formatVND(Number(quote.subtotal))}</span>
            </div>
            <div>
              <span><Truck size={16} /> Phí vận chuyển ({shippingMethodLabels[quote.shippingMethod] || quote.shippingMethod})</span>
              <span>{Number(quote.shippingFee) === 0 ? 'Miễn phí' : formatVND(Number(quote.shippingFee))}</span>
            </div>
            {Number(quote.taxAmount) > 0 && (
              <div>
                <span>Thuế</span>
                <span>{formatVND(Number(quote.taxAmount))}</span>
              </div>
            )}
            {Number(quote.discountAmount) > 0 && (
              <div className="checkout-discount-row">
                <span><Tag size={15} /> Giảm giá ({quote.voucherCode})</span>
                <strong>−{formatVND(Number(quote.discountAmount))}</strong>
              </div>
            )}
          </div>
          <div className="checkout-total">
            <span>Tổng thanh toán</span>
            <strong>{formatVND(Number(quote.total))}</strong>
          </div>
          <p className="checkout-help">
            {remaining
              ? `Báo giá có hiệu lực trong ${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')}.`
              : 'Báo giá đã hết hạn. Vui lòng quay lại để tính lại tổng tiền.'}
          </p>
          <div className="checkout-footer">
            <button
              className="checkout-back"
              type="button"
              disabled={busy}
              onClick={() => { setQuote(null); setError(''); setReconfirmNotice('') }}
            >
              <ArrowLeft size={16} /> Chỉnh sửa thông tin / voucher
            </button>
            <button
              className="store-button"
              type="button"
              onClick={place}
              disabled={busy || !remaining}
            >
              {busy ? 'Đang xử lý đặt hàng…' : 'Xác nhận đặt hàng'} <ArrowRight size={17} />
            </button>
          </div>
          {busy && <p className="checkout-status" role="status">Đang kiểm tra lại giá, tồn kho, voucher và tạo đơn hàng…</p>}
        </section>
      )}
      <p className="checkout-assurance"><ShieldCheck size={15} /> Thông tin của bạn chỉ được sử dụng để xử lý và giao đơn hàng.</p>
    </div>
  )
}
