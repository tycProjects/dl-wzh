/* Makes the Web Clipboard API work inside NOMAN GO the way it does in Chrome.
   Android WebView refuses navigator.clipboard.readText() because it has no permission prompt, so the page's
   Paste button fails. Reading goes through the native bridge, which needs a tap on the page and the site's
   permission (asked once, like Chrome). Writing needs a tap. Nothing here is specific to a website. */
(function () {
  if (window.__ngCb) return;
  window.__ngCb = 1;
  var B = window.NomanClip;
  var C = navigator.clipboard;
  if (!B || !C) return;
  var Q = {}, n = 0;
  var orig = {
    writeText: C.writeText ? C.writeText.bind(C) : null,
    write: C.write ? C.write.bind(C) : null
  };
  function denied(w) { return new DOMException((w || 'Read') + ' permission denied.', 'NotAllowedError'); }
  /* the app answers a read request: text, or null when it was refused */
  window.__ngCbDone = function (id, text) {
    var r = Q[id];
    if (!r) return;
    delete Q[id];
    if (text === null || text === undefined) r.j(denied('Read')); else r.s(String(text));
  };
  function readText() {
    return new Promise(function (s, j) {
      var id = 'c' + (++n);
      Q[id] = { s: s, j: j };
      try { B.read(id); } catch (e) { delete Q[id]; j(denied('Read')); return; }
      setTimeout(function () { if (Q[id]) { delete Q[id]; j(denied('Read')); } }, 60000);
    });
  }
  function writeText(t) {
    return new Promise(function (s, j) {
      var ok = false;
      try { ok = B.write(String(t)); } catch (e) { }
      if (ok) s();
      else if (orig.writeText) orig.writeText(t).then(s, j);
      else j(denied('Write'));
    });
  }
  function read() {
    return readText().then(function (t) {
      if (typeof ClipboardItem === 'undefined') throw denied('Read');
      return [new ClipboardItem({ 'text/plain': new Blob([t], { type: 'text/plain' }) })];
    });
  }
  function write(items) {
    var it = items && items[0];
    if (!it || !it.types || it.types.indexOf('text/plain') < 0) {
      return orig.write ? orig.write(items) : Promise.reject(denied('Write'));
    }
    return it.getType('text/plain').then(function (b) { return b.text(); }).then(writeText);
  }
  function def(o, k, v) {
    try { Object.defineProperty(o, k, { value: v, configurable: true, writable: true }); } catch (e) { try { o[k] = v; } catch (x) { } }
  }
  def(C, 'readText', readText);
  def(C, 'writeText', writeText);
  def(C, 'read', read);
  def(C, 'write', write);
  /* sites ask first whether they may use the clipboard */
  try {
    var P = navigator.permissions;
    if (P && P.query) {
      var oq = P.query.bind(P);
      def(P, 'query', function (d) {
        if (d && (d.name === 'clipboard-read' || d.name === 'clipboard-write')) {
          return Promise.resolve({
            state: d.name === 'clipboard-read' ? 'prompt' : 'granted', name: d.name, onchange: null,
            addEventListener: function () { }, removeEventListener: function () { }, dispatchEvent: function () { return false; }
          });
        }
        return oq(d);
      });
    }
  } catch (e) { }
})();
