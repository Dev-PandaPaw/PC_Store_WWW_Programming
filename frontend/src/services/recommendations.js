import http from './http'
import { resolveProductImage } from '../assets/productImages'
import initialProductsRaw from '../data/catalog-core.json'
import initialGearProductsRaw from '../data/catalog-gear.json'

const allCatalogProducts = [...initialProductsRaw, ...initialGearProductsRaw]

function isUserLoggedIn() {
  try {
    const cached = localStorage.getItem('pc_auth_user')
    if (!cached) return false
    const parsed = JSON.parse(cached)
    return Boolean(parsed?.id || parsed?.email)
  } catch {
    return false
  }
}

function formatProduct(product, defaultScore = 0.92) {
  let image = product.image
  const rawKey = product.imageKey || product.image

  if (rawKey && typeof rawKey === 'string') {
    if (
      rawKey.startsWith('http://') ||
      rawKey.startsWith('https://') ||
      rawKey.startsWith('data:') ||
      rawKey.startsWith('/src/') ||
      rawKey.startsWith('/assets/') ||
      rawKey.startsWith('/@')
    ) {
      image = rawKey
    } else {
      try {
        image = resolveProductImage(rawKey)
      } catch {
        const matched = allCatalogProducts.find(
          p => p.id === product.id || p.slug === product.slug || p.name === product.name
        )
        if (matched?.image) {
          try {
            image = resolveProductImage(matched.image)
          } catch {}
        }
      }
    }
  }

  if (!image || typeof image !== 'string' || (!image.includes('/') && !image.includes('.'))) {
    const matched = allCatalogProducts.find(
      p => p.id === product.id || p.slug === product.slug || p.name === product.name
    )
    if (matched?.image) {
      try {
        image = resolveProductImage(matched.image)
      } catch {}
    } else {
      try {
        image = resolveProductImage('products/pc-gvn-i5-12400f-rtx-3050.png')
      } catch {}
    }
  }

  const score = product.similarityScore !== undefined
    ? Number(product.similarityScore)
    : defaultScore

  return {
    ...product,
    image,
    similarityScore: score,
    similarityPercent: Math.round(score <= 1 ? score * 100 : score),
    specs: Array.isArray(product.specs) ? product.specs : [],
  }
}

let vectorServiceUnavailable = false

export async function getPersonalizedRecommendations(options = {}) {
  const { limit = 6, context = 'home', user = undefined, authLoading = false } = options

  const hasUser = user !== undefined ? Boolean(user?.id || user?.email) : isUserLoggedIn()
  if (!hasUser || authLoading) {
    return getFallbackPersonalizedProducts(limit, context)
  }

  if (vectorServiceUnavailable) {
    return getFallbackPersonalizedProducts(limit, context)
  }

  try {
    const response = await http.get('/v1/recommendations/personalized', {
      params: { limit, context },
      timeout: 8000,
    })

    const rawList = Array.isArray(response.data)
      ? response.data
      : Array.isArray(response.data?.products)
        ? response.data.products.map(item => ({
            ...(item.product || item),
            similarityScore: item.similarityScore ?? item.score,
            compatibilityStatus: item.compatibilityStatus,
          }))
        : Array.isArray(response.data?.recommendations)
          ? response.data.recommendations
          : Array.isArray(response.data?.items)
            ? response.data.items
            : []

    if (rawList.length > 0) {
      return rawList.map((item, index) =>
        formatProduct(
          item,
          item.similarityScore ?? (0.96 - index * 0.03)
        )
      )
    }
  } catch (err) {
    const status = err?.response?.status
    if (status === 503 || status === 401 || status === 404) {
      vectorServiceUnavailable = true
    }
    console.debug('[Recommendations] Backend API not available or no history, using smart fallback:', err.message)
  }

  return getFallbackPersonalizedProducts(limit, context)
}


export async function getSimilarProducts(productId, options = {}) {
  const { limit = 4 } = options

  if (vectorServiceUnavailable) {
    return getFallbackPersonalizedProducts(limit, 'similar')
  }

  try {
    const response = await http.get(`/v1/recommendations/similar/${productId}`, {
      params: { limit },
      timeout: 8000,
    })

    const rawList = Array.isArray(response.data)
      ? response.data
      : Array.isArray(response.data?.products)
        ? response.data.products.map(item => ({
            ...(item.product || item),
            similarityScore: item.similarityScore ?? item.score,
            compatibilityStatus: item.compatibilityStatus,
          }))
        : Array.isArray(response.data?.items)
          ? response.data.items
          : []

    if (rawList.length > 0) {
      return rawList.map((item, index) =>
        formatProduct(item, item.similarityScore ?? (0.95 - index * 0.04))
      )
    }
  } catch (err) {
    if (err?.response?.status === 503 || err?.response?.status === 404) {
      vectorServiceUnavailable = true
    }
    console.debug('[Recommendations] Backend similar API not available, using catalog fallback:', err.message)
  }

  return getFallbackPersonalizedProducts(limit, 'similar')
}

export async function syncVectorDatabase() {
  try {
    const response = await http.post('/v1/admin/recommendations/reindex', {}, { timeout: 30000 })
    return {
      success: true,
      count: response.data?.count ?? response.data?.indexedCount ?? response.data?.total ?? 0,
      message: response.data?.message || 'Đã vector hoá thành công sản phẩm vào Vector Store.',
    }
  } catch (err) {
    if (err?.response?.status === 404) {
      const fallbackResponse = await http.post('/admin/vector/reindex', {}, { timeout: 30000 })
      return {
        success: true,
        count: fallbackResponse.data?.count ?? fallbackResponse.data?.indexedCount ?? 0,
        message: fallbackResponse.data?.message || 'Đã vector hoá thành công sản phẩm vào Vector Store.',
      }
    }
    throw err
  }
}

function getRecentUserInterests() {
  try {
    const raw = localStorage.getItem('pc_recent_interests')
    if (!raw) return []
    const list = JSON.parse(raw)
    if (!Array.isArray(list)) return []
    const categories = []
    for (const item of list) {
      if (item && item.category && !categories.includes(item.category)) {
        categories.push(item.category)
      }
    }
    return categories
  } catch {
    return []
  }
}

function getFallbackPersonalizedProducts(limit = 6, context = 'home') {
  let catalog = []
  try {
    const cached = localStorage.getItem('pc_catalog_cache')
    if (cached) {
      const parsed = JSON.parse(cached)
      if (Array.isArray(parsed.products) && parsed.products.length > 0) {
        catalog = parsed.products
      }
    }
  } catch {}

  const sourceList = catalog.length > 0 ? catalog : allCatalogProducts
  const activeProducts = sourceList.filter(p => p.status === 'ACTIVE' || !p.status)

  const recentCategories = getRecentUserInterests()

  let selected = []

  if (recentCategories.length > 0) {
    // 1. Nhặt các sản phẩm thuộc danh mục người dùng vừa xem nhiều nhất
    const matched = []
    for (const cat of recentCategories) {
      const prodsInCat = activeProducts.filter(p => (p.category || '').toLowerCase() === cat)
      matched.push(...prodsInCat)
    }

    // 2. Nhặt thêm các sản phẩm khác nếu chưa đủ limit
    const matchedIds = new Set(matched.map(p => p.id))
    const others = activeProducts.filter(p => !matchedIds.has(p.id))

    selected = [...matched, ...others].slice(0, limit)
  } else {
    selected = activeProducts.slice(0, limit)
  }

  const scores = [0.96, 0.94, 0.91, 0.89, 0.86, 0.82, 0.80, 0.78]

  return selected.map((item, index) =>
    formatProduct(
      item,
      scores[index % scores.length],
    )
  )
}
