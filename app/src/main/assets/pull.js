/* Tells native code whether a downward drag that starts here may pull to refresh.
   It must not when the page itself (or a box inside it) can still scroll up, or when the page
   handles the gesture on its own. */
(function () {
  if (window.__ngPT) return;
  window.__ngPT = 1;
  function ok(el) {
    try {
      var se = document.scrollingElement;
      if (window.scrollY > 0 || (se && se.scrollTop > 0)) return false;
      for (var e = el; e && e.nodeType === 1; e = e.parentElement) {
        var cs = getComputedStyle(e);
        if (cs.overscrollBehaviorY === 'contain' || cs.overscrollBehaviorY === 'none') return false;
        if (cs.touchAction === 'none') return false;
        var oy = cs.overflowY;
        if ((oy === 'auto' || oy === 'scroll' || oy === 'overlay') && e !== document.body && e !== document.documentElement &&
            e.scrollHeight > e.clientHeight + 1 && e.scrollTop > 0) return false;
      }
      return true;
    } catch (x) { return false; }
  }
  function send(v) { try { NomanPull.arm(v); } catch (x) { } }
  addEventListener('touchstart', function (ev) {
    if (ev.touches && ev.touches.length > 1) { send(false); return; }
    var t = ev.target;
    if (t && t.nodeType !== 1) t = t.parentElement;
    send(ok(t));
  }, { passive: true, capture: true });
})();
