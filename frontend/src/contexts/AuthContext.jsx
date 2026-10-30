import React, { createContext, useContext, useState, useEffect, useRef } from 'react'
import http, { ensureCsrfToken } from '../services/http'
import { splitLoginResponse } from '../utils/loginCart'

const AuthContext = createContext(null)
const cacheUser = user => {
  try {
    if (user) localStorage.setItem('pc_auth_user', JSON.stringify(user))
    else localStorage.removeItem('pc_auth_user')
  } catch { /* Storage may be unavailable; the server session remains authoritative. */ }
}

export function AuthProvider({ children }) {
  const [{ user, loginCart }, setSession] = useState(() => {
    try {
      const cached = localStorage.getItem('pc_auth_user')
      return { user: cached ? JSON.parse(cached) : null, loginCart: null }
    } catch { return { user: null, loginCart: null } }
  })
  const [loading, setLoading] = useState(true)
  const revision = useRef(0)
  useEffect(() => {
    const current = revision.current
    const controller = new AbortController()
    http.get('/auth/me', { signal: controller.signal }).then(({ data }) => {
      if (!controller.signal.aborted && current === revision.current) {
        const { user } = splitLoginResponse(data)
        setSession({ user, loginCart: null }); cacheUser(user)
      }
    }).catch(() => {
      if (!controller.signal.aborted && current === revision.current) {
        setSession({ user: null, loginCart: null }); cacheUser(null)
      }
    }).finally(() => { if (!controller.signal.aborted && current === revision.current) setLoading(false) })
    return () => controller.abort()
  }, [])

  const login = async (username, password) => {
    const current = ++revision.current
    try {
      await ensureCsrfToken()
      const { data } = await http.post('/auth/login', { username, password })
      const session = splitLoginResponse(data)
      if (current === revision.current) { setSession(session); cacheUser(session.user) }
      return session.user
    } finally { if (current === revision.current) setLoading(false) }
  }
  const register = async ({ email, password, fullName, phone }) => {
    await ensureCsrfToken()
    const { data } = await http.post('/auth/register', { email, password, fullName, phone })
    return data
  }
  const logout = async () => {
    const current = ++revision.current
    try {
      await ensureCsrfToken()
      await http.post('/auth/logout')
    } finally {
      if (current === revision.current) {
        setSession({ user: null, loginCart: null }); cacheUser(null); setLoading(false)
      }
    }
  }
  return <AuthContext.Provider value={{ user, loginCart, loading, login, logout, register }}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }
