import test from 'node:test'
import assert from 'node:assert/strict'
import { cartStockError, productCartState } from '../src/utils/cartStock.js'
import { projectCart } from '../src/utils/optimisticCart.js'

const product = { id: 'pc', name: 'PC', price: 100, stockOnHand: 2 }

test('rejects out of stock before a product has been added to the cart', () => {
  assert.ok(cartStockError([], { ...product, stockOnHand: 0 }, 1, true))
  assert.equal(cartStockError([], product, 2, true), '')
})

test('rapid pending additions count toward stock before the server responds', () => {
  const cart = { items: [], cartVersion: 0 }
  const operation = { type: 'add', id: product.id, product, quantity: 1 }
  const pending = projectCart(cart, [operation, operation])
  assert.ok(cartStockError(pending.items, product, 1, true))
  assert.equal(cartStockError(projectCart(cart, [operation]).items, product, 1, true), '')
})

test('server availability overrides old catalog stock and quantity edits are absolute', () => {
  const items = [{ productId: 'pc', quantity: 1, availableQuantity: 1 }]
  assert.ok(cartStockError(items, product, 1, true))
  assert.equal(cartStockError(items, product, 1), '')
  assert.ok(cartStockError(items, product, 2))
})

test('unavailable products and total quantities over 99 are rejected', () => {
  assert.ok(cartStockError([{ productId: 'pc', purchasable: false }], product, 1, true))
  assert.ok(cartStockError([{ productId: 'pc', quantity: 99 }], { ...product, stockOnHand: 200 }, 1, true))
})

test('every product has stock and disabled state before the first click', () => {
  assert.equal(productCartState([], product).label, 'Còn 2 sản phẩm')
  assert.equal(productCartState([], product).canAdd, true)
  const soldOut = productCartState([], { ...product, stockOnHand: 0 })
  assert.equal(soldOut.label, 'Hết hàng')
  assert.equal(soldOut.canAdd, false)
})

test('pending additions synchronize every occurrence of the product and roll back on failure', () => {
  const confirmed = { items: [], cartVersion: 0 }
  const operation = { type: 'add', id: product.id, product, quantity: 2 }
  const pending = projectCart(confirmed, [operation])
  assert.equal(productCartState(pending.items, product).status, 'limit')
  assert.equal(productCartState(pending.items, { ...product }).canAdd, false)
  assert.equal(productCartState(projectCart(confirmed, []).items, product).status, 'available')
})

test('server cart stock takes precedence and removal re-enables an in-stock product', () => {
  const confirmed = { items: [{ productId: 'pc', quantity: 1, availableQuantity: 1 }], cartVersion: 2 }
  assert.equal(productCartState(confirmed.items, product).canAdd, false)
  assert.equal(productCartState(confirmed.items, product).available, 1)
  assert.equal(productCartState(projectCart(confirmed, [{ type: 'remove', id: 'pc' }]).items, product).canAdd, true)
  assert.equal(productCartState([{ ...confirmed.items[0], purchasable: false }], product).status, 'sold-out')
})
