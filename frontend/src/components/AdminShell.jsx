import React from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { ArrowUpRight, Box, LogOut } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'

export default function AdminShell() {
  const { user, logout } = useAuth()

  return (
    <div className="admin-shell">
      <header className="admin-shell__header">
        <div className="admin-shell__inner">
          <Link to="/admin/products" className="admin-shell__brand">
            <span className="admin-shell__brand-mark"><Box size={18} /></span>
            <span>PixelCore <small>ADMIN</small></span>
          </Link>
          <nav className="admin-shell__nav" aria-label="Điều hướng quản trị">
            <NavLink to="/admin/products" className={({ isActive }) => isActive ? 'is-active' : ''}>
              Sản phẩm
            </NavLink>
          </nav>
          <div className="admin-shell__account">
            <span className="admin-shell__user"><i />{user?.fullName || user?.username || 'Quản trị viên'}</span>
            <Link to="/" className="admin-shell__store-link">Cửa hàng <ArrowUpRight size={14} /></Link>
            <button type="button" onClick={logout} aria-label="Đăng xuất"><LogOut size={16} /></button>
          </div>
        </div>
      </header>
      <main className="admin-shell__main"><Outlet /></main>
    </div>
  )
}
