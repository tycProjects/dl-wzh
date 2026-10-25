const Bridge = {
  available() { return typeof Android !== 'undefined'; },
  call(name, ...args) {
    if (!this.available() || typeof Android[name] !== 'function') return null;
    try { return Android[name](...args); } catch (e) { console.warn(e); return null; }
  },
  startProxy() { return this.call('startProxy'); },
  stopProxy() { return this.call('stopProxy'); },
  setFeature(k, v) { return this.call('setFeature', k, v); },
  apply() { return this.call('apply'); },
  reset() { return this.call('reset'); },
  scanGame() { return this.call('scanGame'); },
  isRoot() { return this.call('isRoot') === true || this.call('isRoot') === 'true'; },
  hasOverlay() { return this.call('hasOverlay') === true || this.call('hasOverlay') === 'true'; },
  hasStorage() { return this.call('hasStorage') === true || this.call('hasStorage') === 'true'; },
  toast(m) { return this.call('toast', m); }
};
