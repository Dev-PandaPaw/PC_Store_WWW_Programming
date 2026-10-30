import React, { createContext, useContext, useEffect, useState, useMemo, useCallback } from 'react'
import http from '../services/http'
import { matchesCategory, selectFlashDeals } from '../data/storeData'
import { resolveProductImage } from '../assets/productImages'

const CatalogContext = createContext(null)

export function CatalogProvider({ children }) {
  const [data, setData] = useState({ products: [], articles: [], categories: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const retry = useCallback(() => setAttempt(value => value + 1), [])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(false)
    http.get('/catalog/storefront', { signal: controller.signal, timeout: 15000 })
      .then(({ data }) => {
        if (!Array.isArray(data.products) || !Array.isArray(data.articles) || !Array.isArray(data.categories)) {
          throw new Error('Invalid storefront response')
        }
        setData({ ...data, products: data.products.map(product => ({
          ...product, imageKey: product.image, image: resolveProductImage(product.image),
        })) })
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
  const { loading, error, retry } = useCatalog()
  if (loading) return <div className="store-empty" role="status">Đang tải sản phẩm…</div>
  if (error) return <div className="store-empty" role="alert">
    <h2>Chưa tải được dữ liệu cửa hàng</h2>
    <p>Vui lòng kiểm tra kết nối và thử lại.</p>
    <button type="button" className="store-button" onClick={retry}>Thử lại</button>
  </div>
  return children
}
