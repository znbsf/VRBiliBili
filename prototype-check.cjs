const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const file = path.join(__dirname, 'piliplus-v1-prototype.html');
const source = fs.readFileSync(file, 'utf8');
const scripts = [...source.matchAll(/<script>([\s\S]*?)<\/script>/g)];
const checks = [];
const check = (name, condition) => { checks.push({name,passed:!!condition}); if(!condition)process.exitCode=1; };
for (const [i, m] of scripts.entries()) new vm.Script(m[1], { filename: 'inline-'+i+'.js' });
check('JavaScript parses', scripts.length === 1);
check('Fragment is below 1 MB', Buffer.byteLength(source) < 1000000);
check('No full-page document tags', !/<(?:!doctype|html|head|body)(?:\s|>)/i.test(source));
check('No network requests or remote resource URLs', !/\b(fetch|XMLHttpRequest|WebSocket)\s*\(|https?:\/\//.test(source));
check('No escaped markup quotes', !source.includes(String.fromCharCode(92,34)));
const ids = [...source.matchAll(/\bid="([a-z][a-z0-9-]+)"/gi)].map(m=>m[1]);
check('Literal IDs are unique', ids.length === new Set(ids).size);
const refs = [...source.matchAll(/querySelector\('#([a-z][a-z0-9-]+)'\)/gi)].map(m=>m[1]);
check('Every literal ID lookup is declared', refs.every(id=>ids.includes(id)));
const actions = new Set([...source.matchAll(/button\('([a-z][a-z0-9-]+)'/g)].map(m=>m[1]).concat([...source.matchAll(/data-action="([a-z][a-z0-9-]+)"/g)].map(m=>m[1])));
check('Every visible action has a handler', [...actions].every(a=>source.includes("a==='"+a+"'")));
const docDir = __dirname;
for (const name of ['README.md','docs/history/V1-FUNCTIONAL-UI-DESIGN.md']) {
 const doc=fs.readFileSync(path.join(docDir,name),'utf8');
 const localLinks=[...doc.matchAll(/\]\(([^)]+\.md)\)/g)].map(m=>m[1]).filter(u=>!u.startsWith('http'));
 check(name+' local links resolve',localLinks.every(p=>fs.existsSync(path.resolve(path.dirname(path.join(docDir,name)),p))));
}
console.log(JSON.stringify({file,bytes:Buffer.byteLength(source),literalIds:ids.length,actions:actions.size,checks},null,2));
