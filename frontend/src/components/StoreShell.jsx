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
        <CatalogBoundary><Outlet /></CatalogBoundary>
      </main>

      <StoreFooter />
    </div>
  )
}
