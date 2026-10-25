import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await login(username, password)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.detail || 'Đăng nhập không thành công. Vui lòng kiểm tra lại thông tin.')
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
              value={password}
              onChange={e => setPassword(e.target.value)}
              style={{ width: '100%', padding: '0.6rem', border: '1px solid var(--pc-border)', borderRadius: 'var(--pc-radius-sm)' }}
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ padding: '0.75rem', marginTop: '0.5rem' }}>
            Đăng nhập
          </button>
        </form>

        <p style={{ marginTop: '1.5rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--pc-text-muted)' }}>
          Chưa có tài khoản? <Link to="/register" style={{ color: 'var(--pc-primary)', fontWeight: 600 }}>Đăng ký ngay</Link>
        </p>
      </div>
    </div>
  )
}
