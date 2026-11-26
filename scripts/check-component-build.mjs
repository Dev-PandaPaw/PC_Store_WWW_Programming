import assert from 'node:assert/strict'

// Public HTTP only. No keys, account creation, cart mutation or checkout.
const base=process.env.AI_TEST_BACKEND_URL || 'http://127.0.0.1:8080'
const budget=Number(process.env.BUILD_TEST_BUDGET || 35000000)
const csrfResponse=await fetch(base+'/api/csrf')
assert.equal(csrfResponse.status,200)
const csrf=await csrfResponse.json()
const cookie=csrfResponse.headers.getSetCookie().map(value=>value.split(';')[0]).join('; ')
async function chat(message,history=[]) {
  const response=await fetch(base+'/api/ai/chat',{method:'POST',signal:AbortSignal.timeout(120000),
    headers:{'Content-Type':'application/json',Cookie:cookie,[csrf.headerName]:csrf.token},body:JSON.stringify({message,history})})
  const body=await response.json()
  assert.equal(response.status,200,body.error?.code || 'Chat HTTP failure')
  return body
}
const initial='Mình muốn build PC từ linh kiện rời'
const clarify=await chat(initial)
assert.equal(clarify.outcome,'clarification');assert.equal(clarify.products.length,0)
const answer=await chat(`Ngân sách tối đa ${budget} đồng`,[{role:'user',content:initial},{role:'assistant',content:clarify.message}])
assert.equal(answer.outcome,'recommendations','Set BUILD_TEST_BUDGET high enough for the current catalog')
assert.equal(answer.build.retrieval,'vector');assert.equal(answer.build.items.length,8)
assert.equal(answer.products.length,8);assert.ok(answer.build.total<=budget)
assert.equal(answer.build.total,answer.products.reduce((sum,p)=>sum+p.price,0))
assert.deepEqual(new Set(answer.products.map(p=>p.category)),new Set(['cpu','mainboard','ram','vga','storage','psu','case','cooling']))
const catalogResponse=await fetch(base+'/api/catalog/storefront')
assert.equal(catalogResponse.status,200)
const catalog=(await catalogResponse.json()).products
for(const product of answer.products) {
  const current=catalog.find(p=>p.id===product.id)
  assert.ok(current,'ID must exist in storefront database')
  for(const key of ['name','price','image','stockOnHand']) assert.equal(product[key],current[key])
  assert.ok(product.stockOnHand>0)
  assert.equal(answer.build.items.find(item=>item.productId===product.id)?.quantity,1)
}
assert.ok(answer.build.checks.length>0)
const tooLow=await chat('Build PC linh kiện rời dưới 1 triệu')
assert.equal(tooLow.outcome,'no_match');assert.equal(tooLow.products.length,0)
console.info(`PASS: clarification → budget follow-up → 8 database components, total ${answer.build.total}, vector retrieval, stock/price checks, low-budget no-match.`)
