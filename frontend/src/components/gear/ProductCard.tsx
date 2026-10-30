import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { ShoppingCart, Check } from 'lucide-react'
import type { StoreProduct } from '../../data/storeData'
import { formatVND, calculateDiscount } from '../../utils/cart'
import { useCart } from '../../contexts/CartContext'

interface ProductCardProps {
  product: StoreProduct
  onAddToCart?: (product: StoreProduct) => void
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onAddToCart,
}) => {
  const { addItem } = useCart()
  const [added, setAdded] = useState(false)

  const discount = calculateDiscount(product.originalPrice, product.price)
  const handleAdd = () => {
    if (onAddToCart) onAddToCart(product)
    else
      addItem(
        {
          id: product.id,
          name: product.name,
          price: product.price,
          image: product.image,
        },
        1,
      )
    setAdded(true)
  }

  return (
    <article className="product-card">
      <Link
        className="product-card__image"
        to={`/products/${product.slug}`}
        tabIndex={-1}
        aria-hidden="true"
        style={{
          position: 'relative',
          display: 'block',
          width: '100%',
          aspectRatio: '1 / 1',
          background: '#f8fafc',
          borderRadius: '8px',
          overflow: 'hidden',
        }}
      >
        {discount > 0 && (
          <span className="product-card__discount" style={{ zIndex: 2 }}>
            −{discount}%
          </span>
        )}

        <img
          src={product.image}
          alt=""
          width="400"
          height="400"
          loading="lazy"
          decoding="async"
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'contain',
            display: 'block',
          }}
        />
      </Link>
      <div className="product-card__content">
        <span className="product-card__brand">{product.brand}</span>
        <h3>
          <Link to={`/products/${product.slug}`}>{product.name}</Link>
        </h3>
        <div className="product-card__specs">
          {product.specs.slice(0, 3).map((spec, index) => (
            <span key={index}>{spec.label}</span>
          ))}
        </div>
        <div className="product-card__bottom">
          <div className="product-card__prices">
            {product.originalPrice > product.price && (
              <del>{formatVND(product.originalPrice)}</del>
            )}
            <strong>{formatVND(product.price)}</strong>
          </div>
          <button
            type="button"
            className={`product-card__add${added ? ' product-card__add--added' : ''}`}
            onClick={handleAdd}
            aria-label={`Thêm ${product.name} vào giỏ`}
          >
            {added ? <Check size={18} /> : <ShoppingCart size={18} />}
          </button>
        </div>
        <span className="sr-only" role="status">
          {added ? `Đã thêm ${product.name} vào giỏ hàng` : ''}
        </span>
      </div>
    </article>
  )
}

/**
 * Skeleton Loader xám bo góc chuẩn CellphoneS khi dữ liệu đang tải
 */
export const ProductCardSkeleton: React.FC = () => (
  <article
    className="product-card"
    style={{
      pointerEvents: 'none',
      border: '1px solid #f1f5f9',
      borderRadius: '10px',
      padding: '12px',
      background: '#ffffff',
    }}
  >
    <div
      style={{
        width: '100%',
        aspectRatio: '1 / 1',
        background: '#e2e8f0',
        borderRadius: '8px',
        marginBottom: '12px',
      }}
    />
    <div>
      <div
        style={{
          height: '12px',
          width: '35%',
          background: '#f1f5f9',
          borderRadius: '4px',
          marginBottom: '8px',
        }}
      />
      <div
        style={{
          height: '16px',
          width: '90%',
          background: '#e2e8f0',
          borderRadius: '4px',
          marginBottom: '6px',
        }}
      />
      <div
        style={{
          height: '16px',
          width: '65%',
          background: '#e2e8f0',
          borderRadius: '4px',
          marginBottom: '16px',
        }}
      />
      <div
        style={{
          height: '20px',
          width: '45%',
          background: '#cbd5e1',
          borderRadius: '4px',
        }}
      />
    </div>
  </article>
)
