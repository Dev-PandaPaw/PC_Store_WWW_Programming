import test from 'node:test'
import assert from 'node:assert/strict'
import { CART_SYNC_DEADLINE_MS, runWithCartDeadline } from '../src/utils/cartDeadline.js'

test('successful sync returns the server result without cancelling the request', async () => {
  let signal
  const response = { data: { cartVersion: 1 } }
  assert.equal(await runWithCartDeadline(current => { signal = current; return response }), response)
  assert.equal(signal.aborted, false)
})

test('a hung request ends the pending state at the total deadline and aborts HTTP work', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  let signal
  let adding = true
  const result = runWithCartDeadline(current => {
    signal = current
    return new Promise(() => {})
  }).finally(() => { adding = false })
  const rejected = assert.rejects(result, { code: 'ETIMEDOUT' })
  await Promise.resolve()
  t.mock.timers.tick(CART_SYNC_DEADLINE_MS)
  await rejected
  assert.equal(adding, false)
  assert.equal(signal.aborted, true)
})

test('queue waiting consumes the deadline and an expired operation never sends a POST', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  let releaseQueue
  const queue = new Promise(resolve => { releaseQueue = resolve })
  let posts = 0
  let queuedTask
  const result = runWithCartDeadline(signal => {
    queuedTask = queue.then(() => {
      signal.throwIfAborted()
      posts += 1
    })
    return queuedTask
  })
  const rejected = assert.rejects(result, { code: 'ETIMEDOUT' })
  await Promise.resolve()
  t.mock.timers.tick(CART_SYNC_DEADLINE_MS)
  await rejected
  releaseQueue()
  await assert.rejects(queuedTask, { code: 'ETIMEDOUT' })
  assert.equal(posts, 0)
})

test('a late response cannot publish success after the UI timed out', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  let completeRequest
  let accepted = 0
  const result = runWithCartDeadline(() => new Promise(resolve => { completeRequest = resolve }))
    .then(() => { accepted += 1 })
  const rejected = assert.rejects(result, { code: 'ETIMEDOUT' })
  await Promise.resolve()
  t.mock.timers.tick(CART_SYNC_DEADLINE_MS)
  await rejected
  completeRequest({ data: { cartVersion: 1 } })
  await Promise.resolve()
  assert.equal(accepted, 0)
})

test('CSRF and POST share one budget instead of restarting the clock per phase', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  let finishCsrf
  let postStarted = false
  const result = runWithCartDeadline(async signal => {
    await new Promise(resolve => { finishCsrf = resolve })
    signal.throwIfAborted()
    postStarted = true
    return new Promise(() => {})
  })
  const rejected = assert.rejects(result, { code: 'ETIMEDOUT' })
  await Promise.resolve()
  t.mock.timers.tick(3000)
  finishCsrf()
  await Promise.resolve()
  assert.equal(postStarted, true)
  t.mock.timers.tick(CART_SYNC_DEADLINE_MS - 3000)
  await rejected
})
