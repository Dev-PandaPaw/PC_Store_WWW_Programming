import React from 'react'
import { Link } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'

export default function CartPage() {
  const { items, removeItem, clearCart, totalAmount } = useCart()

  if (items.length === 0) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
        <h2 style={{ fontSize: '1.5rem', marginBottom: '0.75rem' }}>Giỏ hàng của bạn đang trống</h2>
        <p style={{ color: 'var(--pc-text-muted)', marginBottom: '1.5rem' }}>Hãy dạo qua cửa hàng và chọn các sản phẩm ưng ý.</p>
        <Link to="/catalog" className="btn btn-primary">Mua sắm ngay</Link>
      </div>
    )
  }

  return (
    <div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem' }}>Giỏ Hàng Của Bạn</h1>
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {items.map(item => (
            <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '1rem', borderBottom: '1px solid var(--pc-border)' }}>
              <div>
                <h4 style={{ fontWeight: 600 }}>{item.name}</h4>
                <p style={{ color: 'var(--pc-text-muted)', fontSize: '0.9rem' }}>
                  Số lượng: {item.quantity} × {item.price.toLocaleString('vi-VN')} đ
                </p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <span style={{ fontWeight: 700 }}>
                  {(item.price * item.quantity).toLocaleString('vi-VN')} đ
                </span>
                <button onClick={() => removeItem(item.id)} style={{ color: 'var(--pc-danger)', fontWeight: 600 }}>
                  Xóa
                </button>
              </div>
            </div>
          ))}
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
            <button onClick={clearCart} style={{ color: 'var(--pc-text-muted)', fontSize: '0.85rem' }}>
              Xóa toàn bộ giỏ
            </button>
          </div>
        </div>

        <div className="card" style={{ height: 'fit-content', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, borderBottom: '1px solid var(--pc-border)', paddingBottom: '0.75rem' }}>
            Tóm tắt đơn hàng
          </h3>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>Tạm tính:</span>
            <span style={{ fontWeight: 700, fontSize: '1.2rem', color: 'var(--pc-primary)' }}>
              {totalAmount.toLocaleString('vi-VN')} đ
            </span>
          </div>
          <Link to="/checkout" className="btn btn-primary" style={{ width: '100%', padding: '0.75rem' }}>
            Tiến hành đặt hàng
          </Link>
        </div>
      </div>
    </div>
  )
}
