/* NOMAN GO page translator (runs inside the page). Native code drives it:
   info() -> language hint + sample, take(n,chars) -> next texts, put(json,lang) -> apply, pending(), restore(). */
(function () {
  if (window.__ngTr) return;
  var SKIP = { SCRIPT: 1, STYLE: 1, NOSCRIPT: 1, TEXTAREA: 1, CODE: 1, PRE: 1, KBD: 1, SAMP: 1, SVG: 1,
    IFRAME: 1, OBJECT: 1, CANVAS: 1, TEMPLATE: 1, HEAD: 1 };
  var ATTRS = ['placeholder', 'title', 'alt', 'aria-label'];
  var MAX = 6000;
  var items = [];            // {k:0 text|1 attribute|2 title, n:node|element, a:attr, o:original, t:trimmed, lead, trail, st:0 new|1 sent|2 done, cur}
  var textIdx = new WeakMap();
  var attrIdx = new WeakMap();
  var cache = Object.create(null);
  var dirty = true, applying = false, obs = null, origLang = null, titleRec = null;

  function skipEl(e) {
    if (SKIP[e.tagName]) return true;
    if (e.hidden) return true;
    var tr = e.getAttribute && e.getAttribute('translate');
    if (tr === 'no') return true;
    if (e.classList && e.classList.contains('notranslate')) return true;
    if (e.isContentEditable) return true;
    return false;
  }
  function worth(t) {
    if (!t || !/\p{L}/u.test(t)) return false;
    if (t.length < 2 && !/[\u3040-\u30ff\u3400-\u9fff\uac00-\ud7af]/.test(t)) return false;
    if (/^(https?:\/\/|www\.)\S+$/i.test(t)) return false;
    if (/^[\w.+-]+@[\w-]+\.[\w.]+$/.test(t)) return false;
    return true;
  }
  function split(v) {
    var m = /^(\s*)([\s\S]*?)(\s*)$/.exec(v);
    return { lead: m[1], t: m[2], trail: m[3] };
  }
  function addRec(rec) { items.push(rec); return items.length - 1; }

  function seeText(node) {
    var v = node.nodeValue;
    var i = textIdx.get(node);
    if (i !== undefined) {
      var r = items[i];
      if (r.st === 2 && v === r.cur) return;          // our own translation
      if (r.st === 1) return;                          // being translated right now
      if (r.st === 2) { r.o = v; var s0 = split(v); r.lead = s0.lead; r.t = s0.t; r.trail = s0.trail; r.st = worth(r.t) ? 0 : 2; r.cur = null; return; }
      return;
    }
    var s = split(v);
    if (!worth(s.t)) return;
    if (items.length >= MAX) return;
    textIdx.set(node, addRec({ k: 0, n: node, o: v, t: s.t, lead: s.lead, trail: s.trail, st: 0, cur: null }));
  }
  function seeAttr(el, a) {
    var v = el.getAttribute(a);
    if (!v) return;
    var m = attrIdx.get(el);
    if (!m) { m = {}; attrIdx.set(el, m); }
    var i = m[a];
    if (i !== undefined) {
      var r = items[i];
      if (r.st === 2 && v === r.cur) return;
      if (r.st === 1) return;
      if (r.st === 2) { r.o = v; var s1 = split(v); r.lead = s1.lead; r.t = s1.t; r.trail = s1.trail; r.st = worth(r.t) ? 0 : 2; r.cur = null; }
      return;
    }
    var s = split(v);
    if (!worth(s.t) || items.length >= MAX) return;
    m[a] = addRec({ k: 1, n: el, a: a, o: v, t: s.t, lead: s.lead, trail: s.trail, st: 0, cur: null });
  }
  function scan() {
    var root = document.body;
    if (!root) return;
    var w = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, {
      acceptNode: function (n) {
        if (n.nodeType === 1) {
          if (skipEl(n)) return NodeFilter.FILTER_REJECT;
          for (var j = 0; j < ATTRS.length; j++) if (n.hasAttribute(ATTRS[j])) seeAttr(n, ATTRS[j]);
          if (n.tagName === 'INPUT' && /^(button|submit|reset)$/i.test(n.type || '')) seeAttr(n, 'value');
          return NodeFilter.FILTER_SKIP;
        }
        return NodeFilter.FILTER_ACCEPT;
      }
    });
    var n;
    while ((n = w.nextNode())) seeText(n);
    var ti = document.title;
    if (ti && !titleRec && worth(ti.trim())) {
      var s = split(ti);
      titleRec = { k: 2, n: null, o: ti, t: s.t, lead: s.lead, trail: s.trail, st: 0, cur: null };
      addRec(titleRec);
    }
  }
  function setVal(r, t) {
    var v = r.lead + t + r.trail;
    if (r.k === 0) r.n.nodeValue = v;
    else if (r.k === 1) r.n.setAttribute(r.a, v);
    else document.title = v;
    r.cur = v;
  }
  function watch() {
    if (obs || !window.MutationObserver) return;
    obs = new MutationObserver(function () { if (!applying) dirty = true; });
    obs.observe(document.documentElement, { childList: true, subtree: true, characterData: true,
      attributes: true, attributeFilter: ATTRS.concat(['value']) });
  }

  window.__ngTr = {
    info: function () {
      var l = (document.documentElement.getAttribute('lang') || '').trim();
      var m = document.querySelector('meta[http-equiv="content-language" i]');
      if (!l && m) l = (m.getAttribute('content') || '').split(',')[0].trim();
      var s = ((document.body && document.body.innerText) || '').replace(/\s+/g, ' ').trim().slice(0, 700);
      return JSON.stringify({ lang: l, sample: s, title: document.title || '' });
    },
    /** next group of untranslated texts (identical texts are sent once) */
    take: function (maxItems, maxChars) {
      watch();
      if (dirty) { scan(); dirty = false; }
      var out = [], map = Object.create(null), chars = 0, done = 0;
      applying = true;
      for (var i = 0; i < items.length; i++) {
        var r = items[i];
        if (r.st === 2) { done++; continue; }
        if (r.st !== 0) continue;
        if (cache[r.t] !== undefined) { setVal(r, cache[r.t]); r.st = 2; done++; continue; }
        var g = map[r.t];
        if (g !== undefined) { out[g].ids.push(i); r.st = 1; continue; }
        if (out.length >= maxItems || (out.length && chars + r.t.length > maxChars)) continue;
        map[r.t] = out.length;
        out.push({ ids: [i], t: r.t });
        chars += r.t.length;
        r.st = 1;
      }
      applying = false;
      if (obs) obs.takeRecords();
      return JSON.stringify({ all: items.length, done: done, items: out });
    },
    put: function (json, lang) {
      var arr = JSON.parse(json), n = 0;
      applying = true;
      for (var a = 0; a < arr.length; a++) {
        var g = arr[a];
        for (var b = 0; b < g.ids.length; b++) {
          var r = items[g.ids[b]];
          if (!r || r.st !== 1) continue;
          cache[r.t] = g.t;
          setVal(r, g.t);
          r.st = 2;
          n++;
        }
      }
      if (lang && origLang === null) { origLang = document.documentElement.getAttribute('lang') || ''; document.documentElement.setAttribute('lang', lang); }
      applying = false;
      if (obs) obs.takeRecords();
      return n;
    },
    /** texts that appeared after the last pass (infinite scroll, single page apps) */
    pending: function () {
      if (!dirty) return 0;
      scan(); dirty = false;
      var c = 0;
      for (var i = 0; i < items.length; i++) if (items[i].st === 0) c++;
      return c;
    },
    /** sent texts that never came back (a failed batch) go back to the queue */
    requeue: function () { for (var i = 0; i < items.length; i++) if (items[i].st === 1) items[i].st = 0; },
    restore: function () {
      var n = 0;
      applying = true;
      for (var i = 0; i < items.length; i++) {
        var r = items[i];
        if (r.st === 2 && r.cur !== null) {
          if (r.k === 0) { if (r.n.nodeValue === r.cur) r.n.nodeValue = r.o; }
          else if (r.k === 1) { if (r.n.getAttribute(r.a) === r.cur) r.n.setAttribute(r.a, r.o); }
          else document.title = r.o;
          n++;
        }
      }
      if (origLang !== null) { if (origLang) document.documentElement.setAttribute('lang', origLang); else document.documentElement.removeAttribute('lang'); }
      applying = false;
      if (obs) { obs.takeRecords(); obs.disconnect(); }
      items = []; textIdx = new WeakMap(); attrIdx = new WeakMap(); cache = Object.create(null);
      dirty = true; obs = null; origLang = null; titleRec = null;
      return n;
    }
  };
})();
