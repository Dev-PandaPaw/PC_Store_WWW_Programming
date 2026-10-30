import test from 'node:test'
import assert from 'node:assert/strict'
import { splitLoginResponse, cartFromLogin } from '../src/utils/loginCart.js'
import { projectCart } from '../src/utils/optimisticCart.js'

test('login adopts the server merge once and the next click uses its confirmed version', () => {
  // Stored cart had 3 units, guest had 2. Login has already committed the combined 5.
  const merged = { cartVersion: 8, items: [{ productId: 'cpu', name: 'CPU', unitPrice: 100, quantity: 5 }], totalQuantity: 5, subtotal: 500 }
  const session = splitLoginResponse({ id: 'customer', username: 'test', cart: merged })
  const confirmed = cartFromLogin(session.user, session.loginCart)
  assert.equal(confirmed.totalQuantity, 5)
  assert.equal(confirmed.cartVersion, 8)
  assert.equal(session.user.cart, undefined)
  const nextClick = projectCart(confirmed, [{ type: 'add', id: 'cpu', quantity: 1, product: { name: 'CPU', price: 100 } }])
  assert.equal(nextClick.totalQuantity, 6)
  assert.equal(nextClick.cartVersion, 8)
})

test('logout and switching accounts cannot reuse the previous login cart', () => {
  const previous = { userId: 'first', cart: { totalQuantity: 5 } }
  assert.equal(cartFromLogin(null, previous), null)
  assert.equal(cartFromLogin({ id: 'second' }, previous), null)
})

test('session restoration without a login snapshot requires a fresh server cart', () => {
  const session = splitLoginResponse({ id: 'customer', username: 'test' })
  assert.equal(cartFromLogin(session.user, session.loginCart), null)
})
