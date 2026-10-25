const App = {
  features: {},
  init() { this.refreshPerms(); },
  refreshPerms() {
    this.setRow('sRoot', Bridge.isRoot());
    this.setRow('sOverlay', Bridge.hasOverlay());
    this.setRow('sStorage', Bridge.hasStorage());
  },
  setRow(id, ok) {
    const el = document.getElementById(id);
    if (!el) return;
    el.className = 's-row ' + (ok ? 'ok' : 'fail');
    el.textContent = (ok ? '✓ ' : '✗ ') + el.textContent.replace(/^[●✓✗]\s*/, '');
  },
  start() {
    Bridge.startProxy();
    setTimeout(() => this.showMenu(), 700);
  },
  showMenu() {
    document.getElementById('screenStart').classList.remove('active');
    document.getElementById('screenMenu').classList.add('active');
  },
  toggle(el) {
    const key = el.dataset.key;
    if (!key) return;
    this.features[key] = !this.features[key];
    el.classList.toggle('on', this.features[key]);
    // Immediate auto-replace on toggle
    Bridge.setFeature(key, this.features[key]);
    this.setMsg(this.features[key]
      ? 'Đang dò & thay file: ' + key + '...'
      : 'Đã tắt & restore: ' + key);
  },
  scan() {
    const r = Bridge.scanGame();
    this.setMsg(r || 'Scan xong');
  },
  apply() {
    Bridge.apply();
    this.setMsg('Đang áp dụng tất cả...');
  },
  reset() {
    Object.keys(this.features).forEach(k => this.features[k] = false);
    document.querySelectorAll('.tog').forEach(t => t.classList.remove('on'));
    Bridge.reset();
    this.setMsg('Reset + restore files');
  },
  stop() {
    Bridge.stopProxy();
    document.getElementById('screenMenu').classList.remove('active');
    document.getElementById('screenStart').classList.add('active');
    this.refreshPerms();
  },
  setMsg(t) {
    const el = document.getElementById('msg');
    if (el) el.textContent = t;
  }
};
document.addEventListener('DOMContentLoaded', () => App.init());
