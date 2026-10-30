import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { cartApi } from '../services/cart'
import { useCatalog } from './CatalogContext'
import { useAuth } from './AuthContext'
import { projectCart } from '../utils/optimisticCart'
import { cartFromLogin } from '../utils/loginCart'
import { cartStockError, productCartState } from '../utils/cartStock'
import './CartNotice.css'
import { CheckCircle2, X } from 'lucide-react'
import { Link } from 'react-router-dom'

const CartContext = createContext(null)
const emptyCart = () => ({ cartVersion: 0, items: [], totalQuantity: 0, subtotal: 0 })
const messageFor = error => error?.response?.data?.detail || 'Không thể cập nhật giỏ hàng. Vui lòng thử lại.'

export function CartProvider({ children }) {
  const { PRODUCTS } = useCatalog()
  const { user, loginCart, login: authenticate, loading: authLoading } = useAuth()
  const [cart, setCart] = useState(emptyCart)
  const [ready, setReady] = useState(false)
  const [notice, setNotice] = useState('')
  const [successNotice, setSuccessNotice] = useState(null)
  const [demo, setDemo] = useState(null)
  const cartRef = useRef(cart)
  const pending = useRef([])
  const queue = useRef(Promise.resolve())
  const generation = useRef(0)
  const authenticating = useRef(false)
  useEffect(() => {
    if (!successNotice) return
    const timer = setTimeout(() => setSuccessNotice(null), 500)
    return () => clearTimeout(timer)
  }, [successNotice])
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
    pending.current = []
    setSuccessNotice(null)
    const merged = cartFromLogin(user, loginCart)
    if (!authLoading && merged) {
      apply(merged)
      setReady(true)
      setNotice('')
      setDemo(null)
      return () => { active = false }
    }
    setReady(false)
    apply(emptyCart())
    if (authLoading) return () => { active = false }
    enqueue(async () => {
      if (!active) return
      try {
        await cartApi.csrf()
        const { data } = await cartApi.get()
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
  }, [user?.id, loginCart, authLoading, apply, enqueue])

  const mutate = useCallback((request, operation = 'cart.mutation', optimistic = null) => {
    const current = generation.current
    const queuedAt = performance.now()
    if (authenticating.current) return Promise.resolve({ success: false, message: 'Đang đăng nhập. Vui lòng chờ trong giây lát.' })
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
    if (entry.type === 'add') {
      setNotice('')
      setSuccessNotice({ name: entry.product.name, token: entry.token })
      entry.onAccepted?.()
    }
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
          if (entry.type === 'add') setSuccessNotice(value => value?.token === entry.token ? null : value)
          setNotice(messageFor(error))
          if (error?.response?.data?.code === 'CART_VERSION_CONFLICT' ||
              error?.response?.data?.errorCode === 'CART_VERSION_CONFLICT' ||
              !error?.response || error.response.status === 409 || error.response.status >= 500) {
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
  const login = async (username, password) => {
    // Complete guest mutations before the server merges that session into the account cart.
    authenticating.current = true
    try { return await enqueue(() => authenticate(username, password)) }
    finally { authenticating.current = false }
  }
  const addItem = (product, quantity = 1, onAccepted) => {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
      return Promise.resolve({ success: false, message: 'Số lượng phải là số nguyên từ 1 đến 99.' })
    }
    const knownProduct = PRODUCTS.find(item => item.id === product.id) || product
    const projected = projectCart(cartRef.current, pending.current)
    const message = cartStockError(projected.items, knownProduct, quantity, true)
    if (message) { setNotice(message); return Promise.resolve({ success: false, message }) }
    setNotice('')
    return mutate(version => cartApi.add(product.id, quantity, version), 'cart.add',
      { type: 'add', id: product.id, product: knownProduct, quantity, onAccepted })
  }
  const setQuantity = useCallback((id, quantity) => {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99) {
      return Promise.resolve({ success: false, message: 'Số lượng phải là số nguyên từ 1 đến 99.' })
    }
    const projected = projectCart(cartRef.current, pending.current)
    const message = cartStockError(projected.items, PRODUCTS.find(item => item.id === id) || { id }, quantity)
    if (message) { setNotice(message); return Promise.resolve({ success: false, message }) }
    return mutate(version => cartRef.current.items.find(item => item.productId === id)?.quantity === quantity
      ? Promise.resolve({ data: cartRef.current })
      : cartApi.update(id, quantity, version), 'cart.quantity', { type: 'quantity', id, quantity })
  }, [mutate, PRODUCTS])
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
      ready, syncing: pending.current.length > 0, refresh, cartVersion: cart.cartVersion, notice, demo, selectDemo,
      productState: product => productCartState(cart.items, PRODUCTS.find(item => item.id === product.id) || product),
      reportCartError: message => setNotice(message || 'Không thể cập nhật giỏ hàng. Vui lòng thử lại.'),
      login,
      mergeCart: () => mutate(cartApi.merge),
    }}>
      {children}
      {successNotice && !notice && <div className="cart-notice cart-notice--success" role="status">
        <CheckCircle2 size={24} aria-hidden="true" />
        <div className="cart-notice__content"><strong>Đã thêm vào giỏ hàng thành công</strong>
          <span>{successNotice.name}</span>
          <Link to="/cart" onClick={() => setSuccessNotice(null)}>Xem giỏ hàng →</Link></div>
        <button type="button" aria-label="Đóng thông báo thành công" onClick={() => setSuccessNotice(null)}><X size={18} /></button>
      </div>}
      {notice && <div role="alert" className="cart-notice">
        <strong>Không thể cập nhật giỏ hàng</strong>
        <span>{notice}</span>
        <div><button type="button" onClick={refresh}>Tải lại giỏ</button>
        <button type="button" aria-label="Đóng thông báo" onClick={() => setNotice('')}>Đóng</button></div>
      </div>}
    </CartContext.Provider>
  )
}

export function useCart() { return useContext(CartContext) }
