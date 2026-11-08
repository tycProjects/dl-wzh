(function () {
  var cfg = __CFG__;
  // Che dau vet: moi ham bi ghi de se tra "[native code]" khi goi toString()
  var fake = new WeakMap();
  var nativeToString = Function.prototype.toString;
  try {
    var ts = function toString() {
      var o = fake.get(this);
      return o !== undefined ? o : nativeToString.call(this);
    };
    fake.set(ts, 'function toString() { [native code] }');
    Object.defineProperty(Function.prototype, 'toString', { value: ts, configurable: true, writable: true });
  } catch (e) {}
  function mask(fn, name) { try { fake.set(fn, 'function ' + name + '() { [native code] }'); } catch (e) {} return fn; }

  function def(o, k, v) {
    try {
      var g = mask(function () { return v; }, 'get ' + k);
      Object.defineProperty(o, k, { get: g, configurable: true, enumerable: true });
    } catch (e) {}
  }

  // ===== CHE DO TU DONG: KHONG ghi de gi cua thiet bi that (tranh bi bao "Tampering") =====
  if (cfg.spoofUa) {
    var N = Navigator.prototype;
    var ua = cfg.ua;
    var isIos = /iPhone|iPad/.test(ua);
    var isAnd = /Android/.test(ua);
    var isWin = /Windows/.test(ua);
    var mobile = /Mobile/.test(ua);

    def(N, 'userAgent', ua);
    def(N, 'appVersion', ua.replace(/^Mozilla\//, ''));
    def(N, 'platform', cfg.platform);
    def(N, 'maxTouchPoints', cfg.touch);
    def(N, 'hardwareConcurrency', cfg.cores);
    def(N, 'language', cfg.lang);
    def(N, 'languages', [cfg.lang, cfg.lang.split('-')[0]]);
    def(N, 'vendor', isIos ? 'Apple Computer, Inc.' : 'Google Inc.');

    // deviceMemory: Chrome chi bao 0.25..8; Safari (iPhone) khong co thuoc tinh nay
    if (isIos) { try { delete N.deviceMemory; } catch (e) {} }
    else {
      var steps = [0.25, 0.5, 1, 2, 4, 8], dm = 8;
      for (var si = 0; si < steps.length; si++) if (steps[si] <= cfg.mem) dm = steps[si];
      def(N, 'deviceMemory', dm);
    }

    // userAgentData: dong bo voi UA (Android/Windows co; iPhone Safari khong co)
    if (!isAnd && !isWin) {
      try { delete N.userAgentData; } catch (e) {}
      def(N, 'userAgentData', undefined);
    } else {
      var cm = /Chrome\/(\d+)/.exec(ua), cv = cm ? cm[1] : '130';
      var am = /Android ([\d.]+); ([^)]+)\)/.exec(ua);
      var brands = [{ brand: 'Not_A Brand', version: '24' }, { brand: 'Chromium', version: cv }, { brand: 'Google Chrome', version: cv }];
      var plat = isAnd ? 'Android' : 'Windows';
      var hi = {
        architecture: mobile ? '' : 'x86', bitness: mobile ? '' : '64', brands: brands,
        fullVersionList: brands.map(function (b) { return { brand: b.brand, version: b.version + '.0.0.0' }; }),
        mobile: mobile, model: am ? am[2] : '', platform: plat,
        platformVersion: am ? am[1] + '.0.0' : '10.0.0', uaFullVersion: cv + '.0.0.0', wow64: false
      };
      var proto = window.NavigatorUAData ? NavigatorUAData.prototype : Object.prototype;
      var uad = Object.create(proto);
      def(uad, 'brands', brands); def(uad, 'mobile', mobile); def(uad, 'platform', plat);
      uad.getHighEntropyValues = mask(function getHighEntropyValues(h) {
        var r = { brands: brands, mobile: mobile, platform: plat };
        (h || []).forEach(function (k) { if (k in hi) r[k] = hi[k]; });
        return Promise.resolve(r);
      }, 'getHighEntropyValues');
      uad.toJSON = mask(function toJSON() { return { brands: brands, mobile: mobile, platform: plat }; }, 'toJSON');
      def(N, 'userAgentData', uad);
    }

    try {
      var S = Screen.prototype;
      def(S, 'width', cfg.sw);
      def(S, 'height', cfg.sh);
      def(S, 'availWidth', cfg.sw);
      def(S, 'availHeight', cfg.sh);
      def(S, 'colorDepth', 24);
      def(S, 'pixelDepth', 24);
    } catch (e) {}
    try { def(window, 'devicePixelRatio', cfg.dpr); } catch (e) {}

    // WebGL vendor/renderer khop thiet bi mau (iPhone -> Apple GPU, Samsung -> Adreno...)
    var gv = cfg.gpuV, gr = cfg.gpuR;
    if (!gv && isIos) { gv = 'Apple Inc.'; gr = 'Apple GPU'; }
    if (gv) {
      [window.WebGLRenderingContext, window.WebGL2RenderingContext].forEach(function (C) {
        if (!C) return;
        var og = C.prototype.getParameter;
        var gf = function getParameter(p) {
          if (p === 37445) return gv;
          if (p === 37446) return gr;
          return og.apply(this, arguments);
        };
        mask(gf, 'getParameter');
        Object.defineProperty(C.prototype, 'getParameter', { value: gf, configurable: true, writable: true });
      });
    }

    var seed = cfg.seed;
    var td = HTMLCanvasElement.prototype.toDataURL;
    var ntd = function toDataURL() {
      try {
        var c = this;
        if (c.width && c.height && c.width * c.height <= 250000) {
          var t = document.createElement('canvas');
          t.width = c.width; t.height = c.height;
          var x = t.getContext('2d');
          x.drawImage(c, 0, 0);
          var w = Math.min(t.width, 8), h = Math.min(t.height, 8);
          var d = x.getImageData(0, 0, w, h);
          for (var i = 0; i < d.data.length; i += 4) d.data[i] = (d.data[i] + ((seed >> (i % 8)) & 1)) & 255;
          x.putImageData(d, 0, 0);
          return td.apply(t, arguments);
        }
      } catch (e) {}
      return td.apply(this, arguments);
    };
    mask(ntd, 'toDataURL');
    HTMLCanvasElement.prototype.toDataURL = ntd;
  }

  // ===== Mui gio theo IP proxy (chi khi ho so co dat mui gio) =====
  if (cfg.tz) {
    try {
      var TZ = cfg.tz;
      var DTF = Intl.DateTimeFormat;
      var fc = new DTF('en-US', { timeZone: TZ, hourCycle: 'h23', year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric' });
      var P = Date.prototype, gt = P.getTime;
      var pad = function (n) { return (n < 10 ? '0' : '') + n; };
      var lastS = NaN, lastO = null;
      var parts = function (t) {
        var s = Math.floor(t / 1000);
        if (s === lastS && lastO) return lastO;
        var o = {};
        fc.formatToParts(new Date(t)).forEach(function (x) { o[x.type] = parseInt(x.value, 10); });
        if (o.hour === 24) o.hour = 0;
        lastS = s; lastO = o;
        return o;
      };
      var offMin = function (t) {
        var o = parts(t);
        return Math.round((Date.UTC(o.year, o.month - 1, o.day, o.hour, o.minute, o.second) - Math.floor(t / 1000) * 1000) / 60000);
      };
      var wrapDate = function (name, fn) {
        var orig = P[name]; if (!orig) return;
        var f = function () { var t = gt.call(this); return t !== t ? NaN : fn(t); };
        mask(f, name);
        Object.defineProperty(P, name, { value: f, configurable: true, writable: true });
      };
      wrapDate('getTimezoneOffset', function (t) { return -offMin(t); });
      wrapDate('getFullYear', function (t) { return parts(t).year; });
      wrapDate('getMonth', function (t) { return parts(t).month - 1; });
      wrapDate('getDate', function (t) { return parts(t).day; });
      wrapDate('getHours', function (t) { return parts(t).hour; });
      wrapDate('getMinutes', function (t) { return parts(t).minute; });
      wrapDate('getSeconds', function (t) { return parts(t).second; });
      wrapDate('getDay', function (t) { var o = parts(t); return new Date(Date.UTC(o.year, o.month - 1, o.day)).getUTCDay(); });

      var DAY = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
      var MON = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      var gmt = function (t) { var m = offMin(t), s = m < 0 ? '-' : '+'; m = Math.abs(m); return 'GMT' + s + pad((m / 60) | 0) + pad(m % 60); };
      var fcName = null;
      try { fcName = new DTF('en-US', { timeZone: TZ, timeZoneName: 'long' }); } catch (e) {}
      var tzn = function (t) {
        var r = '';
        try { if (fcName) fcName.formatToParts(new Date(t)).forEach(function (x) { if (x.type === 'timeZoneName') r = x.value; }); } catch (e) {}
        return r;
      };
      var ds = function (t) { var o = parts(t); return DAY[new Date(Date.UTC(o.year, o.month - 1, o.day)).getUTCDay()] + ' ' + MON[o.month - 1] + ' ' + pad(o.day) + ' ' + o.year; };
      var tm = function (t) { var o = parts(t); return pad(o.hour) + ':' + pad(o.minute) + ':' + pad(o.second) + ' ' + gmt(t) + ' (' + tzn(t) + ')'; };
      var wrapStr = function (name, fn) {
        var f = function () { var t = gt.call(this); return t !== t ? 'Invalid Date' : fn(t); };
        mask(f, name);
        Object.defineProperty(P, name, { value: f, configurable: true, writable: true });
      };
      wrapStr('toString', function (t) { return ds(t) + ' ' + tm(t); });
      wrapStr('toDateString', ds);
      wrapStr('toTimeString', tm);

      ['toLocaleString', 'toLocaleDateString', 'toLocaleTimeString'].forEach(function (name) {
        var orig = P[name];
        var f = function (l, o) { o = o ? Object.assign({}, o) : {}; if (o.timeZone === undefined) o.timeZone = TZ; return orig.call(this, l, o); };
        mask(f, name);
        Object.defineProperty(P, name, { value: f, configurable: true, writable: true });
      });

      var NewDTF = function DateTimeFormat(l, o) { o = o ? Object.assign({}, o) : {}; if (o.timeZone === undefined) o.timeZone = TZ; return new DTF(l, o); };
      NewDTF.prototype = DTF.prototype;
      NewDTF.supportedLocalesOf = DTF.supportedLocalesOf;
      mask(NewDTF, 'DateTimeFormat');
      Object.defineProperty(Intl, 'DateTimeFormat', { value: NewDTF, configurable: true, writable: true });

      // Chromium tra ten cu (Asia/Saigon) cho Asia/Ho_Chi_Minh... -> doi lai dung ten IANA cua IP proxy
      var canon = new DTF('en-US', { timeZone: TZ }).resolvedOptions().timeZone;
      var oRes = DTF.prototype.resolvedOptions;
      var nRes = function resolvedOptions() {
        var r = oRes.apply(this, arguments);
        if (r && r.timeZone === canon) r.timeZone = TZ;
        return r;
      };
      mask(nRes, 'resolvedOptions');
      Object.defineProperty(DTF.prototype, 'resolvedOptions', { value: nRes, configurable: true, writable: true });
    } catch (e) {}
  }

  // ===== WebRTC =====
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
      mask(P, k);
      P.prototype = Orig.prototype;
      try { window[k] = P; } catch (e) {}
    });
  }
})();
