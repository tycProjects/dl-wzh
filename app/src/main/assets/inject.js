(function () {
  var cfg = __CFG__;
  function def(o, k, v) { try { Object.defineProperty(o, k, { get: function () { return v; }, configurable: true }); } catch (e) {} }
  var N = Navigator.prototype;
  if (cfg.spoofUa) {
    def(N, 'userAgent', cfg.ua);
    def(N, 'appVersion', cfg.ua.replace(/^Mozilla\//, ''));
    def(N, 'platform', cfg.platform);
    def(N, 'maxTouchPoints', cfg.touch);
    def(N, 'userAgentData', undefined);
  }
  def(N, 'hardwareConcurrency', cfg.cores);
  def(N, 'deviceMemory', cfg.mem);
  def(N, 'language', cfg.lang);
  def(N, 'languages', [cfg.lang, cfg.lang.split('-')[0]]);

  var seed = cfg.seed;
  function noisy(c) {
    try {
      var ctx = c.getContext('2d'); if (!ctx || !c.width || !c.height) return;
      var w = Math.min(c.width, 8), h = Math.min(c.height, 8);
      var d = ctx.getImageData(0, 0, w, h);
      for (var i = 0; i < d.data.length; i += 4) d.data[i] = (d.data[i] + ((seed >> (i % 8)) & 1)) & 255;
      ctx.putImageData(d, 0, 0);
    } catch (e) {}
  }
  var td = HTMLCanvasElement.prototype.toDataURL;
  HTMLCanvasElement.prototype.toDataURL = function () { noisy(this); return td.apply(this, arguments); };

  var names = ['RTCPeerConnection', 'webkitRTCPeerConnection'];
  if (cfg.rtc === 'off') {
    names.concat(['RTCDataChannel', 'RTCSessionDescription', 'RTCIceCandidate']).forEach(function (k) {
      try { delete window[k]; } catch (e) {}
      try { window[k] = undefined; } catch (e) {}
    });
  } else {
    names.forEach(function (k) {
      var Orig = window[k]; if (!Orig) return;
      var P = function (conf, c) {
        conf = Object.assign({}, conf || {}, { iceTransportPolicy: 'relay' });
        return new Orig(conf, c);
      };
      P.prototype = Orig.prototype;
      try { window[k] = P; } catch (e) {}
    });
  }
})();
