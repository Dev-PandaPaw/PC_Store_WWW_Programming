import React from 'react'
import { Routes, Route, useLocation } from 'react-router-dom'
import StoreShell from './components/StoreShell'
import AdminShell from './components/AdminShell'

import HomePage from './pages/HomePage'
import CatalogPage from './pages/CatalogPage'
import AdvisoryPage from './pages/AdvisoryPage'
import CartPage from './pages/CartPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import BlogPage from './pages/BlogPage'
import ArticlePage from './pages/ArticlePage'
import ProductDetailPage from './pages/ProductDetailPage'
import NotFoundPage from './pages/NotFoundPage'
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
        <Route path="/advisory" element={<AdvisoryPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* Admin Routes */}
      <Route path="/admin" element={<AdminShell />}>
        <Route index element={<CatalogPage />} />
        <Route path="products" element={<CatalogPage />} />
      </Route>
    </Routes>
    </>
  )
}
