const http=require('http');
const catalog=require('./catalog.json');
http.createServer((req,res)=>{
  res.setHeader('Content-Type','application/json');
  if(req.method==='GET'&&req.url==='/api/catalog/storefront') {res.writeHead(200);return res.end(JSON.stringify(catalog));}
  res.writeHead(501);res.end(JSON.stringify({error:'Endpoint not modeled'}));
}).listen(process.env.PORT||4500);
