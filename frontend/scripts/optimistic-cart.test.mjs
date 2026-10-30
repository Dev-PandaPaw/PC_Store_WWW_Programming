import test from 'node:test'
import assert from 'node:assert/strict'
import { projectCart } from '../src/utils/optimisticCart.js'

const base = { cartVersion: 4, items: [{ productId: 'cpu', name: 'CPU', unitPrice: 100, quantity: 2 }], totalQuantity: 2, subtotal: 200 }
const add = { type: 'add', id: 'cpu', product: { name: 'CPU', price: 100 }, quantity: 1 }

test('pending clicks update badge and subtotal without changing confirmed version or data', () => {
  const result = projectCart(base, [add, add, add])
  assert.equal(result.totalQuantity, 5)
  assert.equal(result.subtotal, 500)
  assert.equal(result.cartVersion, 4)
  assert.equal(base.items[0].quantity, 2)
})

test('an earlier response keeps later pending additions visible without double counting', () => {
  const acknowledged = { ...base, cartVersion: 5, items: [{ ...base.items[0], quantity: 3 }] }
  assert.equal(projectCart(acknowledged, [add, add]).totalQuantity, 5)
})

test('rejecting one addition preserves later additions and removing all pending operations rolls back', () => {
  assert.equal(projectCart(base, [add]).totalQuantity, 3)
  assert.equal(projectCart(base, []).totalQuantity, 2)
})

test('latest quantity remains visible when an earlier quantity is acknowledged or rejected', () => {
  const operations = [{ type: 'quantity', id: 'cpu', quantity: 6 }, { type: 'quantity', id: 'cpu', quantity: 9 }]
  assert.equal(projectCart(base, operations).totalQuantity, 9)
  const acknowledged = { ...base, items: [{ ...base.items[0], quantity: 6 }] }
  assert.equal(projectCart(acknowledged, operations.slice(1)).totalQuantity, 9)
  assert.equal(projectCart(base, operations.slice(1)).totalQuantity, 9)
})

test('clear, new product and remove preserve user operation order and totals', () => {
  const gpu = { type: 'add', id: 'gpu', product: { name: 'GPU', price: 400, image: 'gpu.png' }, quantity: 2 }
  const result = projectCart(base, [add, { type: 'clear' }, gpu])
  assert.equal(result.totalQuantity, 2)
  assert.equal(result.subtotal, 800)
  assert.equal(result.items[0].image, 'gpu.png')
  assert.equal(projectCart(base, [{ type: 'remove', id: 'cpu' }]).totalQuantity, 0)
})
