import React, { createContext, useContext, useEffect, useState, useMemo, useCallback } from 'react'
import http from '../services/http'
import { matchesCategory, selectFlashDeals, CATEGORIES } from '../data/storeData'
import { resolveProductImage } from '../assets/productImages'
import initialProductsRaw from '../data/catalog-core.json'
import { ARTICLES as initialArticles } from '../data/articles'

const initialProducts = initialProductsRaw.map((p) => ({
  ...p,
  imageKey: p.image,
  image: resolveProductImage(p.image),
}))

const CatalogContext = createContext(null)

export function CatalogProvider({ children }) {
  const [data, setData] = useState(() => {
    try {
      const cached = localStorage.getItem('pc_catalog_cache')
      if (cached) {
        const parsed = JSON.parse(cached)
        if (Array.isArray(parsed.products) && parsed.products.length > 0) {
          return {
            ...parsed,
            products: parsed.products.map(product => ({
              ...product,
              image: resolveProductImage(product.imageKey || product.image),
            })),
          }
        }
      }
    } catch {}
    return {
      products: initialProducts,
      articles: initialArticles,
      categories: CATEGORIES,
    }
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const retry = useCallback(() => setAttempt(value => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    http.get('/catalog/storefront', { signal: controller.signal, timeout: 15000 })
      .then(({ data: fetchedData }) => {
        if (!Array.isArray(fetchedData.products) || !Array.isArray(fetchedData.articles) || !Array.isArray(fetchedData.categories)) {
          throw new Error('Invalid storefront response')
        }
        const currentRaw = JSON.stringify({
          products: fetchedData.products,
          articles: fetchedData.articles,
          categories: fetchedData.categories,
        })
        const cachedRaw = localStorage.getItem('pc_catalog_cache')

        // Nếu dữ liệu giống hệt trong cache thì giữ nguyên state, triệt tiêu hoàn toàn nhấp nháy
        if (cachedRaw !== currentRaw) {
          const formatted = {
            ...fetchedData,
            products: fetchedData.products.map(product => ({
              ...product,
              imageKey: product.image,
              image: resolveProductImage(product.image),
            })),
          }
          setData(formatted)
          try {
            localStorage.setItem('pc_catalog_cache', currentRaw)
          } catch {}
        }
      })
      .catch(() => { if (!controller.signal.aborted) setError(true) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [attempt])

  const value = useMemo(() => ({
    PRODUCTS: data.products,
    ARTICLES: data.articles,
    categories: data.categories,
    FLASH_DEAL_PRODUCTS: selectFlashDeals(data.products),
    findProduct: slug => data.products.find(product => product.slug === slug),
    productsInCategory: category => data.products.filter(product => matchesCategory(product, category)),
    loading, error, retry,
  }), [data, loading, error, retry])

  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>
}

export function useCatalog() {
  return useContext(CatalogContext)
}

export function CatalogBoundary({ children }) {
  const { loading, error, retry, PRODUCTS } = useCatalog()
  // Nếu đã có dữ liệu sản phẩm trong cache, giữ nguyên giao diện hiện tại không bị giật trang
  if (loading && (!PRODUCTS || PRODUCTS.length === 0)) {
    return <div className="store-empty" role="status">Đang tải sản phẩm…</div>
  }
  if (error && (!PRODUCTS || PRODUCTS.length === 0)) {
    return (
      <div className="store-empty" role="alert">
        <h2>Chưa tải được dữ liệu cửa hàng</h2>
        <p>Vui lòng kiểm tra kết nối và thử lại.</p>
        <button type="button" className="store-button" onClick={retry}>Thử lại</button>
      </div>
    )
  }
  return children
}
