import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'

function loadRecommendations(httpMock, storage = new Map()) {
  const source = readFileSync(new URL('../src/services/recommendations.js', import.meta.url), 'utf8')
    .replace(/^import .*$/gm, '')
    .replace(/export async function /g, 'async function ')
  const runtime = {
    http: httpMock,
    resolveProductImage: key => `/resolved/${key || 'default.svg'}`,
    localStorage: {
      getItem: key => (storage.has(key) ? storage.get(key) : null),
    },
    console: { debug: () => {} },
  }
  vm.runInNewContext(source, runtime)
  return runtime
}

test('returns empty list when user is not logged in without calling API or static fallback', async () => {
  let called = 0
  const storage = new Map([
    ['pc_recent_interests', JSON.stringify([{ category: 'cpu' }])],
    ['pc_catalog_cache', JSON.stringify({ products: [{ id: '1', name: 'Cached CPU', category: 'cpu', status: 'ACTIVE' }] })],
  ])
  const api = loadRecommendations({ get: async () => { called += 1; return { data: {} } } }, storage)

  const result = await api.getPersonalizedRecommendations({ user: null, authLoading: false })
  assert.deepEqual([...result], [])
  assert.equal(called, 0)
})

test('returns empty list when personalized API fails instead of static fallback', async () => {
  const storage = new Map([
    ['pc_recent_interests', JSON.stringify([{ category: 'cpu' }])],
    ['pc_catalog_cache', JSON.stringify({ products: [{ id: '1', name: 'Cached CPU', category: 'cpu', status: 'ACTIVE' }] })],
  ])
  const api = loadRecommendations({
    get: async () => {
      throw Object.assign(new Error('Service Unavailable'), { response: { status: 503 } })
    },
  }, storage)

  const result = await api.getPersonalizedRecommendations({ user: { id: 'user-1' }, authLoading: false })
  assert.deepEqual([...result], [])
})

test('returns empty list when similar API fails instead of static fallback', async () => {
  const storage = new Map([
    ['pc_catalog_cache', JSON.stringify({ products: [{ id: '1', name: 'Cached CPU', category: 'cpu', status: 'ACTIVE' }] })],
  ])
  const api = loadRecommendations({
    get: async () => {
      throw Object.assign(new Error('Internal Server Error'), { response: { status: 500 } })
    },
  }, storage)

  const result = await api.getSimilarProducts('prod-1', { limit: 4 })
  assert.deepEqual([...result], [])
})

test('requests recommendations again after a temporary service failure', async () => {
  let calls = 0
  const api = loadRecommendations({
    get: async () => {
      if (++calls === 1) {
        throw Object.assign(new Error('Service Unavailable'), { response: { status: 503 } })
      }
      return { data: { products: [{ product: { id: 'cpu-1' }, similarityScore: 0.9 }] } }
    },
  })
  const options = { user: { id: 'user-1' } }

  assert.equal((await api.getPersonalizedRecommendations(options)).length, 0)
  assert.equal((await api.getPersonalizedRecommendations(options))[0].id, 'cpu-1')
})

test('a missing similar product does not block personalized recommendations', async () => {
  const api = loadRecommendations({
    get: async path => {
      if (path.includes('/similar/')) {
        throw Object.assign(new Error('Not Found'), { response: { status: 404 } })
      }
      return { data: { products: [{ product: { id: 'cpu-1' }, similarityScore: 0.9 }] } }
    },
  })

  assert.equal((await api.getSimilarProducts('missing')).length, 0)
  assert.equal((await api.getPersonalizedRecommendations({ user: { id: 'user-1' } }))[0].id, 'cpu-1')
})

test('formats products returned from backend recommendation API', async () => {
  const api = loadRecommendations({
    get: async () => ({
      data: {
        status: 'ok',
        products: [
          {
            product: {
              id: 'cpu-1',
              name: 'Intel Core i5-12400F',
              slug: 'intel-core-i5-12400f',
              image: 'products/cpu-i5.png',
              specs: [{ label: 'LGA1700' }],
            },
            similarityScore: 0.874,
            compatibilityStatus: 'compatible',
          },
        ],
      },
    }),
  })

  const result = await api.getPersonalizedRecommendations({ user: { id: 'user-1' }, authLoading: false })
  assert.equal(result.length, 1)
  assert.equal(result[0].id, 'cpu-1')
  assert.equal(result[0].image, '/resolved/products/cpu-i5.png')
  assert.equal(result[0].similarityScore, 0.874)
  assert.equal(result[0].similarityPercent, 87)
  assert.equal(result[0].compatibilityStatus, 'compatible')
})
