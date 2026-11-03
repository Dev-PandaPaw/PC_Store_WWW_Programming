// Repository-owned subset of Google's generateContent contract used by Spring AI.
const http = require('http');
let mode = 'normal';
http.createServer(async (req, res) => {
  const send = (status, body) => { res.writeHead(status, {'Content-Type':'application/json'}); res.end(JSON.stringify(body)); };
  let body = req.body;
  if (!body) { let raw=''; for await (const chunk of req) raw+=chunk; try { body=raw ? JSON.parse(raw) : {}; } catch { return send(400,{error:{code:400,message:'Invalid JSON'}}); } }
  if (req.url === '/__control' && req.method === 'POST') { mode=body.mode || 'normal'; return send(200,{mode}); }
  if (req.method !== 'POST' || !/^\/v1beta\/models\/[a-z0-9.-]+:generateContent$/.test(req.url))
    return send(501,{error:{code:501,message:'Endpoint not modeled'}});
  if (!req.headers['x-goog-api-key'] || !body.systemInstruction || body.generationConfig?.responseMimeType !== 'application/json' || !body.generationConfig?.responseJsonSchema)
    return send(400,{error:{code:400,message:'Request does not match generateContent contract'}});
  let input;
  try { input=JSON.parse(body.contents.at(-1).parts.map(p=>p.text || '').join('')); }
  catch { return send(400,{error:{code:400,message:'Expected JSON user context'}}); }
  const question=input.query || input.gameTitle || input.conversation?.at(-1)?.content || '';
  const scenario=question.match(/\[test:([a-z]+)\]/)?.[1] || mode;
  if (scenario === 'quota') return send(429,{error:{code:429,status:'RESOURCE_EXHAUSTED',message:'Quota exhausted'}});
  if (scenario === 'provider') return send(503,{error:{code:503,status:'UNAVAILABLE',message:'Unavailable'}});
  if (scenario === 'access') return send(403,{error:{code:403,status:'PERMISSION_DENIED',message:'Key not authorized'}});
  if (scenario === 'timeout') await new Promise(resolve=>setTimeout(resolve,6000));
  let decision;
  const grounded=body.tools?.some(tool=>tool.googleSearch);
  if (input.gameTitle !== undefined) {
    decision={found:!question.includes('Unknown'),title:question,
      minimum:{cpu:'Intel Core i5-8400',gpu:'NVIDIA GeForce GTX 1060 6GB',ramGb:16,storageGb:130,ssdRequired:false,notes:'Fixture requirements for protocol testing, not a real game.'},
      recommended:{cpu:'Intel Core i7-9700',gpu:'NVIDIA GeForce RTX 2060',ramGb:16,storageGb:130,ssdRequired:true,notes:'Fixture requirements for protocol testing.'}};
  } else if (input.requirements !== undefined) {
    decision={assessments:input.candidates.map(product=>{
      const gpu=product.hardware.gpu || '';
      const minimum=gpu.includes('3050') || gpu.includes('5060') ? 'meets' : 'unknown';
      const recommended=gpu.includes('5060') ? 'meets' : 'below';
      return {productId:product.id,cpuMinimum:'meets',gpuMinimum:minimum,cpuRecommended:'meets',gpuRecommended:recommended};
    })};
    if(scenario==='allpass') decision.assessments=decision.assessments.map(value=>({...value,gpuMinimum:'meets',gpuRecommended:'meets'}));
    if(scenario==='forgedcomparison') decision.assessments[0].productId='aaaaaaaa-aaaa-5aaa-8aaa-aaaaaaaaaaaa';
  } else if (input.query !== undefined) {
    const text=question.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d');
    const group=/chuot|mouse|con tro/.test(text)?'mouse':/man hinh|man de|monitor/.test(text)?'monitor':/phim|keyboard/.test(text)?'keyboard':/pc|may tinh|bo may|may choi/.test(text)?'pc':null;
    const max=text.match(/duoi\s*(\d+)\s*tr/);
    decision={groups:group?[group]:[],purpose:/game|gaming|valorant|ban sung/.test(text)?'gaming':/lam viec|van phong/.test(text)?'office':null,
      minBudget:null,maxBudget:max?Number(max[1])*1e6:null,inStockOnly:false,cheaper:/gia re|re nhat|tiet kiem/.test(text),cheapest:/re nhat/.test(text) || /gia re/.test(text)&&!max,keywords:/khong day|wireless/.test(text)?['wireless']:[]};
    if (scenario==='relax') decision={...decision,groups:['pc'],maxBudget:99999999,keywords:[]};
    if (scenario==='badintent') decision={...decision,groups:['laptop']};
  } else {
    const groups={pc:['pc-gaming','pc-office','pc-workstation'],cpu:['cpu'],vga:['vga'],mainboard:['mainboard'],ram:['ram'],storage:['storage'],cooling:['cooling'],case:['case'],psu:['psu'],monitor:['monitor'],keyboard:['keyboard'],mouse:['mouse']};
    const selected=input.constraints.groups.length>1?input.constraints.groups.map(group=>input.candidates.find(p=>groups[group]?.includes(p.category))).filter(Boolean):input.candidates.slice(0,2);
    decision={intent:'recommend',recommendations:selected.map(p=>({productId:p.id,reasons:['budget','specs']})),followUp:'requirements'};
    if (scenario==='forged') decision.recommendations[0].productId='aaaaaaaa-aaaa-5aaa-8aaa-aaaaaaaaaaaa';
    if (scenario==='prices') decision.recommendations[0].price=1;
    if (scenario==='clarify') decision={intent:'clarify',recommendations:[],followUp:'purpose'};
    if (scenario==='offtopic') decision={intent:'out_of_scope',recommendations:[],followUp:'none'};
  }
  const text=scenario==='invalid'?'{not valid JSON':JSON.stringify(decision);
  const groundingMetadata=grounded && scenario!=='noground' ? {groundingChunks:[{web:{uri:scenario==='badsource'?'javascript:alert(1)':'https://store.steampowered.com/app/2358720/Black_Myth_Wukong/',title:'Protocol fixture source'}}]} : undefined;
  send(200,{candidates:[{content:{role:'model',parts:[{text}]},finishReason:'STOP',index:0,...(groundingMetadata?{groundingMetadata}:{})}],
    modelVersion:'local-gemini-contract',usageMetadata:{promptTokenCount:30,candidatesTokenCount:40,totalTokenCount:70}});
}).listen(process.env.PORT || 4501);
