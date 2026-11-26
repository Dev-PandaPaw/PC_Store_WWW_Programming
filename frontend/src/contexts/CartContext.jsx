import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { cartApi } from '../services/cart'
import { pcBuildApi } from '../services/pcBuild'
import { useCatalog } from './catalogContextStore'
import { useAuth } from './authContextStore'
import { projectCart } from '../utils/optimisticCart'
import { runWithCartDeadline } from '../utils/cartDeadline'
import { cartFromLogin } from '../utils/loginCart'
import { cartStockError, productCartState } from '../utils/cartStock'
import { executeIdempotentAdd, isRetryableAddError, prepareAddAttempt } from '../utils/cartAddIdempotency'
import './CartNotice.css'
import { CheckCircle2, X } from 'lucide-react'
import { Link } from 'react-router-dom'

import { CartContext } from './cartContextStore'
const emptyCart = () => ({ cartVersion: 0, items: [], totalQuantity: 0, subtotal: 0 })
const messageFor = error => error?.response?.data?.detail ||
  (['ECONNABORTED', 'ETIMEDOUT'].includes(error?.code)
    ? 'Đồng bộ giỏ hàng quá thời gian chờ. Bấm thử lại thao tác thêm để kiểm tra và đồng bộ, không cộng trùng sản phẩm.'
    : 'Không thể cập nhật giỏ hàng. Vui lòng thử lại.')

export function CartProvider({ children }) {
  const { PRODUCTS } = useCatalog()
  const { user, loginCart, login: authenticate, loading: authLoading } = useAuth()
  const [cart, setCart] = useState(emptyCart)
  const [ready, setReady] = useState(false)
  const [notice, setNotice] = useState('')
  const [successNotice, setSuccessNotice] = useState(null)
  const [demo, setDemo] = useState(null)
  const [addingIds, setAddingIds] = useState([])
  const [stockOverrides, setStockOverrides] = useState({})
  const [retryableAdd, setRetryableAdd] = useState(null)
  const cartRef = useRef(cart)
  const pending = useRef([])
  const queue = useRef(Promise.resolve())
  const generation = useRef(0)
  const authenticating = useRef(false)
  const addingLockRef = useRef(false)

  useEffect(() => {
    if (!successNotice) return
    const timer = setTimeout(() => setSuccessNotice(null), 3500)
    return () => clearTimeout(timer)
  }, [successNotice])

  const enqueue = useCallback(work => {
    const result = queue.current.catch(() => undefined).then(work)
    queue.current = result.catch(() => undefined)
    return result
  }, [])

  const apply = useCallback(data => {
    cartRef.current = data
    if (Array.isArray(data?.items) && data.items.length > 0) {
      setStockOverrides(prev => {
        const next = { ...prev }
        for (const item of data.items) {
          if (typeof item.availableQuantity === 'number') {
            next[item.productId] = item.availableQuantity
          }
        }
        return next
      })
    }
    setCart(projectCart(data, pending.current))
  }, [])

  const load = useCallback(async () => {
    const { data } = await cartApi.get()
    apply(data)
    return data
  }, [apply])

  useEffect(() => {
    const current = ++generation.current
    let active = true
    pending.current = []
    addingLockRef.current = false
    setAddingIds([])
    setRetryableAdd(null)
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
          const availableFromError = error?.response?.data?.availableQuantity
          if (typeof availableFromError === 'number' && optimistic?.id) {
            setStockOverrides(prev => ({ ...prev, [optimistic.id]: availableFromError }))
          }
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
        return {
          success: false,
          message: messageFor(error),
          availableQuantity: error?.response?.data?.availableQuantity,
          remainingAddableQuantity: error?.response?.data?.remainingAddableQuantity,
        }
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

  const resolveKnownProduct = useCallback(product => {
    const catalogProduct = PRODUCTS.find(item => item.id === product.id) || product
    const override = stockOverrides[product.id]
    return typeof override === 'number'
      ? { ...catalogProduct, availableQuantity: override, stockOnHand: override }
      : catalogProduct
  }, [PRODUCTS, stockOverrides])

  const dispatchAddOperation = useCallback(({ knownProduct, quantity, operationId, optimistic = true }) => {
    const current = generation.current
    const queuedAt = performance.now()
    const entry = { type: 'add', id: knownProduct.id, product: knownProduct, quantity, token: Symbol(operationId) }
    const settle = () => {
      pending.current = pending.current.filter(item => item.token !== entry.token)
    }
    if (optimistic) {
      pending.current.push(entry)
      setCart(projectCart(cartRef.current, pending.current))
    }
    addingLockRef.current = true
    setAddingIds(prev => (prev.includes(knownProduct.id) ? prev : [...prev, knownProduct.id]))
    setNotice('')
    if (optimistic) {
      setSuccessNotice({ name: knownProduct.name, quantity, token: Symbol('cart.add.accepted') })
    }

    return (async () => {
      const startedAt = performance.now()
      try {
        const { data } = await runWithCartDeadline(signal => enqueue(async () => {
          // Expired queued operations must never send a delayed POST.
          signal.throwIfAborted()
          if (current !== generation.current) throw new Error('Tài khoản đã thay đổi. Vui lòng thử lại.')
          if (import.meta.env.DEV) performance.measure('cart.add.queue', { start: queuedAt, end: performance.now() })
          return executeIdempotentAdd({
            productId: knownProduct.id,
            quantity,
            operationId,
            getVersion: () => cartRef.current.cartVersion,
            addRequest: (...args) => {
              signal.throwIfAborted()
              return cartApi.add(...args, { signal })
            },
            getCart: () => {
              signal.throwIfAborted()
              return cartApi.get({ signal })
            },
            onCartLoaded: fresh => {
              if (!signal.aborted && current === generation.current) {
                // A lost response may already have committed this add on the server.
                settle()
                apply(fresh)
              }
            },
          })
        }))
        if (current !== generation.current) {
          return { success: false, message: 'Tài khoản đã thay đổi.', operationId }
        }
        settle()
        apply(data)
        setNotice('')
        setRetryableAdd(null)
        if (!optimistic) {
          setSuccessNotice({ name: knownProduct.name, quantity, token: Symbol('cart.add.success') })
        }
        return { success: true, operationId }
      } catch (error) {
        const message = messageFor(error)
        const availableFromError = error?.response?.data?.availableQuantity
        const remainingFromError = error?.response?.data?.remainingAddableQuantity
        if (current === generation.current) {
          setSuccessNotice(null)
          settle()
          setCart(projectCart(cartRef.current, pending.current))
          if (typeof availableFromError === 'number') {
            setStockOverrides(prev => ({ ...prev, [knownProduct.id]: availableFromError }))
          }
          if (isRetryableAddError(error)) {
            setRetryableAdd({ product: knownProduct, quantity, operationId })
          } else {
            setRetryableAdd(null)
          }
          setNotice(message)
        }
        return {
          success: false,
          message,
          operationId,
          availableQuantity: availableFromError,
          remainingAddableQuantity: remainingFromError,
        }
      } finally {
        if (current === generation.current) {
          addingLockRef.current = false
          setAddingIds(prev => prev.filter(id => id !== knownProduct.id))
        }
        if (import.meta.env.DEV) performance.measure('cart.add.request', { start: startedAt, end: performance.now() })
      }
    })()
  }, [enqueue, apply])

  // Creates a NEW add-to-cart operation (performs additive stock preflight and mints a fresh operationId).
  const addItem = useCallback((product, quantity = 1, onAccepted) => {
    if (authenticating.current) {
      return Promise.resolve({ success: false, message: 'Đang đăng nhập. Vui lòng chờ trong giây lát.' })
    }
    if (!ready) {
      return Promise.resolve({ success: false, message: 'Giỏ hàng chưa sẵn sàng. Vui lòng tải lại.' })
    }
    if (addingLockRef.current) {
      return Promise.resolve({ success: false, message: 'Đang xử lý thao tác thêm vào giỏ hàng. Vui lòng chờ.' })
    }

    const knownProduct = resolveKnownProduct(product)
    const projected = projectCart(cartRef.current, pending.current)
    const attempt = prepareAddAttempt({
      items: projected.items,
      product: knownProduct,
      quantity,
      isRetry: false,
    })
    if (!attempt.allowed) {
      setNotice(attempt.message)
      return Promise.resolve({ success: false, message: attempt.message })
    }

    const result = dispatchAddOperation({
      knownProduct,
      quantity,
      operationId: attempt.operationId,
    })
    onAccepted?.()
    return result
  }, [ready, resolveKnownProduct, dispatchAddOperation])

  // Retries an EXISTING unacknowledged add operation using its original operationId,
  // bypassing client-side additive stock preflight so an already-committed server cart is reconciled rather than rejected.
  const retryLastAdd = useCallback(() => {
    if (authenticating.current) {
      return Promise.resolve({ success: false, message: 'Đang đăng nhập. Vui lòng chờ trong giây lát.' })
    }
    if (!ready) {
      return Promise.resolve({ success: false, message: 'Giỏ hàng chưa sẵn sàng. Vui lòng tải lại.' })
    }
    if (addingLockRef.current) {
      return Promise.resolve({ success: false, message: 'Đang xử lý thao tác thêm vào giỏ hàng. Vui lòng chờ.' })
    }
    if (!retryableAdd) {
      return Promise.resolve({ success: false, message: 'Không có thao tác thêm nào cần thử lại.' })
    }

    const knownProduct = resolveKnownProduct(retryableAdd.product)
    const projected = projectCart(cartRef.current, pending.current)
    const attempt = prepareAddAttempt({
      items: projected.items,
      product: knownProduct,
      quantity: retryableAdd.quantity,
      isRetry: true,
      operationId: retryableAdd.operationId,
    })
    if (!attempt.allowed) {
      setNotice(attempt.message)
      return Promise.resolve({ success: false, message: attempt.message })
    }

    return dispatchAddOperation({
      knownProduct,
      quantity: retryableAdd.quantity,
      operationId: attempt.operationId,
      optimistic: false,
    })
  }, [ready, retryableAdd, resolveKnownProduct, dispatchAddOperation])

  const setQuantity = useCallback((id, quantity) => {
    if (typeof quantity !== 'number' || !Number.isInteger(quantity)) {
      const message = 'Số lượng phải là số nguyên từ 1 đến 99, không chấp nhận số thập phân.'
      setNotice(message)
      return Promise.resolve({ success: false, message })
    }
    if (quantity < 1 || quantity > 99) {
      return Promise.resolve({ success: false, message: 'Số lượng phải là số nguyên từ 1 đến 99.' })
    }
    const projected = projectCart(cartRef.current, pending.current)
    const message = cartStockError(projected.items, resolveKnownProduct({ id }), quantity, false)
    if (message) { setNotice(message); return Promise.resolve({ success: false, message }) }
    return mutate(version => cartRef.current.items.find(item => item.productId === id)?.quantity === quantity
      ? Promise.resolve({ data: cartRef.current })
      : cartApi.update(id, quantity, version), 'cart.quantity', { type: 'quantity', id, quantity })
  }, [mutate, resolveKnownProduct])

  const selectDemo = async userId => {
    const result = await mutate(() => cartApi.selectDemo(userId))
    if (result.success) setDemo({ userId: userId || null })
    return result
  }

  const refresh = () => enqueue(async () => {
    try {
      await cartApi.csrf()
      const data = await load()
      setReady(true)
      if (!retryableAdd) setNotice('')
      return data
    } catch (error) {
      setNotice(messageFor(error))
      return null
    }
  })

  const dismissNotice = () => {
    setNotice('')
    setRetryableAdd(null)
  }

  const productsById = useMemo(() => new Map(PRODUCTS.map(product => [product.id, product])), [PRODUCTS])
  const items = useMemo(() => cart.items.map(item => {
    const product = productsById.get(item.productId)
    return { ...item, id: item.productId, price: Number(item.unitPrice), image: product?.image || item.image || '' }
  }), [cart.items, productsById])

  const adding = addingIds.length > 0
  const isAdding = useCallback(productId => addingIds.includes(productId), [addingIds])

  return (
    <CartContext.Provider value={{
      items,
      addItem,
      retryLastAdd,
      adding,
      isAdding,
      setQuantity,
      removeItem: id => mutate(version => cartApi.remove(id, version), 'cart.remove', { type: 'remove', id }),
      clearCart: () => mutate(cartApi.clear, 'cart.clear', { type: 'clear' }),
      totalCount: cart.totalQuantity,
      totalAmount: Number(cart.subtotal),
      ready,
      syncing: adding || pending.current.length > 0,
      refresh,
      cartVersion: cart.cartVersion,
      notice,
      demo,
      selectDemo,
      productState: product => productCartState(cart.items, resolveKnownProduct(product)),
      reportCartError: message => setNotice(message || 'Không thể cập nhật giỏ hàng. Vui lòng thử lại.'),
      login,
      mergeCart: () => mutate(cartApi.merge),
      applyBuildProposal: (id, acknowledged) => mutate(() => pcBuildApi.confirm(id, acknowledged), 'cart.build-proposal'),
    }}>
      {children}
      {successNotice && !notice && <div className="cart-notice cart-notice--success" role="status">
        <CheckCircle2 size={24} aria-hidden="true" />
        <div className="cart-notice__content"><strong>Đã thêm vào giỏ hàng thành công</strong>
          <span>{successNotice.name}{successNotice.quantity > 1 ? ` (×${successNotice.quantity})` : ''}</span>
          <Link to="/cart" onClick={() => setSuccessNotice(null)}>Xem giỏ hàng →</Link></div>
        <button type="button" aria-label="Đóng thông báo thành công" onClick={() => setSuccessNotice(null)}><X size={18} /></button>
      </div>}
      {notice && <div role="alert" className="cart-notice">
        <strong>Không thể cập nhật giỏ hàng</strong>
        <span>{notice}</span>
        <div>
          {retryableAdd && !adding && (
            <button type="button" onClick={retryLastAdd}>Thử lại thao tác thêm</button>
          )}
          <button type="button" onClick={refresh}>Tải lại giỏ</button>
          <button type="button" aria-label="Đóng thông báo" onClick={dismissNotice}>Đóng</button>
        </div>
      </div>}
    </CartContext.Provider>
  )
}
