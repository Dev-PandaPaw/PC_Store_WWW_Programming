import React, { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import {
  CreditCard,
  Truck,
  ShieldCheck,
  CheckCircle2,
  Clock,
  AlertCircle,
  Loader2,
  ChevronRight,
  Package,
  Headphones,
  RotateCcw
} from 'lucide-react'
import '../styles/payment.css'

export default function PaymentPage() {
  const { orderId } = useParams()
  const navigate = useNavigate()

  const [paymentMethod, setPaymentMethod] = useState('VNPAY') // 'VNPAY' | 'COD'
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  // Đơn hàng mẫu hiển thị trực quan (tổng tiền 9.990.000 ₫ khớp với amount API)
  const orderSummary = {
    code: orderId ? `ORD-${orderId.substring(0, 8).toUpperCase()}` : 'ORD-20261029',
    recipientName: 'Huỳnh Đoàn Nhân',
    phone: '0901 234 567',
    address: '12 Nguyễn Văn Bảo, Phường 4, Quận Gò Vấp, TP. Hồ Chí Minh',
    items: [
      {
        id: 1,
        name: 'PC Học tập & Làm việc Essential (Core i5 12400F / RAM 16GB / SSD 512GB / GTX 1650)',
        qty: 1,
        price: 9940000,
        image: 'https://images.unsplash.com/photo-1587831990711-23ca6441447b?auto=format&fit=crop&w=150&q=80'
      }
    ],
    subtotal: 9940000,
    shippingFee: 50000,
    total: 9990000
  }

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
  }

  const handlePayment = async () => {
    setErrorMessage('')
    setLoading(true)

    try {
      if (paymentMethod === 'VNPAY') {
        const payload = {
          orderId: orderId,
          amount: 9990000,
          idempotencyKey: crypto.randomUUID()
        }

        const response = await axios.post('/api/v1/payments/vnpay/create-url', payload)
        
        // Hỗ trợ cả trường hợp response.data là string URL hoặc object { paymentUrl: '...' }
        const redirectUrl = typeof response.data === 'string' 
          ? response.data 
          : (response.data?.paymentUrl || response.data?.url || response.data)

        if (redirectUrl && typeof redirectUrl === 'string' && redirectUrl.startsWith('http')) {
          window.location.href = redirectUrl
        } else {
          throw new Error('Không nhận được đường dẫn thanh toán hợp lệ từ máy chủ.')
        }
      } else if (paymentMethod === 'COD') {
        // Thanh toán khi nhận hàng
        navigate(`/checkout/complete?result=success&orderId=${encodeURIComponent(orderId)}&method=COD`)
      }
    } catch (err) {
      console.error('Lỗi khi khởi tạo thanh toán:', err)
      const serverMsg = err.response?.data?.message || err.response?.data?.error || err.message
      setErrorMessage(serverMsg || 'Có lỗi xảy ra khi tạo giao dịch thanh toán. Vui lòng thử lại!')
      setLoading(false)
    }
  }

  return (
    <div className="payment-page-container">
      {/* ── Breadcrumbs & Steps ── */}
      <nav className="breadcrumbs" aria-label="Đường dẫn" style={{ marginBottom: '16px' }}>
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <Link to="/cart">Giỏ hàng</Link>
        <span>/</span>
        <span>Thanh toán đơn hàng</span>
      </nav>

      {/* ── Step Indicator kiểu GEARVN ── */}
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
            <strong>Lỗi khởi tạo thanh toán: </strong>
            <span>{errorMessage}</span>
          </div>
        </div>
      )}

      {/* ── Main Layout: 2 Columns ── */}
      <div className="payment-grid">
        {/* CỘT TRÁI: CHỌN PHƯƠNG THỨC THANH TOÁN */}
        <div className="payment-left-col">
          <section className="payment-card">
            <div className="payment-card__header">
              <CreditCard className="payment-card__icon" size={22} />
              <h2 className="payment-card__title">Chọn phương thức thanh toán</h2>
            </div>

            <div className="payment-methods-list" role="radiogroup" aria-label="Phương thức thanh toán">
              {/* Option 1: VNPay Sandbox */}
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

              {/* Option 2: COD */}
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
            </div>
          </section>

          {/* Thông tin người nhận hàng */}
          <section className="payment-card">
            <div className="payment-card__header">
              <Truck className="payment-card__icon" size={20} />
              <h2 className="payment-card__title">Thông tin nhận hàng</h2>
            </div>
            <div style={{ fontSize: '14px', lineHeight: '1.7', color: '#475569' }}>
              <div><strong>Người nhận:</strong> {orderSummary.recipientName} ({orderSummary.phone})</div>
              <div><strong>Địa chỉ:</strong> {orderSummary.address}</div>
              <div><strong>Hình thức vận chuyển:</strong> Giao hàng tiêu chuẩn (1-3 ngày làm việc)</div>
            </div>
          </section>
        </div>

        {/* CỘT PHẢI: TÓM TẮT ĐƠN HÀNG & NÚT THANH TOÁN */}
        <aside className="payment-right-col">
          <div className="order-summary-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
              <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 700 }}>Thông tin đơn hàng</h3>
              <span className="order-code-badge" title="Mã đơn hàng">{orderSummary.code}</span>
            </div>

            {/* Danh sách sản phẩm */}
            <div className="order-items-list">
              {orderSummary.items.map(item => (
                <div className="order-item-row" key={item.id}>
                  <img src={item.image} alt={item.name} className="order-item-img" />
                  <div className="order-item-details">
                    <h4 className="order-item-name" title={item.name}>{item.name}</h4>
                    <p className="order-item-qty">Số lượng: {item.qty}</p>
                  </div>
                  <div className="order-item-price">{formatCurrency(item.price * item.qty)}</div>
                </div>
              ))}
            </div>

            {/* Bảng chi tiết giá */}
            <div className="order-pricing-breakdown">
              <div className="pricing-row">
                <span>Tạm tính</span>
                <span>{formatCurrency(orderSummary.subtotal)}</span>
              </div>
              <div className="pricing-row">
                <span>Phí vận chuyển</span>
                <span>{formatCurrency(orderSummary.shippingFee)}</span>
              </div>
              <div className="pricing-row pricing-row--total">
                <span>Tổng thanh toán</span>
                <span className="pricing-total-amount">{formatCurrency(orderSummary.total)}</span>
              </div>
            </div>

            {/* NÚT THANH TOÁN NGAY MÀU ĐỎ (GEARVN STYLE) */}
            <button
              type="button"
              className="btn-pay-now"
              onClick={handlePayment}
              disabled={loading}
              id="btn-submit-payment"
            >
              {loading ? (
                <>
                  <Loader2 className="animate-spin" size={20} />
                  <span>ĐANG XỬ LÝ...</span>
                </>
              ) : (
                <>
                  <span>THANH TOÁN NGAY</span>
                  <ChevronRight size={20} />
                </>
              )}
            </button>

            {/* Cam kết GEARVN */}
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
    </div>
  )
}
