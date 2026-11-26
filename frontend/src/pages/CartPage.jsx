import React, { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, ShoppingBag, Trash2 } from 'lucide-react'
import { useCart } from '../contexts/cartContextStore'
import { formatVND } from '../utils/cart'
import { SELECTED_CART_STORAGE_KEY, calculateSelectedPreview } from '../utils/checkout'
import CartDemoPanel from '../components/CartDemoPanel'
import Breadcrumbs from '../components/Breadcrumbs'
import CartQuantity from '../components/CartQuantity'
import PersonalizedRecommendations from '../components/PersonalizedRecommendations'

function loadStoredSelectedIds() {
  try {
    const raw = sessionStorage.getItem(SELECTED_CART_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

export default function CartPage() {
  const { items, addItem, setQuantity, removeItem, clearCart, totalCount, ready } = useCart()
  const navigate = useNavigate()

  useEffect(() => {
    document.title = `Giỏ hàng (${totalCount}) | PixelCore`
    return () => { document.title = 'PixelCore - Cửa Hàng PC & Linh Kiện' }
  }, [totalCount])

  const [feedback, setFeedback] = useState('')
  const [clearing, setClearing] = useState(false)
  const [selectedIds, setSelectedIds] = useState(() => loadStoredSelectedIds())

  // Keep selection synchronized with current cart items (auto-selecting newly added purchasable items when first initialized)
  useEffect(() => {
    if (!ready) return
    const currentIds = items.map(item => item.id)
    setSelectedIds(prev => {
      const next = prev == null
        ? items.filter(item => item.availableQuantity !== 0).map(item => item.id)
        : prev.filter(id => currentIds.includes(id))
      try {
        sessionStorage.setItem(SELECTED_CART_STORAGE_KEY, JSON.stringify(next))
      } catch { /* ignore storage quota */ }
      return next
    })
  }, [ready, items])

  const effectiveSelectedIds = useMemo(
    () => (selectedIds == null ? items.filter(i => i.availableQuantity !== 0).map(i => i.id) : selectedIds),
    [selectedIds, items],
  )

  const toggleItem = id => {
    setFeedback('')
    setSelectedIds(prev => {
      const base = prev == null ? effectiveSelectedIds : prev
      const next = base.includes(id) ? base.filter(item => item !== id) : [...base, id]
      try {
        sessionStorage.setItem(SELECTED_CART_STORAGE_KEY, JSON.stringify(next))
      } catch { /* ignore */ }
      return next
    })
  }

  const selectableIds = useMemo(
    () => items.filter(item => item.availableQuantity !== 0).map(item => item.id),
    [items],
  )
  const allSelected = selectableIds.length > 0 && selectableIds.every(id => effectiveSelectedIds.includes(id))

  const toggleSelectAll = () => {
    setFeedback('')
    const next = allSelected ? [] : selectableIds
    setSelectedIds(next)
    try {
      sessionStorage.setItem(SELECTED_CART_STORAGE_KEY, JSON.stringify(next))
    } catch { /* ignore */ }
  }

  const preview = useMemo(
    () => calculateSelectedPreview(items, effectiveSelectedIds),
    [items, effectiveSelectedIds],
  )

  const hasOverStockSelected = preview.selectedItems.some(
    item => item.availableQuantity != null && item.quantity > item.availableQuantity,
  )

  const proceedToCheckout = () => {
    if (preview.selectedItems.length === 0) {
      setFeedback('Vui lòng chọn ít nhất một sản phẩm trong giỏ hàng để đặt mua.')
      return
    }
    if (hasOverStockSelected) {
      setFeedback('Một số sản phẩm đã chọn đang vượt quá tồn kho khả dụng. Vui lòng điều chỉnh số lượng trước khi mua.')
      return
    }
    try {
      sessionStorage.setItem(SELECTED_CART_STORAGE_KEY, JSON.stringify(effectiveSelectedIds))
    } catch { /* ignore */ }
    navigate('/checkout', { state: { selectedProductIds: effectiveSelectedIds } })
  }

  const clear = async () => {
    if (clearing) return
    setClearing(true)
    try {
      const result = await clearCart()
      setFeedback(result.success ? '' : result.message)
      if (result.success) {
        setSelectedIds([])
        try { sessionStorage.removeItem(SELECTED_CART_STORAGE_KEY) } catch { /* ignore */ }
      }
    } finally { setClearing(false) }
  }

  return (
    <div className="cart-page">
      <Breadcrumbs items={[{ label: 'Giỏ hàng' }]} />
      <CartDemoPanel />
      <Link className="store-button" to="/build-pc">Hoàn thiện cấu hình từ giỏ hàng</Link>
      <Link className="text-link" to="/orders">Đơn hàng của tôi</Link>
      <div className="page-heading">
        <p className="section-eyebrow">GÓC MÁY TIẾP THEO CỦA BẠN</p>
        <h1>Giỏ hàng ({totalCount}).</h1>
      </div>
      {feedback && <p className="order-error" role="alert">{feedback}</p>}
      {!ready ? <p className="cart-loading" role="status">Đang tải giỏ hàng…</p> : items.length === 0 ? (
        <div className="store-empty">
          <ShoppingBag size={42} />
          <h2>Giỏ hàng của bạn đang trống</h2>
          <p>Khám phá PC và các linh kiện để bắt đầu.</p>
          <Link className="store-button" to="/catalog">
            Khám phá sản phẩm <ArrowRight size={16} />
          </Link>
        </div>
      ) : (
        <div className="cart-layout">
          <div className="cart-items">
            <div className="cart-select-toolbar">
              <label className="cart-checkbox-label">
                <input
                  type="checkbox"
                  checked={allSelected}
                  disabled={clearing || selectableIds.length === 0}
                  onChange={toggleSelectAll}
                />
                <span>Chọn tất cả sản phẩm còn hàng ({effectiveSelectedIds.length}/{items.length})</span>
              </label>
            </div>
            {items.map((item) => {
              const checked = effectiveSelectedIds.includes(item.id)
              const soldOut = item.availableQuantity === 0
              const overStock = !soldOut && item.availableQuantity != null && item.quantity > item.availableQuantity
              return (
                <article className={`cart-item${checked ? ' is-selected' : ''}`} key={item.id}>
                  <label className="cart-item__check" aria-label={`Chọn mua ${item.name}`}>
                    <input
                      type="checkbox"
                      checked={checked}
                      disabled={clearing || soldOut}
                      onChange={() => toggleItem(item.id)}
                    />
                  </label>
                  <img src={item.image} alt="" width="100" height="100" loading="lazy" decoding="async" />
                  <div className="cart-item__info">
                    <h2>{item.name}</h2>
                    <span>{formatVND(item.price)}</span>
                    {soldOut && (
                      <p className="cart-quantity-error" role="status">
                        Sản phẩm đã hết hàng — không thể chọn mua.
                      </p>
                    )}
                    {overStock && (
                      <p className="cart-quantity-error" role="status">
                        Chỉ còn {item.availableQuantity} sản phẩm khả dụng — vui lòng giảm số lượng.
                      </p>
                    )}
                    <CartQuantity {...item} onUpdate={setQuantity} disabled={clearing} />
                  </div>
                  <div className="cart-item__total">
                    <strong>{formatVND(item.price * item.quantity)}</strong>
                    <button
                      aria-label={`Xóa ${item.name}`}
                      disabled={clearing}
                      onClick={async () => {
                        const result = await removeItem(item.id)
                        setFeedback(result.success ? '' : result.message)
                      }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </article>
              )
            })}
            <button className="cart-clear" onClick={clear} disabled={clearing}>
              {clearing ? 'Đang xóa…' : 'Xóa toàn bộ giỏ hàng'}
            </button>
          </div>
          <aside className="cart-summary">
            <button
              type="button"
              className="store-button"
              onClick={proceedToCheckout}
              disabled={preview.selectedItems.length === 0 || hasOverStockSelected || clearing}
            >
              Mua hàng ({preview.selectedQuantity}) <ArrowRight size={16} />
            </button>
            <h2>Tổng quan chọn mua</h2>
            <div>
              <span>Mặt hàng đã chọn</span>
              <strong>{preview.selectedSkuCount}/{items.length}</strong>
            </div>
            <div>
              <span>Số lượng đặt mua</span>
              <strong>{preview.selectedQuantity}</strong>
            </div>
            <div className="cart-summary__total">
              <span>Tạm tính</span>
              <strong>{formatVND(preview.selectedSubtotal)}</strong>
            </div>
            <p>Các sản phẩm không chọn sẽ được giữ nguyên trong giỏ hàng của bạn.</p>
            <Link className="store-button" to="/catalog">
              Tiếp tục mua sắm <ArrowRight size={16} />
            </Link>
            <Link className="text-link" to="/advisory">
              Tìm cấu hình phù hợp <ArrowRight size={15} />
            </Link>
          </aside>
        </div>
      )}

      <PersonalizedRecommendations
        id="cart-personalized-recommendations"
        context="cart"
        eyebrow="PixelCore AI"
        title="Linh kiện gợi ý thêm cho dàn máy của bạn."
        description="Được đề xuất tự động dựa trên độ tương thích với các linh kiện trong giỏ hàng."
        onAddToCart={(product, quantity = 1) => addItem(product, quantity)}
      />
    </div>
  )
}
