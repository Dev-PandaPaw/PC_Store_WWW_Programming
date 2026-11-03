import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function RequireLogin({ children }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="orders-page" role="status">Đang kiểm tra đăng nhập…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  return children
}
