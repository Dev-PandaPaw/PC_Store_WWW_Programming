import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { cartApi } from '../services/cart'
import { useCatalog } from './CatalogContext'
import { useAuth } from './AuthContext'
import { projectCart } from '../utils/optimisticCart'

const CartContext = createContext(null)
const emptyCart = () => ({ cartVersion: 0, items: [], totalQuantity: 0, subtotal: 0 })
const messageFor = error => error?.response?.data?.detail || 'Không thể cập nhật giỏ hàng. Vui lòng thử lại.'

export function CartProvider({ children }) {
  const { PRODUCTS } = useCatalog()
  const { user, loading: authLoading } = useAuth()
  const [cart, setCart] = useState(emptyCart)
  const [ready, setReady] = useState(false)
  const [notice, setNotice] = useState('')
  const [demo, setDemo] = useState(null)
  const cartRef = useRef(cart)
  const pending = useRef([])
  const queue = useRef(Promise.resolve())
  const generation = useRef(0)
  const enqueue = useCallback(work => {
    const result = queue.current.catch(() => undefined).then(work)
    queue.current = result.catch(() => undefined)
    return result
  }, [])
  const apply = useCallback(data => {
    cartRef.current = data
    setCart(projectCart(data, pending.current))
  }, [])
  const load = useCallback(async () => {
    const { data } = await cartApi.get()
    apply(data)
  }, [apply])

  useEffect(() => {
    const current = ++generation.current
    let active = true
    setReady(false)
    pending.current = []
    apply(emptyCart())
    if (authLoading) return () => { active = false }
    enqueue(async () => {
      if (!active) return
      try {
        await cartApi.csrf()
        let merged
        if (user?.id) {
          try { merged = await cartApi.merge() } catch (error) {
            if (active) setNotice(messageFor(error))
          }
        }
        const { data } = merged || await cartApi.get()
        if (active && current === generation.current) {
          apply(data)
          setReady(true)
        }
        if (import.meta.env.DEV) {
          // Diagnostic requests must not hold up user mutations in the queue.
          cartApi.demo().then(result => {
            if (active) setDemo(result.data)
          }).catch(() => { if (active) setDemo(null) })
        }
      } catch (error) {
        if (active) setNotice(messageFor(error))
      }
    })
    return () => { active = false }
  }, [user?.id, authLoading, apply, enqueue])

  const mutate = useCallback((request, operation = 'cart.mutation', optimistic = null) => {
    const current = generation.current
    const queuedAt = performance.now()
    if (!ready) return Promise.resolve({ success: false, message: 'Giỏ hàng chưa sẵn sàng. Vui lòng tải lại.' })
    const previous = pending.current.at(-1)
    if (optimistic?.type === 'quantity' && previous?.type === 'quantity' &&
        previous.id === optimistic.id && !previous.started) {
      previous.quantity = optimistic.quantity
      previous.request = request
      setCart(projectCart(cartRef.current, pending.current))
      return previous.promise
    }
    const entry = { ...optimistic, token: Symbol(operation), request, started: false }
    pending.current.push(entry)
    setCart(projectCart(cartRef.current, pending.current))
    const settle = () => { pending.current = pending.current.filter(item => item.token !== entry.token) }
    entry.promise = enqueue(async () => {
      if (entry.type === 'quantity') await new Promise(resolve => setTimeout(resolve, 250))
      if (current !== generation.current) return { success: false, message: 'Tài khoản đã thay đổi. Vui lòng thử lại.' }
      entry.started = true
      const startedAt = performance.now()
      if (import.meta.env.DEV) performance.measure(`${operation}.queue`, { start: queuedAt, end: startedAt })
      try {
        const { data } = await entry.request(cartRef.current.cartVersion)
        if (current !== generation.current) return { success: false, message: 'Tài khoản đã thay đổi.' }
        settle()
        apply(data)
        setNotice('')
        return { success: true }
      } catch (error) {
        if (current === generation.current) {
          settle()
          setCart(projectCart(cartRef.current, pending.current))
          setNotice(messageFor(error))
          if (error?.response?.data?.code === 'CART_VERSION_CONFLICT' ||
              error?.response?.data?.errorCode === 'CART_VERSION_CONFLICT' ||
              !error?.response || error.response.status >= 500) {
            try {
              const { data } = await cartApi.get()
              if (current === generation.current) apply(data)
            } catch { /* keep last known cart */ }
          }
        }
        return { success: false, message: messageFor(error) }
      } finally {
        if (import.meta.env.DEV) performance.measure(`${operation}.request`, { start: startedAt, end: performance.now() })
      }
    })
    return entry.promise
  }, [ready, enqueue, apply])
  const addItem = (product, quantity = 1) => {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
      return Promise.resolve({ success: false, message: 'Số lượng phải là số nguyên từ 1 đến 99.' })
    }
    return mutate(version => cartApi.add(product.id, quantity, version), 'cart.add',
      { type: 'add', id: product.id, product, quantity })
  }
  const setQuantity = useCallback((id, quantity) => {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
      return Promise.resolve({ success: false, message: 'Số lượng phải là số nguyên từ 1 đến 99.' })
    }
    return mutate(version => cartRef.current.items.find(item => item.productId === id)?.quantity === quantity
      ? Promise.resolve({ data: cartRef.current })
      : cartApi.update(id, quantity, version), 'cart.quantity', { type: 'quantity', id, quantity })
  }, [mutate])
  const selectDemo = async userId => {
    const result = await mutate(() => cartApi.selectDemo(userId))
    if (result.success) setDemo({ userId: userId || null })
    return result
  }
  const refresh = () => enqueue(async () => {
    try {
      await cartApi.csrf()
      await load()
      setReady(true)
      setNotice('')
    } catch (error) { setNotice(messageFor(error)) }
  })
  const productsById = useMemo(() => new Map(PRODUCTS.map(product => [product.id, product])), [PRODUCTS])
  const items = useMemo(() => cart.items.map(item => {
    const product = productsById.get(item.productId)
    return { ...item, id: item.productId, price: Number(item.unitPrice), image: product?.image || item.image || '' }
  }), [cart.items, productsById])
  return (
    <CartContext.Provider value={{
      items, addItem, setQuantity, removeItem: id => mutate(version => cartApi.remove(id, version), 'cart.remove', { type: 'remove', id }),
      clearCart: () => mutate(cartApi.clear, 'cart.clear', { type: 'clear' }), totalCount: cart.totalQuantity, totalAmount: Number(cart.subtotal),
      ready, refresh, cartVersion: cart.cartVersion, notice, demo, selectDemo,
      mergeCart: () => mutate(cartApi.merge),
    }}>
      {children}
      {notice && <div role="alert" style={{ position: 'fixed', bottom: 16, left: 16, right: 16,
        zIndex: 100, padding: 16, background: '#fff4ed', color: '#9a3412', border: '1px solid #fdba74' }}>
        {notice} <button type="button" onClick={refresh}>Tải lại giỏ</button>
        <button type="button" onClick={() => setNotice('')}>Đóng</button>
      </div>}
    </CartContext.Provider>
  )
}

export function useCart() { return useContext(CartContext) }
