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
    var LG = [cfg.lang, cfg.lang.split('-')[0]]; if (cfg.lang.indexOf('en') !== 0) LG.push('en-US', 'en');
    def(N, 'languages', Object.freeze(LG));   // v3.3.5: navigator.languages that la mang dong bang (frozen)
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

  }

  // ===== v3.3.5: Ngon ngu cung ap dung o che do Tu dong (truoc day o "NGON NGU" bi bo qua neu khong gia UA) =====
  if (!cfg.spoofUa && cfg.lang) {
    try {
      if (navigator.language !== cfg.lang) {
        var LG2 = [cfg.lang, cfg.lang.split('-')[0]]; if (cfg.lang.indexOf('en') !== 0) LG2.push('en-US', 'en');
        def(Navigator.prototype, 'language', cfg.lang);
        def(Navigator.prototype, 'languages', Object.freeze(LG2));
      }
    } catch (e) {}
  }

  // ===== v3.3.5: GAY NHIEU van tay kieu GPMLogin (canvas / WebGL / audio / rects) =====
  // Nhieu CO DINH theo UUID (cung ho so -> cung ket qua moi lan goi, khac ho so khac) va rat nho (+-1 tren ~3% diem anh).
  // Khong chay trong iframe captcha (hCaptcha / reCAPTCHA / Cloudflare / Arkose / GeeTest...) de captcha khong bi do.
  if (cfg.noise !== false) {
    try {
      var NH = location.hostname || '', NP = location.pathname || '';
      var capFrame = /(^|\.)(hcaptcha\.com|recaptcha\.net|challenges\.cloudflare\.com|arkoselabs\.com|funcaptcha\.com|geetest\.com|captcha-delivery\.com|perimeterx\.net)$/.test(NH)
        || (/(^|\.)(google|gstatic)\.com$/.test(NH) && /recaptcha/.test(NP));
      if (!capFrame) {
        var SEED = cfg.seed | 0;
        var rng = function (salt) {   // mulberry32: nhanh, xac dinh
          var a = (SEED ^ Math.imul(salt | 0, 0x9E3779B1)) >>> 0;
          return function () {
            a = (a + 0x6D2B79F5) >>> 0;
            var t = a;
            t = Math.imul(t ^ (t >>> 15), t | 1);
            t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
          };
        };
        var hook = function (proto, name, make) {
          try {
            var orig = proto && proto[name];
            if (typeof orig !== 'function') return null;
            var f = make(orig);
            try { Object.defineProperty(f, 'name', { value: name, configurable: true }); } catch (e) {}
            mask(f, name);
            Object.defineProperty(proto, name, { value: f, configurable: true, writable: true, enumerable: true });
            return orig;
          } catch (e) { return null; }
        };
        var MAXPX = 90000;   // chi can thiep canvas nho (kieu canvas van tay); anh/game/editor lon khong bi dong cham
        var noisePx = function (d, n, salt) {
          var r = rng(salt);
          for (var p = 0; p < n; p++) {
            var v = r();
            if (v < 0.03) {
              var i = p * 4;
              if (d[i + 3] === 0) continue;
              var ch = ((v * 100000) | 0) % 3;
              var x = d[i + ch] + ((((v * 1000000) | 0) & 1) ? 1 : -1);
              d[i + ch] = x < 0 ? 0 : x > 255 ? 255 : x;
            }
          }
        };

        // --- Canvas 2D: toDataURL / toBlob / getImageData ---
        var C2 = window.CanvasRenderingContext2D && CanvasRenderingContext2D.prototype;
        var oGID = C2 && C2.getImageData;
        var cloneNoisy = function (c) {   // ban sao da them nhieu; canvas goc giu nguyen
          if (!oGID || !(c.width && c.height) || c.width * c.height > MAXPX) return null;
          var t = document.createElement('canvas');
          t.width = c.width; t.height = c.height;
          var x = t.getContext('2d');
          x.drawImage(c, 0, 0);
          var im = oGID.call(x, 0, 0, t.width, t.height);
          noisePx(im.data, t.width * t.height, c.width * 7 + c.height);
          x.putImageData(im, 0, 0);
          return t;
        };
        var HC = window.HTMLCanvasElement && HTMLCanvasElement.prototype;
        ['toDataURL', 'toBlob'].forEach(function (nm) {
          hook(HC, nm, function (orig) {
            return function () {
              try { var t = cloneNoisy(this); if (t) return orig.apply(t, arguments); } catch (e) {}
              return orig.apply(this, arguments);
            };
          });
        });
        hook(C2, 'getImageData', function (orig) {
          return function getImageData() {
            var im = orig.apply(this, arguments);
            try { if (im && im.data && im.width * im.height <= MAXPX) noisePx(im.data, im.width * im.height, im.width * 13 + im.height + 5); } catch (e) {}
            return im;
          };
        });

        // --- WebGL: readPixels ---
        [window.WebGLRenderingContext, window.WebGL2RenderingContext].forEach(function (G) {
          if (!G) return;
          hook(G.prototype, 'readPixels', function (orig) {
            return function readPixels(x, y, w, h, fmt, type, pix) {
              var ret = orig.apply(this, arguments);
              try {
                if (pix && pix.length && w * h <= MAXPX && (pix instanceof Uint8Array || pix instanceof Uint8ClampedArray))
                  noisePx(pix, Math.min(pix.length >> 2, w * h), w * 11 + h + 3);
              } catch (e) {}
              return ret;
            };
          });
        });

        // --- Audio: AudioBuffer.getChannelData (OfflineAudioContext) + AnalyserNode ---
        var doneA = new WeakSet();
        hook(window.AudioBuffer && AudioBuffer.prototype, 'getChannelData', function (orig) {
          return function getChannelData() {
            var a = orig.apply(this, arguments);
            try {
              if (a && a.length && a.length <= 500000 && !doneA.has(a)) {
                doneA.add(a);
                var r = rng(a.length + 17);
                for (var i = 0; i < a.length; i += 8) a[i] += (r() - 0.5) * 2e-7;
              }
            } catch (e) {}
            return a;
          };
        });
        hook(window.AnalyserNode && AnalyserNode.prototype, 'getFloatFrequencyData', function (orig) {
          return function getFloatFrequencyData(arr) {
            var ret = orig.apply(this, arguments);
            try { var r = rng(arr.length + 29); for (var i = 0; i < arr.length; i += 4) arr[i] += (r() - 0.5) * 1e-3; } catch (e) {}
            return ret;
          };
        });

        // --- Rects: getBoundingClientRect lech ~1e-5..1e-4 px (nho hon do chinh xac layout 1/64px nen khong anh huong bo cuc) ---
        var RN = ((SEED >>> 0) % 1000) / 1e7 + 1e-5;
        var noisyRect = function (r) {
          try {
            var w = r.width, h = r.height, on = (w || h) ? RN : 0;
            return new DOMRect(r.x + on, r.y + on, w ? w + RN : 0, h ? h + RN : 0);   // phan tu rong (0) giu nguyen de khong pha kiem tra an/hien
          } catch (e) { return r; }
        };
        ['Element', 'Range'].forEach(function (cn) {
          hook(window[cn] && window[cn].prototype, 'getBoundingClientRect', function (orig) {
            return function getBoundingClientRect() { return noisyRect(orig.apply(this, arguments)); };
          });
        });
      }
    } catch (e) {}
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
      try { Object.defineProperty(DTF.prototype, 'constructor', { value: NewDTF, configurable: true, writable: true }); } catch (e) {}   // v3.3.5: Intl.DateTimeFormat.prototype.constructor === Intl.DateTimeFormat

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
      try { if (Orig.generateCertificate) P.generateCertificate = Orig.generateCertificate.bind(Orig); } catch (e) {}   // v3.3.5: giu ham tinh
      try { window[k] = P; } catch (e) {}
    });
  }
})();
