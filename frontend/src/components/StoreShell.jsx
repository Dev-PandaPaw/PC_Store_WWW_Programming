import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import StoreHeader from './StoreHeader'
import StoreFooter from './StoreFooter'
import { CatalogBoundary } from '../contexts/CatalogContext'

export default function StoreShell() {
  const { pathname } = useLocation()

  return (
    <div className="app-shell">
      <StoreHeader />

      <main className={`main-content${pathname === '/' ? ' main-content--home' : ''}`}>
        {/* The order-history and checkout pages use independent fixtures/APIs and need no catalog API */}
        {pathname === '/account/orders' || pathname.startsWith('/checkout') ? (
          <Outlet />
        ) : (
          <CatalogBoundary>
            <Outlet />
          </CatalogBoundary>
        )}
      </main>

      <StoreFooter />
    </div>
  )
}
