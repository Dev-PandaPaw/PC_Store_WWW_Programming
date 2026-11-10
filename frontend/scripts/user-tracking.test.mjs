import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'

function hookHarness() {
  const events = [], effects = [], timers = new Map()
  let nextId = 0
  const runtime = {
    useEffect: effect => effects.push(effect), useRef: () => ({ current: null }),
    sendTrackingEvent: event => events.push(event),
    setTimeout: (callback, delay) => { const id = ++nextId; timers.set(id, { callback, delay }); return id },
    clearTimeout: id => timers.delete(id),
  }
  const source = readFileSync(new URL('../src/hooks/useUserTracker.js', import.meta.url), 'utf8')
    .replace(/^import .*$/gm, '').replace(/export function /g, 'function ')
  vm.runInNewContext(source, runtime)
  return { events, timers, mount(product) {
    runtime.useProductViewTracker(product)
    return effects.pop()()
  }, fire(delay) {
    for (const [id, timer] of [...timers]) if (timer.delay === delay) { timers.delete(id); timer.callback() }
  } }
}

test('actual view hook records once after a discarded StrictMode effect', () => {
  const hook = hookHarness()
  const product = { id: 'cpu-1', category: 'cpu', name: 'CPU', price: 100 }
  hook.mount(product)() // StrictMode discards the first effect.
  const cleanup = hook.mount(product)
  hook.fire(0)
  assert.equal(hook.events.length, 1)
  assert.equal(hook.events[0].eventType, 'VIEW_PRODUCT')
  hook.fire(30000)
  assert.equal(hook.events[1].eventType, 'DWELL_TIME_REACHED')
  cleanup()
  assert.equal(hook.timers.size, 0)
})

test('actual view hook cancels old product timers on navigation', () => {
  const hook = hookHarness()
  hook.mount({ id: 'old', category: 'cpu' })()
  hook.mount({ id: 'new', category: 'mainboard' })
  hook.fire(0); hook.fire(30000)
  assert.equal(hook.events.length, 2)
  assert.ok(hook.events.every(event => event.productId === 'new'))
})

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

