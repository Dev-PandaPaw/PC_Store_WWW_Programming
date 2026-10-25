import React from 'react'
import { Link, Outlet } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'

export default function StoreShell() {
  const { totalCount } = useCart()
  const { user, logout } = useAuth()

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="nav-container">
          <Link to="/" className="brand-logo">
            <span>💻</span> PC Store
          </Link>
          <nav className="nav-links">
            <Link to="/">Trang chủ</Link>
            <Link to="/catalog">Sản phẩm</Link>
            <Link to="/advisory">Tư vấn PC</Link>
          </nav>
          <div className="nav-actions">
            <Link to="/cart" className="btn btn-outline">
              🛒 Giỏ hàng ({totalCount})
            </Link>
            {user ? (
              <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
                <Link to="/account/orders" style={{ fontWeight: 500 }}>
                  {user.fullName || user.username}
                </Link>
                <button onClick={logout} className="btn btn-outline" style={{ padding: '0.35rem 0.75rem' }}>
                  Đăng xuất
                </button>
              </div>
            ) : (
              <Link to="/login" className="btn btn-primary">
                Đăng nhập
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="main-content">
        <Outlet />
      </main>

      <footer className="site-footer">
        <p>© 2026 PC Store - Đồ án tốt nghiệp WWW Nhóm 16. Mọi quyền được bảo lưu.</p>
      </footer>
    </div>
  )
}
