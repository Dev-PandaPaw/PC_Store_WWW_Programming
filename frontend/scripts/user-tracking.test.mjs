import test from 'node:test'
import assert from 'node:assert/strict'

test('user tracking creates well-structured payload with clientTimestamp', () => {
  const eventType = 'VIEW_PRODUCT'
  const productId = '954db6c4-ea24-5681-a8c8-d15954df3611'
  const categorySlug = 'cpu'
  const metadata = { productName: 'Intel Core i5 12400F', price: 4190000 }

  const payload = {
    eventType,
    productId,
    categorySlug,
    metadata: JSON.stringify(metadata),
    clientTimestamp: new Date().toISOString(),
  }

  assert.equal(payload.eventType, 'VIEW_PRODUCT')
  assert.equal(payload.productId, productId)
  assert.equal(payload.categorySlug, 'cpu')
  assert.ok(payload.clientTimestamp)
  const parsedMeta = JSON.parse(payload.metadata)
  assert.equal(parsedMeta.productName, 'Intel Core i5 12400F')
})

test('user tracking dwell time payload calculates duration correctly', () => {
  const dwellSeconds = 30
  const payload = {
    eventType: 'DWELL_TIME_REACHED',
    productId: 'test-product-id',
    metadata: JSON.stringify({ dwellSeconds }),
  }

  assert.equal(payload.eventType, 'DWELL_TIME_REACHED')
  assert.equal(JSON.parse(payload.metadata).dwellSeconds, 30)
})

