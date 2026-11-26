import React, { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../contexts/authContextStore'
import {
  extractIdentityError,
  fetchAdminUsers,
  updateAdminUserStatus,
} from '../services/identity'

const tableHeaderStyle = {
  textAlign: 'left',
  padding: '0.75rem 0.9rem',
  fontSize: '0.8rem',
  fontWeight: 700,
  color: '#475569',
  borderBottom: '1px solid #e2e8f0',
  background: '#f8fafc',
  whiteSpace: 'nowrap',
}

const tableCellStyle = {
  padding: '0.8rem 0.9rem',
  fontSize: '0.86rem',
  color: '#0f172a',
  borderBottom: '1px solid #f1f5f9',
  verticalAlign: 'middle',
}

function formatRoleLabel(role) {
  const clean = String(role || '').replace(/^ROLE_/, '')
  if (clean === 'ADMIN') return 'ADMIN'
  if (clean === 'STAFF') return 'STAFF'
  return 'CUSTOMER'
}

function formatDate(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  } catch {
    return '—'
  }
}

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth()

  const [queryInput, setQueryInput] = useState('')
  const [appliedQuery, setAppliedQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(0)

  const [data, setData] = useState({ items: [], page: 0, size: 15, totalElements: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const [lockingUser, setLockingUser] = useState(null)
  const [lockReason, setLockReason] = useState('')
  const [submittingId, setSubmittingId] = useState('')

  const loadUsers = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await fetchAdminUsers({
        query: appliedQuery,
        status: statusFilter,
        page,
        size: 15,
      })
      setData(res)
    } catch (err) {
      setError(extractIdentityError(err, 'Không tải được danh sách người dùng.'))
    } finally {
      setLoading(false)
    }
  }, [appliedQuery, statusFilter, page])

  useEffect(() => {
    document.title = 'Quản lý tài khoản | PixelCore Admin'
    return () => {
      document.title = 'PixelCore - Cửa Hàng PC & Linh Kiện'
    }
  }, [])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    setPage(0)
    setAppliedQuery(queryInput.trim())
  }

  const handleConfirmLock = async (e) => {
    e.preventDefault()
    if (!lockingUser || submittingId) return
    const reason = lockReason.trim()
    if (!reason) {
      setError('Vui lòng nhập lý do khóa tài khoản.')
      return
    }

    setError('')
    setNotice('')
    setSubmittingId(lockingUser.id)
    try {
      const updated = await updateAdminUserStatus(lockingUser.id, {
        status: 'LOCKED',
        reason,
      })
      setData((prev) => ({
        ...prev,
        items: prev.items.map((item) => (item.id === updated.id ? updated : item)),
      }))
      setNotice(`Đã khóa tài khoản ${updated.email} và thu hồi phiên đăng nhập.`)
      setLockingUser(null)
      setLockReason('')
    } catch (err) {
      setError(extractIdentityError(err, 'Không thể khóa tài khoản này.'))
    } finally {
      setSubmittingId('')
    }
  }

  const handleUnlock = async (targetUser) => {
    if (submittingId) return
    setError('')
    setNotice('')
    setSubmittingId(targetUser.id)
    try {
      const updated = await updateAdminUserStatus(targetUser.id, {
        status: 'ACTIVE',
      })
      setData((prev) => ({
        ...prev,
        items: prev.items.map((item) => (item.id === updated.id ? updated : item)),
      }))
      setNotice(`Đã mở khóa tài khoản ${updated.email}.`)
    } catch (err) {
      setError(extractIdentityError(err, 'Không thể mở khóa tài khoản này.'))
    } finally {
      setSubmittingId('')
    }
  }

  return (
    <div style={{ maxWidth: '1240px', margin: '1.75rem auto', padding: '0 1.25rem' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-end',
          flexWrap: 'wrap',
          gap: '1rem',
          marginBottom: '1.25rem',
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.4rem', fontWeight: 700, margin: 0, color: '#0f172a' }}>
            Quản Lý Tài Khoản Người Dùng
          </h1>
          <p style={{ margin: '0.3rem 0 0', fontSize: '0.86rem', color: '#64748b' }}>
            Tổng cộng {data.totalElements || 0} tài khoản trong hệ thống.
          </p>
        </div>

        <form
          onSubmit={handleSearchSubmit}
          style={{ display: 'flex', gap: '0.55rem', flexWrap: 'wrap', alignItems: 'center' }}
        >
          <input
            type="search"
            placeholder="Tìm theo email, họ tên, SĐT..."
            value={queryInput}
            onChange={(e) => setQueryInput(e.target.value)}
            style={{
              width: '250px',
              padding: '0.55rem 0.8rem',
              fontSize: '0.86rem',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              background: '#ffffff',
            }}
          />
          <select
            aria-label="Lọc trạng thái tài khoản"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(0)
            }}
            style={{
              padding: '0.55rem 0.75rem',
              fontSize: '0.86rem',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              background: '#ffffff',
            }}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang hoạt động</option>
            <option value="LOCKED">Đã bị khóa</option>
          </select>
          <button
            type="submit"
            style={{
              padding: '0.56rem 1rem',
              background: '#2563eb',
              color: '#ffffff',
              border: 'none',
              borderRadius: '6px',
              fontWeight: 600,
              fontSize: '0.85rem',
              cursor: 'pointer',
            }}
          >
            Tìm kiếm
          </button>
          {(appliedQuery || statusFilter !== 'ALL') && (
            <button
              type="button"
              onClick={() => {
                setQueryInput('')
                setAppliedQuery('')
                setStatusFilter('ALL')
                setPage(0)
              }}
              style={{
                padding: '0.56rem 0.85rem',
                background: '#f1f5f9',
                color: '#334155',
                border: '1px solid #cbd5e1',
                borderRadius: '6px',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
              }}
            >
              Đặt lại
            </button>
          )}
        </form>
      </div>

      {error && (
        <div
          role="alert"
          style={{
            background: '#fef2f2',
            border: '1px solid #fecaca',
            color: '#b91c1c',
            padding: '0.7rem 0.9rem',
            borderRadius: '6px',
            marginBottom: '1rem',
            fontSize: '0.86rem',
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
            padding: '0.7rem 0.9rem',
            borderRadius: '6px',
            marginBottom: '1rem',
            fontSize: '0.86rem',
          }}
        >
          {notice}
        </div>
      )}

      {lockingUser && (
        <form
          onSubmit={handleConfirmLock}
          style={{
            background: '#fffbeb',
            border: '1px solid #fde68a',
            borderRadius: '8px',
            padding: '1rem 1.25rem',
            marginBottom: '1.15rem',
            display: 'flex',
            flexWrap: 'wrap',
            gap: '0.75rem',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ flex: '1 1 280px' }}>
            <strong style={{ display: 'block', fontSize: '0.9rem', color: '#92400e', marginBottom: '0.25rem' }}>
              Khóa tài khoản: {lockingUser.fullName} ({lockingUser.email})
            </strong>
            <input
              type="text"
              required
              autoFocus
              maxLength={255}
              placeholder="Nhập lý do khóa tài khoản (bắt buộc)..."
              value={lockReason}
              onChange={(e) => setLockReason(e.target.value)}
              style={{
                width: '100%',
                boxSizing: 'border-box',
                padding: '0.55rem 0.75rem',
                fontSize: '0.86rem',
                border: '1px solid #fcd34d',
                borderRadius: '6px',
                background: '#ffffff',
              }}
            />
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              type="submit"
              disabled={Boolean(submittingId)}
              style={{
                padding: '0.55rem 0.95rem',
                background: '#dc2626',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                fontWeight: 600,
                fontSize: '0.84rem',
                cursor: 'pointer',
              }}
            >
              {submittingId === lockingUser.id ? 'Đang khóa...' : 'Xác nhận khóa'}
            </button>
            <button
              type="button"
              onClick={() => {
                setLockingUser(null)
                setLockReason('')
              }}
              style={{
                padding: '0.55rem 0.9rem',
                background: '#ffffff',
                color: '#334155',
                border: '1px solid #cbd5e1',
                borderRadius: '6px',
                fontWeight: 600,
                fontSize: '0.84rem',
                cursor: 'pointer',
              }}
            >
              Hủy
            </button>
          </div>
        </form>
      )}

      <div
        style={{
          background: '#ffffff',
          border: '1px solid #e2e8f0',
          borderRadius: '10px',
          overflowX: 'auto',
          boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={tableHeaderStyle}>Họ và tên</th>
              <th style={tableHeaderStyle}>Email</th>
              <th style={tableHeaderStyle}>Số điện thoại</th>
              <th style={tableHeaderStyle}>Vai trò</th>
              <th style={tableHeaderStyle}>Trạng thái</th>
              <th style={tableHeaderStyle}>Ngày tạo</th>
              <th style={{ ...tableHeaderStyle, textAlign: 'right' }}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} style={{ ...tableCellStyle, textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                  Đang tải danh sách tài khoản...
                </td>
              </tr>
            ) : data.items.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ ...tableCellStyle, textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                  Không tìm thấy tài khoản phù hợp.
                </td>
              </tr>
            ) : (
              data.items.map((item) => {
                const isSelf = currentUser?.id && currentUser.id === item.id
                const isActive = item.status === 'ACTIVE'
                const roles = Array.from(item.roles || []).map(formatRoleLabel)

                return (
                  <tr key={item.id}>
                    <td style={tableCellStyle}>
                      <div style={{ fontWeight: 600 }}>{item.fullName}</div>
                      {item.province && (
                        <div style={{ fontSize: '0.76rem', color: '#64748b' }}>
                          {[item.district, item.province].filter(Boolean).join(', ')}
                        </div>
                      )}
                    </td>
                    <td style={tableCellStyle}>{item.email}</td>
                    <td style={tableCellStyle}>
                      {item.phone ? (
                        <div>
                          <span>{item.phone}</span>
                          <span
                            style={{
                              display: 'block',
                              fontSize: '0.73rem',
                              color: item.phoneVerified ? '#15803d' : '#b45309',
                            }}
                          >
                            {item.phoneVerified ? 'Đã xác minh' : 'Chưa xác minh'}
                          </span>
                        </div>
                      ) : (
                        <span style={{ color: '#94a3b8' }}>—</span>
                      )}
                    </td>
                    <td style={tableCellStyle}>
                      <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                        {roles.map((r) => (
                          <span
                            key={r}
                            style={{
                              fontSize: '0.72rem',
                              fontWeight: 700,
                              padding: '0.18rem 0.48rem',
                              borderRadius: '4px',
                              background: r === 'ADMIN' ? '#dbeafe' : '#f1f5f9',
                              color: r === 'ADMIN' ? '#1d4ed8' : '#334155',
                            }}
                          >
                            {r}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td style={tableCellStyle}>
                      <span
                        style={{
                          display: 'inline-block',
                          fontSize: '0.76rem',
                          fontWeight: 600,
                          padding: '0.2rem 0.55rem',
                          borderRadius: '999px',
                          background: isActive ? '#dcfce7' : '#fee2e2',
                          color: isActive ? '#166534' : '#991b1b',
                        }}
                      >
                        {isActive ? 'Hoạt động' : 'Đã khóa'}
                      </span>
                      {!isActive && item.statusReason && (
                        <div style={{ fontSize: '0.75rem', color: '#b91c1c', marginTop: '0.25rem', maxWidth: '220px' }}>
                          Lý do: {item.statusReason}
                        </div>
                      )}
                    </td>
                    <td style={tableCellStyle}>{formatDate(item.createdAt)}</td>
                    <td style={{ ...tableCellStyle, textAlign: 'right', whiteSpace: 'nowrap' }}>
                      {isSelf ? (
                        <span style={{ fontSize: '0.78rem', color: '#64748b' }}>Tài khoản hiện tại</span>
                      ) : isActive ? (
                        <button
                          type="button"
                          disabled={Boolean(submittingId)}
                          onClick={() => {
                            setLockingUser(item)
                            setLockReason('')
                            setError('')
                          }}
                          style={{
                            padding: '0.4rem 0.75rem',
                            border: '1px solid #fecaca',
                            background: '#fef2f2',
                            color: '#b91c1c',
                            borderRadius: '6px',
                            fontSize: '0.8rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                          }}
                        >
                          Khóa
                        </button>
                      ) : (
                        <button
                          type="button"
                          disabled={Boolean(submittingId)}
                          onClick={() => handleUnlock(item)}
                          style={{
                            padding: '0.4rem 0.75rem',
                            border: '1px solid #bbf7d0',
                            background: '#f0fdf4',
                            color: '#166534',
                            borderRadius: '6px',
                            fontSize: '0.8rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                          }}
                        >
                          {submittingId === item.id ? 'Đang mở...' : 'Mở khóa'}
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {data.totalPages > 1 && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: '1rem',
            fontSize: '0.85rem',
            color: '#475569',
          }}
        >
          <span>
            Trang {data.page + 1} / {data.totalPages}
          </span>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              type="button"
              disabled={page <= 0 || loading}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              style={{
                padding: '0.45rem 0.85rem',
                border: '1px solid #cbd5e1',
                borderRadius: '6px',
                background: '#ffffff',
                cursor: page <= 0 ? 'not-allowed' : 'pointer',
                opacity: page <= 0 ? 0.5 : 1,
              }}
            >
              Trang trước
            </button>
            <button
              type="button"
              disabled={page + 1 >= data.totalPages || loading}
              onClick={() => setPage((p) => p + 1)}
              style={{
                padding: '0.45rem 0.85rem',
                border: '1px solid #cbd5e1',
                borderRadius: '6px',
                background: '#ffffff',
                cursor: page + 1 >= data.totalPages ? 'not-allowed' : 'pointer',
                opacity: page + 1 >= data.totalPages ? 0.5 : 1,
              }}
            >
              Trang sau
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

