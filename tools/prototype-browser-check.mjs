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
try{
 server=http.createServer((req,res)=>{if(req.url!=='/prototype-preview.html'){res.writeHead(404);res.end();return;}res.setHeader('Content-Type','text/html; charset=utf-8');res.end(fs.readFileSync(preview));});
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));report.url=`http://127.0.0.1:${server.address().port}/prototype-preview.html`;
 proc=spawn(browser,['--headless=new','--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-component-update','--disable-sync','--disable-extensions','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:['ignore','ignore','pipe']});
 let browserStderr='';let spawnError;proc.on('error',e=>{spawnError=e});proc.stderr.on('data',b=>{browserStderr+=b.toString()});
 const active=path.join(profile,'DevToolsActivePort');for(let i=0;i<200&&!fs.existsSync(active);i++){if(spawnError)throw spawnError;if(proc.exitCode!==null)throw Error('Chromium exited '+proc.exitCode+' '+browserStderr.slice(-1500));await delay(50);}if(!fs.existsSync(active))throw Error('Chromium did not expose CDP '+browserStderr.slice(-1500));
 const port=fs.readFileSync(active,'utf8').split('\n')[0];const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json();const target=pages.find(p=>p.type==='page');ws=new WebSocket(target.webSocketDebuggerUrl);
 ws.addEventListener('message',e=>{const r=JSON.parse(e.data);if(r.id){const p=pending.get(r.id);if(p){pending.delete(r.id);clearTimeout(p.timer);r.error?p.reject(Error(r.error.message)):p.resolve(r.result);}}else if(r.method==='Runtime.exceptionThrown')errors.push(r.params.exceptionDetails);else if(r.method==='Network.requestWillBeSent'&&!/^(http:\/\/127\.0\.0\.1:|about:|data:)/.test(r.params.request.url))blocked.push(r.params.request.url);});
 await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});
 await cdp('Page.enable');await cdp('Runtime.enable');await cdp('Network.enable');report.version=await cdp('Browser.getVersion');
 for(const width of [320,736,1024,1280]){
  await fresh(width);await layoutCheck(`发现页 ${width}px 无横向溢出`);await shot(`home-${width}`);
  await click('[data-action="resume-card"]');await layoutCheck(`播放页 ${width}px 无横向溢出`);
  await click('[data-action="layout"][data-layout="desk"]');await layoutCheck(`桌面布局 ${width}px 无横向溢出`);await shot(`desk-${width}`);
  await click('[data-action="inspector"][data-tool="settings"]');await layoutCheck(`观看设置 ${width}px 无横向溢出`);
 }
 await fresh();await click('#pv-search');await cdp('Input.insertText',{text:'旅行'});await key('Enter');
 assert('真实键盘提交搜索得到两个结果',await evaluate('document.querySelectorAll(".pv-video-card").length===2'));
 await click('[data-action="detail"][data-id="valley"]');await click('[data-action="later"]');await click('[data-action="select-detail"][data-part="2"]');await click('[data-action="play-detail"]');await click('[data-action="play"]');await change('#pv-seek','42');
 assert('选集与独立进度正确',(await state()).progress['valley:2']===42&&await evaluate('document.querySelector("#pv-seek").max==="636"'));
 await click('[data-action="exit-watch"]');assert('详情进入播放后返回原详情',(await state()).view==='detail');
 await click('[data-action="nav"][data-to="library"]');await click('[data-action="library-tab"][data-tab="history"]');await click('[data-action="resume-card"][data-id="valley"][data-part="2"]');await click('[data-action="play"]');await key('Escape');
 assert('历史续播按 Escape 返回历史页',(await state()).view==='library'&&(await state()).library==='history');
 await click('[data-action="nav"][data-to="watch"]');await click('[data-action="layout"][data-layout="desk"]');await click('[data-action="inspector"][data-tool="settings"]');await change('#pv-opacity','35');await click('[data-action="layout"][data-layout="focus"]');await click('[data-action="inspector"][data-tool="settings"]');
 assert('预设分别保存透明度',(await state()).opacity===88);
 await click('[data-action="layout"][data-layout="desk"]');assert('切回桌面透明度恢复35%',(await state()).opacity===35);
 await click('[data-action="inspector"][data-tool="layout"]');await change('#pv-target','queue');await click('#pv-lock');await change('#pv-target','main');const before=await state();
 const grip=await rect('[data-panel="main"] [data-drag]');await cdp('Input.dispatchMouseEvent',{type:'mousePressed',x:grip.x,y:grip.y,button:'left',clickCount:1});await cdp('Input.dispatchMouseEvent',{type:'mouseMoved',x:grip.x-35,y:grip.y+18,button:'left',buttons:1});await cdp('Input.dispatchMouseEvent',{type:'mouseReleased',x:grip.x-35,y:grip.y+18,button:'left',clickCount:1});const after=await state();
 assert('真实标题栏拖动主屏成功',after.layouts.desk.panels.main.x!==before.layouts.desk.panels.main.x);
 assert('锁定选集不随主屏拖动',after.layouts.desk.panels.queue.x===before.layouts.desk.panels.queue.x&&after.layouts.desk.panels.queue.y===before.layouts.desk.panels.queue.y);
 assert('未锁定评论随主屏拖动',after.layouts.desk.panels.comments.x!==before.layouts.desk.panels.comments.x);
 await change('#pv-target','comments');await click('#pv-follow');const hiddenX=(await state()).layouts.desk.panels.comments.x;await click('[data-action="toggle-panel"][data-panel="comments"]');await click('[data-action="recall"]');assert('召回不移动隐藏评论',(await state()).layouts.desk.panels.comments.x===hiddenX&&!(await state()).layouts.desk.visible.comments);
 await change('#pv-target','main');await click('#pv-lock');const locked=await state();await click('[data-action="nudge"][data-dx="-3"]');assert('锁定主屏禁止方向按钮移动',(await state()).layouts.desk.panels.main.x===locked.layouts.desk.panels.main.x);
 await click('[data-action="recall"]');assert('召回保留锁定与透明度',(await state()).layouts.desk.panels.main.locked&&(await state()).opacity===35);
 await click('[data-action="exit-watch"]');assert('一级返回先关闭摆放',(await state()).view==='watch'&&await evaluate('document.querySelector("#pv-inspector").hidden'));
 await click('#pv-seek');const oldSeek=Number(await evaluate('document.querySelector("#pv-seek").value'));await key('ArrowRight');assert('真实键盘滑杆步进改变进度',Number(await evaluate('document.querySelector("#pv-seek").value'))===oldSeek+1);
 await change('#pv-review-state','error');await shot('playback-error');const progress=(await state()).progress['valley:2'];await click('[data-action="retry"]');assert('失败重试保留进度',(await state()).progress['valley:2']===progress);
 await click('[data-action="play"]');await click('[data-action="inspector"][data-tool="danmaku"]');await change('#pv-danmaku','river');await click('[data-action="preview"]');assert('河流处于独立通道',await evaluate('document.querySelector("#pv-effects").parentElement.id==="pv-stage"'));await click('[data-action="stop-effects"]');assert('停止清除所有效果',await evaluate('document.querySelectorAll(".pv-effect-tag").length===0'));
 await change('#pv-review-state','ended');await click('[data-action="replay"]');assert('结束后重播从零开始',(await state()).progress['valley:2']===0);
 await click('[data-action="previous"]');await change('#pv-review-state','ended');assert('非末集结束同时提供下一段和返回详情',await evaluate('!!document.querySelector("#pv-play-status [data-action=next]")&&!!document.querySelector("#pv-play-status [data-action=watch-detail]")'));
 await change('#pv-review-state','expired');assert('仅显示模拟登录入口',await evaluate('!document.querySelector("#pv-auth").hidden'));await click('[data-action="login-confirm"]');assert('模拟登录保留布局',(await state()).signed&&(await state()).layouts.desk.panels.main.locked);
 await cdp('Page.reload');await delay(150);assert('真实刷新恢复状态并暂停',await evaluate('window.openai.widgetState.privateContent.layout==="desk"&&document.querySelector("[data-action=play]").textContent.includes("播放")'));await shot('restored-desk');
 await fresh(1280,1000,'dark');await click('[data-action="resume-card"]');await click('[data-action="layout"][data-layout="desk"]');await layoutCheck('深色桌面布局无横向溢出');await shot('desk-dark');
 await fresh(320);assert('320px 主导航文字保持单行',await evaluate('[...document.querySelectorAll(".pv-nav button")].every(e=>e.getBoundingClientRect().height<=44)'));
 assert('没有浏览器脚本异常',errors.length===0,{count:errors.length});assert('没有外部网络资源请求',blocked.length===0,{requests:blocked});
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
