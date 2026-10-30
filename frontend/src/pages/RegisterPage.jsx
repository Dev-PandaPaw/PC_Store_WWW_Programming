import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function RegisterPage() {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const { register, login, user, loading } = useAuth()
  const navigate = useNavigate()

  React.useEffect(() => {
    if (user && !loading) {
      navigate('/', { replace: true })
    }
  }, [user, loading, navigate])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (password.length < 6) {
      setError('Mật khẩu phải chứa ít nhất 6 ký tự.')
      return
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }

    setSubmitting(true)
    try {
      await register({
        fullName: fullName.trim(),
        email: email.trim(),
        phone: phone.trim() || null,
        password,
      })

      // Đăng ký xong tự động đăng nhập luôn
      await login(email.trim(), password)
      navigate('/')
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.detail || 'Đăng ký không thành công. Vui lòng thử lại.'
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: '440px', margin: '3rem auto', padding: '0 1rem' }}>
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
          Đăng Ký Tài Khoản
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

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label
              htmlFor="register-fullname"
              style={{
                display: 'block',
                fontWeight: 600,
                fontSize: '0.85rem',
                marginBottom: '0.4rem',
                color: 'var(--pc-text, #0f172a)',
              }}
            >
              Họ và tên
            </label>
            <input
              id="register-fullname"
              type="text"
              required
              autoFocus
              value={fullName}
              onChange={(e) => {
                setFullName(e.target.value)
                if (error) setError('')
              }}
              placeholder="Nguyễn Văn A"
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
            <label
              htmlFor="register-email"
              style={{
                display: 'block',
                fontWeight: 600,
                fontSize: '0.85rem',
                marginBottom: '0.4rem',
                color: 'var(--pc-text, #0f172a)',
              }}
            >
              Email
            </label>
            <input
              id="register-email"
              type="email"
              required
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
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
            <label
              htmlFor="register-phone"
              style={{
                display: 'block',
                fontWeight: 600,
                fontSize: '0.85rem',
                marginBottom: '0.4rem',
                color: 'var(--pc-text, #0f172a)',
              }}
            >
              Số điện thoại
            </label>
            <input
              id="register-phone"
              type="tel"
              value={phone}
              onChange={(e) => {
                setPhone(e.target.value)
                if (error) setError('')
              }}
              placeholder="0901234567"
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
            <label
              htmlFor="register-password"
              style={{
                display: 'block',
                fontWeight: 600,
                fontSize: '0.85rem',
                marginBottom: '0.4rem',
                color: 'var(--pc-text, #0f172a)',
              }}
            >
              Mật khẩu
            </label>
            <input
              id="register-password"
              type="password"
              required
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                if (error) setError('')
              }}
              placeholder="Tối thiểu 6 ký tự"
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
            <label
              htmlFor="register-confirm-password"
              style={{
                display: 'block',
                fontWeight: 600,
                fontSize: '0.85rem',
                marginBottom: '0.4rem',
                color: 'var(--pc-text, #0f172a)',
              }}
            >
              Xác nhận mật khẩu
            </label>
            <input
              id="register-confirm-password"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value)
                if (error) setError('')
              }}
              placeholder="Nhập lại mật khẩu"
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
            {submitting ? 'Đang tạo tài khoản...' : 'Đăng ký'}
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
          Đã có tài khoản?{' '}
          <Link
            to="/login"
            style={{
              color: 'var(--pc-primary, #2563eb)',
              fontWeight: 600,
              textDecoration: 'none',
            }}
          >
            Đăng nhập ngay
          </Link>
        </div>
      </div>
    </div>
  )
}
