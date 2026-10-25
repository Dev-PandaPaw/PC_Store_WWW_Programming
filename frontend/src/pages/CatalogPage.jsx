import React, { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { SlidersHorizontal, Search, ArrowLeft } from 'lucide-react'
import {
  CATEGORIES,
  CATEGORY_LABELS,
  matchesCategory,
  normalizeSearch,
} from '../data/storeData'
import CategoryIcon from '../components/CategoryIcon'
import { ProductCard } from '../components/gear/ProductCard'
import { useCatalog, CatalogBoundary } from '../contexts/CatalogContext'

export default function CatalogPage() {
  return <CatalogBoundary><CatalogContent /></CatalogBoundary>
}

function CatalogContent() {
  const { PRODUCTS } = useCatalog()
  const [params, setParams] = useSearchParams()
  const category = params.get('category') || 'ALL'
  const keyword = params.get('keyword') || ''
  const sort = params.get('sort') || 'featured'
  const dealsOnly = params.get('deals') === '1'
  const title = dealsOnly
    ? 'Flash Deal PC & linh kiện'
    : CATEGORY_LABELS[category] || 'Sản phẩm'
  const update = (key, value) =>
    setParams(
      (previous) => {
        const next = new URLSearchParams(previous)
        if (value && value !== 'ALL' && value !== 'featured')
          next.set(key, value)
        else next.delete(key)
        return next
      },
      { replace: true },
    )
  const products = useMemo(() => {
    const tokens = normalizeSearch(keyword).trim().split(/\s+/).filter(Boolean)
    const result = PRODUCTS.filter(
      (product) =>
        matchesCategory(product, category) &&
        (!dealsOnly || product.originalPrice > product.price) &&
        tokens.every((token) =>
          normalizeSearch(
            `${product.name} ${product.brand} ${CATEGORY_LABELS[product.category]} ${product.specs.map((spec) => spec.label).join(' ')}`,
          ).includes(token),
        ),
    )
    if (sort === 'price-asc') result.sort((a, b) => a.price - b.price)
    if (sort === 'price-desc') result.sort((a, b) => b.price - a.price)
    if (sort === 'name')
      result.sort((a, b) => a.name.localeCompare(b.name, 'vi'))
    if (sort === 'featured' && dealsOnly)
      result.sort(
        (a, b) =>
          1 - b.price / b.originalPrice - (1 - a.price / a.originalPrice),
      )
    return result
  }, [PRODUCTS, category, keyword, sort, dealsOnly])

  return (
    <div className="catalog-page">
      <nav className="breadcrumbs" aria-label="Đường dẫn">
        <Link to="/">Trang chủ</Link>
        <span>/</span>
        <span>{title}</span>
      </nav>
      <div className="page-heading">
        <p className="section-eyebrow">PIXELCORE COLLECTION</p>
        <h1>{title}</h1>
        <p>
          {dealsOnly
            ? 'Ưu đãi cho PC, linh kiện và thiết bị trong góc máy của bạn.'
            : 'PC bộ, linh kiện và thiết bị cho góc máy của bạn.'}
        </p>
      </div>
      <div className="catalog-layout">
        <aside className="catalog-filters">
          <h2>
            <SlidersHorizontal size={18} /> Danh mục
          </h2>
          <button
            className={category === 'ALL' ? 'is-active' : ''}
            onClick={() => update('category', 'ALL')}
          >
            Tất cả sản phẩm <span>{PRODUCTS.length}</span>
          </button>
          {CATEGORIES.map((item) => (
            <button
              key={item.id}
              className={category === item.id ? 'is-active' : ''}
              onClick={() => update('category', item.id)}
            >
              <CategoryIcon name={item.icon} />
              <span>{item.name}</span>
            </button>
          ))}
        </aside>
        <div className="catalog-results">
          <div className="catalog-toolbar">
            <label className="catalog-search">
              <Search size={18} />
              <input
                aria-label="Tìm trong danh mục"
                placeholder="Tên sản phẩm, thương hiệu..."
                value={keyword}
                onChange={(event) => update('keyword', event.target.value)}
              />
            </label>
            <label className="catalog-sort">
              <span>Sắp xếp</span>
              <select
                aria-label="Sắp xếp sản phẩm"
                value={sort}
                onChange={(event) => update('sort', event.target.value)}
              >
                <option value="featured">Nổi bật</option>
                <option value="price-asc">Giá tăng dần</option>
                <option value="price-desc">Giá giảm dần</option>
                <option value="name">Tên A–Z</option>
              </select>
            </label>
          </div>
          <p className="catalog-count">
            {products.length} sản phẩm
            {keyword.trim() && <> cho “{keyword.trim()}”</>}
          </p>
          {products.length ? (
            <div className="product-grid product-grid--catalog">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <div className="store-empty">
              <Search size={36} />
              <h2>Chưa tìm thấy sản phẩm phù hợp</h2>
              <p>Thử từ khóa ngắn hơn hoặc chọn danh mục khác.</p>
              <button className="store-button" onClick={() => setParams({})}>
                <ArrowLeft size={16} /> Xem tất cả sản phẩm
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
