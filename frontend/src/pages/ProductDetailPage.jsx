import React, { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, ShoppingCart, Check } from 'lucide-react'
import {
  CATEGORY_LABELS,
} from '../data/storeData'
import { formatVND } from '../utils/cart'
import { useCart } from '../contexts/CartContext'
import { ProductCard } from '../components/gear/ProductCard'
import { useCatalog } from '../contexts/CatalogContext'

export default function ProductDetailPage() {
  const { findProduct, productsInCategory, loading } = useCatalog()
  const { slug } = useParams()
  const product = findProduct(slug)
  const { addItem } = useCart()
  const [added, setAdded] = useState(false)
  useEffect(() => {
    setAdded(false)
    document.title = product
      ? `${product.name} | PixelCore`
      : 'Không tìm thấy sản phẩm | PixelCore'
    return () => {
      document.title = 'PixelCore'
    }
  }, [product])
  if (!product) {
    if (loading) {
      return (
        <div className="product-page">
          <nav className="breadcrumbs" aria-label="Đường dẫn">
            <Link to="/">Trang chủ</Link>
            <span>/</span>
            <Link to="/catalog">Sản phẩm</Link>
          </nav>
          <div className="product-detail">
            <div className="product-detail__image">
              <div className="sk-box" style={{ width: '100%', aspectRatio: '1/1', borderRadius: '12px' }} />
            </div>
            <div className="product-detail__info">
              <div className="sk-box" style={{ width: '80px', height: '14px', marginBottom: '10px' }} />
              <div className="sk-box" style={{ width: '90%', height: '32px', marginBottom: '12px', borderRadius: '6px' }} />
              <div className="sk-box" style={{ width: '65%', height: '26px', marginBottom: '18px', borderRadius: '6px' }} />
              <div className="sk-box" style={{ width: '100%', height: '48px', marginBottom: '24px', borderRadius: '6px' }} />
              <div className="product-detail__price" style={{ marginBottom: '22px' }}>
                <span className="sk-box" style={{ width: '50px', height: '12px', marginBottom: '6px' }} />
                <div className="sk-box" style={{ width: '200px', height: '36px', borderRadius: '6px' }} />
              </div>
              <div className="product-detail__specs" style={{ marginBottom: '24px' }}>
                <span className="sk-box" style={{ width: '80px', height: '30px', borderRadius: '6px' }} />
                <span className="sk-box" style={{ width: '100px', height: '30px', borderRadius: '6px' }} />
                <span className="sk-box" style={{ width: '90px', height: '30px', borderRadius: '6px' }} />
              </div>
              <div className="sk-box" style={{ width: '190px', height: '46px', borderRadius: '8px', background: '#2563eb', opacity: 0.65 }} />
            </div>
          </div>
        </div>
      )
    }
    return (
      <div className="store-empty">
        <h1>Không tìm thấy sản phẩm</h1>
        <Link className="store-button" to="/catalog">
          <ArrowLeft size={16} /> Về danh mục
        </Link>
      </div>
    )
  }
  const related = productsInCategory(product.category)
    .filter((item) => item.id !== product.id)
    .slice(0, 4)
  const handleAdd = () => {
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
    <div className="product-page">
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <Link to={`/catalog?category=${product.category}`}>
          {CATEGORY_LABELS[product.category]}
        </Link>
      </nav>
      <div className="product-detail">
        <div className="product-detail__image">
          <img
            src={product.image}
            alt={product.name}
            width="600"
            height="600"
          />
        </div>
        <div className="product-detail__info">
          <p className="section-eyebrow">{product.brand}</p>
          <h1>{product.name}</h1>
          <p className="product-detail__description">{product.description}</p>
          <div className="product-detail__price">
            <span>Giá bán</span>
            <strong>{formatVND(product.price)}</strong>
            {product.originalPrice > product.price && (
              <del>{formatVND(product.originalPrice)}</del>
            )}
          </div>
          <div className="product-detail__specs">
            {product.specs.map((spec, index) => (
              <span key={index}>{spec.label}</span>
            ))}
          </div>
          <button className="store-button" onClick={handleAdd}>
            {added ? <Check size={18} /> : <ShoppingCart size={18} />}
            {added ? 'Đã thêm · Thêm một sản phẩm nữa' : 'Thêm vào giỏ hàng'}
          </button>
          {added && (
            <p className="product-detail__added" role="status">
              Sản phẩm đã ở trong giỏ. <Link to="/cart">Xem giỏ hàng →</Link>
            </p>
          )}
        </div>
      </div>
      {related.length > 0 && (
        <section className="store-section">
          <div className="store-section__heading">
            <div>
              <p className="section-eyebrow">CÙNG NHÓM SẢN PHẨM</p>
              <h2>Có thể bạn cũng thích.</h2>
            </div>
          </div>
          <div className="product-grid">
            {related.map((item) => (
              <ProductCard key={item.id} product={item} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
