import test from 'node:test'
import assert from 'node:assert/strict'
import { proposalOption, proposalChanges, canConfirm } from '../src/utils/pcBuildView.js'

const proposal = { proposalId: 'opaque', group: 'provisional_builds', optionIndex: 0,
  expiresAt: new Date(Date.now() + 600000).toISOString(), requiresVerification: true,
  removedProductIds: ['cpu-old'], items: [
    { productId: 'cpu-new', name: 'New CPU', quantity: 1 },
    { productId: 'ram', name: 'RAM', quantity: 2 },
  ] }
test('previews removed, added and changed lines without mutating the cart', () => {
  const cart = [{ id: 'cpu-old', name: 'Old CPU', quantity: 1 }, { id: 'ram', name: 'RAM', quantity: 1 }]
  const before = structuredClone(cart)
  const changes = proposalChanges(proposal, cart)
  assert.deepEqual(changes.removed, ['Old CPU'])
  assert.deepEqual(changes.added.map(i => i.productId), ['cpu-new'])
  assert.deepEqual(changes.changed.map(i => i.productId), ['ram'])
  assert.deepEqual(cart, before)
})
test('stage A accepts unknown builds without checkout acknowledgement', () => {
  assert.equal(canConfirm(proposal, false, false, false), true)
  assert.equal(canConfirm(proposal, true, false, false), true)
})
test('stale, expired or in-flight proposals cannot be confirmed', () => {
  assert.equal(canConfirm(proposal, true, true, false), false)
  assert.equal(canConfirm(proposal, true, false, true), false)
  assert.equal(canConfirm({ ...proposal, expiresAt: new Date(0).toISOString() }, true, false, false), false)
  assert.equal(canConfirm(null, true, false, false), false)
})
test('selects the exact proposal option rather than treating all evidence as shared', () => {
  const option = { compatibility: { status: 'needs_verification' } }
  assert.equal(proposalOption({ advice: { build_search: { provisional_builds: [option] } } }, proposal), option)
  assert.equal(proposalOption({}, proposal), null)
})
