import React from 'react'
import { Link } from 'react-router-dom'
import { ShoppingCart } from 'lucide-react'
import type { StoreProduct } from '../../data/storeData'
import { formatVND, calculateDiscount } from '../../utils/cart'
import { useCart } from '../../contexts/CartContext'
import ProductStock from '../ProductStock'

interface ProductCardProps {
  product: StoreProduct
  onAddToCart?: (product: StoreProduct) => Promise<{ success: boolean, message?: string }> | { success: boolean, message?: string }
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onAddToCart,
}) => {
  const { addItem, ready, productState, reportCartError } = useCart()
  const availability = productState(product)
  const discount = calculateDiscount(product.originalPrice, product.price)
  const handleAdd = async () => {
    try {
      const result = onAddToCart ? await onAddToCart(product) : await addItem(product, 1)
      if (!result.success) reportCartError(result.message || 'Không thể thêm vào giỏ hàng.')
    } catch {
      reportCartError('Không thể thêm vào giỏ hàng. Vui lòng thử lại.')
    }
  }

  return (
    <article className="product-card">
      <Link
        className="product-card__image"
        to={`/products/${product.slug}`}
        tabIndex={-1}
        aria-hidden="true"
      >
        {discount > 0 && (
          <span className="product-card__discount">−{discount}%</span>
        )}
        <img
          src={product.image}
          alt=""
          width="400"
          height="400"
          loading="lazy"
          decoding="async"
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
            className="product-card__add"
            onClick={handleAdd}
            disabled={!ready || !availability.canAdd}
            aria-label={availability.canAdd ? `Thêm ${product.name} vào giỏ` : `${product.name}: ${availability.label}`}
            title={availability.canAdd ? "Thêm vào giỏ hàng" : availability.label}
          >
            <ShoppingCart size={18} />
          </button>
        </div>
        <ProductStock product={product} />
      </div>
    </article>
  )
}

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
