import React, { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import Breadcrumbs from '../components/Breadcrumbs'
import {
  CreditCard,
  Truck,
  ShieldCheck,
  Clock,
  AlertCircle,
  Loader2,
  ChevronRight,
  Package,
  Headphones,
  RotateCcw
} from 'lucide-react'
import { orderApi, orderError, shippingMethodLabels } from '../services/orders'
import { shortOrderCode } from '../utils/checkout'
import '../styles/payment.css'

export default function PaymentPage() {
  const { orderId } = useParams()
  const navigate = useNavigate()

  const [order, setOrder] = useState(null)
  const [fetching, setFetching] = useState(true)
  const [paymentMethod, setPaymentMethod] = useState('VNPAY')
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let active = true
    if (!orderId) {
      setFetching(false)
      setErrorMessage('Thiếu mã đơn hàng cần thanh toán.')
      return
    }
    setFetching(true)
    orderApi.get(orderId)
      .then(({ data }) => {
        if (!active) return
        setOrder(data)
        if (data?.paymentMethod === 'COD' || data?.paymentMethod === 'VNPAY') {
          setPaymentMethod(data.paymentMethod)
        }
        setErrorMessage('')
      })
      .catch(err => {
        if (!active) return
        if (err?.response?.status === 401) {
          navigate('/login', { replace: true, state: { from: `/checkout/payment/${orderId}` } })
          return
        }
        setErrorMessage(orderError(err))
      })
      .finally(() => {
        if (active) setFetching(false)
      })
    return () => { active = false }
  }, [orderId, navigate])

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(val) || 0)
  }

  const handlePayment = async () => {
    if (!order) return
    setErrorMessage('')
    setLoading(true)

    try {
      if (paymentMethod === 'VNPAY') {
        const { data } = await orderApi.createVnpayUrl(order.id, order.total, crypto.randomUUID())
        const redirectUrl = typeof data === 'string'
          ? data
          : (data?.paymentUrl || data?.url)

        if (redirectUrl && typeof redirectUrl === 'string' && redirectUrl.startsWith('http')) {
          window.location.assign(redirectUrl)
        } else {
          throw new Error('Không nhận được đường dẫn thanh toán hợp lệ từ máy chủ.')
        }
      } else {
        navigate(`/checkout/complete?result=success&orderId=${encodeURIComponent(order.id)}&method=COD`)
      }
    } catch (err) {
      const serverMsg = err?.response?.data?.detail || err?.response?.data?.message || err.message
      setErrorMessage(serverMsg || 'Có lỗi xảy ra khi tạo giao dịch thanh toán. Vui lòng thử lại!')
      setLoading(false)
    }
  }

  return (
    <div className="payment-page-container">
      {/* ── Breadcrumbs & Steps ── */}
      <Breadcrumbs items={[{ label: 'Giỏ hàng', to: '/cart' }, { label: 'Thanh toán đơn hàng' }]} style={{ marginBottom: '16px' }} />

      <div className="checkout-steps">
        <div className="checkout-step checkout-step--done">
          <span className="checkout-step-number">✓</span>
          <span>1. Giỏ hàng</span>
        </div>
        <div className="checkout-step-divider"></div>
        <div className="checkout-step checkout-step--done">
          <span className="checkout-step-number">✓</span>
          <span>2. Thông tin giao hàng</span>
        </div>
        <div className="checkout-step-divider"></div>
        <div className="checkout-step checkout-step--active">
          <span className="checkout-step-number">3</span>
          <span>3. Thanh toán</span>
        </div>
      </div>

      {errorMessage && (
        <div className="payment-alert payment-alert--error" role="alert">
          <AlertCircle size={20} />
          <div>
            <strong>Thông báo thanh toán: </strong>
            <span>{errorMessage}</span>
          </div>
        </div>
      )}

      {fetching ? (
        <div className="payment-card" style={{ textAlign: 'center', padding: '48px 24px' }}>
          <Loader2 className="animate-spin" size={28} style={{ margin: '0 auto 12px' }} />
          <p>Đang tải thông tin đơn hàng...</p>
        </div>
      ) : !order ? (
        <div className="payment-card" style={{ textAlign: 'center', padding: '40px 24px' }}>
          <p style={{ marginBottom: '16px' }}>Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập đơn hàng này.</p>
          <Link to="/orders" className="btn-complete-primary" style={{ display: 'inline-flex' }}>
            Về danh sách đơn hàng
          </Link>
        </div>
      ) : (
        <div className="payment-grid">
          <div className="payment-left-col">
            <section className="payment-card">
              <div className="payment-card__header">
                <CreditCard className="payment-card__icon" size={22} />
                <h2 className="payment-card__title">Phương thức thanh toán</h2>
              </div>

              <div className="payment-methods-list" role="radiogroup" aria-label="Phương thức thanh toán">
                <label
                  className={`payment-method-item ${paymentMethod === 'VNPAY' ? 'payment-method-item--selected' : ''}`}
                  onClick={() => setPaymentMethod('VNPAY')}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="VNPAY"
                    checked={paymentMethod === 'VNPAY'}
                    onChange={() => setPaymentMethod('VNPAY')}
                    className="payment-radio"
                  />
                  <div className="payment-method-info">
                    <div className="payment-method-title">
                      <span>Thanh toán qua VNPAY (Sandbox)</span>
                      <div className="payment-method-badges">
                        <span className="vnpay-badge">VNPAY</span>
                        <span className="vnpay-sub-badge">QR / ATM</span>
                      </div>
                    </div>
                    <p className="payment-method-desc">
                      Hỗ trợ quét mã VNPAY-QR, thẻ ATM nội địa Internet Banking, thẻ Visa/Mastercard/JCB.
                    </p>
                    {paymentMethod === 'VNPAY' && (
                      <div className="payment-method-extra">
                        <Clock size={14} />
                        <span>Hệ thống chuyển sang cổng thanh toán VNPAY Sandbox để hoàn tất giao dịch.</span>
                      </div>
                    )}
                  </div>
                </label>

                {order.paymentMethod === 'COD' && (
                  <label
                    className={`payment-method-item ${paymentMethod === 'COD' ? 'payment-method-item--selected' : ''}`}
                    onClick={() => setPaymentMethod('COD')}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={paymentMethod === 'COD'}
                      onChange={() => setPaymentMethod('COD')}
                      className="payment-radio"
                    />
                    <div className="payment-method-info">
                      <div className="payment-method-title">
                        <span>Thanh toán khi nhận hàng (COD)</span>
                        <Truck size={20} color="#64748b" />
                      </div>
                      <p className="payment-method-desc">
                        Quý khách sẽ thanh toán tiền mặt trực tiếp cho nhân viên giao hàng khi nhận máy tính.
                      </p>
                    </div>
                  </label>
                )}
              </div>
            </section>

            <section className="payment-card">
              <div className="payment-card__header">
                <Truck className="payment-card__icon" size={20} />
                <h2 className="payment-card__title">Thông tin nhận hàng</h2>
              </div>
              <div style={{ fontSize: '14px', lineHeight: '1.7', color: '#475569' }}>
                <div><strong>Người nhận:</strong> {order.recipient} ({order.phone})</div>
                <div><strong>Địa chỉ:</strong> {order.address}</div>
                <div><strong>Hình thức vận chuyển:</strong> {shippingMethodLabels[order.shippingMethod] || order.shippingMethod}</div>
              </div>
            </section>
          </div>

          <aside className="payment-right-col">
            <div className="order-summary-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700 }}>Thông tin đơn hàng</h3>
                <span className="order-code-badge" title={order.code}>{shortOrderCode(order.code)}</span>
              </div>

              <div className="order-items-list">
                {(order.items || []).map(item => (
                  <div className="order-item-row" key={item.productId}>
                    <div className="order-item-img" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f1f5f9', color: '#64748b' }}>
                      <Package size={22} />
                    </div>
                    <div className="order-item-details">
                      <h4 className="order-item-name" title={item.name}>{item.name}</h4>
                      <p className="order-item-qty">SKU: {item.sku} · SL: {item.quantity}</p>
                    </div>
                    <div className="order-item-price">{formatCurrency(Number(item.unitPrice) * item.quantity)}</div>
                  </div>
                ))}
              </div>

              <div className="order-pricing-breakdown">
                <div className="pricing-row">
                  <span>Tạm tính</span>
                  <span>{formatCurrency(order.subtotal ?? order.total)}</span>
                </div>
                <div className="pricing-row">
                  <span>Phí vận chuyển</span>
                  <span>{Number(order.shippingFee) === 0 ? 'Miễn phí' : formatCurrency(order.shippingFee)}</span>
                </div>
                {Number(order.discountAmount) > 0 && (
                  <div className="pricing-row" style={{ color: '#059669' }}>
                    <span>Giảm giá ({order.voucherCode})</span>
                    <span>−{formatCurrency(order.discountAmount)}</span>
                  </div>
                )}
                <div className="pricing-row pricing-row--total">
                  <span>Tổng thanh toán</span>
                  <span className="pricing-total-amount">{formatCurrency(order.total)}</span>
                </div>
              </div>

              <button
                type="button"
                className="btn-pay-now"
                onClick={handlePayment}
                disabled={loading || order.paymentStatus === 'PAID' || order.status === 'CANCELLED' || order.status === 'EXPIRED'}
                id="btn-submit-payment"
              >
                {loading ? (
                  <>
                    <Loader2 className="animate-spin" size={20} />
                    <span>ĐANG CHUYỂN HƯỚNG VNPAY...</span>
                  </>
                ) : order.paymentStatus === 'PAID' ? (
                  <span>ĐƠN HÀNG ĐÃ THANH TOÁN</span>
                ) : (
                  <>
                    <span>THANH TOÁN NGAY</span>
                    <ChevronRight size={20} />
                  </>
                )}
              </button>

              <div className="payment-trust-badges">
                <div className="trust-badge-item">
                  <ShieldCheck size={18} />
                  <span>Bảo mật 100%</span>
                </div>
                <div className="trust-badge-item">
                  <RotateCcw size={18} />
                  <span>Đổi trả 7 ngày</span>
                </div>
                <div className="trust-badge-item">
                  <Headphones size={18} />
                  <span>Hotline 24/7</span>
                </div>
              </div>
            </div>
          </aside>
        </div>
      )}
    </div>
  )
}
