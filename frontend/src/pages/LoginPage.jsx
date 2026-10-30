import React, { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'
import { loginDestination } from '../utils/checkout'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const { user, loading } = useAuth()
  const { login } = useCart()
  const navigate = useNavigate()
  const location = useLocation()

  const redirectPath = loginDestination(location.state?.from)

  React.useEffect(() => {
    if (user && !loading) {
      navigate(redirectPath, { replace: true })
    }
  }, [user, loading, navigate, redirectPath])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (submitting) return
    setError('')
    setSubmitting(true)
    try {
      await login(username.trim(), password)
      navigate(redirectPath, { replace: true })
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.detail || 'Email hoặc mật khẩu không chính xác.'
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: '400px', margin: '3.5rem auto', padding: '0 1rem' }}>
      <div
        style={{
          background: 'var(--pc-surface, #ffffff)',
          border: '1px solid var(--pc-border, #e2e8f0)',
          borderRadius: 'var(--pc-radius-md, 10px)',
          padding: '2rem',
          boxShadow: 'var(--pc-shadow-sm, 0 1px 2px rgba(0,0,0,0.05))',
        }}
      >
        <h1
          style={{
            fontSize: '1.35rem',
            fontWeight: 700,
            textAlign: 'center',
            marginBottom: '1.5rem',
            color: 'var(--pc-text, #0f172a)',
          }}
        >
          Đăng Nhập
        </h1>

        {error && (
          <div
            style={{
              background: '#fef2f2',
              border: '1px solid #fecaca',
              color: '#b91c1c',
              padding: '0.65rem 0.85rem',
              borderRadius: 'var(--pc-radius-sm, 6px)',
              marginBottom: '1.25rem',
              fontSize: '0.85rem',
              lineHeight: 1.4,
            }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem' }}>
          <div>
            <label
              htmlFor="login-username"
              style={{
                display: 'block',
                fontWeight: 600,
                fontSize: '0.85rem',
                marginBottom: '0.4rem',
                color: 'var(--pc-text, #0f172a)',
              }}
            >
              Email hoặc Tên đăng nhập
            </label>
            <input
              id="login-username"
              type="text"
              required
              autoFocus
              autoComplete="username"
              disabled={submitting}
              value={username}
              onChange={(e) => {
                setUsername(e.target.value)
                if (error) setError('')
              }}
              placeholder="name@example.com"
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                fontSize: '0.9rem',
                border: '1px solid var(--pc-border, #e2e8f0)',
                borderRadius: 'var(--pc-radius-sm, 6px)',
                outline: 'none',
                background: '#ffffff',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
              <label
                htmlFor="login-password"
                style={{
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  color: 'var(--pc-text, #0f172a)',
                }}
              >
                Mật khẩu
              </label>
              <Link
                to="/forgot-password"
                style={{
                  fontSize: '0.8rem',
                  color: 'var(--pc-primary, #2563eb)',
                  textDecoration: 'none',
                }}
              >
                Quên mật khẩu?
              </Link>
            </div>
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              disabled={submitting}
              required
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                if (error) setError('')
              }}
              placeholder="••••••••"
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                fontSize: '0.9rem',
                border: '1px solid var(--pc-border, #e2e8f0)',
                borderRadius: 'var(--pc-radius-sm, 6px)',
                outline: 'none',
                background: '#ffffff',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            style={{
              marginTop: '0.5rem',
              padding: '0.75rem',
              background: 'var(--pc-primary, #2563eb)',
              color: '#ffffff',
              border: 'none',
              borderRadius: 'var(--pc-radius-sm, 6px)',
              fontWeight: 600,
              fontSize: '0.92rem',
              cursor: submitting ? 'not-allowed' : 'pointer',
              opacity: submitting ? 0.7 : 1,
              transition: 'background 0.2s ease',
            }}
          >
            {submitting ? 'Đang xử lý...' : 'Đăng nhập'}
          </button>
        </form>

        <div
          style={{
            marginTop: '1.5rem',
            paddingTop: '1.25rem',
            borderTop: '1px solid var(--pc-border, #e2e8f0)',
            textAlign: 'center',
            fontSize: '0.85rem',
            color: 'var(--pc-text-muted, #64748b)',
          }}
        >
          Chưa có tài khoản?{' '}
          <Link
            to="/register"
            style={{
              color: 'var(--pc-primary, #2563eb)',
              fontWeight: 600,
              textDecoration: 'none',
            }}
          >
            Đăng ký ngay
          </Link>
        </div>
      </div>
    </div>
  )
}
