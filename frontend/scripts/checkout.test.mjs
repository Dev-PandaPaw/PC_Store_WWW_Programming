import test from 'node:test'
import assert from 'node:assert/strict'
import { shippingFromCustomer, mergeCustomerShipping, shortOrderCode, loginDestination } from '../src/utils/checkout.js'
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
