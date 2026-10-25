import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import StoreHeader from './StoreHeader'
import StoreFooter from './StoreFooter'

export default function StoreShell() {
  const { pathname } = useLocation()

  return (
    <div className="app-shell">
      <StoreHeader />

      <main className={`main-content${pathname === '/' ? ' main-content--home' : ''}`}>
        <Outlet />
      </main>

      <StoreFooter />
    </div>
  )
}
