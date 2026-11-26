import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createAddOperationId,
  executeIdempotentAdd,
  isValidOperationId,
  parseStrictIntegerQuantity,
  prepareAddAttempt,
} from '../src/utils/cartAddIdempotency.js'

test('createAddOperationId produces valid RFC-4122 UUIDs', () => {
  const first = createAddOperationId()
  const second = createAddOperationId()
  assert.ok(isValidOperationId(first))
  assert.ok(isValidOperationId(second))
  assert.notEqual(first, second)
})

test('parseStrictIntegerQuantity rejects decimals without coercing to integer', () => {
  assert.equal(parseStrictIntegerQuantity(1.5, 10).valid, false)
  assert.equal(parseStrictIntegerQuantity('1.5', 10).valid, false)
  assert.equal(parseStrictIntegerQuantity('2,0', 10).valid, false)
  assert.equal(parseStrictIntegerQuantity('2.0', 10).valid, false)
  assert.equal(parseStrictIntegerQuantity('0', 10).valid, false)
  assert.equal(parseStrictIntegerQuantity('11', 10).valid, false)
  assert.deepEqual(parseStrictIntegerQuantity('3', 10), { valid: true, value: 3, message: '' })
  assert.deepEqual(parseStrictIntegerQuantity(4, 10), { valid: true, value: 4, message: '' })
})

test('lost response after backend commit reloads cart and retries with SAME operationId without double-adding', async () => {
  const appliedOps = new Set()
  let serverCart = { cartVersion: 0, items: [], totalQuantity: 0, subtotal: 0 }
  const calls = []

  const addRequest = async (productId, quantity, cartVersion, operationId) => {
    calls.push({ productId, quantity, cartVersion, operationId })
    if (!appliedOps.has(operationId)) {
      assert.equal(cartVersion, serverCart.cartVersion)
      appliedOps.add(operationId)
      serverCart = {
        cartVersion: serverCart.cartVersion + 1,
        items: [{ productId, quantity }],
        totalQuantity: quantity,
        subtotal: quantity * 100,
      }
      if (calls.length === 1) {
        throw new Error('Network timeout - response lost')
      }
    }
    return { data: serverCart }
  }

  let clientVersion = 0
  const result = await executeIdempotentAdd({
    productId: 'gpu-1',
    quantity: 2,
    getVersion: () => clientVersion,
    addRequest,
    getCart: async () => ({ data: serverCart }),
    onCartLoaded: fresh => { clientVersion = fresh.cartVersion },
  })

  assert.equal(result.retried, true)
  assert.equal(calls.length, 2)
  assert.equal(calls[0].cartVersion, 0)
  assert.equal(calls[1].cartVersion, 1)
  assert.equal(calls[0].operationId, calls[1].operationId)
  assert.equal(result.data.totalQuantity, 2)
  assert.equal(result.data.cartVersion, 1)
})

test('manual retry after cart reloaded to full stock (3 + 2 = 5/5) sends original operationId to backend instead of failing client stock preflight', async () => {
  const product = { id: 'gpu-1', name: 'RTX 5070', price: 100, stockOnHand: 5 }
  const appliedOps = new Set()
  let serverCart = {
    cartVersion: 1,
    items: [{ productId: 'gpu-1', quantity: 3, availableQuantity: 5, purchasable: true }],
    totalQuantity: 3,
    subtotal: 300,
  }
  let clientCart = structuredClone(serverCart)

  // Step 1: Customer creates a NEW add operation (+2 when cart has 3/5)
  const initialAttempt = prepareAddAttempt({
    items: clientCart.items,
    product,
    quantity: 2,
    isRetry: false,
  })
  assert.equal(initialAttempt.allowed, true)
  assert.ok(isValidOperationId(initialAttempt.operationId))

  // Step 2: Backend commits 3 + 2 = 5, response is lost; reload sees 5/5, AND automatic retry also fails network!
  let postCount = 0
  const addRequest = async (productId, quantity, cartVersion, operationId) => {
    postCount += 1
    if (!appliedOps.has(operationId)) {
      assert.equal(cartVersion, serverCart.cartVersion)
      appliedOps.add(operationId)
      serverCart = {
        cartVersion: serverCart.cartVersion + 1,
        items: [{ productId, quantity: 5, availableQuantity: 5, purchasable: true }],
        totalQuantity: 5,
        subtotal: 500,
      }
    }
    if (postCount <= 2) {
      throw new Error('Network lost on both initial POST and auto-retry POST')
    }
    return { data: serverCart }
  }

  await assert.rejects(() =>
    executeIdempotentAdd({
      productId: product.id,
      quantity: 2,
      operationId: initialAttempt.operationId,
      getVersion: () => clientCart.cartVersion,
      addRequest,
      getCart: async () => ({ data: serverCart }),
      onCartLoaded: fresh => { clientCart = fresh },
    }),
  )

  // Client cart now already shows 5/5 from the intermediate reload!
  assert.equal(clientCart.items[0].quantity, 5)
  assert.equal(clientCart.cartVersion, 2)

  // Step 3a: A brand-NEW add operation (+2) when cart has 5/5 MUST be rejected on client
  const newAddAttempt = prepareAddAttempt({
    items: clientCart.items,
    product,
    quantity: 2,
    isRetry: false,
  })
  assert.equal(newAddAttempt.allowed, false)
  assert.match(newAddAttempt.message, /0/)

  // Step 3b: Manual RETRY of the previous operationId MUST be allowed past client stock preflight
  const manualRetryAttempt = prepareAddAttempt({
    items: clientCart.items,
    product,
    quantity: 2,
    isRetry: true,
    operationId: initialAttempt.operationId,
  })
  assert.equal(manualRetryAttempt.allowed, true)
  assert.equal(manualRetryAttempt.operationId, initialAttempt.operationId)

  const reconciled = await executeIdempotentAdd({
    productId: product.id,
    quantity: 2,
    operationId: manualRetryAttempt.operationId,
    getVersion: () => clientCart.cartVersion,
    addRequest,
    getCart: async () => ({ data: serverCart }),
    onCartLoaded: fresh => { clientCart = fresh },
  })
  assert.equal(reconciled.data.totalQuantity, 5)
  assert.equal(reconciled.data.cartVersion, 2)
})

test('non-retryable stock conflict is not retried and preserves operationId', async () => {
  let attempts = 0
  await assert.rejects(
    () =>
      executeIdempotentAdd({
        productId: 'gpu-1',
        quantity: 5,
        getVersion: () => 0,
        addRequest: async (_id, _qty, _ver, opId) => {
          attempts += 1
          const err = new Error('Insufficient stock')
          err.response = {
            status: 409,
            data: { errorCode: 'CART_INSUFFICIENT_STOCK', remainingAddableQuantity: 2 },
          }
          err.operationId = opId
          throw err
        },
        getCart: async () => ({ data: { cartVersion: 0, items: [] } }),
      }),
    err => {
      assert.equal(attempts, 1)
      assert.ok(isValidOperationId(err.operationId))
      return true
    },
  )
})

test('timeouts and server failures release the queue without another GET + POST cycle', async () => {
  for (const error of [
    Object.assign(new Error('timeout'), { code: 'ECONNABORTED' }),
    Object.assign(new Error('timeout'), { code: 'ETIMEDOUT' }),
    Object.assign(new Error('busy'), { response: { status: 503, data: {} } }),
  ]) {
    let posts = 0
    let reads = 0
    const operationId = createAddOperationId()
    await assert.rejects(executeIdempotentAdd({
      productId: 'gpu', quantity: 1, operationId, getVersion: () => 0,
      addRequest: async () => { posts += 1; throw error },
      getCart: async () => { reads += 1; return { data: { cartVersion: 1 } } },
    }), caught => caught === error && caught.operationId === operationId)
    assert.equal(posts, 1)
    assert.equal(reads, 0)
  }
})

test('version conflict refreshes once and retries with the original operation ID', async () => {
  const operationId = createAddOperationId()
  const calls = []
  const result = await executeIdempotentAdd({
    productId: 'gpu', quantity: 1, operationId, getVersion: () => 0,
    addRequest: async (_id, _qty, version, op) => {
      calls.push({ version, op })
      if (calls.length === 1) throw Object.assign(new Error('stale'), {
        response: { status: 409, data: { errorCode: 'CART_VERSION_CONFLICT' } },
      })
      return { data: { cartVersion: 5, totalQuantity: 1 } }
    },
    getCart: async () => ({ data: { cartVersion: 4 } }),
  })
  assert.equal(result.retried, true)
  assert.deepEqual(calls, [{ version: 0, op: operationId }, { version: 4, op: operationId }])
})
