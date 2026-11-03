import http from './http'

const options = { timeout: 15000 }

export const cartApi = {
  csrf: () => http.get('/csrf', options),
  get: () => http.get('/v1/cart', options),
  merge: () => http.post('/v1/cart/merge', null, options),
  add: (productId, quantity, cartVersion) => http.post('/v1/cart/items', { productId, quantity, cartVersion }, options),
  update: (productId, quantity, cartVersion) => http.patch(`/v1/cart/items/${productId}`, { quantity, cartVersion }, options),
  remove: (productId, cartVersion) => http.delete(`/v1/cart/items/${productId}`, { ...options, params: { cartVersion } }),
  clear: (cartVersion) => http.delete('/v1/cart', { ...options, params: { cartVersion } }),
  demo: () => http.get('/v1/cart/demo', options),
  selectDemo: (userId) => http.post('/v1/cart/demo', { userId: userId || null }, options),
}
