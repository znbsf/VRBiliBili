import {PlaybackSession} from './session.mjs';
const $ = id => document.getElementById(id);
const session = new PlaybackSession($('media'));
const names = {empty:'请选择媒体',loading:'正在读取真实媒体…',ready:'媒体已就绪，点击播放',playing:'正在播放',paused:'已暂停',buffering:'正在缓冲…',offline:'网络已断开',error:'媒体加载失败',ended:'播放结束'};
const time = seconds => `${Math.floor(seconds/60)}:${String(Math.floor(seconds%60)).padStart(2,'0')}`;
session.subscribe(state => {
  $('status').textContent = state.reason || names[state.phase];
  $('status').dataset.phase = state.phase;
  $('play').disabled = !state.sourceSelected || ['loading','offline','playing'].includes(state.phase);
  $('pause').disabled = !state.sourceSelected || ['empty','ready','ended'].includes(state.phase);
  $('retry').disabled = !state.canRetry;
  $('seek').disabled = !state.duration || state.canRetry || state.phase === 'loading';
  $('seek').max = state.duration; $('seek').value = state.position;
  $('time').textContent = `${time(state.position)} / ${time(state.duration)}`;
  $('storage-note').textContent = state.storageAvailable ? '进度保存在当前浏览器；重新打开需重新选择素材。' : '本地存储不可用，本次仍可播放；跨刷新进度无法保存。';
});
async function load(fn) { $('input-error').textContent = ''; try { await fn(); } catch (e) { $('input-error').textContent = e.message; } }
$('file').addEventListener('change', () => { const file = $('file').files[0]; if (file) void load(() => session.selectFile(file)); });
$('url-form').addEventListener('submit', event => { event.preventDefault(); void load(() => session.selectUrl($('url').value)); });
$('play').addEventListener('click', () => void session.play());
$('pause').addEventListener('click', () => session.pause());
$('retry').addEventListener('click', () => session.retry());
$('seek').addEventListener('input', event => session.seek(Number(event.target.value)));
// Narrow read-only diagnostics for the isolated automated feasibility checks.
window.playbackProbe = Object.freeze({get snapshot() { return session.snapshot; }});
