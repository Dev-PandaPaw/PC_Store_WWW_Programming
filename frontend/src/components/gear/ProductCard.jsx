import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LoaderCircle, ShoppingCart } from 'lucide-react'
import { formatVND, calculateDiscount } from '../../utils/cart'
import { useCart } from '../../contexts/cartContextStore'
import ProductStock from '../ProductStock'
import AddQuantitySelector from '../AddQuantitySelector'

export const ProductCard = ({
  product,
  onAddToCart,
}) => {
  const { addItem, ready, adding, isAdding, productState, reportCartError } = useCart()
  const availability = productState(product)
  const productAdding = isAdding(product.id)
  const maxAddable = Math.max(1, Math.min(99, availability.remaining || 99))
  const [quantity, setQuantity] = useState(1)
  const [quantityError, setQuantityError] = useState('')
  const discount = calculateDiscount(product.originalPrice, product.price)

  useEffect(() => {
    if (availability.canAdd && quantity > maxAddable) {
      setQuantity(maxAddable)
    }
  }, [availability.canAdd, maxAddable, quantity])

  const handleAdd = async () => {
    if (quantityError) {
      reportCartError(quantityError)
      return
    }
    try {
      const result = onAddToCart
        ? await onAddToCart(product, quantity)
        : await addItem(product, quantity)
      if (result?.success) {
        setQuantity(1)
        setQuantityError('')
      } else {
        reportCartError(result?.message || 'Không thể thêm vào giỏ hàng.')
      }
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
          <div className="product-card__actions">
            {availability.canAdd && (
              <AddQuantitySelector
                productName={product.name}
                value={quantity}
                onChange={setQuantity}
                max={maxAddable}
                disabled={!ready || adding || !availability.canAdd}
                compact
                error={quantityError}
                onErrorChange={setQuantityError}
              />
            )}
            <button
              type="button"
              className="product-card__add"
              onClick={handleAdd}
              disabled={!ready || adding || !availability.canAdd || Boolean(quantityError)}
              aria-busy={productAdding}
              aria-label={
                productAdding
                  ? `Đang thêm ${product.name} vào giỏ`
                  : availability.canAdd
                    ? `Thêm ${quantity} ${product.name} vào giỏ`
                    : `${product.name}: ${availability.label}`
              }
              title={
                productAdding
                  ? 'Đang xử lý…'
                  : availability.canAdd
                    ? 'Thêm vào giỏ hàng'
                    : availability.label
              }
            >
              {productAdding ? <LoaderCircle size={17} className="cart-add-spinner" /> : <ShoppingCart size={18} />}
            </button>
          </div>
        </div>
        {quantityError && (
          <small className="cart-quantity-error" role="alert">{quantityError}</small>
        )}
        <ProductStock product={product} />
      </div>
    </article>
  )
}

export const ProductCardSkeleton = () => (
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
