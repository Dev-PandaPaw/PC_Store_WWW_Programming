import test from 'node:test'
import assert from 'node:assert/strict'
import {
  administrativeAddressError,
  calculateSelectedPreview,
  invalidatesQuote,
  isReconfirmationError,
  loginDestination,
  mergeCustomerShipping,
  placeOrderWithTimeoutCheck,
  resolveSelectedCartItems,
  shippingFromCustomer,
  shortOrderCode,
} from '../src/utils/checkout.js'
import { filterAddresses, selectedAddress, changeProvince, changeDistrict } from '../src/utils/addresses.js'
import { readFileSync } from 'node:fs'

const provinces = JSON.parse(readFileSync(new URL('../src/data/vietnamDistricts.json', import.meta.url), 'utf8'))

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

test('login returns to checkout and rejects external redirect destinations', () => {
  assert.equal(loginDestination('/checkout'), '/checkout')
  assert.equal(loginDestination('/checkout?step=shipping'), '/checkout?step=shipping')
  assert.equal(loginDestination({ pathname: '/checkout', search: '?step=shipping' }), '/checkout?step=shipping')
  assert.equal(loginDestination({ pathname: '//example.com' }), '/')
  for (const from of [undefined, 'https://example.com', '//example.com', '/\\example.com', '/login']) {
    assert.equal(loginDestination(from), '/')
  }
})

test('province suggestions support Vietnamese with and without accents', () => {
  assert.equal(provinces.length, 63)
  assert.equal(filterAddresses(provinces, 'ho chi minh')[0].name, 'Thành phố Hồ Chí Minh')
  assert.equal(filterAddresses(provinces, 'HÀ NỘI')[0].name, 'Thành phố Hà Nội')
  assert.equal(filterAddresses(provinces, 'da nang')[0].name, 'Thành phố Đà Nẵng')
  assert.equal(filterAddresses(provinces, 'not-a-province').length, 0)
})

test('district choices belong to the selected province only', () => {
  const hanoi = selectedAddress(provinces, 'Thành phố Hà Nội')
  const hcm = selectedAddress(provinces, 'Thành phố Hồ Chí Minh')
  assert.ok(selectedAddress(hanoi.districts, 'Quận Ba Đình'))
  assert.equal(selectedAddress(hcm.districts, 'Quận Ba Đình'), undefined)
  assert.ok(filterAddresses(hcm.districts, 'go vap').some(district => district.name === 'Quận Gò Vấp'))
  assert.equal(new Set(provinces.map(p => p.code)).size, provinces.length)
})

test('changing province clears district and ward, retaining recipient and street', () => {
  const shipping = { ...shippingFromCustomer({ name: 'An' }), province: 'Thành phố Hà Nội', district: 'Quận Ba Đình', ward: 'Phường A', addressLine: '123 Street' }
  const changed = changeProvince(shipping, 'Thành phố Hồ Chí Minh')
  assert.equal(changed.district, '')
  assert.equal(changed.ward, '')
  assert.equal(changed.recipient, 'An')
  assert.equal(changed.addressLine, '123 Street')
  const district = changeDistrict(shipping, 'Quận Hoàn Kiếm')
  assert.equal(district.ward, '')
  assert.equal(district.province, shipping.province)
})

test('selective cart preview computes subtotal and quantity only for chosen items', () => {
  const items = [
    { id: 'cpu', name: 'CPU', price: 3_000_000, quantity: 1 },
    { id: 'gpu', name: 'GPU', price: 12_000_000, quantity: 2 },
    { id: 'mouse', name: 'Mouse', price: 500_000, quantity: 1 },
  ]
  assert.equal(resolveSelectedCartItems(items, ['cpu', 'mouse']).length, 2)
  const preview = calculateSelectedPreview(items, ['cpu', 'mouse'])
  assert.equal(preview.selectedSkuCount, 2)
  assert.equal(preview.selectedQuantity, 2)
  assert.equal(preview.selectedSubtotal, 3_500_000)
})

test('timeout during order placement checks backend idempotency result before allowing retry', async () => {
  // Case 1: Backend already created the order before response timed out -> recovers immediately without duplicate POST
  let postAttempts = 0
  let checkAttempts = 0
  const recovered = await placeOrderWithTimeoutCheck({
    quoteId: 'quote-1',
    paymentMethod: 'COD',
    idempotencyKey: 'idem-key-001',
    placeRequest: async () => {
      postAttempts += 1
      const err = new Error('timeout of 20000ms exceeded')
      err.code = 'ECONNABORTED'
      throw err
    },
    checkIdempotency: async key => {
      checkAttempts += 1
      assert.equal(key, 'idem-key-001')
      return { status: 200, data: { id: 'order-99', code: 'PC-99' } }
    },
  })
  assert.equal(postAttempts, 1)
  assert.equal(checkAttempts, 1)
  assert.equal(recovered.recoveredFromTimeout, true)
  assert.equal(recovered.order.id, 'order-99')

  // Case 2: Backend had NOT created the order yet (204) -> surfaces retryable error with same idempotencyKey
  await assert.rejects(
    () =>
      placeOrderWithTimeoutCheck({
        quoteId: 'quote-2',
        paymentMethod: 'COD',
        idempotencyKey: 'idem-key-002',
        placeRequest: async () => {
          const err = new Error('network error')
          throw err
        },
        checkIdempotency: async () => ({ status: 204, data: null }),
      }),
    err => {
      assert.equal(err.canRetrySameKey, true)
      assert.equal(err.idempotencyKey, 'idem-key-002')
      return true
    },
  )
})

test('recognizes price, voucher, and shipping reconfirmation errors from backend', () => {
  assert.equal(isReconfirmationError({ response: { data: { errorCode: 'PRICE_CHANGED' } } }), true)
  assert.equal(isReconfirmationError({ response: { data: { errorCode: 'VOUCHER_RECONFIRMATION_REQUIRED' } } }), true)
  assert.equal(isReconfirmationError({ response: { data: { errorCode: 'INSUFFICIENT_STOCK' } } }), false)
})

test('retry after a timed-out attempt checks the backend first and never re-POSTs a committed order', async () => {
  let postAttempts = 0
  const calls = []
  const result = await placeOrderWithTimeoutCheck({
    quoteId: 'quote-3',
    paymentMethod: 'COD',
    idempotencyKey: 'idem-key-003',
    hadPriorTimeout: true,
    placeRequest: async () => { postAttempts += 1; calls.push('post'); return { data: { id: 'dup' } } },
    checkIdempotency: async () => { calls.push('check'); return { status: 200, data: { id: 'order-3' } } },
  })
  assert.equal(postAttempts, 0)
  assert.deepEqual(calls, ['check'])
  assert.equal(result.order.id, 'order-3')
  assert.equal(result.recoveredFromTimeout, true)

  // Nothing committed yet (404): resend once with the same key
  const sent = []
  const retried = await placeOrderWithTimeoutCheck({
    quoteId: 'quote-4',
    paymentMethod: 'COD',
    idempotencyKey: 'idem-key-004',
    hadPriorTimeout: true,
    placeRequest: async (_body, key) => { sent.push(key); return { data: { id: 'order-4' } } },
    checkIdempotency: async () => { const err = new Error('not found'); err.response = { status: 404 }; throw err },
  })
  assert.deepEqual(sent, ['idem-key-004'])
  assert.equal(retried.order.id, 'order-4')
  assert.equal(retried.recoveredFromTimeout, false)
})

test('failed placements that make the quote unusable send the customer back to re-quote', () => {
  for (const code of ['QUOTE_EXPIRED', 'QUOTE_USED', 'INSUFFICIENT_STOCK', 'CART_ITEM_NOT_IN_CART', 'INVALID_SHIPPING_ADDRESS']) {
    assert.equal(invalidatesQuote({ response: { data: { errorCode: code } } }), true, code)
  }
  assert.equal(invalidatesQuote({ response: { data: { errorCode: 'PHONE_NOT_VERIFIED' } } }), false)
  assert.equal(invalidatesQuote(new Error('network')), false)
})

test('ward list comes from the official district code and blocks unselected wards', () => {
  const wards = JSON.parse(readFileSync(new URL('../src/data/vietnamWards.json', import.meta.url), 'utf8'))
  const hcm = selectedAddress(provinces, 'Thành phố Hồ Chí Minh')
  const district1 = selectedAddress(hcm.districts, 'Quận 1')
  const district1Wards = wards[district1.code]
  assert.ok(selectedAddress(district1Wards, 'Phường Bến Nghé'))
  assert.equal(selectedAddress(district1Wards, 'Phường Phúc Xá'), undefined)

  assert.match(administrativeAddressError({ province: hcm, district: district1, ward: undefined, wards: district1Wards }), /phường/i)
  assert.equal(administrativeAddressError({ province: hcm, district: district1, ward: district1Wards[0], wards: district1Wards }), '')
  assert.match(administrativeAddressError({ province: hcm, district: undefined, ward: undefined, wards: [] }), /quận/i)
  assert.match(administrativeAddressError({ province: undefined }), /tỉnh/i)
})
