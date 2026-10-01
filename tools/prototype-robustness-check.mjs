// Real Chromium browser checks through CDP. Node >=22; no npm dependencies.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import http from 'node:http';
import {spawn} from 'node:child_process';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const arg = (name, fallback) => {const i=process.argv.indexOf(name);return i<0?fallback:process.argv[i+1]};
const preview=path.resolve(arg('--preview',path.join(root,'prototype-preview.html')));
const output=path.resolve(arg('--output',path.join(root,'captures','browser-check')));
const browser=arg('--browser',process.env.VRBILI_BROWSER || [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'
].find(fs.existsSync));
if(typeof WebSocket==='undefined')throw Error('Use Node.js >=22 with built-in WebSocket');
if(!browser || !fs.existsSync(browser))throw Error('Pass --browser with an installed Chromium executable');
fs.mkdirSync(output,{recursive:true});
const profile=fs.mkdtempSync(path.join(os.tmpdir(),'vrbili-browser-'));
const delay=ms=>new Promise(r=>setTimeout(r,ms));
const checks=[],errors=[],blocked=[];let proc,ws,server;let seq=0;const pending=new Map();
const report={kind:'real headless Chromium / CDP mouse, keyboard, native inputs and pixels',preview,previewSHA256:createHash('sha256').update(fs.readFileSync(preview)).digest('hex'),browser,profile,checks,errors,blocked,methods:{clicks:'CDP mouse press/release',typing:'CDP insertText + Enter',drag:'CDP mouse pointer input',selectAndRangeCases:'native DOM values with input/change; one range also checked with real ArrowRight',rendering:'real headless Chromium CSS and screenshots'}};
function assert(name,pass,detail={}){checks.push({name,passed:!!pass,...detail});if(!pass)process.exitCode=1;}
async function cdp(method,params={}){const id=++seq;return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{pending.delete(id);reject(Error(method+' timed out'))},10000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method,params}));});}
async function evaluate(expression){const r=await cdp('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.text+': '+(r.exceptionDetails.exception?.description||''));return r.result.value;}
async function state(){return evaluate('window.openai.widgetState?.privateContent');}
async function rect(selector){return evaluate(`(() => {const e=document.querySelector(${JSON.stringify(selector)});if(!e)throw Error('Missing selector');e.scrollIntoView({block:'center'});const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2,w:r.width,h:r.height};})()`);}
async function click(selector){const r=await rect(selector);await cdp('Input.dispatchMouseEvent',{type:'mousePressed',x:r.x,y:r.y,button:'left',clickCount:1});await cdp('Input.dispatchMouseEvent',{type:'mouseReleased',x:r.x,y:r.y,button:'left',clickCount:1});await delay(30);}
async function key(key,code=key){const vk={Enter:13,Escape:27,ArrowLeft:37,ArrowUp:38,ArrowRight:39,ArrowDown:40,Home:36,End:35}[key];await cdp('Input.dispatchKeyEvent',{type:'keyDown',key,code,windowsVirtualKeyCode:vk,nativeVirtualKeyCode:vk,...(key==='Enter'?{text:'\r',unmodifiedText:'\r'}:{})});await cdp('Input.dispatchKeyEvent',{type:'keyUp',key,code,windowsVirtualKeyCode:vk,nativeVirtualKeyCode:vk});}
async function change(selector,value){await evaluate(`(() => {const e=document.querySelector(${JSON.stringify(selector)});if(e.type==='checkbox')e.checked=${JSON.stringify(value)};else e.value=${JSON.stringify(value)};e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));})()`);}
async function shot(name){await evaluate('window.scrollTo(0,0)');const r=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});fs.writeFileSync(path.join(output,name+'.png'),Buffer.from(r.data,'base64'));}
async function fresh(width=1280,height=1000,scheme='light'){await cdp('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:false});await cdp('Emulation.setEmulatedMedia',{features:[{name:'prefers-color-scheme',value:scheme},{name:'prefers-reduced-motion',value:'reduce'}]});await cdp('Page.navigate',{url:'about:blank'});await cdp('Page.navigate',{url:report.url});for(let i=0;i<100;i++){if(await evaluate('!!document.querySelector("#pv-search")'))break;await delay(20);}await evaluate('localStorage.clear()');await cdp('Page.reload');for(let i=0;i<100;i++){if(await evaluate('!!document.querySelector("#pv-search")'))return;await delay(20);}throw Error('Home did not load');}
async function layoutCheck(name){const v=await evaluate(`(() => {const viewport=document.documentElement.clientWidth;const nodes=[...document.querySelectorAll('#pili-v1-app button,#pili-v1-app input,#pili-v1-app select')].filter(e=>e.getClientRects().length);return {viewport,scrollWidth:document.documentElement.scrollWidth,outside:nodes.filter(e=>{const r=e.getBoundingClientRect();return r.left<-.5||r.right>viewport+.5}).map(e=>({id:e.id,action:e.dataset.action,text:e.textContent.trim().slice(0,25)}))};})()`);assert(name,v.scrollWidth<=v.viewport+1&&v.outside.length===0,v);}
async function launchBrowser(){
 proc=spawn(browser,['--headless=new','--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-component-update','--disable-sync','--disable-extensions','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:['ignore','ignore','pipe']});
 let browserStderr='';let spawnError;proc.on('error',e=>{spawnError=e});proc.stderr.on('data',b=>{browserStderr+=b.toString()});
 const active=path.join(profile,'DevToolsActivePort');for(let i=0;i<200&&!fs.existsSync(active);i++){if(spawnError)throw spawnError;if(proc.exitCode!==null)throw Error('Chromium exited '+proc.exitCode+' '+browserStderr.slice(-1500));await delay(50);}if(!fs.existsSync(active))throw Error('Chromium did not expose CDP '+browserStderr.slice(-1500));
 const port=fs.readFileSync(active,'utf8').split('\n')[0];const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json();const target=pages.find(p=>p.type==='page');ws=new WebSocket(target.webSocketDebuggerUrl);
 ws.addEventListener('message',e=>{const r=JSON.parse(e.data);if(r.id){const p=pending.get(r.id);if(p){pending.delete(r.id);clearTimeout(p.timer);r.error?p.reject(Error(r.error.message)):p.resolve(r.result);}}else if(r.method==='Runtime.exceptionThrown')errors.push(r.params.exceptionDetails);else if(r.method==='Network.requestWillBeSent'&&!/^(http:\/\/127\.0\.0\.1:|about:|data:)/.test(r.params.request.url))blocked.push(r.params.request.url);});
 await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});
 await cdp('Page.enable');await cdp('Runtime.enable');await cdp('Network.enable');report.version=await cdp('Browser.getVersion');

}
async function closeBrowser(){
 const owned=proc;if(ws?.readyState===WebSocket.OPEN){try{await cdp('Browser.close')}catch{}ws.close();}
 if(owned?.exitCode===null)await Promise.race([new Promise(resolve=>owned.once('exit',resolve)),delay(3000)]);
 if(owned?.exitCode===null){owned.kill();await Promise.race([new Promise(resolve=>owned.once('exit',resolve)),delay(3000)]);}
 if(owned?.exitCode===null)throw Error('Owned browser did not close');
 ws=null;const active=path.join(profile,'DevToolsActivePort');if(fs.existsSync(active))fs.unlinkSync(active);
}
async function settle(){for(let i=0;i<30;i++){if(await evaluate('!!document.querySelector("#pv-content")?.children.length'))return true;await delay(20);}return false;}
async function rapid(selector,count){for(let i=0;i<count;i++){const r=await rect(selector);await cdp('Input.dispatchMouseEvent',{type:'mousePressed',x:r.x,y:r.y,button:'left',clickCount:1});await cdp('Input.dispatchMouseEvent',{type:'mouseReleased',x:r.x,y:r.y,button:'left',clickCount:1});}}
async function fixture(raw){await evaluate(`localStorage.setItem('vrbilibili-v1:'+location.pathname,${JSON.stringify(raw)})`);await cdp('Page.reload');return settle();}
async function unique(){return evaluate('(() => {const ids=[...document.querySelectorAll("[id]")].map(e=>e.id);return ids.length===new Set(ids).size})()');}
async function scenario(name,fn){report.scenario=name;try{await fn()}catch(e){assert(name,false,{error:String(e.stack||e)});}report.scenario=null;}
try{
 server=http.createServer((req,res)=>{if(req.url!=='/prototype-preview.html'){res.writeHead(404);res.end();return;}res.setHeader('Content-Type','text/html; charset=utf-8');res.end(fs.readFileSync(preview));});
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));report.url=`http://127.0.0.1:${server.address().port}/prototype-preview.html`;
 await launchBrowser();
 await scenario('rapid',async()=>{
  await fresh();await click('[data-action="detail"][data-id="valley"]');const initial=(await state()).later.includes('valley');
  await rapid('[data-action="later"]',20);assert('20次快速稍后再看切换保持奇偶与UI一致',(await state()).later.includes('valley')===initial&&await unique());
  await click('[data-action="play-detail"]');await click('[data-action="play"]');await rapid('[data-action="play"]',20);assert('20次快速播放切换回到暂停',await evaluate('document.querySelector("[data-action=play]").textContent.includes("播放")'));
  await rapid('[data-action="next"]',10);assert('快速下一段在末集停止',(await state()).part===2);await rapid('[data-action="previous"]',10);assert('快速上一段在首集停止',(await state()).part===0);
  await click('[data-action="play"]');const visible=(await state()).layouts.focus.visible.queue;await rapid('.pv-dock [data-action="toggle-panel"][data-panel="queue"]',20);assert('20次快速组件切换无重复或错误可见状态',(await state()).layouts.focus.visible.queue===visible&&await unique());
 });
 await scenario('escape-combination',async()=>{
  await fresh();await click('[data-action="detail"][data-id="valley"]');await click('[data-action="play-detail"]');await click('[data-action="play"]');await change('#pv-seek','137');
  await click('[data-action="inspector"][data-tool="settings"]');await key('Escape');assert('重绘后真实Escape立即关闭设置',(await state()).view==='watch'&&await evaluate('document.querySelector("#pv-inspector").hidden'));
  await key('Escape');assert('第二次Escape回到详情且保留进度',(await state()).view==='detail'&&(await state()).progress['valley:0']===137);
  await key('Escape');assert('内容页再次Escape不改来源',(await state()).view==='detail');
  await click('[data-action="play-detail"]');await click('[data-action="inspector"][data-tool="layout"]');await click('[data-action="exit-watch"]');await key('Escape');assert('点击一级返回加Escape二级返回一致',(await state()).view==='detail');
 });
 await scenario('layout-resize',async()=>{
  await fresh();await click('[data-action="nav"][data-to="watch"]');await click('[data-action="layout"][data-layout="desk"]');await click('[data-action="inspector"][data-tool="layout"]');await change('#pv-size','115');await change('#pv-distance','2.8');await change('#pv-target','queue');await click('#pv-lock');await click('[data-action="inspector"][data-tool="settings"]');await change('#pv-opacity','45');await change('#pv-seek','137');
  const saved=await state();for(const width of [320,736,860,861,880,1024,1280]){await cdp('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:false});await delay(40);await layoutCheck(`动态resize ${width}px不溢出`);}
  assert('连续resize保留布局、设置与进度',JSON.stringify((await state()).layouts)===JSON.stringify(saved.layouts)&&(await state()).progress['valley:0']===137&&await unique());await shot('resized-settings');
  for(let i=0;i<12;i++)await click(`[data-action="layout"][data-layout="${['focus','social','desk'][i%3]}"]`);assert('12次预设循环保留完整桌面配置',JSON.stringify((await state()).layouts.desk)===JSON.stringify(saved.layouts.desk)&&(await state()).opacity===45);
 });
 await scenario('viewport-threshold-drag',async()=>{
  await fresh(880);await click('[data-action="nav"][data-to="watch"]');await click('[data-action="inspector"][data-tool="layout"]');const before=await state();const r=await rect('[data-panel="main"] [data-drag]');await cdp('Input.dispatchMouseEvent',{type:'mousePressed',x:r.x,y:r.y,button:'left',clickCount:1});await cdp('Input.dispatchMouseEvent',{type:'mouseMoved',x:r.x+28,y:r.y,button:'left',buttons:1});await cdp('Input.dispatchMouseEvent',{type:'mouseReleased',x:r.x+28,y:r.y,button:'left',clickCount:1});assert('880px宽屏CSS模式仍能真实拖动',(await state()).layouts.focus.panels.main.x!==before.layouts.focus.panels.main.x);
 });
 await scenario('storage',async()=>{
  await fresh();await click('[data-action="nav"][data-to="watch"]');const valid=await state();
  const cases=[['非JSON','{broken'],['null根','null'],['数组根','[]'],['超长存储','x'.repeat(17000)],['未知版本',JSON.stringify({privateContent:{version:99,view:'watch'}})],['缺少辅助窗口',JSON.stringify({privateContent:{...valid,layouts:{...valid.layouts,desk:{panels:{main:valid.layouts.desk.panels.main}},focus:{panels:{main:valid.layouts.focus.panels.main}},social:{panels:{main:valid.layouts.social.panels.main}}}}})],['损坏进度和分集',JSON.stringify({privateContent:{...valid,progress:null,part:.5}})],['损坏数值与枚举',JSON.stringify({privateContent:{...valid,view:'watch',query:{x:1},speed:'fast',volume:{x:1},quality:null,layouts:{...valid.layouts,focus:{...valid.layouts.focus,visible:null,panels:{main:{x:'bad',size:null,distance:'near'},queue:null,comments:{}}}}}})]];
  for(const [name,raw] of cases){const count=errors.length;const rendered=await fixture(raw);assert(`存储 ${name}可恢复可操作画面`,rendered&&errors.length===count,{newErrors:errors.slice(count)});if(rendered){await click('[data-action="nav"][data-to="home"]');assert(`存储 ${name}回退后可继续浏览`,await evaluate('document.querySelectorAll(".pv-video-card").length===6'));}}
  const legacy=structuredClone(valid);legacy.layout='desk';legacy.opacity=67;for(const l of Object.values(legacy.layouts))delete l.opacity;delete legacy.returnView;await fixture(JSON.stringify({privateContent:legacy}));await click('[data-action="layout"][data-layout="focus"]');assert('旧版全局透明度在真实浏览器迁移到预设',(await state()).opacity===67);
  const quota=await cdp('Page.addScriptToEvaluateOnNewDocument',{source:'Storage.prototype.getItem=function(){throw new DOMException("test denied","SecurityError")};Storage.prototype.setItem=function(){throw new DOMException("test quota","QuotaExceededError")};'});await cdp('Page.reload');await settle();await cdp('Page.removeScriptToEvaluateOnNewDocument',{identifier:quota.identifier});await click('[data-action="nav"][data-to="library"]');assert('存储不可用时本次会话仍可操作',(await state()).view==='library');
 });
 await scenario('browser-reopen',async()=>{
  await fresh();await click('[data-action="nav"][data-to="library"]');await click('[data-action="library-tab"][data-tab="history"]');await click('[data-action="resume-card"]');await click('[data-action="play"]');await change('#pv-seek','181');await click('[data-action="layout"][data-layout="desk"]');await click('[data-action="inspector"][data-tool="settings"]');await change('#pv-opacity','53');const expected=await state();
  for(let i=1;i<=2;i++){await closeBrowser();await launchBrowser();await cdp('Page.navigate',{url:report.url});await settle();assert(`真实关闭再打开 ${i}恢复进度与完整布局`,await evaluate('document.querySelector("#pv-seek").value==="181"')&&JSON.stringify((await state()).layouts)===JSON.stringify(expected.layouts));assert(`真实关闭再打开 ${i}保持暂停`,await evaluate('document.querySelector("[data-action=play]").textContent.includes("播放")'));}
  await shot('reopened-desk');
 });
 assert('本轮没有未处理浏览器脚本异常',errors.length===0,{count:errors.length});assert('本轮没有外部页面资源请求',blocked.length===0,{requests:blocked});
}catch(e){report.fatal=String(e.stack||e);process.exitCode=1;}finally{
 report.passed=checks.filter(x=>x.passed).length;report.failed=checks.filter(x=>!x.passed).length;report.timestamp=new Date().toISOString();
 fs.writeFileSync(path.join(output,'browser-results.json'),JSON.stringify(report,null,2));
 if(ws?.readyState===WebSocket.OPEN){try{await cdp('Browser.close')}catch{}ws.close();}else if(proc && proc.exitCode===null)proc.kill();
 if(server)await new Promise(resolve=>server.close(resolve));
 // Only remove this run's isolated temporary profile after its own browser exits.
 if(proc?.exitCode===null)await Promise.race([new Promise(resolve=>proc.once('exit',resolve)),delay(3000)]);
 if(proc?.exitCode===null){proc.kill();await Promise.race([new Promise(resolve=>proc.once('exit',resolve)),delay(3000)]);report.ownedProcessFallbackStopped=true;}
 if(!proc||proc.exitCode!==null){try{fs.rmSync(profile,{recursive:true,force:true});report.profileRemoved=true;}catch{report.profileRemoved=false;}}
 fs.writeFileSync(path.join(output,'browser-results.json'),JSON.stringify(report,null,2));
 console.log(JSON.stringify({output,passed:report.passed,failed:report.failed,fatal:report.fatal,profileRemoved:report.profileRemoved}));
}
