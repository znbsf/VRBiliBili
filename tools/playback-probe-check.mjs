// Actual encoded media, decoder, HTTP failures and offline events in isolated Chrome.
// Node >=22, installed Chromium; no npm/browser download or external test media.
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import http from 'node:http';
import {spawn} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const arg=(name,fallback)=>{const i=process.argv.indexOf(name);return i<0?fallback:process.argv[i+1]};
const output=path.resolve(arg('--output',path.join(root,'captures/playback-probe')));
const browser=arg('--browser',process.env.VRBILI_BROWSER||['C:/Program Files/Google/Chrome/Application/chrome.exe','C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(fs.existsSync));
if(!browser||typeof WebSocket==='undefined')throw Error('Use Node >=22 and --browser with installed Chromium');
fs.mkdirSync(output,{recursive:true});
const profile=fs.mkdtempSync(path.join(os.tmpdir(),'vrbili-playback-'));
const delay=ms=>new Promise(r=>setTimeout(r,ms));
const alive=()=>proc&&proc.exitCode===null&&proc.signalCode===null;
const checks=[],errors=[],requests=[];let proc,ws,server,media,mime,flaky=false,sequence=0,slowAborted=0;const pending=new Map();
const report={kind:'actual owned H.264/AAC video+audio and real HTMLMediaElement / isolated Chromium',checks,errors,profile,methods:{media:'committed tiny owned H.264/AAC color sequence and sine tone; generator included',input:'CDP mouse clicks, native file chooser assignment and range input',network:'real HTTP 404/corrupt response and CDP offline/online',persistence:'same-profile refresh and actual browser close/reopen; source reselected'},limitations:['Does not validate PiliPlus/media-kit decoder, DASH split tracks, Bilibili login/network, XR or headset audio/comfort']};
function assert(name,pass,detail={}){checks.push({name,passed:!!pass,...detail});if(!pass)process.exitCode=1;}
async function cdp(method,params={}){const id=++sequence;return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{pending.delete(id);reject(Error(method+' timed out'))},12000);pending.set(id,{resolve,reject,timer});ws.send(JSON.stringify({id,method,params}));});}
async function evaluate(expression){const r=await cdp('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true,userGesture:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description||r.exceptionDetails.text);return r.result.value;}
async function until(expression){for(let i=0;i<150;i++){if(await evaluate(expression))return;await delay(40);}throw Error('Condition timed out: '+expression);}
const state=()=>evaluate('window.playbackProbe.snapshot');
async function click(id){const r=await evaluate(`(()=>{const e=document.getElementById(${JSON.stringify(id)});e.scrollIntoView({block:'center'});const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2}})()`);await cdp('Input.dispatchMouseEvent',{type:'mousePressed',...r,button:'left',clickCount:1});await cdp('Input.dispatchMouseEvent',{type:'mouseReleased',...r,button:'left',clickCount:1});}
async function loadUrl(url){await evaluate(`document.getElementById('url').value=${JSON.stringify(url)}`);await click('load-url');}
async function file(){await cdp('DOM.setFileInputFiles',{files:[report.fixturePath],objectId:(await cdp('Runtime.evaluate',{expression:'document.getElementById("file")'})).result.objectId});await until('window.playbackProbe.snapshot.phase==="ready"');}
async function seek(value){await evaluate(`(()=>{const e=document.getElementById('seek');e.value=${value};e.dispatchEvent(new Event('input',{bubbles:true}))})()`);await until(`Math.abs(document.getElementById('media').currentTime-${value})<.25`);}
async function page(){await cdp('Page.navigate',{url:report.url});await until('!!window.playbackProbe');}
async function storageFixture(raw){const script=await cdp('Page.addScriptToEvaluateOnNewDocument',{source:`localStorage.setItem('vrbili-real-media-progress-v1',${JSON.stringify(raw)})`});await cdp('Page.reload');await until('!!window.playbackProbe');await cdp('Page.removeScriptToEvaluateOnNewDocument',{identifier:script.identifier});}
async function launch(){
 proc=spawn(browser,['--headless=new','--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-component-update','--disable-sync','--disable-extensions','--disable-background-timer-throttling','--disable-backgrounding-occluded-windows','--disable-renderer-backgrounding','--disable-features=CalculateNativeWinOcclusion','--mute-audio','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank'],{windowsHide:true,stdio:['ignore','ignore','pipe']});
 let failure;let stderr='';proc.on('error',e=>failure=e);proc.stderr.on('data',b=>stderr+=b.toString());
 const active=path.join(profile,'DevToolsActivePort');let activeText='';
 for(let i=0;i<200;i++){
  if(failure)throw failure;if(!alive())throw Error('Chrome exited '+proc.exitCode);
  try{activeText=fs.readFileSync(active,'utf8');if(/^\d+\r?\n\//.test(activeText))break}catch(e){if(!['ENOENT','EBUSY','EACCES'].includes(e.code))throw e}
  await delay(40);
 }
 if(!/^\d+\r?\n\//.test(activeText))throw Error('Chrome CDP unavailable');
 const port=activeText.split('\n')[0];const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json();ws=new WebSocket(pages.find(x=>x.type==='page').webSocketDebuggerUrl);
 ws.addEventListener('message',e=>{const data=JSON.parse(e.data);if(data.id){const p=pending.get(data.id);if(p){pending.delete(data.id);clearTimeout(p.timer);data.error?p.reject(Error(data.error.message)):p.resolve(data.result);}}else if(data.method==='Runtime.exceptionThrown')errors.push(data.params.exceptionDetails);});
 await new Promise((resolve,reject)=>{ws.addEventListener('open',resolve,{once:true});ws.addEventListener('error',reject,{once:true});});await cdp('Page.enable');await cdp('Runtime.enable');await cdp('Network.enable');await cdp('Emulation.setDeviceMetricsOverride',{width:1024,height:1000,deviceScaleFactor:1,mobile:false});report.browser=await cdp('Browser.getVersion');
}
async function close(){if(ws?.readyState===WebSocket.OPEN){try{await cdp('Browser.close')}catch{}ws.close();}if(alive())await Promise.race([new Promise(r=>proc.once('exit',r)),delay(3000)]);if(alive()){proc.kill();await Promise.race([new Promise(r=>proc.once('exit',r)),delay(3000)]);}for(const p of pending.values()){clearTimeout(p.timer);p.reject(Error('Browser closed'));}pending.clear();const active=path.join(profile,'DevToolsActivePort');if(!alive()&&fs.existsSync(active))fs.unlinkSync(active);}
try{
 const files={'/':'index.html','/app.mjs':'app.mjs','/session.mjs':'session.mjs'};
 server=http.createServer((req,res)=>{
  const url=new URL(req.url,'http://127.0.0.1');
  if(files[url.pathname]){res.setHeader('Content-Type',url.pathname==='/'?'text/html; charset=utf-8':'text/javascript; charset=utf-8');res.setHeader('Cache-Control','no-store');res.end(fs.readFileSync(path.join(root,'playback-probe',files[url.pathname])));return;}
  requests.push(url.pathname);
  if(url.pathname==='/corrupt'){res.setHeader('Content-Type','video/mp4');res.end('invalid media bytes');return;}
  if(url.pathname==='/slow'){res.on('close',()=>{if(!res.writableEnded)slowAborted++});setTimeout(()=>{if(!res.destroyed){res.writeHead(503);res.end();}},1000);return;}
  if(url.pathname!=='/media'||!media||flaky){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',mime);res.setHeader('Cache-Control','no-store');res.setHeader('Accept-Ranges','bytes');
  const match=req.headers.range?.match(/^bytes=(\d+)-(\d*)$/);const start=match?Number(match[1]):0;const end=match&&match[2]?Math.min(Number(match[2]),media.length-1):media.length-1;
  if(start>=media.length||end<start){res.writeHead(416,{'Content-Range':`bytes */${media.length}`});res.end();return;}
  if(match)res.writeHead(206,{'Content-Range':`bytes ${start}-${end}/${media.length}`,'Content-Length':end-start+1});else res.setHeader('Content-Length',media.length);
  res.end(media.subarray(start,end+1));
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));report.url=`http://127.0.0.1:${server.address().port}/`;
 await launch();await page();
 const fixturePath=path.resolve(arg('--fixture',path.join(root,'tests/fixtures/playback-h264-aac.mp4')));
 media=fs.readFileSync(fixturePath);mime='video/mp4';report.fixturePath=fixturePath;
 report.fixture={mime,bytes:media.length,sha256:createHash('sha256').update(media).digest('hex'),containsAVC:media.includes(Buffer.from('avc1')),containsAAC:media.includes(Buffer.from('mp4a')),containsVideoTrack:media.includes(Buffer.from('vide')),origin:'Owned 2D colors and sine tone; generator script included; no external/user media'};
 assert('原创H.264/AAC测试素材含视频及音轨',media.length>1000&&report.fixture.containsAVC&&report.fixture.containsAAC&&report.fixture.containsVideoTrack);
 await file();assert('本地素材取得真实时长',(await state()).duration>4&&(await state()).duration<8);
 await click('play');await evaluate('document.getElementById("media").scrollIntoView({block:"center"})');await until('document.getElementById("media").currentTime>.35&&document.getElementById("media").getVideoPlaybackQuality().totalVideoFrames>0');
 const decoded=await evaluate('(()=>{const v=document.getElementById("media");return {video:v.getVideoPlaybackQuality().totalVideoFrames,audio:v.webkitAudioDecodedByteCount,time:v.currentTime,width:v.videoWidth,height:v.videoHeight}})()');report.decoded=decoded;
 assert('真实视频帧已解码',decoded.video>0,decoded);assert('真实伴音已解码',decoded.audio>0);
 await click('pause');const paused=(await state()).position;await delay(250);assert('暂停停止真实媒体时钟',Math.abs((await state()).position-paused)<.12);
 await seek(2);assert('进度条改变实际媒体位置',Math.abs((await state()).position-2)<.25);
 await cdp('Page.reload');await until('!!window.playbackProbe');assert('刷新不保存/自动重载媒体URL',!(await state()).sourceSelected);
 await file();assert('重选同一文件恢复实际进度',Math.abs((await state()).position-2)<.25);assert('恢复后保持暂停',await evaluate('document.getElementById("media").paused'));
 await loadUrl(report.url+'media');await until('window.playbackProbe.snapshot.phase==="ready"');assert('不同来源不串进度',(await state()).position<.1);
 await seek(1.5);await click('play');await until('window.playbackProbe.snapshot.phase==="playing"');
 await cdp('Network.emulateNetworkConditions',{offline:true,latency:0,downloadThroughput:0,uploadThroughput:0});await until('window.playbackProbe.snapshot.phase==="offline"');
 const offline=(await state()).position;assert('真实离线事件暂停并保留进度',await evaluate('document.getElementById("media").paused')&&offline>1.4);
 const count=requests.length;await click('retry');await delay(100);assert('离线重试不请求或无界循环',requests.length===count&&(await state()).phase==='offline');
 await click('pause');await cdp('Network.emulateNetworkConditions',{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});await until('window.playbackProbe.snapshot.phase==="paused"');
 assert('网络恢复不自动发声',await evaluate('document.getElementById("media").paused')&&(await state()).canRetry);
 // Discard the browser's decoded-media memory cache before injecting an HTTP failure.
 // Already buffered video is allowed to work offline; it must not be counted as a new request.
 flaky=true;await close();await launch();await page();await loadUrl(report.url+'media');await until('window.playbackProbe.snapshot.phase==="error"');assert('真实HTTP失败展示可重试状态',(await state()).canRetry);assert('首次加载失败仍保留上次真实进度',Math.abs((await state()).position-offline)<.25);
 flaky=false;await click('retry');await until('window.playbackProbe.snapshot.phase==="ready"');assert('加载失败再重试保留真实进度',Math.abs((await state()).position-offline)<.25);assert('重试尊重此前暂停意图',await evaluate('document.getElementById("media").paused'));
 await click('play');await until('window.playbackProbe.snapshot.phase==="playing"');await cdp('Network.emulateNetworkConditions',{offline:true,latency:0,downloadThroughput:0,uploadThroughput:0});await until('window.playbackProbe.snapshot.phase==="offline"');
 await cdp('Network.emulateNetworkConditions',{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});await until('window.playbackProbe.snapshot.phase==="paused"');await click('retry');await until('window.playbackProbe.snapshot.phase==="playing"');assert('显式重试可恢复此前播放意图',!(await evaluate('document.getElementById("media").paused')));
 await click('pause');await seek(3);await close();await launch();await page();await loadUrl(report.url+'media');await until('window.playbackProbe.snapshot.phase==="ready"');assert('真实浏览器关闭重开后重选恢复进度',Math.abs((await state()).position-3)<.25);assert('浏览器重开不会自动播放',await evaluate('document.getElementById("media").paused'));
 await loadUrl(report.url+'corrupt');await until('window.playbackProbe.snapshot.phase==="error"');assert('真实坏媒体错误可重新选素材',(await state()).canRetry);await file();assert('加载失败后本地素材仍可用',(await state()).phase==='ready');
 await click('play');await until('window.playbackProbe.snapshot.phase==="playing"');const localTime=(await state()).position;await cdp('Network.emulateNetworkConditions',{offline:true,latency:0,downloadThroughput:0,uploadThroughput:0});await delay(300);assert('本地文件离线仍可真实播放',(await state()).phase==='playing'&&(await state()).position>localTime);
 await cdp('Network.emulateNetworkConditions',{offline:false,latency:0,downloadThroughput:-1,uploadThroughput:-1});await click('pause');
 await seek((await state()).duration-.2);await click('play');await until('window.playbackProbe.snapshot.phase==="ended"');assert('结束来自真实媒体ended事件',await evaluate('document.getElementById("media").ended'));await click('play');await until('window.playbackProbe.snapshot.phase==="playing"');assert('播放结束后可重播',await evaluate('document.getElementById("media").currentTime<1'));await click('pause');
 const storage=await evaluate('JSON.parse(localStorage.getItem("vrbili-real-media-progress-v1"))');assert('持久化不含URL/Blob/文件名',storage.entries.length<=20&&storage.entries.every(x=>/^[a-f0-9]{64}$/.test(x.id))&&!JSON.stringify(storage).includes(report.url)&&!JSON.stringify(storage).includes('blob:')&&!JSON.stringify(storage).includes('fixture.'));
 await evaluate(`(async()=>{const {PlaybackSession}=await import('/session.mjs');window.timeoutVideo=document.createElement('video');document.body.append(timeoutVideo);window.timeoutSession=new PlaybackSession(timeoutVideo,{loadTimeoutMs:200});await timeoutSession.selectUrl(${JSON.stringify(report.url+'slow')});timeoutSession.pause()})()`);
 await until('window.timeoutSession.snapshot.phase==="error"');assert('加载期间暂停仍执行有界超时',await evaluate('timeoutSession.snapshot.reason.includes("超时")'));
 await delay(100);assert('加载超时终止真实HTTP请求',slowAborted>0&&await evaluate('!timeoutVideo.getAttribute("src")'));
 await evaluate('timeoutSession.dispose();timeoutVideo.remove()');
 await storageFixture(JSON.stringify({version:1,entries:Array.from({length:25},(_,i)=>({id:i.toString(16).padStart(64,'0'),position:1,updated:i}))}));await file();await click('play');await until('window.playbackProbe.snapshot.phase==="playing"');await click('pause');assert('大量历史素材进度被限制到20项',await evaluate('JSON.parse(localStorage.getItem("vrbili-real-media-progress-v1")).entries.length<=20'));
 // Inject on the new document: the old page's real pagehide handler saves a valid checkpoint.
 for(const raw of ['{bad','null',JSON.stringify({version:1,entries:[{id:'a'.repeat(64),position:-1,updated:1}]})]){await storageFixture(raw);await file();const restored=await state();assert('损坏存储仍可读取实际媒体 '+raw.slice(0,12),restored.phase==='ready'&&restored.position===0,{position:restored.position});}
 const blocked=await cdp('Page.addScriptToEvaluateOnNewDocument',{source:'Storage.prototype.getItem=function(){throw new DOMException("test denied","SecurityError")};Storage.prototype.setItem=function(){throw new DOMException("test quota","QuotaExceededError")};'});await cdp('Page.reload');await until('!!window.playbackProbe');await file();await click('play');await until('window.playbackProbe.snapshot.phase==="playing"');assert('存储不可用不阻断实际播放',!(await state()).storageAvailable);await click('pause');await cdp('Page.removeScriptToEvaluateOnNewDocument',{identifier:blocked.identifier});
 await evaluate(`document.getElementById('url').value='https://user:password@example.invalid/video.mp4'`);await click('load-url');await delay(80);assert('拒绝含账号密码的媒体URL',await evaluate('document.getElementById("input-error").textContent.includes("账号密码")'));
 const latest=report.url+'media?latest=1';await evaluate(`(()=>{const input=document.getElementById('url'),form=document.getElementById('url-form');input.value=${JSON.stringify(report.url+'corrupt')};form.requestSubmit();input.value=${JSON.stringify(latest)};form.requestSubmit()})()`);await until('window.playbackProbe.snapshot.phase==="ready"');assert('快速换素材以最后选择为准',await evaluate(`document.getElementById('media').getAttribute('src')===${JSON.stringify(latest)}`)&&(await state()).position===0);
 const shot=await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true});fs.writeFileSync(path.join(output,'playback-probe.png'),Buffer.from(shot.data,'base64'));
 assert('没有未处理的浏览器脚本异常',errors.length===0,{count:errors.length});
 report.sourceHashes=Object.fromEntries(['playback-probe/session.mjs','playback-probe/app.mjs','playback-probe/index.html'].map(n=>[n,createHash('sha256').update(fs.readFileSync(path.join(root,n))).digest('hex')]));
}catch(e){report.fatal=String(e.stack||e);try{report.finalState=await state()}catch{}process.exitCode=1;}finally{
 await close();if(server)await new Promise(r=>server.close(r));
 report.profileRemoved=false;
 if(!alive()){try{const resolved=path.resolve(profile),prefix=path.join(path.resolve(os.tmpdir()),'vrbili-playback-');if(!resolved.startsWith(prefix))throw Error('Unsafe cleanup path');fs.rmSync(resolved,{recursive:true,force:true});report.profileRemoved=true}catch{report.profileRemoved=false}}
 report.requests=requests;report.passed=checks.filter(x=>x.passed).length;report.failed=checks.filter(x=>!x.passed).length;report.timestamp=new Date().toISOString();fs.writeFileSync(path.join(output,'playback-results.json'),JSON.stringify(report,null,2));
 console.log(JSON.stringify({output,passed:report.passed,failed:report.failed,fatal:report.fatal,profileRemoved:report.profileRemoved,fixture:report.fixture}));
}
