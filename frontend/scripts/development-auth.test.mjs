import test from 'node:test'
import assert from 'node:assert/strict'
import { isDevelopmentAdminPath } from '../src/utils/developmentAuth.js'

test('development admin credentials are limited to management endpoints', () => {
  for (const url of ['/admin/catalog/products', '/api/admin/catalog/products', '/staff/products', '/admin?status=active'])
    assert.equal(isDevelopmentAdminPath(url), true, url)
})
test('cart, build and customer login never inherit the demo admin identity', () => {
  for (const url of ['/v1/cart', '/v1/cart/items', '/v1/pc-build/advice', '/v1/pc-build/confirm',
    '/auth/me', '/auth/login', '/auth/logout', '/csrf', '/catalog', '/v1/orders', '/v1/admin/orders',
    'https://other.invalid/admin/products', '/administrator', undefined])
    assert.equal(isDevelopmentAdminPath(url), false, url)
})
