import http from './http'
import { resolveProductImage } from '../assets/productImages'

export async function searchProducts({ keyword, category, sort, deals }, signal) {
  const { data } = await http.get('/catalog/search', {
    params: { keyword, category, sort, deals }, signal, timeout: 28000,
  })
  if (!Array.isArray(data.products)) throw new Error('Invalid search response')
  if (import.meta.env.DEV) {
    console.groupCollapsed(`[SmartSearch] ${keyword}`)
    console.info('1. SmartSearchService phân tích', {
      input: keyword, mode: data.mode, groups: data.criteria?.groups, purpose: data.criteria?.purpose,
    })
    console.info('2. ProductMatcher lọc sản phẩm', {
      category, minBudget: data.criteria?.minBudget, maxBudget: data.criteria?.maxBudget,
      inStockOnly: data.criteria?.inStockOnly, keywords: data.criteria?.keywords,
      cheaper: data.criteria?.cheaper, cheapest: data.criteria?.cheapest, deals, sort,
    })
    console.info('3. Trả kết quả', {
      total: data.total, categories: [...new Set(data.products.map(product => product.category))],
      products: data.products.map(({ id, name, category, price, stockOnHand }) => ({ id, name, category, price, stockOnHand })),
    })
    console.groupEnd()
  }
  return {
    ...data,
    products: data.products.map(product => ({
      ...product, imageKey: product.image, image: resolveProductImage(product.image),
    })),
  }
}
