import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../contexts/authContextStore'
import provinces from '../data/vietnamDistricts.json'
import { selectedAddress } from '../utils/addresses'
import {
  changeAccountPassword,
  extractIdentityError,
  fetchProfile,
  saveProfile,
  sendPhoneOtp,
  verifyPhoneOtp,
} from '../services/identity'

const sectionCardStyle = {
  background: 'var(--pc-surface, #ffffff)',
  border: '1px solid var(--pc-border, #e2e8f0)',
  borderRadius: 'var(--pc-radius-md, 10px)',
  padding: '1.75rem',
  boxShadow: 'var(--pc-shadow-sm, 0 1px 2px rgba(0,0,0,0.05))',
}

const labelStyle = {
  display: 'block',
  fontWeight: 600,
  fontSize: '0.84rem',
  marginBottom: '0.35rem',
  color: 'var(--pc-text, #0f172a)',
}

const inputStyle = {
  width: '100%',
  padding: '0.62rem 0.82rem',
  fontSize: '0.9rem',
  border: '1px solid var(--pc-border, #e2e8f0)',
  borderRadius: 'var(--pc-radius-sm, 6px)',
  outline: 'none',
  background: '#ffffff',
  boxSizing: 'border-box',
  color: '#0f172a',
}

const primaryBtnStyle = (disabled) => ({
  padding: '0.68rem 1.25rem',
  background: 'var(--pc-primary, #2563eb)',
  color: '#ffffff',
  border: 'none',
  borderRadius: 'var(--pc-radius-sm, 6px)',
  fontWeight: 600,
  fontSize: '0.88rem',
  cursor: disabled ? 'not-allowed' : 'pointer',
  opacity: disabled ? 0.7 : 1,
})

export default function ProfilePage() {
  const { user, updateCachedUser } = useAuth()

  const [loadingProfile, setLoadingProfile] = useState(true)
  const [profile, setProfile] = useState({
    fullName: user?.fullName || '',
    email: user?.email || '',
    phone: user?.phone || '',
    phoneVerified: Boolean(user?.phoneVerified),
    recipientName: user?.recipientName || '',
    province: user?.province || '',
    district: user?.district || '',
    ward: user?.ward || '',
    addressLine: user?.addressLine || '',
    roles: user?.roles || [],
  })

  const [profileMsg, setProfileMsg] = useState(null)
  const [savingProfile, setSavingProfile] = useState(false)

  // Xác minh số điện thoại OTP
  const [otpBoxOpen, setOtpBoxOpen] = useState(false)
  const [phoneOtp, setPhoneOtp] = useState('')
  const [otpCooldown, setOtpCooldown] = useState(0)
  const [otpBusy, setOtpBusy] = useState(false)
  const [otpMsg, setOtpMsg] = useState(null)

  // Đổi mật khẩu
  const [passwords, setPasswords] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [savingPassword, setSavingPassword] = useState(false)
  const [passwordMsg, setPasswordMsg] = useState(null)

  useEffect(() => {
    document.title = 'Hồ sơ tài khoản | PixelCore'
    return () => {
      document.title = 'PixelCore - Cửa Hàng PC & Linh Kiện'
    }
  }, [])

  useEffect(() => {
    let active = true
    setLoadingProfile(true)
    fetchProfile()
      .then((data) => {
        if (!active) return
        setProfile({
          fullName: data.fullName || '',
          email: data.email || '',
          phone: data.phone || '',
          phoneVerified: Boolean(data.phoneVerified),
          recipientName: data.recipientName || '',
          province: data.province || '',
          district: data.district || '',
          ward: data.ward || '',
          addressLine: data.addressLine || '',
          roles: data.roles || [],
        })
        if (updateCachedUser) updateCachedUser(data)
      })
      .catch((err) => {
        if (active) {
          setProfileMsg({ type: 'error', text: extractIdentityError(err, 'Không tải được thông tin hồ sơ.') })
        }
      })
      .finally(() => {
        if (active) setLoadingProfile(false)
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (otpCooldown <= 0) return undefined
    const timer = setInterval(() => {
      setOtpCooldown((prev) => (prev > 1 ? prev - 1 : 0))
    }, 1000)
    return () => clearInterval(timer)
  }, [otpCooldown])

  const selectedProvinceObj = useMemo(
    () => selectedAddress(provinces, profile.province),
    [profile.province]
  )
  const districtOptions = selectedProvinceObj?.districts || []

  const handleSaveProfile = async (e) => {
    e.preventDefault()
    if (savingProfile) return
    setProfileMsg(null)
    setOtpMsg(null)
    setSavingProfile(true)

    try {
      const updated = await saveProfile({
        fullName: profile.fullName.trim(),
        phone: profile.phone.trim() || null,
        recipientName: profile.recipientName.trim() || null,
        province: profile.province || null,
        district: profile.district || null,
        ward: profile.ward.trim() || null,
        addressLine: profile.addressLine.trim() || null,
      })

      setProfile((prev) => ({
        ...prev,
        fullName: updated.fullName || '',
        phone: updated.phone || '',
        phoneVerified: Boolean(updated.phoneVerified),
        recipientName: updated.recipientName || '',
        province: updated.province || '',
        district: updated.district || '',
        ward: updated.ward || '',
        addressLine: updated.addressLine || '',
      }))
      if (updateCachedUser) updateCachedUser(updated)
      if (updated.phoneVerified) setOtpBoxOpen(false)

      setProfileMsg({ type: 'success', text: 'Cập nhật hồ sơ và địa chỉ nhận hàng mặc định thành công.' })
    } catch (err) {
      setProfileMsg({ type: 'error', text: extractIdentityError(err, 'Không thể lưu hồ sơ.') })
    } finally {
      setSavingProfile(false)
    }
  }

  const handleSendPhoneOtp = async () => {
    if (otpBusy || otpCooldown > 0) return
    setOtpMsg(null)
    setOtpBusy(true)
    try {
      const res = await sendPhoneOtp()
      setOtpBoxOpen(true)
      setOtpCooldown(60)
      setOtpMsg({
        type: 'success',
        text: res?.message || 'Đã gửi mã OTP 6 số đến email tài khoản của bạn.',
      })
    } catch (err) {
      setOtpMsg({
        type: 'error',
        text: extractIdentityError(err, 'Vui lòng lưu số điện thoại hợp lệ trước khi gửi mã xác minh.'),
      })
    } finally {
      setOtpBusy(false)
    }
  }

  const handleVerifyPhoneOtp = async (e) => {
    e.preventDefault()
    if (otpBusy) return
    const code = phoneOtp.trim()
    if (!/^[0-9]{6}$/.test(code)) {
      setOtpMsg({ type: 'error', text: 'Mã OTP phải gồm đúng 6 chữ số.' })
      return
    }

    setOtpMsg(null)
    setOtpBusy(true)
    try {
      const updated = await verifyPhoneOtp(code)
      setProfile((prev) => ({
        ...prev,
        phone: updated.phone || prev.phone,
        phoneVerified: Boolean(updated.phoneVerified),
      }))
      if (updateCachedUser) updateCachedUser(updated)
      setPhoneOtp('')
      setOtpBoxOpen(false)
      setOtpMsg({ type: 'success', text: 'Xác minh số điện thoại thành công. Bạn đã đủ điều kiện đặt hàng.' })
    } catch (err) {
      setOtpMsg({ type: 'error', text: extractIdentityError(err, 'Mã xác minh không hợp lệ.') })
    } finally {
      setOtpBusy(false)
    }
  }

  const handleChangePassword = async (e) => {
    e.preventDefault()
    if (savingPassword) return
    setPasswordMsg(null)

    if (passwords.newPassword.length < 6) {
      setPasswordMsg({ type: 'error', text: 'Mật khẩu mới phải từ 6 ký tự trở lên.' })
      return
    }
    if (passwords.newPassword !== passwords.confirmPassword) {
      setPasswordMsg({ type: 'error', text: 'Mật khẩu xác nhận không khớp.' })
      return
    }

    setSavingPassword(true)
    try {
      const res = await changeAccountPassword({
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      })
      setPasswords({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setPasswordMsg({
        type: 'success',
        text: res?.message || 'Đổi mật khẩu thành công.',
      })
    } catch (err) {
      setPasswordMsg({
        type: 'error',
        text: extractIdentityError(err, 'Đổi mật khẩu không thành công.'),
      })
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <div style={{ maxWidth: '880px', margin: '2.5rem auto', padding: '0 1rem' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '0.75rem',
          marginBottom: '1.5rem',
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.45rem', fontWeight: 700, margin: 0, color: 'var(--pc-text, #0f172a)' }}>
            Hồ Sơ Tài Khoản
          </h1>
          <p style={{ margin: '0.3rem 0 0', fontSize: '0.88rem', color: '#64748b' }}>
            Quản lý thông tin cá nhân, xác minh số điện thoại đặt hàng và địa chỉ nhận hàng mặc định.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <Link
            to="/orders"
            style={{
              padding: '0.55rem 0.95rem',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              background: '#ffffff',
              color: '#0f172a',
              fontSize: '0.85rem',
              fontWeight: 600,
              textDecoration: 'none',
            }}
          >
            Lịch sử đơn hàng
          </Link>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {/* KHỐI 1: THÔNG TIN HỒ SƠ & ĐỊA CHỈ MẶC ĐỊNH */}
        <section style={sectionCardStyle}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '0.5rem',
              marginBottom: '1.25rem',
              paddingBottom: '0.85rem',
              borderBottom: '1px solid #e2e8f0',
            }}
          >
            <h2 style={{ fontSize: '1.05rem', fontWeight: 700, margin: 0, color: '#0f172a' }}>
              Thông tin cá nhân & Địa chỉ mặc định
            </h2>
            <span
              style={{
                fontSize: '0.78rem',
                fontWeight: 600,
                padding: '0.25rem 0.65rem',
                borderRadius: '999px',
                background: profile.phoneVerified ? '#dcfce7' : '#fef3c7',
                color: profile.phoneVerified ? '#166534' : '#92400e',
              }}
            >
              {profile.phoneVerified ? 'SĐT đã xác minh' : 'SĐT chưa xác minh'}
            </span>
          </div>

          {profileMsg && (
            <div
              role={profileMsg.type === 'error' ? 'alert' : 'status'}
              style={{
                background: profileMsg.type === 'error' ? '#fef2f2' : '#f0fdf4',
                border: `1px solid ${profileMsg.type === 'error' ? '#fecaca' : '#bbf7d0'}`,
                color: profileMsg.type === 'error' ? '#b91c1c' : '#166534',
                padding: '0.65rem 0.85rem',
                borderRadius: '6px',
                marginBottom: '1.1rem',
                fontSize: '0.85rem',
              }}
            >
              {profileMsg.text}
            </div>
          )}

          {loadingProfile ? (
            <p style={{ color: '#64748b', fontSize: '0.9rem' }}>Đang tải thông tin tài khoản...</p>
          ) : (
            <form onSubmit={handleSaveProfile} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                <div>
                  <label htmlFor="profile-email" style={labelStyle}>
                    Email đăng nhập
                  </label>
                  <input
                    id="profile-email"
                    type="email"
                    value={profile.email}
                    readOnly
                    style={{ ...inputStyle, background: '#f8fafc', color: '#64748b' }}
                  />
                </div>

                <div>
                  <label htmlFor="profile-fullname" style={labelStyle}>
                    Họ và tên *
                  </label>
                  <input
                    id="profile-fullname"
                    type="text"
                    required
                    maxLength={150}
                    value={profile.fullName}
                    onChange={(e) => setProfile((p) => ({ ...p, fullName: e.target.value }))}
                    style={inputStyle}
                  />
                </div>

                <div>
                  <label htmlFor="profile-phone" style={labelStyle}>
                    Số điện thoại liên hệ
                  </label>
                  <input
                    id="profile-phone"
                    type="tel"
                    maxLength={20}
                    placeholder="0912345678"
                    value={profile.phone}
                    onChange={(e) => setProfile((p) => ({ ...p, phone: e.target.value }))}
                    style={inputStyle}
                  />
                </div>

                <div>
                  <label htmlFor="profile-recipient" style={labelStyle}>
                    Tên người nhận mặc định
                  </label>
                  <input
                    id="profile-recipient"
                    type="text"
                    maxLength={150}
                    placeholder="Để trống sẽ lấy theo Họ và tên"
                    value={profile.recipientName}
                    onChange={(e) => setProfile((p) => ({ ...p, recipientName: e.target.value }))}
                    style={inputStyle}
                  />
                </div>

                <div>
                  <label htmlFor="profile-province" style={labelStyle}>
                    Tỉnh / Thành phố
                  </label>
                  <select
                    id="profile-province"
                    value={profile.province}
                    onChange={(e) =>
                      setProfile((p) => ({
                        ...p,
                        province: e.target.value,
                        district: '',
                      }))
                    }
                    style={inputStyle}
                  >
                    <option value="">-- Chọn Tỉnh / Thành phố --</option>
                    {provinces.map((item) => (
                      <option key={item.code} value={item.name}>
                        {item.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label htmlFor="profile-district" style={labelStyle}>
                    Quận / Huyện
                  </label>
                  <select
                    id="profile-district"
                    value={profile.district}
                    disabled={!profile.province}
                    onChange={(e) => setProfile((p) => ({ ...p, district: e.target.value }))}
                    style={{
                      ...inputStyle,
                      background: !profile.province ? '#f8fafc' : '#ffffff',
                    }}
                  >
                    <option value="">-- Chọn Quận / Huyện --</option>
                    {districtOptions.map((item) => (
                      <option key={item.code} value={item.name}>
                        {item.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label htmlFor="profile-ward" style={labelStyle}>
                    Phường / Xã
                  </label>
                  <input
                    id="profile-ward"
                    type="text"
                    maxLength={100}
                    placeholder="Ví dụ: Phường Bến Nghé"
                    value={profile.ward}
                    onChange={(e) => setProfile((p) => ({ ...p, ward: e.target.value }))}
                    style={inputStyle}
                  />
                </div>

                <div>
                  <label htmlFor="profile-address-line" style={labelStyle}>
                    Số nhà, tên đường
                  </label>
                  <input
                    id="profile-address-line"
                    type="text"
                    maxLength={255}
                    placeholder="Số nhà, đường, tòa nhà..."
                    value={profile.addressLine}
                    onChange={(e) => setProfile((p) => ({ ...p, addressLine: e.target.value }))}
                    style={inputStyle}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.4rem' }}>
                <button type="submit" disabled={savingProfile} style={primaryBtnStyle(savingProfile)}>
                  {savingProfile ? 'Đang lưu...' : 'Lưu thông tin hồ sơ'}
                </button>
              </div>
            </form>
          )}

          {/* KHU VỰC XÁC MINH SỐ ĐIỆN THOẠI */}
          {!loadingProfile && !profile.phoneVerified && (
            <div
              style={{
                marginTop: '1.35rem',
                paddingTop: '1.25rem',
                borderTop: '1px dashed #cbd5e1',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '0.75rem',
                }}
              >
                <div>
                  <strong style={{ fontSize: '0.9rem', color: '#0f172a', display: 'block' }}>
                    Xác minh số điện thoại đặt hàng
                  </strong>
                  <span style={{ fontSize: '0.82rem', color: '#64748b' }}>
                    Lưu số điện thoại ở trên, sau đó nhận mã OTP 6 chữ số để kích hoạt quyền đặt hàng.
                  </span>
                </div>
                <button
                  type="button"
                  disabled={otpBusy || otpCooldown > 0 || !profile.phone.trim()}
                  onClick={handleSendPhoneOtp}
                  style={{
                    padding: '0.55rem 0.95rem',
                    borderRadius: '6px',
                    border: '1px solid #2563eb',
                    background: '#eff6ff',
                    color: '#1d4ed8',
                    fontWeight: 600,
                    fontSize: '0.84rem',
                    cursor: otpBusy || otpCooldown > 0 || !profile.phone.trim() ? 'not-allowed' : 'pointer',
                    opacity: otpBusy || otpCooldown > 0 || !profile.phone.trim() ? 0.65 : 1,
                  }}
                >
                  {otpCooldown > 0 ? `Gửi lại sau ${otpCooldown}s` : 'Gửi mã OTP xác minh'}
                </button>
              </div>

              {otpMsg && (
                <div
                  role={otpMsg.type === 'error' ? 'alert' : 'status'}
                  style={{
                    marginTop: '0.85rem',
                    background: otpMsg.type === 'error' ? '#fef2f2' : '#f0fdf4',
                    border: `1px solid ${otpMsg.type === 'error' ? '#fecaca' : '#bbf7d0'}`,
                    color: otpMsg.type === 'error' ? '#b91c1c' : '#166534',
                    padding: '0.6rem 0.85rem',
                    borderRadius: '6px',
                    fontSize: '0.84rem',
                  }}
                >
                  {otpMsg.text}
                </div>
              )}

              {otpBoxOpen && (
                <form
                  onSubmit={handleVerifyPhoneOtp}
                  style={{
                    display: 'flex',
                    gap: '0.75rem',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    marginTop: '0.85rem',
                  }}
                >
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    required
                    placeholder="Nhập 6 chữ số OTP"
                    value={phoneOtp}
                    onChange={(e) => setPhoneOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    style={{
                      ...inputStyle,
                      width: '200px',
                      letterSpacing: '0.2rem',
                      fontWeight: 600,
                    }}
                  />
                  <button type="submit" disabled={otpBusy} style={primaryBtnStyle(otpBusy)}>
                    {otpBusy ? 'Đang kiểm tra...' : 'Xác nhận mã'}
                  </button>
                </form>
              )}
            </div>
          )}

          {otpMsg && profile.phoneVerified && (
            <div
              role="status"
              style={{
                marginTop: '1rem',
                background: '#f0fdf4',
                border: '1px solid #bbf7d0',
                color: '#166534',
                padding: '0.6rem 0.85rem',
                borderRadius: '6px',
                fontSize: '0.84rem',
              }}
            >
              {otpMsg.text}
            </div>
          )}
        </section>

        {/* KHỐI 2: ĐỔI MẬT KHẨU */}
        <section style={sectionCardStyle}>
          <h2
            style={{
              fontSize: '1.05rem',
              fontWeight: 700,
              margin: '0 0 1.25rem',
              paddingBottom: '0.85rem',
              borderBottom: '1px solid #e2e8f0',
              color: '#0f172a',
            }}
          >
            Đổi mật khẩu bảo mật
          </h2>

          {passwordMsg && (
            <div
              role={passwordMsg.type === 'error' ? 'alert' : 'status'}
              style={{
                background: passwordMsg.type === 'error' ? '#fef2f2' : '#f0fdf4',
                border: `1px solid ${passwordMsg.type === 'error' ? '#fecaca' : '#bbf7d0'}`,
                color: passwordMsg.type === 'error' ? '#b91c1c' : '#166534',
                padding: '0.65rem 0.85rem',
                borderRadius: '6px',
                marginBottom: '1.1rem',
                fontSize: '0.85rem',
              }}
            >
              {passwordMsg.text}
            </div>
          )}

          <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
              <div>
                <label htmlFor="current-password" style={labelStyle}>
                  Mật khẩu hiện tại *
                </label>
                <input
                  id="current-password"
                  type="password"
                  required
                  autoComplete="current-password"
                  value={passwords.currentPassword}
                  onChange={(e) => setPasswords((p) => ({ ...p, currentPassword: e.target.value }))}
                  style={inputStyle}
                />
              </div>

              <div>
                <label htmlFor="new-password" style={labelStyle}>
                  Mật khẩu mới *
                </label>
                <input
                  id="new-password"
                  type="password"
                  required
                  autoComplete="new-password"
                  placeholder="Tối thiểu 6 ký tự"
                  value={passwords.newPassword}
                  onChange={(e) => setPasswords((p) => ({ ...p, newPassword: e.target.value }))}
                  style={inputStyle}
                />
              </div>

              <div>
                <label htmlFor="confirm-new-password" style={labelStyle}>
                  Xác nhận mật khẩu mới *
                </label>
                <input
                  id="confirm-new-password"
                  type="password"
                  required
                  autoComplete="new-password"
                  value={passwords.confirmPassword}
                  onChange={(e) => setPasswords((p) => ({ ...p, confirmPassword: e.target.value }))}
                  style={inputStyle}
                />
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.25rem' }}>
              <button type="submit" disabled={savingPassword} style={primaryBtnStyle(savingPassword)}>
                {savingPassword ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </div>
  )
}

