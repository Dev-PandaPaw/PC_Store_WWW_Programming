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
