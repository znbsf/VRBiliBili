import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../playback-probe');
const files = new Map([['/','index.html'],['/index.html','index.html'],['/app.mjs','app.mjs'],['/session.mjs','session.mjs']]);
const server = http.createServer((req,res) => {
  const name = files.get(req.url); if (!name || req.method !== 'GET') { res.writeHead(404);res.end();return; }
  res.setHeader('Content-Type', name.endsWith('.html') ? 'text/html; charset=utf-8' : 'text/javascript; charset=utf-8');
  res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');
  res.end(fs.readFileSync(path.join(root,name)));
});
server.listen(0,'127.0.0.1', () => console.log(`技术探针：http://127.0.0.1:${server.address().port}/`));
for (const signal of ['SIGINT','SIGTERM']) process.on(signal, () => server.close(() => process.exit()));
