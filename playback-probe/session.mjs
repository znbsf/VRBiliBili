// Disposable desktop feasibility probe. This adapter is not a PiliPlus/XR player.
const STORAGE_KEY = 'vrbili-real-media-progress-v1';
const finite = value => typeof value === 'number' && Number.isFinite(value) && value >= 0;
const sha = async bytes => [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))].map(x => x.toString(16).padStart(2, '0')).join('');
export class PlaybackSession {
  #video; #storage; #records = new Map(); #listeners = []; #subscribers = new Set();
  #source; #id; #kind; #phase = 'empty'; #reason = ''; #position = 0; #duration = 0;
  #restoring = false; #intent = false; #needsRetry = false; #sequence = 0;
  #timer; #timeout; #lastSave = 0; #storageAvailable = true; #disposed = false;
  constructor(video, {storage, loadTimeoutMs = 15000} = {}) {
    this.#video = video; this.#timeout = loadTimeoutMs;
    try { this.#storage = storage ?? window.localStorage; this.#read(); } catch { this.#storageAvailable = false; }
    const on = (target, event, handler) => { target.addEventListener(event, handler); this.#listeners.push(() => target.removeEventListener(event, handler)); };
    on(video, 'loadedmetadata', () => {
      if (!this.#active() || this.#needsRetry) return;
      clearTimeout(this.#timer); this.#duration = Number.isFinite(video.duration) ? video.duration : 0;
      this.#position = Math.min(this.#position, this.#duration || this.#position);
      if (this.#position > 0 && this.#duration) video.currentTime = this.#position;
      this.#restoring = false; this.#set('ready');
      if (this.#intent) void this.play();
    });
    on(video, 'playing', () => { if (this.#active() && !this.#restoring && !this.#needsRetry) this.#set('playing'); });
    on(video, 'waiting', () => { if (this.#active() && this.#intent && !this.#needsRetry) this.#set('buffering'); });
    on(video, 'pause', () => {
      if (!this.#active() || this.#restoring || ['error','offline','ended'].includes(this.#phase)) return;
      this.#capture(); this.#save(true); this.#set('paused');
    });
    on(video, 'timeupdate', () => {
      if (!this.#active() || this.#restoring || this.#needsRetry) return;
      this.#capture(); this.#save(); this.#emit();
    });
    on(video, 'seeked', () => { if (this.#active() && !this.#restoring && !this.#needsRetry) { this.#capture(); this.#save(true); this.#emit(); } });
    on(video, 'ended', () => { if (!this.#active() || this.#restoring || !video.ended) return; this.#capture(); this.#intent = false; this.#save(true); this.#set('ended'); });
    on(video, 'error', () => {
      if (!this.#active() || !video.error) return;
      const code = video.error?.code;
      const reason = {1:'加载已中断',2:'网络读取失败',3:'媒体解码失败',4:'素材不可读取或编码不受支持'}[code] || '媒体加载失败';
      this.#fail(this.#kind === 'url' && !navigator.onLine ? 'offline' : 'error', reason);
    });
    on(window, 'offline', () => { if (this.#kind === 'url' && this.#source) this.#fail('offline', '网络已断开，进度已保留'); });
    on(window, 'online', () => { if (this.#phase === 'offline') this.#set('paused', '网络已恢复，点击重试继续'); });
    on(window, 'pagehide', () => { this.#capture(); this.#save(true); });
  }
  get snapshot() { return {phase:this.#phase, reason:this.#reason, position:this.#position, duration:this.#duration, sourceKind:this.#kind ?? null, sourceSelected:!!this.#source, canRetry:!!this.#source && this.#needsRetry, storageAvailable:this.#storageAvailable, wantsPlayback:this.#intent}; }
  subscribe(fn) { this.#subscribers.add(fn); fn(this.snapshot); return () => this.#subscribers.delete(fn); }
  #emit() { for (const fn of this.#subscribers) fn(this.snapshot); }
  #set(phase, reason = '') { this.#phase = phase; this.#reason = reason; this.#emit(); }
  #active() { return !this.#disposed && !!this.#source && this.#video.getAttribute('src') === this.#source; }
  #capture() { if (!this.#restoring && !this.#needsRetry && finite(this.#video.currentTime)) this.#position = this.#video.currentTime; }
  #read() {
    const raw = this.#storage.getItem(STORAGE_KEY);
    if (!raw || raw.length > 20000) return;
    let data; try { data = JSON.parse(raw); } catch { return; }
    if (data?.version !== 1 || !Array.isArray(data.entries)) return;
    for (const row of data.entries.slice(-20)) {
      if (row && /^[a-f0-9]{64}$/.test(row.id) && finite(row.position) && row.position <= 86400 && finite(row.updated)) this.#records.set(row.id, {id:row.id, position:row.position, updated:row.updated});
    }
  }
  #save(force = false) {
    if (!this.#id || !finite(this.#position) || (!force && Date.now() - this.#lastSave < 1000)) return;
    this.#lastSave = Date.now(); this.#records.delete(this.#id);
    this.#records.set(this.#id, {id:this.#id, position:Math.min(this.#position, 86400), updated:Date.now()});
    while (this.#records.size > 20) this.#records.delete(this.#records.keys().next().value);
    try { this.#storage.setItem(STORAGE_KEY, JSON.stringify({version:1, entries:[...this.#records.values()]})); }
    catch { this.#storageAvailable = false; }
  }
  async selectFile(file) {
    if (!(file instanceof Blob) || file.size === 0 || file.size > 2 * 1024 ** 3) throw new TypeError('请选择非空、2GB 以内的本地媒体');
    const sequence = ++this.#sequence;
    // Bounded sample fingerprint: avoids loading a large video entirely into memory.
    const first = new Uint8Array(await file.slice(0, 65536).arrayBuffer());
    const last = new Uint8Array(await file.slice(Math.max(0, file.size - 65536)).arrayBuffer());
    const size = new TextEncoder().encode('file:' + file.size + ':');
    const bytes = new Uint8Array(size.length + first.length + last.length); bytes.set(size); bytes.set(first, size.length); bytes.set(last, size.length + first.length);
    const id = await sha(bytes); if (sequence !== this.#sequence || this.#disposed) return;
    this.#select(URL.createObjectURL(file), id, 'file');
  }
  async selectUrl(input) {
    const url = new URL(input);
    if (!['http:','https:'].includes(url.protocol) || url.username || url.password) throw new TypeError('仅支持不含账号密码的 HTTP(S) 直接媒体地址');
    const sequence = ++this.#sequence; const id = await sha(new TextEncoder().encode('url:' + url.href));
    if (sequence !== this.#sequence || this.#disposed) return;
    this.#select(url.href, id, 'url');
  }
  #select(source, id, kind) {
    this.#capture(); this.#save(true); this.#intent = false;
    this.#release(); this.#source = source; this.#id = id; this.#kind = kind;
    this.#position = this.#records.get(id)?.position ?? 0; this.#duration = 0;
    this.#load();
  }
  #load() {
    this.#restoring = true; this.#needsRetry = false; this.#set('loading');
    clearTimeout(this.#timer); this.#video.src = this.#source; this.#video.load();
    this.#timer = setTimeout(() => { if (this.#phase === 'loading') this.#fail('error', '加载超时，请检查素材或网络后重试'); }, this.#timeout);
  }
  #fail(phase, reason) {
    clearTimeout(this.#timer); this.#capture(); this.#save(true); this.#needsRetry = true;
    this.#set(phase, reason); this.#video.pause();
    // Abort the failed request/decoder while retaining the source descriptor and checkpoint.
    this.#video.removeAttribute('src'); this.#video.load();
  }
  async play() {
    if (!this.#source || this.#disposed) return;
    this.#intent = true;
    if (this.#needsRetry) { this.retry(); return; }
    if (this.#restoring) return;
    if (this.#video.ended) this.#video.currentTime = 0;
    const sequence = this.#sequence;
    try { await this.#video.play(); }
    catch (error) {
      if (!this.#active() || sequence !== this.#sequence) return;
      if (error.name === 'NotAllowedError') { this.#intent = false; this.#set('paused', '浏览器需要再次点击播放'); }
      else if (error.name !== 'AbortError') this.#fail('error', '暂时无法开始播放，请重试');
    }
  }
  pause() { this.#intent = false; this.#capture(); this.#save(true); this.#video.pause(); if (!this.#restoring && !['empty','error','offline','ended'].includes(this.#phase)) this.#set('paused'); }
  seek(position) {
    if (!finite(position) || !this.#duration || this.#restoring || this.#needsRetry) return;
    this.#position = Math.min(position, this.#duration); this.#video.currentTime = this.#position; this.#save(true); this.#emit();
  }
  retry() {
    if (!this.#source || this.#disposed) return;
    if (this.#kind === 'url' && !navigator.onLine) { this.#set('offline', '网络仍未恢复，进度已保留'); return; }
    this.#capture(); this.#save(true); this.#load();
  }
  #release() {
    clearTimeout(this.#timer); const previous = this.#source; this.#source = undefined;
    this.#video.pause(); this.#video.removeAttribute('src'); this.#video.load();
    if (previous?.startsWith('blob:')) URL.revokeObjectURL(previous);
  }
  dispose() {
    this.#capture(); this.#save(true); this.#disposed = true; this.#sequence++;
    for (const remove of this.#listeners) remove(); this.#release(); this.#subscribers.clear();
  }
}
