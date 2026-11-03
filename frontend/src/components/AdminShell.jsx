import React from 'react'
import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import useAdminAccess from '../hooks/useAdminAccess'
import CartDemoPanel from './CartDemoPanel'

export default function AdminShell() {
  const { logout } = useAuth()
  const access = useAdminAccess()

  if (!access.account) return <main className="orders-page">
    <CartDemoPanel />
    <h1>Quản trị cửa hàng</h1>
    {access.loading ? <p role="status">Đang kiểm tra quyền quản trị…</p> : <>
      <p role="alert">{access.error || 'Vui lòng đăng nhập tài khoản admin.'}</p>
      <Link to="/login">Đăng nhập</Link>
    </>}
    <p><Link to="/">Về cửa hàng</Link></p>
  </main>

  return (
    <div className="app-shell">
      <header className="site-header" style={{ borderBottomColor: '#cbd5e1', background: '#0f172a', color: '#fff' }}>
        <div className="nav-container">
          <Link to="/admin/products" className="brand-logo" style={{ color: '#38bdf8' }}>
            <span>⚙️</span> PixelCore Admin
          </Link>
          <nav className="nav-links">
            <Link to="/admin/products" style={{ color: '#e2e8f0' }}>Sản phẩm</Link>
            <Link to="/admin/categories" style={{ color: '#e2e8f0' }}>Danh mục</Link>
            <Link to="/admin/orders" style={{ color: '#e2e8f0' }}>Đơn hàng</Link>
            <Link to="/admin/users" style={{ color: '#e2e8f0' }}>Người dùng</Link>
            <Link to="/admin/warranties" style={{ color: '#e2e8f0' }}>Bảo hành</Link>
          </nav>
          <div className="nav-actions">
            <Link to="/" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Về cửa hàng</Link>
            <button onClick={logout} className="btn" style={{ background: '#334155', color: '#fff' }}>
              Đăng xuất
            </button>
          </div>
        </div>
      </header>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
