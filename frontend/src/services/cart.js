import http from './http'

const options = { timeout: 8000 }

export const cartApi = {
  csrf: () => http.get('/csrf', options),
  get: (requestOptions = {}) => http.get('/v1/cart', { ...options, ...requestOptions }),
  merge: () => http.post('/v1/cart/merge', null, options),
  add: (productId, quantity, cartVersion, operationId, requestOptions = {}) =>
    http.post(
      '/v1/cart/items',
      operationId ? { productId, quantity, cartVersion, operationId } : { productId, quantity, cartVersion },
      operationId
        ? { ...options, ...requestOptions, headers: { 'Idempotency-Key': operationId } }
        : { ...options, ...requestOptions },
    ),
  update: (productId, quantity, cartVersion) => http.patch(`/v1/cart/items/${productId}`, { quantity, cartVersion }, options),
  remove: (productId, cartVersion) => http.delete(`/v1/cart/items/${productId}`, { ...options, params: { cartVersion } }),
  clear: (cartVersion) => http.delete('/v1/cart', { ...options, params: { cartVersion } }),
  demo: () => http.get('/v1/cart/demo', options),
  selectDemo: (userId) => http.post('/v1/cart/demo', { userId: userId || null }, options),
}
