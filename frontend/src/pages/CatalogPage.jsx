import React, { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { SlidersHorizontal, Search, ArrowLeft, LoaderCircle, Sparkles } from 'lucide-react'
import {
  CATEGORIES,
  CATEGORY_LABELS,
  matchesCategory,
} from '../data/storeData'
import CategoryIcon from '../components/CategoryIcon'
import { ProductCard } from '../components/gear/ProductCard'
import { useCatalog, CatalogBoundary } from '../contexts/CatalogContext'
import { searchProducts } from '../services/productSearch'

export default function CatalogPage() {
  return <CatalogBoundary><CatalogContent /></CatalogBoundary>
}

function CatalogContent() {
  const { PRODUCTS } = useCatalog()
  const [params, setParams] = useSearchParams()
  const category = params.get('category') || 'ALL'
  const keyword = params.get('keyword') || ''
  const [draft, setDraft] = useState(keyword)
  const sort = params.get('sort') || 'featured'
  const dealsOnly = params.get('deals') === '1'
  const [searchState, setSearchState] = useState({ key: '', products: [], summary: '', mode: '', error: false, pending: false })
  const [attempt, setAttempt] = useState(0)
  const searchKey = JSON.stringify([draft.trim(), category, sort, dealsOnly, attempt])
  const searching = Boolean(draft.trim()) && (searchState.key !== searchKey || searchState.pending)
  const searchError = Boolean(draft.trim()) && searchState.key === searchKey && searchState.error

  useEffect(() => { setDraft(keyword) }, [keyword])
  useEffect(() => {
    if (draft.trim() === keyword.trim()) return
    const timer = setTimeout(() => {
      setParams(previous => {
        const next = new URLSearchParams(previous)
        if (draft.trim()) next.set('keyword', draft.trim())
        else next.delete('keyword')
        return next
      }, { replace: true })
    }, 500)
    return () => clearTimeout(timer)
  }, [draft, keyword, setParams])

  useEffect(() => {
    if (!draft.trim()) return
    const controller = new AbortController()
    let active = true
    setSearchState(previous => ({ ...previous, key: searchKey, pending: true, error: false }))
    const timer = setTimeout(() => {
      searchProducts({ keyword: draft.trim(), category, sort, deals: dealsOnly }, controller.signal)
        .then(result => {
          if (active) setSearchState({ ...result, key: searchKey, pending: false, error: false })
        })
        .catch(() => {
          if (active) setSearchState({ key: searchKey, products: [], summary: '', mode: '', pending: false, error: true })
        })
    }, 500)
    return () => { active = false; clearTimeout(timer); controller.abort() }
  }, [draft, category, sort, dealsOnly, searchKey])
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
    if (draft.trim()) return searchState.key === searchKey && !searchState.pending ? searchState.products : []
    const result = PRODUCTS.filter(
      (product) =>
        matchesCategory(product, category) &&
        (!dealsOnly || product.originalPrice > product.price),
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
  }, [PRODUCTS, category, draft, sort, dealsOnly, searchState, searchKey])

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
                placeholder="Thử: chuột giá rẻ chơi game, PC cá nhân dưới 20 triệu..."
                maxLength={300}
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') update('keyword', draft.trim()) }}
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
          <div className="catalog-search-suggestions" aria-label="Gợi ý tìm kiếm">
            <span><Sparkles size={14} /> Tìm theo nhu cầu</span>
            {['Chuột giá rẻ chơi game', 'PC cá nhân', 'Màn hình làm việc dưới 4 triệu'].map(query => (
              <button type="button" key={query} onClick={() => update('keyword', query)}>{query}</button>
            ))}
          </div>
          <p className="catalog-count" role="status" aria-live="polite">
            {searching ? 'Đang tìm sản phẩm phù hợp…' : searchError ? 'Chưa tải được kết quả tìm kiếm' : `${products.length} sản phẩm`}
            {draft.trim() && <> cho “{draft.trim()}”</>}
          </p>
          {!searching && !searchError && draft.trim() && searchState.summary && (
            <p className="catalog-search-summary"><Sparkles size={15} />{searchState.summary}</p>
          )}
          {searching ? (
            <div className="store-empty" aria-busy="true"><LoaderCircle className="catalog-search-spinner" size={30} /><p>Đang tìm sản phẩm theo nhu cầu của bạn…</p></div>
          ) : searchError ? (
            <div className="store-empty" role="alert"><h2>Chưa tải được kết quả</h2><p>Vui lòng kiểm tra kết nối và thử lại.</p><button type="button" className="store-button" onClick={() => setAttempt(value => value + 1)}>Thử lại</button></div>
          ) : products.length ? (
            <div className="product-grid product-grid--catalog">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <div className="store-empty">
              <Search size={36} />
              <h2>Chưa tìm thấy sản phẩm phù hợp</h2>
              <p>Thử điều chỉnh ngân sách, nhu cầu hoặc chọn danh mục khác.</p>
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
