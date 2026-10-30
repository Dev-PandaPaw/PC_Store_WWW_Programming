import React from 'react'
import { Routes, Route, useLocation, Navigate } from 'react-router-dom'
import StoreShell from './components/StoreShell'
import AdminShell from './components/AdminShell'

import HomePage from './pages/HomePage'
import CatalogPage from './pages/CatalogPage'
import AiChatPage from './pages/AiChatPage'
import CartPage from './pages/CartPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import BlogPage from './pages/BlogPage'
import ArticlePage from './pages/ArticlePage'
import ProductDetailPage from './pages/ProductDetailPage'
import NotFoundPage from './pages/NotFoundPage'
import OrderHistoryPage from './pages/OrderHistoryPage'
import PaymentPage from './pages/PaymentPage'
import PaymentCompletePage from './pages/PaymentCompletePage'

import CheckoutPage from './pages/CheckoutPage'
import OrdersPage from './pages/OrdersPage'
import RequireLogin from './components/RequireLogin'

import TopProgressBar from './components/TopProgressBar'

export default function App() {
  const { pathname } = useLocation()
  React.useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return (
    <>
      <TopProgressBar />
      <Routes>
        {/* Storefront Routes */}
        <Route element={<StoreShell />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/catalog" element={<CatalogPage />} />
          <Route path="/products/:slug" element={<ProductDetailPage />} />
          <Route path="/blog" element={<BlogPage />} />
          <Route path="/blog/:slug" element={<ArticlePage />} />
          <Route path="/advisory" element={<AiChatPage />} />
          <Route path="/advisory/ai" element={<Navigate to="/advisory" replace />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<RequireLogin><CheckoutPage /></RequireLogin>} />
          <Route path="/checkout/payment/:orderId" element={<PaymentPage />} />
          <Route path="/checkout/complete" element={<PaymentCompletePage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/account/orders" element={<OrderHistoryPage />} />
          <Route path="/manage-orders" element={<Navigate to="/admin/orders" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>

        {/* Admin Routes */}
        <Route path="/admin" element={<AdminShell />}>
          <Route index element={<CatalogPage />} />
          <Route path="products" element={<CatalogPage />} />
          <Route path="orders" element={<OrdersPage management />} />
        </Route>
      </Routes>
    </>
  )
}
