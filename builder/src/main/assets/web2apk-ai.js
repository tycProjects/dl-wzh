(function () {
  if (window.Web2APKAI) return;
  var pending = Object.create(null);
  var sequence = 0;
  function receive(event) {
    var payload;
    try { payload = JSON.parse(event && event.data || '{}'); }
    catch (error) { return; }
    var task = pending[payload.id];
    if (!task) return;
    delete pending[payload.id];
    clearTimeout(task.timer);
    if (payload.ok) task.resolve(payload);
    else task.reject(new Error(payload.error || 'AI API request failed'));
  }
  if (window.Web2ApkAIHost && typeof window.Web2ApkAIHost.addEventListener === 'function') {
    window.Web2ApkAIHost.addEventListener('message', receive);
  } else if (window.Web2ApkAIHost) {
    window.Web2ApkAIHost.onmessage = receive;
  }
  window.Web2APKAI = {
    configure: function () {
      if (!window.Web2ApkNative) throw new Error('WEB2APK Android bridge is unavailable');
      window.Web2ApkNative.openAiSettings();
    },
    isConfigured: function () {
      return !!(window.Web2ApkNative && window.Web2ApkNative.isAiConfigured());
    },
    chat: function (messages) {
      return new Promise(function (resolve, reject) {
        if (!window.Web2ApkAIHost || typeof window.Web2ApkAIHost.postMessage !== 'function') {
          reject(new Error('Secure AI bridge is unavailable. Update Android System WebView and rebuild the APK.')); return;
        }
        if (!Array.isArray(messages) || messages.length < 1 || messages.length > 50) {
          reject(new Error('messages must be an array containing 1 to 50 items')); return;
        }
        var id = 'ai_' + Date.now() + '_' + (++sequence);
        var timer = setTimeout(function () {
          if (pending[id]) { delete pending[id]; reject(new Error('AI API request timed out')); }
        }, 70000);
        pending[id] = { resolve: resolve, reject: reject, timer: timer };
        try { window.Web2ApkAIHost.postMessage(JSON.stringify({ type: 'chat', id: id, messages: messages })); }
        catch (error) { clearTimeout(timer); delete pending[id]; reject(error); }
      });
    }
  };
})();
