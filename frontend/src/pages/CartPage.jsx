import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ShoppingBag, Trash2 } from 'lucide-react'
import { useCart } from '../contexts/CartContext'
import { formatVND } from '../utils/cart'
import CartDemoPanel from '../components/CartDemoPanel'
import CartQuantity from '../components/CartQuantity'

export default function CartPage() {
  const { items, setQuantity, removeItem, clearCart, totalAmount, totalCount, ready } =
    useCart()
  React.useEffect(() => {
    document.title = `Giỏ hàng (${totalCount}) | PixelCore`
    return () => { document.title = 'PixelCore - Cửa Hàng PC & Linh Kiện' }
  }, [totalCount])
  const [feedback, setFeedback] = React.useState('')
  const [clearing, setClearing] = React.useState(false)
  const clear = async () => {
    if (clearing) return
    setClearing(true)
    try {
      const result = await clearCart()
      setFeedback(result.success ? '' : result.message)
    } finally { setClearing(false) }
  }
  return (
    <div className="cart-page">
      <CartDemoPanel />
      <Link className="text-link" to="/orders">Đơn hàng của tôi</Link>
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <span>Giỏ hàng</span>
      </nav>
      <div className="page-heading">
        <p className="section-eyebrow">GÓC MÁY TIẾP THEO CỦA BẠN</p>
        <h1>Giỏ hàng ({totalCount}).</h1>
      </div>
      {feedback && <p className="product-detail__added" role="alert">{feedback}</p>}
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
            {items.map((item) => (
              <article className="cart-item" key={item.id}>
                <img src={item.image} alt="" width="100" height="100" loading="lazy" decoding="async" />
                <div className="cart-item__info">
                  <h2>{item.name}</h2>
                  <span>{formatVND(item.price)}</span>
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
            ))}
            <button className="cart-clear" onClick={clear} disabled={clearing}>
              {clearing ? 'Đang xóa…' : 'Xóa toàn bộ giỏ hàng'}
            </button>
          </div>
          <aside className="cart-summary">
            <Link className="store-button" to="/checkout">Đặt hàng</Link>
            <h2>Tổng quan giỏ hàng</h2>
            <div>
              <span>Sản phẩm</span>
              <strong>{totalCount}</strong>
            </div>
            <div className="cart-summary__total">
              <span>Tạm tính</span>
              <strong>{formatVND(totalAmount)}</strong>
            </div>
            <Link className="store-button" to="/catalog">
              Tiếp tục mua sắm <ArrowRight size={16} />
            </Link>
            <Link className="text-link" to="/advisory">
              Tìm cấu hình phù hợp <ArrowRight size={15} />
            </Link>
          </aside>
        </div>
      )}
    </div>
  )
}
