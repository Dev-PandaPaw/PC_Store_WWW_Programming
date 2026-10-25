import React from 'react'
import { Routes, Route } from 'react-router-dom'
import StoreShell from './components/StoreShell'
import AdminShell from './components/AdminShell'

import HomePage from './pages/HomePage'
import CatalogPage from './pages/CatalogPage'
import AdvisoryPage from './pages/AdvisoryPage'
import CartPage from './pages/CartPage'
import LoginPage from './pages/LoginPage'

export default function App() {
  return (
    <Routes>
      {/* Storefront Routes */}
      <Route element={<StoreShell />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/catalog" element={<CatalogPage />} />
        <Route path="/advisory" element={<AdvisoryPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* Admin Routes */}
      <Route path="/admin" element={<AdminShell />}>
        <Route index element={<CatalogPage />} />
        <Route path="products" element={<CatalogPage />} />
      </Route>
    </Routes>
  )
}
