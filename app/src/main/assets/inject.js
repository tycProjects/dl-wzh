(function () {
  var cfg = __CFG__;
  function def(o, k, v) { try { Object.defineProperty(o, k, { get: function () { return v; }, configurable: true }); } catch (e) {} }
  var N = Navigator.prototype;
  def(N, 'userAgent', cfg.ua);
  def(N, 'appVersion', cfg.ua.replace(/^Mozilla\//, ''));
  def(N, 'platform', cfg.platform);
  def(N, 'hardwareConcurrency', cfg.cores);
  def(N, 'deviceMemory', cfg.mem);
  def(N, 'maxTouchPoints', cfg.touch);
  def(N, 'webdriver', false);
  def(N, 'language', cfg.lang);
  def(N, 'languages', [cfg.lang, cfg.lang.split('-')[0]]);
  def(N, 'userAgentData', undefined);

  // Canvas noise on dinh theo UUID
  var seed = cfg.seed;
  function noisy(c) {
    try {
      var ctx = c.getContext('2d'); if (!ctx || !c.width || !c.height) return;
      var w = Math.min(c.width, 16), h = Math.min(c.height, 16);
      var d = ctx.getImageData(0, 0, w, h);
      for (var i = 0; i < d.data.length; i += 4) d.data[i] = (d.data[i] + ((seed >> (i % 8)) & 1)) & 255;
      ctx.putImageData(d, 0, 0);
    } catch (e) {}
  }
  var td = HTMLCanvasElement.prototype.toDataURL;
  HTMLCanvasElement.prototype.toDataURL = function () { noisy(this); return td.apply(this, arguments); };
  var tb = HTMLCanvasElement.prototype.toBlob;
  HTMLCanvasElement.prototype.toBlob = function () { noisy(this); return tb.apply(this, arguments); };

  // WebRTC
  var names = ['RTCPeerConnection', 'webkitRTCPeerConnection'];
  if (cfg.rtc === 'off') {
    names.concat(['RTCDataChannel', 'RTCSessionDescription', 'RTCIceCandidate']).forEach(function (k) {
      try { delete window[k]; } catch (e) {}
      try { window[k] = undefined; } catch (e) {}
    });
    try { def(N, 'mediaDevices', undefined); } catch (e) {}
  } else {
    // relay: chi dung ung vien relay => khong lo host/srflx (IP that / LAN)
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
