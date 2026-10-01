// DOM event/state checks only. This does not render pixels, run a browser, or use a headset.
const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const path=require('node:path');
const {parseHTML}=require('./tools/prototype/node_modules/linkedom');
const source=fs.readFileSync(path.join(__dirname,'piliplus-v1-prototype.html'),'utf8');
const {window}=parseHTML('<html><body>'+source+'</body></html>');
// linkedom does not implement native select.value assignment; supply ordinary selection semantics.
Object.defineProperty(window.HTMLSelectElement.prototype,'value',{
 get(){const o=this.querySelector('option[selected]')||this.querySelector('option');return o?o.getAttribute('value')||o.textContent:''},
 set(value){for(const o of this.querySelectorAll('option'))o.toggleAttribute('selected',(o.getAttribute('value')||o.textContent)===String(value))}
});
let snapshot,tick;
window.openai={setWidgetState:s=>{snapshot=JSON.parse(JSON.stringify(s));return Promise.resolve()}};
const context={window,document:window.document,console,matchMedia:()=>({matches:false}),setInterval:fn=>{tick=fn;return 1},setTimeout:()=>1,clearTimeout:()=>{}};
context.globalThis=context;
vm.createContext(context);
new vm.Script([...source.matchAll(/<script>([\s\S]*?)<\/script>/g)][0][1]).runInContext(context);
const doc=window.document,results=[];
const q=s=>{const el=doc.querySelector(s);assert.ok(el,'Missing DOM element '+s);return el};
const click=s=>{q(s).dispatchEvent(new window.Event('click',{bubbles:true}));uniqueIds()};
const act=(a,attrs='')=>click('[data-action="'+a+'"]'+attrs);
const change=(s,value)=>{const el=q(s);if(el.type==='checkbox')el.checked=value;else el.value=value;el.dispatchEvent(new window.Event('input',{bubbles:true}));el.dispatchEvent(new window.Event('change',{bubbles:true}));uniqueIds()};
const saved=()=>snapshot.privateContent;
const test=(name,fn)=>{fn();results.push(name)};
function uniqueIds(){const ids=[...doc.querySelectorAll('[id]')].map(e=>e.id);assert.equal(ids.length,new Set(ids).size,'Duplicate DOM ids')}
test('Home renders six sample cards',()=>assert.equal(doc.querySelectorAll('.pv-video-card').length,6));
test('Search returns matching videos',()=>{q('#pv-search').value='旅行';act('search');assert.equal(doc.querySelectorAll('.pv-video-card').length,2)});
test('Detail and later list update together',()=>{act('detail','[data-id="valley"]');act('later');assert.ok(saved().later.includes('valley'));assert.match(q('.pv-detail-info').textContent,/已加入稍后再看/)});
test('Selected episode opens with correct duration',()=>{act('select-detail','[data-part="2"]');act('play-detail');assert.equal(q('#pv-seek').getAttribute('max'),'636')});
test('Playback clock advances and seek persists',()=>{tick();tick();assert.match(q('#pv-time').textContent,/0:02/);change('#pv-seek','42');assert.equal(saved().progress['valley:2'],42)});
test('Previous and next retain per-episode progress',()=>{act('previous');assert.equal(q('#pv-seek').getAttribute('max'),'942');act('next');assert.equal(q('#pv-seek').value,'42')});
test('Desk layout opens two auxiliary components',()=>{act('layout','[data-layout="desk"]');assert.equal(q('.pv-window[data-panel="queue"]').hidden,false);assert.equal(q('.pv-window[data-panel="comments"]').hidden,false)});
test('Auxiliary opacity does not fade the video',()=>{act('inspector','[data-tool="settings"]');change('#pv-opacity','35');assert.equal(q('.pv-window[data-panel="comments"]').style.getPropertyValue('--panel-alpha'),'35%');assert.equal(q('.pv-window[data-panel="main"]').style.getPropertyValue('--panel-alpha'),'100%')});
test('Locked auxiliary window stays put during group move',()=>{act('inspector','[data-tool="layout"]');change('#pv-target','queue');change('#pv-lock',true);change('#pv-target','main');const before=saved().layouts.desk.panels.queue.x;act('nudge','[data-dx="-3"]');assert.equal(saved().layouts.desk.panels.queue.x,before);assert.equal(saved().layouts.desk.panels.comments.x,76)});
test('Unfollowing component stays put during group move',()=>{change('#pv-target','comments');change('#pv-follow',false);change('#pv-target','main');const before=saved().layouts.desk.panels.comments.x;act('nudge','[data-dx="-3"]');assert.equal(saved().layouts.desk.panels.comments.x,before)});
test('Locked main cannot be nudged',()=>{change('#pv-lock',true);const before=saved().layouts.desk.panels.main.x;act('nudge','[data-dx="-3"]');assert.equal(saved().layouts.desk.panels.main.x,before)});
test('Recall preserves lock and opacity',()=>{act('recall');assert.equal(saved().layouts.desk.panels.main.locked,true);assert.equal(saved().opacity,35)});
test('Layouts retain independent settings',()=>{act('layout','[data-layout="focus"]');act('layout','[data-layout="desk"]');assert.equal(saved().layouts.desk.panels.queue.locked,true);assert.equal(saved().layouts.focus.panels.main.locked,false)});
test('Error retry preserves progress',()=>{change('#pv-review-state','error');assert.match(q('#pv-play-status').textContent,/暂时无法播放/);act('retry');assert.equal(q('#pv-play-status').hidden,true);assert.equal(q('#pv-seek').value,'42')});
test('Flat effect is contained inside the film',()=>{act('inspector','[data-tool="danmaku"]');change('#pv-danmaku','flat');act('preview');assert.equal(q('#pv-effects').parentElement.id,'pv-film')});
test('River uses a separate stage lane and can stop',()=>{change('#pv-danmaku','river');act('preview');assert.equal(q('#pv-effects').parentElement.id,'pv-stage');assert.equal(doc.querySelectorAll('.pv-effect-tag').length,1);act('stop-effects');assert.equal(doc.querySelectorAll('.pv-effect-tag').length,0)});
test('Expired-login simulation can recover',()=>{change('#pv-review-state','expired');assert.equal(q('#pv-auth').hidden,false);act('login-confirm');assert.equal(saved().signed,true);assert.equal(q('#pv-auth').hidden,true)});
test('End state offers replay without automatic next',()=>{change('#pv-review-state','ended');assert.match(q('#pv-play-status').textContent,/这一段看完了/);act('replay');assert.equal(q('#pv-seek').value,'0')});
test('Clock reaches end and stops',()=>{change('#pv-seek','635');tick();assert.match(q('#pv-play-status').textContent,/这一段看完了/);assert.equal(q('#pv-review-state').value,'ended')});
test('History opens exact episode and completed items restart',()=>{act('nav','[data-to="library"]');act('library-tab','[data-tab="history"]');assert.ok(doc.querySelectorAll('.pv-video-card').length>=2);act('resume-card','[data-id="valley"][data-part="2"]');assert.equal(q('#pv-seek').getAttribute('max'),'636');assert.equal(q('#pv-seek').value,'0')});
test('Empty search retains a recovery action',()=>{act('nav','[data-to="home"]');q('#pv-search').value='不存在的关键词';act('search');assert.match(q('.pv-empty').textContent,/没有找到/);act('clear-search');assert.equal(doc.querySelectorAll('.pv-video-card').length,6)});
test('Host snapshot restores meaningful state',()=>{const copy=JSON.parse(JSON.stringify(snapshot));copy.privateContent.view='watch';copy.privateContent.layout='desk';const event=new window.Event('openai:set_globals');event.detail={globals:{widgetState:copy}};window.dispatchEvent(event);assert.equal(q('.pv-window[data-panel="comments"]').hidden,false);assert.match(q('[data-action="play"]').textContent,/播放/)});
test('Return first closes settings then returns to history',()=>{act('nav','[data-to="library"]');act('library-tab','[data-tab="history"]');act('resume-card','[data-id="valley"][data-part="2"]');act('inspector','[data-tool="settings"]');act('exit-watch');assert.equal(saved().view,'watch');assert.equal(q('#pv-inspector').hidden,true);act('exit-watch');assert.equal(saved().view,'library');assert.equal(saved().library,'history')});
test('Escape follows history origin and saves exact progress',()=>{act('resume-card','[data-id="valley"][data-part="2"]');change('#pv-seek','123');const e=new window.Event('keydown',{bubbles:true});e.key='Escape';q('#pili-v1-app').dispatchEvent(e);assert.equal(saved().view,'library');assert.equal(saved().progress['valley:2'],123)});
test('Playback from detail returns to same detail episode',()=>{act('nav','[data-to="home"]');act('detail','[data-id="valley"]');act('select-detail','[data-part="1"]');act('play-detail');act('exit-watch');assert.equal(saved().view,'detail');assert.equal(saved().selected,'valley');assert.equal(saved().part,1)});
test('Opacity is restored independently for each layout',()=>{act('play-detail');act('layout','[data-layout="desk"]');act('inspector','[data-tool="settings"]');change('#pv-opacity','35');act('layout','[data-layout="focus"]');change('#pv-opacity','100');act('layout','[data-layout="desk"]');assert.equal(saved().opacity,35);act('layout','[data-layout="focus"]');assert.equal(saved().opacity,100)});
test('Recall leaves hidden component placement intact',()=>{act('layout','[data-layout="desk"]');act('inspector','[data-tool="layout"]');change('#pv-target','comments');change('#pv-follow',false);act('nudge','[data-dx="-3"]');const x=saved().layouts.desk.panels.comments.x;act('toggle-panel','[data-panel="comments"]');act('recall');assert.equal(saved().layouts.desk.panels.comments.x,x);assert.equal(saved().layouts.desk.visible.comments,false)});
test('Every completed episode offers return to detail',()=>{act('episode','[data-part="0"]');change('#pv-review-state','ended');assert.ok(q('#pv-play-status').querySelector('[data-action="watch-detail"]'));assert.ok(q('#pv-play-status').querySelector('[data-action="next"]'))});
test('Old global-opacity snapshots migrate all presets',()=>{const copy=JSON.parse(JSON.stringify(snapshot));copy.privateContent.opacity=67;for(const preset of Object.values(copy.privateContent.layouts))delete preset.opacity;delete copy.privateContent.returnView;const event=new window.Event('openai:set_globals');event.detail={globals:{widgetState:copy}};window.dispatchEvent(event);act('layout','[data-layout="social"]');assert.equal(saved().opacity,67);act('layout','[data-layout="focus"]');assert.equal(saved().opacity,67)});
test('Partial damaged snapshot cannot blank the watch view',()=>{const raw={privateContent:{version:1,view:'watch',part:.5,progress:null,layouts:{focus:{panels:{main:{distance:'near',size:null}}}}}};const event=new window.Event('openai:set_globals');event.detail={globals:{widgetState:raw}};window.dispatchEvent(event);assert.equal(q('#pv-seek').getAttribute('max'),'768');act('layout','[data-layout="focus"]');assert.equal(saved().part,0);assert.equal(saved().layouts.focus.panels.main.distance,2.5);assert.ok(saved().layouts.focus.panels.queue);assert.equal(raw.privateContent.layouts.focus.panels.main.distance,'near')});
test('Nonfinite progress and malformed collections normalize safely',()=>{const raw={privateContent:{version:1,view:'home',progress:{'valley:0':Infinity,'valley:1':-10,'valley:2':99999},history:['valley:1','valley:1','missing:0'],later:['space','space'],query:{invalid:true},speed:'fast'}};const event=new window.Event('openai:set_globals');event.detail={globals:{widgetState:raw}};window.dispatchEvent(event);act('nav','[data-to="library"]');assert.equal(saved().progress['valley:0'],undefined);assert.equal(saved().progress['valley:1'],0);assert.equal(saved().progress['valley:2'],636);assert.equal(saved().history.length,1);assert.equal(saved().later.length,1);assert.equal(saved().query,'');assert.equal(saved().speed,1)});
console.log(JSON.stringify({kind:'DOM-only event and state checks',passed:results.length,results,notValidated:['CSS layout','actual browser inputs','audio','XR rendering','Quest device']},null,2));
