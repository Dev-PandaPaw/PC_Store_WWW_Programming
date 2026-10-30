import React, { createContext, useContext, useState, useEffect } from 'react'
import http from '../services/http'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const cached = localStorage.getItem('pc_auth_user')
      return cached ? JSON.parse(cached) : null
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    http.get('/auth/me')
      .then(res => {
        setUser(res.data)
        try {
          localStorage.setItem('pc_auth_user', JSON.stringify(res.data))
        } catch {}
      })
      .catch(() => {
        setUser(prev => {
          if (prev !== null) {
            try {
              localStorage.removeItem('pc_auth_user')
            } catch {}
          }
          return null
        })
      })
  }, [])

  const login = async (username, password) => {
    const res = await http.post('/auth/login', { username, password })
    setUser(res.data)
    try {
      localStorage.setItem('pc_auth_user', JSON.stringify(res.data))
    } catch {}
    return res.data
  }

  const register = async ({ email, password, fullName, phone }) => {
    const res = await http.post('/auth/register', { email, password, fullName, phone })
    return res.data
  }

  const logout = async () => {
    try {
      await http.post('/auth/logout')
    } finally {
      setUser(null)
      try {
        localStorage.removeItem('pc_auth_user')
      } catch {}
    }
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
