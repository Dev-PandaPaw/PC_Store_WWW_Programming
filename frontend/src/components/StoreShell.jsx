import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import StoreHeader from './StoreHeader'
import StoreFooter from './StoreFooter'
import Breadcrumbs from './Breadcrumbs'
import { CatalogBoundary } from '../contexts/CatalogContext'

export default function StoreShell() {
  const { pathname } = useLocation()
  const isAiChat = ['/advisory', '/advisory/ai'].includes(pathname.replace(/\/$/, ''))
  const pageBreadcrumbs = {
    '/login': [{ label: 'Đăng nhập' }],
    '/register': [{ label: 'Đăng ký' }],
    '/orders': [{ label: 'Đơn hàng của tôi' }],
    '/checkout': [{ label: 'Giỏ hàng', to: '/cart' }, { label: 'Thanh toán' }],
    '/checkout/complete': [{ label: 'Giỏ hàng', to: '/cart' }, { label: 'Kết quả thanh toán' }],
  }[pathname.replace(/\/$/, '')]

  return (
    <div className={`app-shell${isAiChat ? ' app-shell--ai' : ''}`}>
      <StoreHeader />

      <main className={`main-content${pathname === '/' ? ' main-content--home' : ''}${pathname.startsWith('/blog/') ? ' main-content--article' : ''}`}>
        {pageBreadcrumbs && <Breadcrumbs items={pageBreadcrumbs} />}
        {/* The order-history and checkout pages use independent fixtures/APIs and need no catalog API */}
        {pathname === '/account/orders' || pathname.startsWith('/checkout') ? (
          <Outlet />
        ) : (
          <CatalogBoundary>
            <Outlet />
          </CatalogBoundary>
        )}
      </main>

      {!isAiChat && <StoreFooter />}
    </div>
  )
}
