import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { loginDestination } from '../utils/checkout'
import { useCart } from '../contexts/CartContext'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const { login } = useCart()
  const navigate = useNavigate()
  const location = useLocation()

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (busy) return
    setError('')
    setBusy(true)
    try {
      await login(username, password)
      navigate(loginDestination(location.state?.from), { replace: true })
    } catch (err) {
      setError(err.response?.data?.detail || 'Đăng nhập không thành công. Vui lòng kiểm tra lại thông tin.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ maxWidth: '420px', margin: '3rem auto' }}>
      <div className="card">
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.5rem', textAlign: 'center' }}>
          Đăng Nhập
        </h1>
        <p style={{ color: 'var(--pc-text-muted)', fontSize: '0.9rem', marginBottom: '1.5rem', textAlign: 'center' }}>
          Chào mừng bạn quay trở lại với PixelCore
        </p>

        <div style={{ marginBottom: '1rem', fontSize: '0.9rem', lineHeight: 1.8 }}>
          <p>Admin: <strong>test</strong> · Mật khẩu: <strong>1234</strong></p>
          <p>Khách hàng: <strong>test2</strong> · Mật khẩu: <strong>1234</strong></p>
        </div>

        {error && (
          <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', padding: '0.75rem', borderRadius: 'var(--pc-radius-sm)', marginBottom: '1rem', fontSize: '0.9rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontWeight: 500, marginBottom: '0.35rem', fontSize: '0.9rem' }}>Tên đăng nhập hoặc Email</label>
            <input
              type="text"
              required
              autoComplete="username"
              disabled={busy}
              value={username}
              onChange={e => setUsername(e.target.value)}
              style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--pc-border)', borderRadius: 'var(--pc-radius-sm)' }}
            />
          </div>
          <div>
            <label style={{ display: 'block', fontWeight: 500, marginBottom: '0.35rem', fontSize: '0.9rem' }}>Mật khẩu</label>
            <input
              type="password"
              required
              autoComplete="current-password"
              disabled={busy}
              value={password}
              onChange={e => setPassword(e.target.value)}
              style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--pc-border)', borderRadius: 'var(--pc-radius-sm)' }}
            />
          </div>
          <button type="submit" disabled={busy} className="btn btn-primary" style={{ padding: '0.75rem', marginTop: '0.5rem' }}>
            {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </button>
        </form>

        <p style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--pc-text-muted)' }}>
          Chưa có tài khoản? <Link to="/register" style={{ color: 'var(--pc-primary)', fontWeight: 600 }}>Đăng ký ngay</Link>
        </p>
      </div>
    </div>
  )
}
