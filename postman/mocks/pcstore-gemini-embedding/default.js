// Repository-owned HTTP contract fixture. These vectors do NOT represent real semantics.
const http = require('http');
let mode = 'normal';
let requests = 0;
http.createServer(async (req, res) => {
  const send = (status, body) => {
    res.writeHead(status, {'Content-Type': 'application/json'});
    res.end(JSON.stringify(body));
  };
  let body = req.body;
  if (!body) {
    let raw = ''; for await (const chunk of req) raw += chunk;
    try { body = raw ? JSON.parse(raw) : {}; }
    catch { return send(400, {error: {code: 400, message: 'Invalid JSON'}}); }
  }
  if (req.url === '/__control' && req.method === 'POST') {
    mode = body.mode || 'normal'; requests = 0;
    return send(200, {mode});
  }
  if (req.url === '/__stats' && req.method === 'GET') return send(200, {mode, requests});
  const route = req.url.match(/^\/v1beta\/models\/([a-z0-9.-]+):(embedContent|batchEmbedContents)$/);
  if (req.method !== 'POST' || !route) return send(501, {error: {code: 501, message: 'Endpoint not modeled'}});
  const batch = route[2] === 'batchEmbedContents';
  const entries = batch ? body.requests : [body];
  if (!req.headers['x-goog-api-key'] || !Array.isArray(entries) || entries.length === 0 ||
      entries.some(entry => !entry.content?.parts?.some(part => typeof part.text === 'string' && part.text.trim()) ||
        entry.outputDimensionality !== 768)) {
    return send(400, {error: {code: 400, message: 'Invalid embedding HTTP contract'}});
  }
  requests++;
  if (mode === 'quota') return send(429, {error: {code: 429, status: 'RESOURCE_EXHAUSTED', message: 'Fixture quota'}});
  if (mode === 'access') return send(403, {error: {code: 403, status: 'PERMISSION_DENIED', message: 'Fixture access'}});
  if (mode === 'provider') return send(503, {error: {code: 503, status: 'UNAVAILABLE', message: 'Fixture unavailable'}});
  if (mode === 'timeout') await new Promise(resolve => setTimeout(resolve, 2500));
  const embeddings = entries.map(() => ({values: Array.from({length: mode === 'wrongdimension' ? 2 : 768},
    (_, index) => mode === 'zero' ? 0 : index === 0 ? 1 : 0)}));
  if (mode === 'empty') return send(200, {});
  return send(200, batch ? {embeddings} : {embedding: embeddings[0]});
}).listen(process.env.PORT || 4502);
