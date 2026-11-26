import test from 'node:test'
import assert from 'node:assert/strict'
import http from '../src/services/http.js'
import { getVectorDocuments, syncVectorDatabase } from '../src/services/vectorSync.js'

test('vector inspection reads one page of stored documents with cancellation', async t => {
  const data = { storage: 'memory', total: 78, offset: 10, limit: 10, documents: [] }
  const { signal } = new AbortController()
  const get = t.mock.method(http, 'get', async () => ({ data }))

  assert.deepEqual(await getVectorDocuments({ offset: 10, limit: 10, signal }), data)
  assert.deepEqual(get.mock.calls[0].arguments, ['/admin/vector/documents', { params: { offset: 10, limit: 10 }, signal, timeout: 15_000 }])
})

test('admin vector sync posts to the RAM and PgVector endpoint and preserves zero counts', async t => {
  const data = { status: 'SUCCESS', productCount: 0, documentCount: 0 }
  const post = t.mock.method(http, 'post', async () => ({ status: 200, data }))

  assert.deepEqual(await syncVectorDatabase(), data)
  assert.equal(post.mock.callCount(), 1)
  assert.deepEqual(post.mock.calls[0].arguments, ['/admin/vector/reindex', {}, { timeout: 600_000 }])
})

test('admin vector sync requires a confirmed result with both counts', async t => {
  const data = { status: 'SUCCESS', productCount: 78, documentCount: 90 }
  const post = t.mock.method(http, 'post')

  for (const response of [
    { status: 202, data },
    { status: 200, data: { ...data, status: 'BUSY' } },
    { status: 200, data: { status: 'SUCCESS', productCount: 78 } },
  ]) {
    post.mock.mockImplementation(async () => response)
    await assert.rejects(syncVectorDatabase(), /chưa xác nhận/)
  }
})

test('admin vector sync propagates backend errors without repeating the request', async t => {
  const error = Object.assign(new Error('Request failed'), {
    response: { status: 503, data: { error: { code: 'VECTOR_NOT_CONFIGURED', message: 'Chưa cấu hình Gemini.' } } },
  })
  const post = t.mock.method(http, 'post', async () => { throw error })

  await assert.rejects(syncVectorDatabase(), failure => failure === error)
  assert.equal(post.mock.callCount(), 1)
})
