import test from 'node:test'
import assert from 'node:assert/strict'
import { shippingFromCustomer, mergeCustomerShipping, shortOrderCode } from '../src/utils/checkout.js'

test('checkout can render the current session name and phone without waiting for an API', () => {
  const result = shippingFromCustomer({ fullName: 'Nguyễn An', phone: '+84912345678' })
  assert.equal(result.recipient, 'Nguyễn An')
  assert.equal(result.phone, '+84912345678')
  assert.equal(result.addressLine, '')
  assert.equal(shippingFromCustomer(null).recipient, '')
})

test('a delayed account response updates untouched fields and preserves user edits', () => {
  const shipping = { ...shippingFromCustomer({ fullName: 'Old name', phone: 'Old phone' }), recipient: 'Người nhận khác', ward: 'Phường 1', addressLine: '123 Đường A' }
  const result = mergeCustomerShipping(shipping, { name: 'Current account name', phone: '0912345678' }, new Set(['recipient', 'ward', 'addressLine']))
  assert.equal(result.recipient, 'Người nhận khác')
  assert.equal(result.phone, '0912345678')
  assert.equal(result.ward, 'Phường 1')
  assert.equal(result.addressLine, '123 Đường A')
})

test('clearing an autofilled field deliberately is preserved when the API responds', () => {
  const shipping = { ...shippingFromCustomer({ fullName: 'An' }), recipient: '', phone: '' }
  const result = mergeCustomerShipping(shipping, { name: 'An', phone: '0912345678' }, new Set(['recipient', 'phone']))
  assert.equal(result.recipient, '')
  assert.equal(result.phone, '')
})

test('long order references are compact for display while short codes stay unchanged', () => {
  const original = 'PC-1234567890abcdef1234567890abcdef'
  assert.equal(shortOrderCode(original), 'PC-1234-CDEF')
  assert.equal(original, 'PC-1234567890abcdef1234567890abcdef')
  assert.equal(shortOrderCode('PC-001'), 'PC-001')
  assert.equal(shortOrderCode(null), '')
})
