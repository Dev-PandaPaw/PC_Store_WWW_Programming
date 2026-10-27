import React, { createContext, useContext, useState, useEffect, useRef } from 'react'
import http, { ensureCsrfToken } from '../services/http'
import { splitLoginResponse } from '../utils/loginCart'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [{ user, loginCart }, setSession] = useState({ user: null, loginCart: null })
  const [loading, setLoading] = useState(true)
  const revision = useRef(0)

  useEffect(() => {
    const current = revision.current
    const controller = new AbortController()
    http.get('/auth/me', { signal: controller.signal })
      .then(res => { if (!controller.signal.aborted && current === revision.current) setSession({ user: res.data, loginCart: null }) })
      .catch(() => { if (!controller.signal.aborted && current === revision.current) setSession({ user: null, loginCart: null }) })
      .finally(() => { if (!controller.signal.aborted && current === revision.current) setLoading(false) })
    return () => controller.abort()
  }, [])

  const login = async (username, password) => {
    const current = ++revision.current
    try {
      await ensureCsrfToken()
      const res = await http.post('/auth/login', { username, password })
      const session = splitLoginResponse(res.data)
      if (current === revision.current) setSession(session)
      return session.user
    } finally { if (current === revision.current) setLoading(false) }
  }

  const logout = async () => {
    const current = ++revision.current
    try {
      await ensureCsrfToken()
      await http.post('/auth/logout')
      if (current === revision.current) setSession({ user: null, loginCart: null })
    } finally { if (current === revision.current) setLoading(false) }
  }

  return (
    <AuthContext.Provider value={{ user, loginCart, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
