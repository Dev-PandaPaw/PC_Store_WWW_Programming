import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ShoppingBag, Minus, Plus, Trash2 } from 'lucide-react'
import { useCart } from '../contexts/CartContext'
import { formatVND } from '../utils/cart'

export default function CartPage() {
  const { items, addItem, removeItem, clearCart, totalAmount, totalCount } =
    useCart()

  React.useEffect(() => {
    document.title = `Giỏ hàng (${totalCount}) | PixelCore`
    return () => {
      document.title = 'PixelCore - Cửa Hàng PC & Linh Kiện'
    }
  }, [totalCount])

  const update = (item, delta) =>
    item.quantity + delta <= 0 ? removeItem(item.id) : addItem(item, delta)
  return (
    <div className="cart-page">
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <span>Giỏ hàng</span>
      </nav>
      <div className="page-heading">
        <p className="section-eyebrow">GÓC MÁY TIẾP THEO CỦA BẠN</p>
        <h1>Giỏ hàng ({totalCount}).</h1>
      </div>
      {items.length === 0 ? (
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
                <img src={item.image} alt="" width="100" height="100" />
                <div className="cart-item__info">
                  <h2>{item.name}</h2>
                  <span>{formatVND(item.price)}</span>
                  <div className="cart-quantity">
                    <button
                      onClick={() => update(item, -1)}
                      aria-label={`Giảm số lượng ${item.name}`}
                    >
                      <Minus size={14} />
                    </button>
                    <span>{item.quantity}</span>
                    <button
                      onClick={() => update(item, 1)}
                      aria-label={`Tăng số lượng ${item.name}`}
                    >
                      <Plus size={14} />
                    </button>
                  </div>
                </div>
                <div className="cart-item__total">
                  <strong>{formatVND(item.price * item.quantity)}</strong>
                  <button
                    aria-label={`Xóa ${item.name}`}
                    onClick={() => removeItem(item.id)}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </article>
            ))}
            <button className="cart-clear" onClick={clearCart}>
              Xóa toàn bộ giỏ hàng
            </button>
          </div>
          <aside className="cart-summary">
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
