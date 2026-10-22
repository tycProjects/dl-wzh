(() => {
'use strict';

const $ = (id) => document.getElementById(id);
const video = $('video');
const overlay = $('playerOverlay');
const stage = $('playerStage');
const controls = $('playerControls');
const popup = $('popupMenu');

const state = {
  source: null, kind: '', mode: '', hls: null, shaka: null, mpegts: null,
  token: 0, retries: 0, retryTimer: null, controlsTimer: null,
  speed: 1, fit: 0, subtitleUrls: [], currentItem: null, dragging: false
};

const MAX_RETRIES = 5;
const FITS = ['contain', 'cover', 'fill'];
const SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2];

if (window.shaka) shaka.polyfill.installAll();

function setIcon(id, symbol){ $(id).setAttribute('href', '#'+symbol); }
function fmt(sec){
  if (!Number.isFinite(sec) || sec < 0) return '0:00';
  sec = Math.floor(sec); const h=Math.floor(sec/3600), m=Math.floor((sec%3600)/60), s=sec%60;
  return h ? `${h}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}` : `${m}:${String(s).padStart(2,'0')}`;
}
function ext(src){
  try { const p=new URL(src, location.href).pathname; return (p.split('.').pop()||'').toLowerCase(); }
  catch { return ''; }
}
function detect(src){
  const e=ext(src), l=String(src).toLowerCase();
  if(e==='m3u8'||l.includes('m3u8'))return'hls';
  if(e==='mpd'||l.includes('.mpd'))return'dash';
  if(['ts','m2ts','mts','flv'].includes(e))return e==='flv'?'flv':'ts';
  return'direct';
}
function badge(text){ $('formatBadge').textContent=text.toUpperCase(); }
function status(title, text='', error=false){
  $('statusTitle').textContent=title; $('statusText').textContent=text;
  $('retryButton').hidden=!error;
  $('playerStatus').classList.toggle('error',error);
  $('playerStatus').classList.add('show');
}
function hideStatus(){ $('playerStatus').classList.remove('show','error'); }
function showControls(){
  stage.classList.remove('controls-hidden');
  clearTimeout(state.controlsTimer);
  state.controlsTimer=setTimeout(()=>{ if(!video.paused) stage.classList.add('controls-hidden'); },3200);
}
function requestLandscape(){
  if(screen.orientation?.lock && document.fullscreenElement){
    screen.orientation.lock('landscape').catch(()=>{});
  }
}
async function enterLandscape(){
  try{
    if(!document.fullscreenElement && stage.requestFullscreen) await stage.requestFullscreen();
    requestLandscape();
  }catch{}
}
async function leaveLandscape(){
  try{
    if(screen.orientation?.unlock) screen.orientation.unlock();
    if(document.fullscreenElement) await document.exitFullscreen();
  }catch{}
}

async function teardown(){
  clearTimeout(state.retryTimer); state.retryTimer=null;
  try{ if(state.hls){state.hls.destroy();state.hls=null;} }catch{}
  try{ if(state.shaka){await state.shaka.destroy();state.shaka=null;} }catch{}
  try{
    if(state.mpegts){state.mpegts.pause();state.mpegts.unload();state.mpegts.detachMediaElement();state.mpegts.destroy();state.mpegts=null;}
  }catch{}
  state.subtitleUrls.forEach(URL.revokeObjectURL); state.subtitleUrls=[];
  [...video.querySelectorAll('track')].forEach(t=>t.remove());
  try{video.pause();video.removeAttribute('src');video.load();}catch{}
}

function jumpGap(){
  const b=video.buffered,t=video.currentTime;
  for(let i=0;i<b.length;i++){
    if(b.start(i)>t+.05 && b.start(i)-t<4.5){video.currentTime=b.start(i)+.08;return true;}
  }
  return false;
}

function scheduleRetry(delay=1200){
  if(!state.source || state.retryTimer || state.retries>=MAX_RETRIES)return;
  state.retries++;
  status(`إعادة الاتصال ${state.retries}/${MAX_RETRIES}`,'يتم استعادة البث');
  state.retryTimer=setTimeout(()=>{state.retryTimer=null;openSource(state.source,state.currentItem,false)},Math.min(delay*state.retries,8000));
}

function startHls(src,token){
  return new Promise((resolve,reject)=>{
    if(window.Hls && Hls.isSupported()){
      state.mode='hls';
      const h=new Hls({
        enableWorker:true,lowLatencyMode:false,backBufferLength:30,maxBufferLength:45,maxMaxBufferLength:90,
        maxBufferHole:1.5,nudgeOffset:.25,nudgeMaxRetry:10,capLevelToPlayerSize:true,startLevel:-1,
        manifestLoadingTimeOut:15000,manifestLoadingMaxRetry:4,levelLoadingMaxRetry:4,fragLoadingTimeOut:20000,fragLoadingMaxRetry:6
      });
      state.hls=h;
      h.on(Hls.Events.MANIFEST_PARSED,()=>{if(token!==state.token)return;badge('HLS');updateQuality();hideStatus();video.play().catch(()=>{});resolve();});
      h.on(Hls.Events.LEVEL_SWITCHED,updateQuality);
      h.on(Hls.Events.ERROR,(_,d)=>{
        if(token!==state.token)return;
        if(!d.fatal){
          if(d.details===Hls.ErrorDetails.BUFFER_STALLED_ERROR){if(!jumpGap())try{h.startLoad()}catch{}}
          return;
        }
        if(d.type===Hls.ErrorTypes.NETWORK_ERROR){try{h.startLoad()}catch{};scheduleRetry();}
        else if(d.type===Hls.ErrorTypes.MEDIA_ERROR){try{h.recoverMediaError()}catch{}}
        else reject(new Error('تعذر تشغيل HLS'));
      });
      h.loadSource(src); h.attachMedia(video);
    }else if(video.canPlayType('application/vnd.apple.mpegurl')){
      state.mode='native';video.src=src;video.load();badge('HLS');video.play().catch(()=>{});resolve();
    }else reject(new Error('HLS غير مدعوم'));
  });
}

async function startDash(src,token){
  if(!window.shaka || !shaka.Player.isBrowserSupported())throw new Error('DASH غير مدعوم');
  state.mode='shaka';
  const p=new shaka.Player(); state.shaka=p; await p.attach(video);
  p.configure({streaming:{bufferingGoal:30,rebufferingGoal:1.5,bufferBehind:25,jumpLargeGaps:true,smallGapLimit:1.5,retryParameters:{maxAttempts:5,baseDelay:800,backoffFactor:1.8,timeout:15000}}});
  p.addEventListener('error',e=>{if(e.detail?.severity===shaka.util.Error.Severity.CRITICAL)scheduleRetry();});
  p.addEventListener('adaptation',updateQuality);
  p.addEventListener('variantchanged',updateQuality);
  await p.load(src); if(token!==state.token)return; badge('DASH');updateQuality();hideStatus();video.play().catch(()=>{});
}

function startMpegts(src,kind,token){
  if(!window.mpegts || !mpegts.getFeatureList().mseLivePlayback)throw new Error('MPEG-TS غير مدعوم');
  state.mode='mpegts';
  const p=mpegts.createPlayer({type:kind==='flv'?'flv':'mpegts',isLive:true,url:src,cors:true},{
    enableWorker:true,enableStashBuffer:true,stashInitialSize:384*1024,lazyLoad:false,fixAudioTimestampGap:true,
    liveBufferLatencyChasing:true,liveBufferLatencyMaxLatency:8,liveBufferLatencyMinRemain:2.5,
    autoCleanupSourceBuffer:true,autoCleanupMaxBackwardDuration:45,autoCleanupMinBackwardDuration:20
  });
  state.mpegts=p;
  p.on(mpegts.Events.ERROR,()=>{if(token===state.token)scheduleRetry();});
  p.attachMediaElement(video);p.load();badge(kind==='flv'?'FLV':'MPEG-TS');Promise.resolve(p.play()).catch(()=>{});
}

function startDirect(src){state.mode='native';video.src=src;video.load();badge(ext(src)||'VIDEO');video.play().catch(()=>{});}

async function loadSubtitles(item){
  [...video.querySelectorAll('track')].forEach(t=>t.remove());
  const subs=Array.isArray(item?.subtitles)?item.subtitles:[];
  for(const s of subs){
    try{
      let url=s.src;
      if(/\.srt(\?|$)/i.test(url)){
        const txt=await fetch(url).then(r=>r.text());
        const vtt='WEBVTT\n\n'+txt.replace(/\r/g,'').replace(/(\d{2}:\d{2}:\d{2}),(\d{3})/g,'$1.$2');
        url=URL.createObjectURL(new Blob([vtt],{type:'text/vtt'})); state.subtitleUrls.push(url);
      }
      const t=document.createElement('track');
      t.kind='subtitles'; t.label=s.label||languageLabel(s.lang); t.srclang=s.lang||'ar'; t.src=url; t.default=!!s.default;
      video.appendChild(t);
    }catch{}
  }
}
function languageLabel(lang){
  return ({ar:'العربية',en:'English',ko:'한국어',fr:'Français',es:'Español',tr:'Türkçe',fa:'فارسی',de:'Deutsch',it:'Italiano'})[lang]||lang||'ترجمة';
}

async function openSource(src,item,resetRetries=true){
  const token=++state.token; if(resetRetries)state.retries=0;
  state.source=src;state.currentItem=item||{};
  $('playerTitle').textContent=item?.title||'SPERSH TV';
  $('playerType').textContent=item?.type==='channel'?'قناة':'فيلم';
  status('جاري التحضير','يتم فتح المصدر');
  await teardown();
  await loadSubtitles(item||{});
  const kind=detect(src); state.kind=kind;
  try{
    if(kind==='hls')await startHls(src,token);
    else if(kind==='dash')await startDash(src,token);
    else if(kind==='ts'||kind==='flv')startMpegts(src,kind,token);
    else startDirect(src);
  }catch(e){status('تعذر التشغيل',e.message||'تحقق من المصدر',true);}
}

async function openItem(item){
  if(!item?.stream)return;
  overlay.classList.add('open');overlay.setAttribute('aria-hidden','false');
  document.body.style.overflow='hidden';
  await enterLandscape();
  await openSource(item.stream,item,true);
  showControls();
}
async function closePlayer(){
  ++state.token;await teardown();overlay.classList.remove('open');overlay.setAttribute('aria-hidden','true');
  document.body.style.overflow='';popup.classList.remove('open');await leaveLandscape();
}

function updateQuality(){
  if(state.mode==='hls'&&state.hls){
    const h=state.hls,current=h.currentLevel;
    $('qualityButton').textContent=current<0?'Auto':(h.levels[current]?.height?`${h.levels[current].height}p`:'Auto');
  }else if(state.mode==='shaka'&&state.shaka){
    const cfg=state.shaka.getConfiguration(),track=state.shaka.getVariantTracks().find(t=>t.active);
    $('qualityButton').textContent=cfg.abr.enabled?'Auto':(track?.height?`${track.height}p`:'Auto');
  }else $('qualityButton').textContent=video.videoHeight?`${video.videoHeight}p`:'Auto';
}
function qualityMenu(){
  const items=[];
  if(state.mode==='hls'&&state.hls){
    items.push({label:'تلقائي',active:state.hls.currentLevel<0,run:()=>{state.hls.currentLevel=-1;updateQuality();}});
    state.hls.levels.forEach((l,i)=>items.push({label:l.height?`${l.height}p`:`جودة ${i+1}`,active:state.hls.currentLevel===i,run:()=>{state.hls.currentLevel=i;updateQuality();}}));
  }else if(state.mode==='shaka'&&state.shaka){
    const p=state.shaka,cfg=p.getConfiguration(),tracks=p.getVariantTracks().filter(t=>t.type==='variant');
    items.push({label:'تلقائي',active:cfg.abr.enabled,run:()=>{p.configure({abr:{enabled:true}});updateQuality();}});
    tracks.sort((a,b)=>(b.height||0)-(a.height||0)).forEach(t=>items.push({label:t.height?`${t.height}p`:'جودة',active:!cfg.abr.enabled&&t.active,run:()=>{p.configure({abr:{enabled:false}});p.selectVariantTrack(t,true);updateQuality();}}));
  }else items.push({label:$('qualityButton').textContent,active:true,run:()=>{}});
  renderMenu('الجودة',items);
}
function subtitleMenu(){
  const tracks=[...video.textTracks];
  const items=[{label:'إيقاف الترجمة',active:tracks.every(t=>t.mode!=='showing'),run:()=>tracks.forEach(t=>t.mode='disabled')}];
  tracks.forEach(t=>items.push({label:t.label||languageLabel(t.language),active:t.mode==='showing',run:()=>{tracks.forEach(x=>x.mode='disabled');t.mode='showing';}}));
  if(!tracks.length)items.push({label:'لا توجد ترجمات لهذا المصدر',active:false,run:()=>{}});
  renderMenu('الترجمة',items);
}
function speedMenu(){
  renderMenu('سرعة التشغيل',SPEEDS.map(s=>({label:`${s}x`,active:state.speed===s,run:()=>{state.speed=s;video.playbackRate=s;$('speedButton').textContent=`${s}x`;}})));
}
function renderMenu(title,items){
  popup.innerHTML=`<div class="menu-title">${title}</div>`;
  items.forEach(it=>{const b=document.createElement('button');b.className='menu-item'+(it.active?' active':'');b.textContent=it.label;b.onclick=()=>{it.run();popup.classList.remove('open')};popup.appendChild(b)});
  popup.classList.add('open');
}

video.addEventListener('loadedmetadata',()=>{hideStatus();updateQuality();});
video.addEventListener('playing',()=>{hideStatus();setIcon('playIcon','i-pause');showControls();});
video.addEventListener('pause',()=>{setIcon('playIcon','i-play');showControls();});
video.addEventListener('waiting',()=>status('جاري التخزين المؤقت','يتم تحميل الجزء التالي'));
video.addEventListener('timeupdate',()=>{
  $('currentTime').textContent=fmt(video.currentTime);$('totalTime').textContent=fmt(video.duration);
  if(Number.isFinite(video.duration)&&video.duration>0){const p=video.currentTime/video.duration*100;$('seekBar').style.width=p+'%';$('seekThumb').style.left=p+'%';}
});
video.addEventListener('progress',()=>{
  if(video.buffered.length&&Number.isFinite(video.duration)&&video.duration>0)$('bufferBar').style.width=(video.buffered.end(video.buffered.length-1)/video.duration*100)+'%';
});
video.addEventListener('error',()=>{if(state.mode==='native')status('خطأ في التشغيل','صيغة المصدر غير مدعومة أو الرابط غير متاح',true);});

$('playButton').onclick=()=>video.paused?video.play().catch(()=>{}):video.pause();
$('backButton').onclick=()=>{if(Number.isFinite(video.currentTime))video.currentTime=Math.max(0,video.currentTime-10)};
$('forwardButton').onclick=()=>{if(Number.isFinite(video.currentTime))video.currentTime+=10};
$('muteButton').onclick=()=>{video.muted=!video.muted;setIcon('volumeIcon',video.muted?'i-muted':'i-volume')};
$('volumeSlider').oninput=e=>{video.volume=+e.target.value;video.muted=false;setIcon('volumeIcon','i-volume')};
$('speedButton').onclick=speedMenu;
$('subtitleButton').onclick=subtitleMenu;
$('qualityButton').onclick=qualityMenu;
$('fitButton').onclick=()=>{state.fit=(state.fit+1)%FITS.length;video.style.objectFit=FITS[state.fit]};
$('fullscreenButton').onclick=()=>document.fullscreenElement?document.exitFullscreen():enterLandscape();
$('closePlayer').onclick=closePlayer;
$('retryButton').onclick=()=>state.source&&openSource(state.source,state.currentItem,true);
$('seekTrack').addEventListener('pointerdown',e=>{state.dragging=true;const move=x=>{if(!Number.isFinite(video.duration))return;const r=$('seekTrack').getBoundingClientRect();video.currentTime=Math.max(0,Math.min(video.duration,video.duration*((x-r.left)/r.width)))};move(e.clientX);const mm=ev=>move(ev.clientX),up=()=>{state.dragging=false;window.removeEventListener('pointermove',mm);window.removeEventListener('pointerup',up)};window.addEventListener('pointermove',mm);window.addEventListener('pointerup',up)});
stage.addEventListener('pointermove',showControls);stage.addEventListener('click',e=>{if(!e.target.closest('button,.popup-menu,.seek-track,input'))showControls()});
document.addEventListener('fullscreenchange',requestLandscape);
document.addEventListener('keydown',e=>{if(!overlay.classList.contains('open'))return;if(e.code==='Space'){e.preventDefault();video.paused?video.play():video.pause()}if(e.code==='ArrowLeft')video.currentTime=Math.max(0,video.currentTime-10);if(e.code==='ArrowRight')video.currentTime+=10;if(e.code==='Escape'&&!document.fullscreenElement)closePlayer()});

setInterval(()=>{if(!overlay.classList.contains('open')||video.paused||state.dragging)return;if(video.readyState<3){jumpGap();if(state.mode==='hls'&&state.hls)try{state.hls.startLoad()}catch{}}},1800);

window.SPERSH_PLAYER={openItem,close:closePlayer,openSource:(src,item={title:'مشغل مباشر',type:'movie',stream:src})=>{overlay.classList.add('open');enterLandscape();return openSource(src,item,true)}};
})();
