(function () {
  // MASTER Turbo v2: anh trong man hinh tai NGAY (uu tien cao), anh gan man hinh tai truoc, anh xa moi tai luoi;
  // giai ma bat dong bo, preconnect may chu anh, bo qua ve phan ngoai man hinh.
  try {
    var CAP = /captcha|recaptcha|hcaptcha|turnstile|challenge|arkose|funcaptcha|geetest/i;
    var vh = window.innerHeight || 800;
    var boosted = 0, measured = 0, pre = {};
    function preconnect(src) {
      try {
        var u = new URL(src, location.href);
        if (u.origin === location.origin || pre[u.origin] || !/^https?:$/.test(u.protocol)) return;
        pre[u.origin] = 1;
        var l = document.createElement('link');
        l.rel = 'preconnect'; l.href = u.origin; l.crossOrigin = 'anonymous';
        (document.head || document.documentElement).appendChild(l);
      } catch (e) {}
    }
    function fix(img) {
      if (img.__tb) return;
      img.__tb = 1;
      try {
        var src = img.currentSrc || img.src || '';
        if (CAP.test(src)) return;
        if (!img.hasAttribute('decoding')) img.decoding = 'async';
        if (src) preconnect(src);
        if (img.hasAttribute('loading') || img.getAttribute('fetchpriority') === 'high') return;
        var top = vh * 5;   // v3.3.2: chi do vi tri 30 anh dau (do nhieu anh lien tuc -> layout thrashing); anh sau coi nhu o xa
        if (measured < 30) { measured++; try { top = img.getBoundingClientRect().top; } catch (e) {} }
        if (top < vh * 1.2) {                        // trong / sat man hinh dau -> tai ngay, uu tien cao
          img.loading = 'eager';
          if (boosted < 4 && top < vh) { boosted++; try { img.fetchPriority = 'high'; } catch (e) {} }
        } else {
          img.loading = 'lazy';                      // xa man hinh -> tai khi sap cuon toi
        }
      } catch (e) {}
    }
    function scan(node) {
      if (!node || node.nodeType !== 1) return;
      if (node.tagName === 'IMG') { fix(node); return; }
      var list = node.getElementsByTagName ? node.getElementsByTagName('img') : [];
      for (var i = 0; i < list.length; i++) fix(list[i]);
    }
    var mo = new MutationObserver(function (ms) {
      for (var i = 0; i < ms.length; i++) {
        var a = ms[i].addedNodes;
        for (var j = 0; j < a.length; j++) scan(a[j]);
      }
    });
    mo.observe(document, { childList: true, subtree: true });
    document.addEventListener('DOMContentLoaded', function () { scan(document.documentElement); }, { once: true });
  } catch (e) {}
})();
