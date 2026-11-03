import http from './http'
import { resolveProductImage } from '../assets/productImages'

export async function searchProducts({ keyword, category, sort, deals }, signal) {
  const { data } = await http.get('/catalog/search', {
    params: { keyword, category, sort, deals }, signal, timeout: 28000,
  })
  if (!Array.isArray(data.products)) throw new Error('Invalid search response')
  return {
    ...data,
    products: data.products.map(product => ({
      ...product, imageKey: product.image, image: resolveProductImage(product.image),
    })),
  }
}
