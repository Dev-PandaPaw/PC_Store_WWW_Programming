import React from 'react'
import { Link } from 'react-router-dom'
import '../styles/store-footer.css'

export default function StoreFooter() {
  return (
    <footer className="gv-footer">
      <div className="gv-footer__top">
        <div className="gv-footer__wrap gv-footer__grid">
          {/* Cột 1: Thông tin liên hệ & Tổng đài */}
          <div className="gv-footer__col">
            <div className="gv-footer__logo">
              <span className="gv-logo-badge">PC</span>
              <span className="gv-logo-text">STORE</span>
            </div>
            <p className="gv-footer__slogan">Hệ thống phân phối máy tính & linh kiện công nghệ cao cấp hàng đầu Việt Nam.</p>
            
            <div className="gv-footer__hotlines">
              <div className="gv-hotline-item">
                <span className="gv-hotline-label">Tổng đài bán hàng (Miễn phí):</span>
                <strong className="gv-hotline-num">1800.6975</strong>
                <span className="gv-hotline-time">(8h30 - 21h30 hàng ngày)</span>
              </div>
              <div className="gv-hotline-item">
                <span className="gv-hotline-label">Chăm sóc khách hàng & Khiếu nại:</span>
                <strong className="gv-hotline-num">1800.6173</strong>
                <span className="gv-hotline-time">(8h30 - 20h30)</span>
              </div>
              <div className="gv-hotline-item">
                <span className="gv-hotline-label">Hỗ trợ kỹ thuật & Bảo hành:</span>
                <strong className="gv-hotline-num">1800.6274</strong>
                <span className="gv-hotline-time">(8h30 - 17h30)</span>
              </div>
            </div>
          </div>

          {/* Cột 2: Hệ thống Showroom */}
          <div className="gv-footer__col">
            <h4 className="gv-footer__title">HỆ THỐNG SHOWROOM</h4>
            <div className="gv-showroom-list">
              <div className="gv-showroom-item">
                <strong>Showroom Hồ Chí Minh:</strong>
                <p>78-80-82 Hoàng Hoa Thám, P. 12, Q. Tân Bình, TP. HCM</p>
                <p>905 Kha Vạn Cân, P. Linh Tây, TP. Thủ Đức, TP. HCM</p>
              </div>
              <div className="gv-showroom-item">
                <strong>Showroom Hà Nội:</strong>
                <p>162 Thái Hà, P. Trung Liệt, Q. Đống Đa, Hà Nội</p>
                <p>460 Khương Đình, P. Hạ Đình, Q. Thanh Xuân, Hà Nội</p>
              </div>
            </div>
          </div>

          {/* Cột 3: Về PC Store & Chính sách */}
          <div className="gv-footer__col">
            <h4 className="gv-footer__title">CHÍNH SÁCH BÁN HÀNG</h4>
            <ul className="gv-footer__links">
              <li><Link to="/catalog">Chính sách bảo hành 36 tháng</Link></li>
              <li><Link to="/catalog">Chính sách đổi mới trong 30 ngày</Link></li>
              <li><Link to="/catalog">Chính sách vận chuyển & Giao hàng</Link></li>
              <li><Link to="/catalog">Chính sách bảo mật thông tin</Link></li>
              <li><Link to="/catalog">Chính sách kiểm hàng & Hoàn tiền</Link></li>
              <li><Link to="/advisory">Hướng dẫn xây dựng cấu hình PC</Link></li>
            </ul>
          </div>

          {/* Cột 4: Thanh toán & Chứng nhận */}
          <div className="gv-footer__col">
            <h4 className="gv-footer__title">PHƯƠNG THỨC THANH TOÁN</h4>
            <div className="gv-pay-methods">
              <span className="gv-pay-badge">VNPAY QR</span>
              <span className="gv-pay-badge">MoMo</span>
              <span className="gv-pay-badge">Visa / Master</span>
              <span className="gv-pay-badge">COD Nhận Hàng</span>
              <span className="gv-pay-badge">Trả Góp 0%</span>
            </div>

            <h4 className="gv-footer__title" style={{ marginTop: '20px' }}>CHỨNG NHẬN DOANH NGHIỆP</h4>
            <div className="gv-cert-badge">
              <span className="gv-cert-icon">✓</span>
              <div>
                <strong>ĐÃ THÔNG BÁO</strong>
                <p>BỘ CÔNG THƯƠNG VIỆT NAM</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="gv-footer__bottom">
        <div className="gv-footer__wrap gv-footer__copyright">
          <p>© 2026 PC STORE - Đồ án môn Lập trình WWW Nhóm 16. Giảng viên hướng dẫn: Bộ môn CNPM - FIT - IUH.</p>
          <p>Mã đề tài: Xây dựng Website Cửa hàng Máy tính & Linh kiện PC Store (Spring Boot 4 + React SPA).</p>
        </div>
      </div>
    </footer>
  )
}
