import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import StoreHeader from './StoreHeader'

export default function StoreShell() {
  const { pathname } = useLocation()

  return (
    <div className="app-shell">
      <StoreHeader />

      <main className={`main-content${pathname === '/' ? ' main-content--home' : ''}`}>
        <Outlet />
      </main>

      <footer className="site-footer">
        <p>© 2026 PC Store - Đồ án tốt nghiệp WWW Nhóm 16. Mọi quyền được bảo lưu.</p>
      </footer>
    </div>
  )
}
