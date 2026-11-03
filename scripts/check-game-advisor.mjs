import assert from 'node:assert/strict'

const base=process.env.AI_TEST_BACKEND_URL || 'http://127.0.0.1:8080'
const csrfResponse=await fetch(base+'/api/csrf')
assert.equal(csrfResponse.status,200)
const csrf=await csrfResponse.json()
const cookies=csrfResponse.headers.getSetCookie().map(value=>value.split(';')[0]).join('; ')
const catalog=await fetch(base+'/api/catalog/storefront').then(r=>r.json()).then(body=>body.products)
async function chat(message,history=[]) {
  const response=await fetch(base+'/api/ai/chat',{method:'POST',signal:AbortSignal.timeout(65000),
    headers:{'Content-Type':'application/json',Cookie:cookies,[csrf.headerName]:csrf.token},body:JSON.stringify({message,history})})
  const body=await response.json()
  assert.equal(response.status,200,body.error?.code || `HTTP ${response.status}`)
  for(const product of body.products) {
    const current=catalog.find(p=>p.id===product.id)
    assert.ok(current)
    for(const field of ['name','price','stockOnHand','image']) assert.equal(product[field],current[field])
  }
  return body
}
const minimumMouse=Math.min(...catalog.filter(p=>p.category==='mouse').map(p=>p.price))
const cheap=await chat('Chuột giá rẻ chơi game')
assert.equal(cheap.outcome,'recommendations')
assert.ok(cheap.products.length && cheap.products.every(p=>p.category==='mouse' && p.price===minimumMouse))
assert.ok(cheap.message.includes('thấp nhất'))
const search=await fetch(base+'/api/catalog/search?'+new URLSearchParams({keyword:'Chuột giá rẻ nhất chơi game'})).then(r=>r.json())
assert.ok(search.products.length && search.products.every(p=>p.price===minimumMouse))
console.info(`PASS cheapest: chat/search only lowest matching mouse price ${minimumMouse}`)
const question='Máy tính có cấu hình chơi Black Myth: Wukong?'
const game=await chat(question)
assert.equal(game.outcome,'recommendations')
assert.equal(game.game.title,'Black Myth: Wukong')
assert.ok(game.game.sources.length)
assert.equal(game.game.minimum.ramGb,16)
assert.equal(game.game.recommended.storageGb,130)
assert.ok(game.products.length && game.products.every(p=>p.gameFit && p.gameFit.ramGb>=16 && p.gameFit.storageGb>=130))
console.info(`PASS Wukong: sourced requirements, ${game.products.length} matching PCs, target=${game.game.selectedTarget}`)
const budget=await chat('Ngân sách tối đa 10 triệu',[
  {role:'user',content:question},{role:'assistant',content:game.message},
])
assert.equal(budget.outcome,'no_match')
assert.equal(budget.products.length,0)
console.info('PASS game follow-up: budget honored; no unsuitable substitutes')
if(process.argv.includes('--grounded')) {
  const another=await chat('Máy chơi Elden Ring?')
  assert.equal(another.outcome,'recommendations')
  assert.equal(another.game.lookupStatus,'live_search')
  assert.ok(another.game.sources.length)
  console.info(`PASS general lookup: ${another.game.title}, provider-grounded sources, ${another.products.length} matching PCs`)
}
