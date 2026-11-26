import http, { ensureCsrfToken } from './http'

export function extractIdentityError(err, fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.') {
  const data = err?.response?.data
  if (data?.fieldErrors && typeof data.fieldErrors === 'object') {
    const firstFieldMsg = Object.values(data.fieldErrors).find(Boolean)
    if (firstFieldMsg) return String(firstFieldMsg)
  }
  if (data?.detail) return data.detail
  if (data?.message) return data.message
  if (err?.response?.status === 429) return 'Thao tác quá nhanh. Vui lòng đợi 60 giây trước khi thử lại.'
  if (err?.response?.status === 403) return 'Bạn không có quyền thực hiện thao tác này hoặc tài khoản đã bị khóa.'
  if (err?.response?.status === 401) return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  return fallback
}

export async function requestPasswordResetOtp(email) {
  await ensureCsrfToken()
  const { data } = await http.post('/auth/forgot-password', { email })
  return data
}

export async function confirmPasswordReset({ email, otpCode, newPassword }) {
  await ensureCsrfToken()
  const { data } = await http.post('/auth/reset-password', { email, otpCode, newPassword })
  return data
}

export async function fetchProfile() {
  const { data } = await http.get('/account/profile')
  return data
}

export async function saveProfile(payload) {
  await ensureCsrfToken()
  const { data } = await http.put('/account/profile', payload)
  return data
}

export async function changeAccountPassword({ currentPassword, newPassword }) {
  await ensureCsrfToken()
  const { data } = await http.post('/account/change-password', { currentPassword, newPassword })
  return data
}

export async function sendPhoneOtp() {
  await ensureCsrfToken()
  const { data } = await http.post('/account/phone/send-otp')
  return data
}

export async function verifyPhoneOtp(otpCode) {
  await ensureCsrfToken()
  const { data } = await http.post('/account/phone/verify-otp', { otpCode })
  return data
}

export async function fetchAdminUsers({ query = '', status = 'ALL', page = 0, size = 15 } = {}) {
  const params = { page, size }
  if (query && query.trim()) params.query = query.trim()
  if (status && status !== 'ALL') params.status = status
  const { data } = await http.get('/admin/users', { params })
  return data
}

export async function updateAdminUserStatus(userId, { status, reason = '' }) {
  await ensureCsrfToken()
  const { data } = await http.patch(`/admin/users/${userId}/status`, { status, reason })
  return data
}

