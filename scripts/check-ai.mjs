import assert from 'node:assert/strict'

// No keys are read here: exercise public Spring endpoints, including real CSRF.
const base = process.env.AI_TEST_BACKEND_URL || 'http://127.0.0.1:8080'
const live = process.argv.includes('--live')
const get = async path => {
  const response = await fetch(base + path, { signal: AbortSignal.timeout(30000) })
  assert.equal(response.status, 200, `GET ${path.split('?')[0]} failed`)
  return response.json()
}
const status = await get('/api/ai/status')
if (live) assert.equal(status.configured, true, 'Configure GEMINI_API_KEY and restart Spring Boot')
const catalog = (await get('/api/catalog/storefront')).products
const verify = products => {
  for (const product of products) {
    const current = catalog.find(item => item.id === product.id)
    assert.ok(current, 'Products must exist in database catalog')
    for (const key of ['name', 'price', 'image', 'stockOnHand']) assert.equal(product[key], current[key])
  }
}
for (const [keyword, category, max] of [
  ['chuột giá rẻ chơi game', 'mouse', null],
  ['PC cá nhân', 'pc-', null],
  ['chuot khong day duoi 1 trieu', 'mouse', 1000000],
]) {
  const result = await get('/api/catalog/search?' + new URLSearchParams({ keyword }))
  if (live) assert.equal(result.mode, 'ai', 'Live check requires provider inference, not local fallback')
  assert.ok(result.products.length > 0, `No products for ${keyword}`)
  assert.ok(result.products.every(p => p.category.startsWith(category) && (!max || p.price <= max)))
  if (keyword.includes('giá rẻ')) {
    const minimum = Math.min(...catalog.filter(p => p.category === category).map(p => p.price))
    assert.ok(result.products.every(p => p.price === minimum), 'Cheap without budget must keep only the lowest matching price')
  }
  verify(result.products)
  console.info(`PASS search: ${keyword} → ${result.total} products (${result.mode})`)
}
const csrfResponse = await fetch(base + '/api/csrf')
assert.equal(csrfResponse.status, 200)
const csrf = await csrfResponse.json()
const cookie = csrfResponse.headers.getSetCookie().map(value => value.split(';')[0]).join('; ')
const denied = await fetch(base + '/api/ai/chat', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:'PC',history:[]})})
assert.equal(denied.status, 403, 'CSRF must remain enabled')
const chat = async (message, history = []) => {
  const response = await fetch(base + '/api/ai/chat', {method:'POST',signal:AbortSignal.timeout(30000),
    headers:{'Content-Type':'application/json',Cookie:cookie,[csrf.headerName]:csrf.token},body:JSON.stringify({message,history})})
  const body = await response.json()
  assert.equal(response.status, 200, `Chat failed: ${body.error?.code || response.status}`)
  assert.equal(body.outcome, 'recommendations')
  assert.equal(body.model, status.model)
  assert.ok(body.products.length > 0)
  verify(body.products)
  return body
}
const question = 'Tư vấn PC chơi game dưới 25 triệu'
const first = await chat(question)
assert.equal(first.criteria.maxBudget, 25000000)
assert.ok(first.products.every(p => p.category === 'pc-gaming' && p.price <= 25000000))
const next = await chat('Nếu tăng ngân sách lên 30 triệu thì sao?',[
  {role:'user',content:question},{role:'assistant',content:first.message},
])
assert.equal(next.criteria.maxBudget, 30000000)
assert.ok(next.products.every(p => p.category === 'pc-gaming' && p.price <= 30000000))
console.info(`PASS advisor: ${status.model}, CSRF, database cards, follow-up budget 25 → 30 triệu`)
