import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import StoreHeader from './StoreHeader'
import StoreFooter from './StoreFooter'
import { CatalogBoundary } from '../contexts/CatalogContext'

export default function StoreShell() {
  const { pathname } = useLocation()
  const isAiChat = ['/advisory', '/advisory/ai'].includes(pathname.replace(/\/$/, ''))

  return (
    <div className={`app-shell${isAiChat ? ' app-shell--ai' : ''}`}>
      <StoreHeader />

      <main className={`main-content${pathname === '/' ? ' main-content--home' : ''}`}>
        <CatalogBoundary><Outlet /></CatalogBoundary>
      </main>

      {!isAiChat && <StoreFooter />}
    </div>
  )
}
