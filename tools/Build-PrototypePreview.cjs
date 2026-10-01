// Build an offline, self-contained review page. No account or network calls.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const source = fs.readFileSync(process.argv[2] || path.join(root, 'piliplus-v1-prototype.html'), 'utf8');
const output = process.argv[3] || path.join(root, 'prototype-preview.html');
const icons = {
  play:'<path d="m6 3 12 9-12 9Z"/>', pause:'<path d="M7 4v16M17 4v16"/>',
  search:'<circle cx="10" cy="10" r="7"/><path d="m15 15 6 6"/>',
  'arrow-left':'<path d="m10 5-7 7 7 7M3 12h18"/>', x:'<path d="m5 5 14 14M19 5 5 19"/>',
  'skip-back':'<path d="M4 4v16m15-16L7 12l12 8Z"/>', 'skip-forward':'<path d="M20 4v16M5 4l12 8-12 8Z"/>',
  bookmark:'<path d="M6 3h12v18l-6-4-6 4Z"/>', 'bookmark-check':'<path d="M6 3h12v18l-6-4-6 4ZM9 9l2 2 4-4"/>',
  history:'<path d="M3 12a9 9 0 1 0 3-7M3 3v6h6m3-3v6l4 2"/>',
  'grip-horizontal':'<path d="M6 9h.01M12 9h.01M18 9h.01M6 15h.01M12 15h.01M18 15h.01" stroke-width="4"/>',
  'list-video':'<path d="M3 6h9M3 12h9M3 18h9m4-10 5 4-5 4Z"/>',
  'message-circle':'<path d="M21 11a9 9 0 0 1-9 9H3l2-5a9 9 0 1 1 16-4Z"/>',
  'messages-square':'<path d="M3 3h14v11H7l-4 4ZM8 19h10l3 3V8"/>',
  move:'<path d="M12 2v20M2 12h20M8 6l4-4 4 4M8 18l4 4 4-4M6 8l-4 4 4 4M18 8l4 4-4 4"/>',
  'locate-fixed':'<circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2"/><path d="M12 1v4m0 14v4M1 12h4m14 0h4"/>',
  'sliders-horizontal':'<path d="M3 6h18M3 12h18M3 18h18M7 3v6m10 0v6M9 15v6"/>',
  'panels-top-left':'<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 9v12"/>'
};
const bootstrap = `(() => {
  const key = 'vrbilibili-v1:' + location.pathname;
  let state = null;
  try { const raw = localStorage.getItem(key); if(raw && raw.length < 16384) state = JSON.parse(raw); } catch {}
  window.openai = { widgetState: state, setWidgetState(value) {
    const raw = JSON.stringify(value);
    if(raw.length > 16384) return Promise.reject(new Error('Review state exceeds 16 KiB'));
    this.widgetState = value;
    try { localStorage.setItem(key, raw); } catch {}
    return Promise.resolve();
  }};
  const paths = ${JSON.stringify(icons)};
  window.lucide = { createIcons() { document.querySelectorAll('i[data-lucide]').forEach(e => {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox','0 0 24 24'); svg.setAttribute('width','16'); svg.setAttribute('height','16');
    svg.setAttribute('fill','none'); svg.setAttribute('stroke','currentColor'); svg.setAttribute('stroke-width','1.8');
    svg.setAttribute('stroke-linecap','round'); svg.setAttribute('stroke-linejoin','round'); svg.setAttribute('aria-hidden','true');
    svg.innerHTML = paths[e.dataset.lucide] || ''; e.replaceWith(svg);
  }); }};
})();`;
fs.writeFileSync(output, `<!doctype html>\n<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="referrer" content="no-referrer"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'"><title>PiliPlus 空间版 V1 · 离线流程评审</title><style>body{margin:0;padding:12px;background:light-dark(#fafafd,#0c0e15);color-scheme:light dark}body>div{max-width:1280px;margin:auto}svg{flex-shrink:0}</style><script>${bootstrap}</script></head><body>\n${source}\n</body></html>\n`, 'utf8');
console.log(JSON.stringify({output, bytes: fs.statSync(output).size, networkDependencies:0}));
