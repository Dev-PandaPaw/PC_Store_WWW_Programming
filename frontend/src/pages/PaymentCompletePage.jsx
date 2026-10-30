import React from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CheckCircle2, XCircle, ArrowRight, RotateCcw, Home, ShoppingBag, FileText } from 'lucide-react'
import '../styles/payment.css'

// Bảng giải mã mã phản hồi VNPay vnp_ResponseCode
const VNPAY_RESPONSE_MESSAGES = {
  '00': 'Giao dịch thành công',
  '07': 'Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).',
  '09': 'Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.',
  '10': 'Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.',
  '11': 'Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.',
  '12': 'Thẻ/Tài khoản của khách hàng bị khóa.',
  '13': 'Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).',
  '24': 'Khách hàng đã hủy giao dịch thanh toán.',
  '51': 'Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.',
  '65': 'Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.',
  '75': 'Ngân hàng thanh toán đang bảo trì.',
  '79': 'KH nhập sai mật khẩu thanh toán quá số lần quy định.',
  '99': 'Các lỗi khác (lỗi từ phía ngân hàng hoặc cổng thanh toán).'
}

export default function PaymentCompletePage() {
  const [searchParams] = useSearchParams()

  const vnpResponseCode = searchParams.get('vnp_ResponseCode')
  const resultParam     = searchParams.get('result')
  const txnRef          = searchParams.get('vnp_TxnRef') || searchParams.get('txnRef') || 'TXN-' + Math.floor(100000 + Math.random() * 900000)
  const transactionNo   = searchParams.get('vnp_TransactionNo')
  const bankCode        = searchParams.get('vnp_BankCode')
  const orderId         = searchParams.get('orderId')
  const method          = searchParams.get('method')
  const rawAmount       = searchParams.get('vnp_Amount')

  // Xác định trạng thái thành công:
  // - vnp_ResponseCode === '00' (từ VNPay redirect)
  // - result === 'success' (từ backend callback redirect hoặc COD)
  const isSuccess = vnpResponseCode === '00' || resultParam === 'success'

  // Tính số tiền hiển thị: VNPay amount = VND * 100
  const parsedAmount = rawAmount ? Number(rawAmount) / 100 : 9990000
  const formattedAmount = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(parsedAmount)

  // Diễn giải thông báo lỗi nếu thất bại
  const errorMessage = vnpResponseCode
    ? (VNPAY_RESPONSE_MESSAGES[vnpResponseCode] || 'Giao dịch không thành công.')
    : (resultParam === 'failed' ? 'Giao dịch bị từ chối hoặc chữ ký không hợp lệ.' : 'Thanh toán không thành công.')

  return (
    <div className="complete-container">
      <div className="complete-card">
        {isSuccess ? (
          <>
            {/* ── THÀNH CÔNG ── */}
            <div className="complete-icon-wrapper complete-icon-wrapper--success">
              <CheckCircle2 size={48} />
            </div>
            <h1 className="complete-title complete-title--success">
              {method === 'COD' ? 'Đặt hàng thành công!' : 'Thanh toán thành công!'}
            </h1>
            <p className="complete-subtitle">
              {method === 'COD'
                ? 'Đơn hàng của bạn đã được tiếp nhận và đang được đóng gói. Shipper sẽ liên hệ trước khi giao hàng.'
                : 'Cảm ơn bạn đã mua sắm tại PC Store. Giao dịch qua cổng VNPAY đã hoàn tất thành công.'}
            </p>

            {/* Chi tiết giao dịch */}
            <div className="complete-details-box">
              <div className="complete-detail-row">
                <span className="complete-detail-label">Mã giao dịch / Tham chiếu:</span>
                <span className="complete-detail-val" style={{ fontFamily: 'monospace' }}>{txnRef}</span>
              </div>
              {transactionNo && (
                <div className="complete-detail-row">
                  <span className="complete-detail-label">Mã giao dịch VNPay:</span>
                  <span className="complete-detail-val" style={{ fontFamily: 'monospace' }}>{transactionNo}</span>
                </div>
              )}
              {bankCode && (
                <div className="complete-detail-row">
                  <span className="complete-detail-label">Ngân hàng thanh toán:</span>
                  <span className="complete-detail-val">{bankCode}</span>
                </div>
              )}
              <div className="complete-detail-row">
                <span className="complete-detail-label">Phương thức:</span>
                <span className="complete-detail-val">
                  {method === 'COD' ? 'Thanh toán khi nhận hàng (COD)' : 'VNPAY Sandbox'}
                </span>
              </div>
              <div className="complete-detail-row">
                <span className="complete-detail-label">Số tiền thanh toán:</span>
                <span className="complete-detail-val" style={{ color: '#059669', fontSize: '16px', fontWeight: 700 }}>
                  {formattedAmount}
                </span>
              </div>
              <div className="complete-detail-row">
                <span className="complete-detail-label">Trạng thái:</span>
                <span className="complete-detail-val" style={{ color: '#059669', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <CheckCircle2 size={16} /> Đã thanh toán
                </span>
              </div>
            </div>

            {/* Nút điều hướng */}
            <div className="complete-actions">
              <Link to="/account/orders" className="btn-complete-primary">
                <FileText size={18} />
                <span>Xem đơn hàng của tôi</span>
              </Link>
              <Link to="/catalog" className="btn-complete-secondary">
                <ShoppingBag size={18} />
                <span>Tiếp tục mua sắm</span>
              </Link>
            </div>
          </>
        ) : (
          <>
            {/* ── THẤT BẠI ── */}
            <div className="complete-icon-wrapper complete-icon-wrapper--failed">
              <XCircle size={48} />
            </div>
            <h1 className="complete-title complete-title--failed">Thanh toán không thành công</h1>
            <p className="complete-subtitle">
              {errorMessage}
            </p>

            {/* Chi tiết lỗi */}
            <div className="complete-details-box">
              <div className="complete-detail-row">
                <span className="complete-detail-label">Mã giao dịch tham chiếu:</span>
                <span className="complete-detail-val" style={{ fontFamily: 'monospace' }}>{txnRef}</span>
              </div>
              {vnpResponseCode && (
                <div className="complete-detail-row">
                  <span className="complete-detail-label">Mã lỗi VNPay:</span>
                  <span className="complete-detail-val" style={{ color: '#dc2626' }}>
                    {vnpResponseCode} ({errorMessage})
                  </span>
                </div>
              )}
              <div className="complete-detail-row">
                <span className="complete-detail-label">Trạng thái:</span>
                <span className="complete-detail-val" style={{ color: '#dc2626' }}>Chưa thanh toán</span>
              </div>
            </div>

            {/* Nút điều hướng */}
            <div className="complete-actions">
              {orderId ? (
                <Link to={`/checkout/payment/${orderId}`} className="btn-complete-primary">
                  <RotateCcw size={18} />
                  <span>Thử thanh toán lại</span>
                </Link>
              ) : (
                <Link to="/account/orders" className="btn-complete-primary">
                  <RotateCcw size={18} />
                  <span>Về lịch sử đơn hàng</span>
                </Link>
              )}
              <Link to="/" className="btn-complete-secondary">
                <Home size={18} />
                <span>Về trang chủ</span>
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
