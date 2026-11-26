import test from 'node:test'
import assert from 'node:assert/strict'
import { cartStockError, productCartState, remainingAddableQuantity } from '../src/utils/cartStock.js'
import { projectCart } from '../src/utils/optimisticCart.js'

const product = { id: 'pc', name: 'PC', price: 100, stockOnHand: 5 }

test('rejects decimal and non-integer quantities without coercing to integer', () => {
  assert.match(cartStockError([], product, 1.5, true), /số nguyên.*không chấp nhận số thập phân/i)
  assert.match(cartStockError([], product, '2', true), /số nguyên/i)
  assert.equal(cartStockError([], product, 2, true), '')
})

test('rejects out of stock and reports remaining addable quantity clearly', () => {
  const soldOutMsg = cartStockError([], { ...product, stockOnHand: 0 }, 1, true)
  assert.match(soldOutMsg, /0/)

  const items = [{ productId: 'pc', quantity: 3, availableQuantity: 5 }]
  assert.equal(remainingAddableQuantity(items, product), 2)
  const exceedMsg = cartStockError(items, product, 3, true)
  assert.match(exceedMsg, /tối đa 2 sản phẩm/)
  assert.equal(cartStockError(items, product, 2, true), '')
})

test('allows reducing cart quantity down to available stock when cart currently exceeds stock (purchasable === false)', () => {
  // Cart has 5 units, stock dropped to 3 -> backend sets purchasable: false on this cart line
  const overStockItems = [{ productId: 'pc', quantity: 5, availableQuantity: 3, purchasable: false }]

  // Customer reduces quantity from 5 down to 3 (or 2): MUST be allowed!
  assert.equal(cartStockError(overStockItems, product, 3, false), '')
  assert.equal(cartStockError(overStockItems, product, 2, false), '')

  // Customer edits quantity from 5 to 4 (still > 3): rejected because target 4 > available 3
  assert.match(cartStockError(overStockItems, product, 4, false), /tối đa 3 sản phẩm/)

  // Customer tries to add +1 more: rejected because 5 + 1 > 3
  assert.match(cartStockError(overStockItems, product, 1, true), /0/)

  // Product state reports limit (over-stock), not false sold-out
  const state = productCartState(overStockItems, product)
  assert.equal(state.canAdd, false)
  assert.equal(state.status, 'limit')
  assert.equal(state.remaining, 0)
})

test('still rejects editing or adding when product is genuinely discontinued (inCart <= available && purchasable === false) or stock is 0', () => {
  // In cart: 2, available: 5, purchasable: false -> product status is INACTIVE
  const discontinuedItems = [{ productId: 'pc', quantity: 2, availableQuantity: 5, purchasable: false }]
  assert.match(cartStockError(discontinuedItems, product, 1, false), /ngừng bán/)
  assert.match(cartStockError(discontinuedItems, product, 1, true), /ngừng bán/)
  assert.equal(productCartState(discontinuedItems, product).status, 'sold-out')

  // Stock dropped to 0 while cart has 5 -> target >= 1 cannot succeed
  const zeroStockItems = [{ productId: 'pc', quantity: 5, availableQuantity: 0, purchasable: false }]
  assert.match(cartStockError(zeroStockItems, product, 1, false), /hết hàng/)
})

test('rapid pending operations count toward stock and report 0 remaining when full', () => {
  const cart = { items: [], cartVersion: 0 }
  const operation = { type: 'add', id: product.id, product, quantity: 3 }
  const pending = projectCart(cart, [operation])
  assert.equal(remainingAddableQuantity(pending.items, product), 2)
  assert.ok(cartStockError(pending.items, product, 3, true))
  assert.equal(cartStockError(pending.items, product, 2, true), '')
})

test('server availability overrides old catalog stock and quantity edits are absolute', () => {
  const items = [{ productId: 'pc', quantity: 1, availableQuantity: 1 }]
  assert.equal(remainingAddableQuantity(items, product), 0)
  assert.match(cartStockError(items, product, 1, true), /0/)
  assert.equal(cartStockError(items, product, 1), '')
  assert.ok(cartStockError(items, product, 2))
})

test('unavailable products and total quantities over 99 are rejected with remaining count', () => {
  assert.ok(cartStockError([{ productId: 'pc', purchasable: false }], product, 1, true))
  const msg99 = cartStockError([{ productId: 'pc', quantity: 97 }], { ...product, stockOnHand: 200 }, 5, true)
  assert.match(msg99, /tối đa 2 sản phẩm/)
})

test('every product has stock, remaining count, and disabled state before the first click', () => {
  const state = productCartState([], product)
  assert.equal(state.label, 'Còn 5 sản phẩm')
  assert.equal(state.remaining, 5)
  assert.equal(state.canAdd, true)
  const soldOut = productCartState([], { ...product, stockOnHand: 0 })
  assert.equal(soldOut.label, 'Hết hàng')
  assert.equal(soldOut.remaining, 0)
  assert.equal(soldOut.canAdd, false)
})

test('server cart stock takes precedence and removal re-enables an in-stock product', () => {
  const confirmed = { items: [{ productId: 'pc', quantity: 1, availableQuantity: 1 }], cartVersion: 2 }
  assert.equal(productCartState(confirmed.items, product).canAdd, false)
  assert.equal(productCartState(confirmed.items, product).available, 1)
  assert.equal(productCartState(confirmed.items, product).remaining, 0)
  assert.equal(productCartState(projectCart(confirmed, [{ type: 'remove', id: 'pc' }]).items, product).canAdd, true)
  assert.equal(productCartState([{ ...confirmed.items[0], purchasable: false }], product).status, 'sold-out')
})
