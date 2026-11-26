import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/authContextStore'
import {
  confirmPasswordReset,
  extractIdentityError,
  requestPasswordResetOtp,
} from '../services/identity'

const cardStyle = {
  background: 'var(--pc-surface, #ffffff)',
  border: '1px solid var(--pc-border, #e2e8f0)',
  borderRadius: 'var(--pc-radius-md, 10px)',
  padding: '2rem',
  boxShadow: 'var(--pc-shadow-sm, 0 1px 2px rgba(0,0,0,0.05))',
}

const labelStyle = {
  display: 'block',
  fontWeight: 600,
  fontSize: '0.85rem',
  marginBottom: '0.4rem',
  color: 'var(--pc-text, #0f172a)',
}

const inputStyle = {
  width: '100%',
  padding: '0.65rem 0.85rem',
  fontSize: '0.9rem',
  border: '1px solid var(--pc-border, #e2e8f0)',
  borderRadius: 'var(--pc-radius-sm, 6px)',
  outline: 'none',
  background: '#ffffff',
  boxSizing: 'border-box',
}

export default function ForgotPasswordPage() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()

  const [step, setStep] = useState(1)
  const [email, setEmail] = useState('')
  const [otpCode, setOtpCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const [cooldown, setCooldown] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [completed, setCompleted] = useState(false)

  useEffect(() => {
    if (user && !loading) {
      navigate('/', { replace: true })
    }
  }, [user, loading, navigate])

  useEffect(() => {
    if (cooldown <= 0) return undefined
    const timer = setInterval(() => {
      setCooldown((prev) => (prev > 1 ? prev - 1 : 0))
    }, 1000)
    return () => clearInterval(timer)
  }, [cooldown])

  const handleSendOtp = async (e) => {
    e.preventDefault()
    if (submitting) return
    const targetEmail = email.trim().toLowerCase()
    if (!targetEmail) {
      setError('Vui lòng nhập địa chỉ email.')
      return
    }

    setError('')
    setNotice('')
    setSubmitting(true)
    try {
      const res = await requestPasswordResetOtp(targetEmail)
      setEmail(targetEmail)
      setStep(2)
      setCooldown(60)
      setNotice(res?.message || 'Mã xác thực OTP 6 chữ số đã được gửi đến email của bạn.')
    } catch (err) {
      setError(extractIdentityError(err, 'Không thể gửi mã OTP. Vui lòng thử lại.'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleResendOtp = async () => {
    if (submitting || cooldown > 0) return
    setError('')
    setNotice('')
    setSubmitting(true)
    try {
      const res = await requestPasswordResetOtp(email.trim().toLowerCase())
      setCooldown(60)
      setNotice(res?.message || 'Đã gửi lại mã OTP mới đến email của bạn.')
    } catch (err) {
      setError(extractIdentityError(err, 'Chưa thể gửi lại mã OTP lúc này.'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()
    if (submitting) return

    const cleanOtp = otpCode.trim()
    if (!/^[0-9]{6}$/.test(cleanOtp)) {
      setError('Mã OTP phải gồm đúng 6 chữ số.')
      return
    }
    if (newPassword.length < 6) {
      setError('Mật khẩu mới phải chứa ít nhất 6 ký tự.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }

    setError('')
    setNotice('')
    setSubmitting(true)
    try {
      const res = await confirmPasswordReset({
        email: email.trim().toLowerCase(),
        otpCode: cleanOtp,
        newPassword,
      })
      setCompleted(true)
      setNotice(res?.message || 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.')
    } catch (err) {
      setError(extractIdentityError(err, 'Đặt lại mật khẩu thất bại. Vui lòng kiểm tra lại mã OTP.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: '440px', margin: '3.5rem auto', padding: '0 1rem' }}>
      <div style={cardStyle}>
        <h1
          style={{
            fontSize: '1.35rem',
            fontWeight: 700,
            textAlign: 'center',
            marginBottom: '1.25rem',
            color: 'var(--pc-text, #0f172a)',
          }}
        >
          Khôi Phục Mật Khẩu
        </h1>

        {error && (
          <div
            role="alert"
            style={{
              background: '#fef2f2',
              border: '1px solid #fecaca',
              color: '#b91c1c',
              padding: '0.65rem 0.85rem',
              borderRadius: 'var(--pc-radius-sm, 6px)',
              marginBottom: '1.1rem',
              fontSize: '0.85rem',
              lineHeight: 1.45,
            }}
          >
            {error}
          </div>
        )}

        {notice && (
          <div
            role="status"
            style={{
              background: '#f0fdf4',
              border: '1px solid #bbf7d0',
              color: '#166534',
              padding: '0.65rem 0.85rem',
              borderRadius: 'var(--pc-radius-sm, 6px)',
              marginBottom: '1.1rem',
              fontSize: '0.85rem',
              lineHeight: 1.45,
            }}
          >
            {notice}
          </div>
        )}

        {completed ? (
          <div style={{ textAlign: 'center', marginTop: '1rem' }}>
            <Link
              to="/login"
              style={{
                display: 'inline-block',
                width: '100%',
                boxSizing: 'border-box',
                padding: '0.75rem',
                background: 'var(--pc-primary, #2563eb)',
                color: '#ffffff',
                borderRadius: 'var(--pc-radius-sm, 6px)',
                fontWeight: 600,
                fontSize: '0.92rem',
                textDecoration: 'none',
              }}
            >
              Đi đến trang Đăng nhập
            </Link>
          </div>
        ) : step === 1 ? (
          <form onSubmit={handleSendOtp} style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem' }}>
            <div>
              <label htmlFor="forgot-email" style={labelStyle}>
                Email tài khoản
              </label>
              <input
                id="forgot-email"
                type="email"
                required
                autoFocus
                autoComplete="email"
                disabled={submitting}
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  if (error) setError('')
                }}
                placeholder="name@example.com"
                style={inputStyle}
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              style={{
                marginTop: '0.25rem',
                padding: '0.75rem',
                background: 'var(--pc-primary, #2563eb)',
                color: '#ffffff',
                border: 'none',
                borderRadius: 'var(--pc-radius-sm, 6px)',
                fontWeight: 600,
                fontSize: '0.92rem',
                cursor: submitting ? 'not-allowed' : 'pointer',
                opacity: submitting ? 0.7 : 1,
              }}
            >
              {submitting ? 'Đang gửi mã...' : 'Nhận mã xác thực OTP'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                <label htmlFor="reset-email" style={{ ...labelStyle, marginBottom: 0 }}>
                  Email nhận mã
                </label>
                <button
                  type="button"
                  disabled={submitting}
                  onClick={() => {
                    setStep(1)
                    setOtpCode('')
                    setError('')
                    setNotice('')
                  }}
                  style={{
                    background: 'none',
                    border: 'none',
                    padding: 0,
                    color: 'var(--pc-primary, #2563eb)',
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  Đổi email
                </button>
              </div>
              <input
                id="reset-email"
                type="email"
                value={email}
                readOnly
                style={{ ...inputStyle, background: '#f8fafc', color: '#475569' }}
              />
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                <label htmlFor="reset-otp" style={{ ...labelStyle, marginBottom: 0 }}>
                  Mã OTP (6 chữ số)
                </label>
                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={submitting || cooldown > 0}
                  style={{
                    background: 'none',
                    border: 'none',
                    padding: 0,
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    color: cooldown > 0 ? '#94a3b8' : 'var(--pc-primary, #2563eb)',
                    cursor: cooldown > 0 || submitting ? 'default' : 'pointer',
                  }}
                >
                  {cooldown > 0 ? `Gửi lại sau ${cooldown}s` : 'Gửi lại mã OTP'}
                </button>
              </div>
              <input
                id="reset-otp"
                type="text"
                inputMode="numeric"
                maxLength={6}
                required
                autoFocus
                disabled={submitting}
                value={otpCode}
                onChange={(e) => {
                  setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))
                  if (error) setError('')
                }}
                placeholder="Nhập 6 chữ số"
                style={{ ...inputStyle, letterSpacing: '0.25rem', fontWeight: 600 }}
              />
            </div>

            <div>
              <label htmlFor="reset-new-password" style={labelStyle}>
                Mật khẩu mới
              </label>
              <input
                id="reset-new-password"
                type="password"
                required
                autoComplete="new-password"
                disabled={submitting}
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value)
                  if (error) setError('')
                }}
                placeholder="Tối thiểu 6 ký tự"
                style={inputStyle}
              />
            </div>

            <div>
              <label htmlFor="reset-confirm-password" style={labelStyle}>
                Xác nhận mật khẩu mới
              </label>
              <input
                id="reset-confirm-password"
                type="password"
                required
                autoComplete="new-password"
                disabled={submitting}
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value)
                  if (error) setError('')
                }}
                placeholder="Nhập lại mật khẩu mới"
                style={inputStyle}
              />
            </div>

            <button
              type="submit"
              disabled={submitting}
              style={{
                marginTop: '0.35rem',
                padding: '0.75rem',
                background: 'var(--pc-primary, #2563eb)',
                color: '#ffffff',
                border: 'none',
                borderRadius: 'var(--pc-radius-sm, 6px)',
                fontWeight: 600,
                fontSize: '0.92rem',
                cursor: submitting ? 'not-allowed' : 'pointer',
                opacity: submitting ? 0.7 : 1,
              }}
            >
              {submitting ? 'Đang xác nhận...' : 'Đặt lại mật khẩu'}
            </button>
          </form>
        )}

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
          Nhớ mật khẩu?{' '}
          <Link
            to="/login"
            style={{
              color: 'var(--pc-primary, #2563eb)',
              fontWeight: 600,
              textDecoration: 'none',
            }}
          >
            Quay lại Đăng nhập
          </Link>
        </div>
      </div>
    </div>
  )
}

