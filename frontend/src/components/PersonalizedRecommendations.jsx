import React, { useEffect, useState, useRef } from 'react'
import { Link } from 'react-router-dom'
import { Sparkles, ChevronLeft, ChevronRight, ShoppingCart, Cpu, Check, Zap } from 'lucide-react'
import { getPersonalizedRecommendations, getSimilarProducts } from '../services/recommendations'
import { useCart } from '../contexts/cartContextStore'
import { formatVND } from '../utils/cart'
import '../styles/personalized-recommendations.css'

/**
 * Component hiển thị danh sách sản phẩm gợi ý cá nhân hóa (AI Semantic Match)
 * Dạng Carousel / Grid với nhãn độ tương đồng Cosine Similarity
 * Phụ trách: Bùi Ngọc Bửu (Nhóm 16)
 * Căn cứ: docs/KE_HOACH_PHAN_CONG_CONG_VIEC_SPRING_AI_NHOM16.md (Mục 3.5)
 */
export default function PersonalizedRecommendations({
  id = 'personalized-recommendations',
  eyebrow = 'AI SEMANTIC MATCH • COSINE SIMILARITY',
  title = 'Gợi ý dành riêng cho bạn',
  description = 'Thuật toán Vector Embedding & Cosine Similarity phân tích sở thích để đề xuất linh kiện tối ưu.',
  context = 'home',
  productId = null,
  limit = 6,
  onAddToCart = null,
  className = '',
}) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [addedId, setAddedId] = useState(null)
  const carouselRef = useRef(null)
  const { addItem, ready, productState, reportCartError } = useCart()

  useEffect(() => {
    let isMounted = true
    setLoading(true)

    const fetchRecommendations = async () => {
      try {
        let results = []
        if (productId) {
          results = await getSimilarProducts(productId, { limit })
        } else {
          results = await getPersonalizedRecommendations({ limit, context })
        }
        if (isMounted) {
          setItems(results || [])
        }
      } catch (err) {
        console.error('[PersonalizedRecommendations] Failed to load:', err)
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    fetchRecommendations()
    return () => {
      isMounted = false
    }
  }, [productId, context, limit])

  const scroll = direction => {
    if (carouselRef.current) {
      const scrollAmount = direction === 'left' ? -280 : 280
      carouselRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }

  const handleAdd = async (product, event) => {
    event.preventDefault()
    event.stopPropagation()
    try {
      const result = onAddToCart ? await onAddToCart(product) : await addItem(product, 1)
      if (result?.success !== false) {
        setAddedId(product.id)
        setTimeout(() => setAddedId(null), 1800)
      } else {
        reportCartError(result?.message || 'Không thể thêm vào giỏ hàng.')
      }
    } catch {
      reportCartError('Không thể thêm vào giỏ hàng. Vui lòng thử lại.')
    }
  }

  // Nếu không loading và không có sản phẩm nào thì ẩn component
  if (!loading && items.length === 0) {
    return null
  }

  return (
    <section className={`ai-recommendations-section ${className}`} id={id} aria-label={title}>
      <div className="ai-recommendations__head">
        <div>
          <div className="ai-recommendations__eyebrow">
            <Sparkles size={13} /> {eyebrow}
          </div>
          <h2 className="ai-recommendations__title">{title}</h2>
          {description && <p className="ai-recommendations__desc">{description}</p>}
        </div>

        <div className="ai-recommendations__controls">
          <button
            type="button"
            className="ai-recommendations__nav-btn"
            onClick={() => scroll('left')}
            aria-label="Cuộn sang trái"
          >
            <ChevronLeft size={18} />
          </button>
          <button
            type="button"
            className="ai-recommendations__nav-btn"
            onClick={() => scroll('right')}
            aria-label="Cuộn sang phải"
          >
            <ChevronRight size={18} />
          </button>
        </div>
      </div>

      <div className="ai-recommendations__carousel" ref={carouselRef}>
        {loading
          ? Array.from({ length: 4 }).map((_, index) => (
              <div className="ai-recommendations__item" key={index}>
                <div
                  style={{
                    height: '340px',
                    borderRadius: '12px',
                    background: '#e2e8f0',
                    animation: 'pulse 1.5s infinite',
                  }}
                />
              </div>
            ))
          : items.map(product => {
              const percent = product.similarityPercent || 92
              const badgeClass =
                percent >= 90
                  ? 'ai-rec-card__match-badge--high'
                  : percent >= 80
                    ? 'ai-rec-card__match-badge--good'
                    : 'ai-rec-card__match-badge--normal'

              const availability = productState ? productState(product) : { canAdd: true }
              const isAdded = addedId === product.id

              return (
                <div className="ai-recommendations__item" key={product.id}>
                  <article className="ai-rec-card">
                    <div className="ai-rec-card__badge-row">
                      <span className={`ai-rec-card__match-badge ${badgeClass}`}>
                        <Zap size={11} /> Phù hợp {percent}%
                      </span>
                    </div>

                    <Link
                      to={`/products/${product.slug}`}
                      className="ai-rec-card__image-link"
                      aria-label={product.name}
                    >
                      <img
                        src={product.image}
                        alt={product.name}
                        loading="lazy"
                        decoding="async"
                        width="240"
                        height="240"
                      />
                    </Link>

                    <div className="ai-rec-card__body">
                      {product.brand && (
                        <span className="ai-rec-card__brand">{product.brand}</span>
                      )}

                      <h3 className="ai-rec-card__name">
                        <Link to={`/products/${product.slug}`}>{product.name}</Link>
                      </h3>

                      {product.matchReason && (
                        <div className="ai-rec-card__reason" title={product.matchReason}>
                          <Sparkles size={12} />
                          <span>{product.matchReason}</span>
                        </div>
                      )}

                      {Array.isArray(product.specs) && product.specs.length > 0 && (
                        <div className="ai-rec-card__specs">
                          {product.specs.slice(0, 2).map((s, idx) => (
                            <span className="ai-rec-card__spec-tag" key={idx}>
                              {s.label || s}
                            </span>
                          ))}
                        </div>
                      )}

                      <div className="ai-rec-card__footer">
                        <div className="ai-rec-card__price-box">
                          {product.originalPrice > product.price && (
                            <span className="ai-rec-card__old-price">
                              {formatVND(product.originalPrice)}
                            </span>
                          )}
                          <strong className="ai-rec-card__price">
                            {formatVND(product.price)}
                          </strong>
                        </div>

                        <button
                          type="button"
                          className="ai-rec-card__add-btn"
                          onClick={e => handleAdd(product, e)}
                          disabled={!ready || !availability.canAdd || isAdded}
                          title={isAdded ? 'Đã thêm!' : 'Thêm vào giỏ'}
                          aria-label={`Thêm ${product.name} vào giỏ hàng`}
                        >
                          {isAdded ? <Check size={16} /> : <ShoppingCart size={16} />}
                        </button>
                      </div>
                    </div>
                  </article>
                </div>
              )
            })}
      </div>
    </section>
  )
}

