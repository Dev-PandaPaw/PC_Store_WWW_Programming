import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { useCatalog } from '../contexts/CatalogContext'
import { ProductCard } from './gear/ProductCard'

export default function ProductSection({
  id,
  eyebrow,
  title,
  description,
  category,
  tabs = [],
  limit = 4,
  onAddToCart,
}) {
  const { productsInCategory } = useCatalog()
  const [activeCategory, setActiveCategory] = useState(category)
  const products = productsInCategory(activeCategory).slice(0, limit)
  return (
    <section className="store-section" id={id}>
      <div className="store-section__heading">
        <div>
          <p className="section-eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
          {description && <p className="section-description">{description}</p>}
        </div>
        <Link className="text-link" to={`/catalog?category=${category}`}>
          Xem tất cả <ArrowRight size={17} />
        </Link>
      </div>
      {tabs.length > 0 && (
        <div className="category-tabs" aria-label={`Lọc ${title}`}>
          {[{ id: category, name: 'Tất cả' }, ...tabs].map((tab) => (
            <button
              key={tab.id}
              type="button"
              aria-pressed={activeCategory === tab.id}
              onClick={() => setActiveCategory(tab.id)}
            >
              {tab.name}
            </button>
          ))}
        </div>
      )}
      <div className="product-grid">
        {products.map((product) => (
          <ProductCard
            key={product.id}
            product={product}
            onAddToCart={onAddToCart}
          />
        ))}
      </div>
    </section>
  )
}
