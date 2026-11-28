const V='pc-v2';
const A=['./','index.html','css/main.css','js/engine.js','js/themes.js','js/app.js','tests/cases.js','manifest.webmanifest','assets/icons/icon.svg','js/vendor/math.min.js','assets/fonts/PressStart2P.woff2','assets/fonts/VT323.woff2'];
self.addEventListener('install',e=>e.waitUntil(caches.open(V).then(c=>Promise.all(A.map(u=>c.add(u).catch(()=>{})))).then(()=>self.skipWaiting())));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!==V).map(x=>caches.delete(x)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{
 if(e.request.method!=='GET')return;
 e.respondWith(caches.match(e.request,{ignoreSearch:true}).then(hit=>hit||fetch(e.request).then(r=>{
  if(r.ok&&new URL(e.request.url).origin===location.origin){const c=r.clone();caches.open(V).then(x=>x.put(e.request,c))}return r})
  .catch(()=>caches.match('index.html'))));
});
