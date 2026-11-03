import { useEffect, useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'
import { orderApi, orderError } from '../services/orders'

export default function useAdminAccess(enabled = true) {
  const { user, loading } = useAuth()
  const { demo, ready } = useCart()
  const identity = `${user?.id || ''}:${demo?.userId || ''}`
  const [result, setResult] = useState(null)
  useEffect(() => {
    if (!enabled || loading || !ready) return
    let active = true
    orderApi.adminAccount().then(({ data }) => {
      if (active) setResult({ identity, account: data })
    }).catch(error => {
      if (active) setResult({ identity, error: orderError(error) })
    })
    return () => { active = false }
  }, [identity, loading, ready, enabled])
  const current = enabled && !loading && ready && result?.identity === identity ? result : null
  return { account: current?.account, error: current?.error, loading: enabled && !current }
}
