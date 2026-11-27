import http from './http'
import { resolveProductImage } from '../assets/productImages'

const RECOMMENDATION_TIMEOUT_MS = 30000

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
  const rawKey = product.imageKey || product.image
  const image = resolveProductImage(rawKey)
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

function extractRecommendationItems(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.products)) {
    return data.products.map(item => ({
      ...(item.product || item),
      similarityScore: item.similarityScore ?? item.score,
      compatibilityStatus: item.compatibilityStatus,
    }))
  }
  if (Array.isArray(data?.recommendations)) return data.recommendations
  if (Array.isArray(data?.items)) return data.items
  return []
}

export async function getPersonalizedRecommendations(options = {}) {
  const { limit = 6, context = 'home', user = undefined, authLoading = false } = options

  const hasUser = user !== undefined ? Boolean(user?.id || user?.email) : isUserLoggedIn()
  if (!hasUser || authLoading) {
    return []
  }

  try {
    const response = await http.get('/v1/recommendations/personalized', {
      params: { limit, context },
      timeout: RECOMMENDATION_TIMEOUT_MS,
    })

    const rawList = extractRecommendationItems(response.data)
    if (rawList.length > 0) {
      return rawList.map((item, index) =>
        formatProduct(
          item,
          item.similarityScore ?? (0.96 - index * 0.03)
        )
      )
    }
  } catch (err) {
    console.debug('[Recommendations] Failed to load personalized recommendations:', err.message)
  }

  return []
}

export async function getSimilarProducts(productId, options = {}) {
  const { limit = 4 } = options

  if (!productId) {
    return []
  }

  try {
    const response = await http.get(`/v1/recommendations/similar/${productId}`, {
      params: { limit },
      timeout: RECOMMENDATION_TIMEOUT_MS,
    })

    const rawList = extractRecommendationItems(response.data)
    if (rawList.length > 0) {
      return rawList.map((item, index) =>
        formatProduct(item, item.similarityScore ?? (0.95 - index * 0.04))
      )
    }
  } catch (err) {
    console.debug('[Recommendations] Failed to load similar products:', err.message)
  }

  return []
}
