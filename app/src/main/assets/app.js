'use strict';
const $=s=>document.querySelector(s);
const ls={get(k){try{return localStorage.getItem(k)}catch(e){return null}},set(k,v){try{localStorage.setItem(k,v)}catch(e){}},del(k){try{localStorage.removeItem(k)}catch(e){}}};

/* ---------- Data: Firebase RTDB (REST) atau lokal ---------- */
let DB=(ls.get('mp_db')||(window.Native?Native.dbUrl():'')||'').replace(/\/+$/,'');
const newKey=()=>String(Date.now()).padStart(14,'0')+Math.random().toString(36).slice(2,6);
const tree=()=>{try{return JSON.parse(ls.get('mp_tree')||'{}')}catch(e){return{}}};
const api={
  async get(p){
    if(!DB)return p.split('/').reduce((a,k)=>a&&a[k],tree())??null;
    const r=await fetch(`${DB}/${p}.json`);if(!r.ok)throw new Error(r.status);return r.json();
  },
  async put(p,v){
    if(!DB){const t=tree(),ks=p.split('/');let o=t;ks.slice(0,-1).forEach(k=>o=o[k]=o[k]||{});if(v===null)delete o[ks[ks.length-1]];else o[ks[ks.length-1]]=v;ls.set('mp_tree',JSON.stringify(t));return}
    const r=await fetch(`${DB}/${p}.json`,{method:'PUT',body:JSON.stringify(v)});if(!r.ok)throw new Error(r.status);
  },
  async msgs(after){
    if(!DB)return tree().msgs||{};
    const q=after?`orderBy=%22$key%22&startAt=%22${after}%22`:'orderBy=%22$key%22&limitToLast=300';
    const r=await fetch(`${DB}/msgs.json?${q}`);if(!r.ok)throw new Error(r.status);return r.json();
  }
};

/* ---------- State ---------- */
let me=null,peer=null,msgs=new Map(),lastKey='',inited=false,peerRead=0,peerTyping=false,tick=0,timer=null;
let names={},myRead={},typingSent=0,online=true;
try{myRead=JSON.parse(ls.get('mp_reads')||'{}')}catch(e){}
const toast=t=>{const e=$('#toast');e.textContent=t;e.classList.add('on');clearTimeout(toast.t);toast.t=setTimeout(()=>e.classList.remove('on'),3000)};
const label=id=>names[id]?names[id]+' #'+id:'ID #'+id;
const hm=t=>new Date(t).toLocaleTimeString('id-ID',{hour:'2-digit',minute:'2-digit'});
const dl=t=>new Date(t).toLocaleDateString('id-ID',{weekday:'long',day:'numeric',month:'short'}).toUpperCase();
function avatar(el,id){el.textContent=id;const h=330+(parseInt(id,10)*37)%70;el.style.background=`linear-gradient(135deg,hsl(${h} 100% 75%),hsl(${(h+40)%360} 90% 72%))`}
const show=id=>{for(const s of ['#login','#home','#chat'])$(s).classList.toggle('hide',s!==id)};

/* ---------- Login ---------- */
const pins=[...document.querySelectorAll('#pin input')];
const ready=()=>{$('#enter').disabled=pins.some(p=>!p.value)};
pins.forEach((p,i)=>{
  p.addEventListener('input',()=>{p.value=p.value.replace(/\D/g,'').slice(-1);if(p.value&&pins[i+1])pins[i+1].focus();ready()});
  p.addEventListener('keydown',e=>{if(e.key==='Backspace'&&!p.value&&pins[i-1])pins[i-1].focus();if(e.key==='Enter')login()});
  p.addEventListener('paste',e=>{const d=(e.clipboardData.getData('text')||'').replace(/\D/g,'').slice(0,4);if(!d)return;e.preventDefault();pins.forEach((q,j)=>q.value=d[j]||'');pins[Math.min(d.length,3)].focus();ready()});
});
$('#rand').onclick=()=>{const n=String(Math.floor(1000+Math.random()*9000));pins.forEach((p,i)=>p.value=n[i]);ready()};
$('#dburl').value=DB;
$('#enter').onclick=login;
function login(){
  const id=pins.map(p=>p.value).join('');
  if(!/^\d{4}$/.test(id)){$('#lerr').textContent='Isi 4 digit angka untuk ID-mu.';return}
  const url=$('#dburl').value.trim().replace(/\/+$/,'');
  if(url&&!/^https:\/\//.test(url)){$('#lerr').textContent='URL server harus diawali https://';return}
  DB=url;ls.set('mp_db',url);
  ls.set('mp_id',id);const n=$('#nick').value.trim();if(n)ls.set('mp_nick',n.slice(0,16));
  start(id);
}
$('#out').onclick=()=>{clearInterval(timer);ls.del('mp_id');me=peer=null;msgs=new Map();lastKey='';inited=false;pins.forEach(p=>p.value='');ready();show('#login')};

/* ---------- Mulai & sinkronisasi ---------- */
async function start(id){
  me=id;show('#home');
  const nick=ls.get('mp_nick');
  if(nick){names[me]=nick;try{await api.put('names/'+me,nick)}catch(e){}}
  await sync();
  clearInterval(timer);timer=setInterval(sync,2000);
}
document.addEventListener('visibilitychange',()=>{if(!document.hidden)sync()});
window.addEventListener('storage',e=>{if(e.key==='mp_tree')sync()});
async function sync(){
  if(!me)return;
  try{
    const d=await api.msgs(lastKey);
    for(const [k,v] of Object.entries(d||{})){
      if(lastKey&&k<=lastKey)continue;
      if(v&&(v.from===me||v.to===me)){
        msgs.set(k,{...v,id:k});
        if(inited&&v.from!==me&&(v.from!==peer||document.hidden))toast('Pesan baru dari '+label(v.from));
      }
      if(k>lastKey)lastKey=k;
    }
    inited=true;
    if(peer){
      peerRead=(await api.get(`reads/${peer}_${me}`))||0;
      const t=await api.get(`typing/${peer}_${me}`);peerTyping=!!t&&Date.now()-t<4500;
    }
    if(tick++%3===0)await loadNames();
    await pushRead();
    online=true;
  }catch(e){online=false}
  render();
}
async function loadNames(){
  const ids=new Set(convos().map(c=>c.p));if(peer)ids.add(peer);
  for(const id of ids){const n=await api.get('names/'+id);if(typeof n==='string')names[id]=n.slice(0,16)}
}
async function pushRead(){
  if(!peer||document.hidden)return;
  const last=[...msgs.values()].filter(m=>m.from===peer).pop();
  if(last&&(myRead[peer]||0)<last.ts){myRead[peer]=last.ts;ls.set('mp_reads',JSON.stringify(myRead));await api.put(`reads/${me}_${peer}`,last.ts)}
}

/* ---------- Render ---------- */
function convos(){
  const m={};for(const x of [...msgs.values()].sort((a,b)=>a.ts-b.ts)){const p=x.from===me?x.to:x.from;(m[p]=m[p]||[]).push(x)}
  return Object.entries(m).map(([p,a])=>({p,last:a[a.length-1],unread:a.filter(x=>x.from===p&&x.ts>(myRead[p]||0)).length})).sort((a,b)=>b.last.ts-a.last.ts);
}
function render(){
  $('#dot').classList.toggle('off',!online);
  $('#mestat').textContent=(names[me]?names[me].toUpperCase()+' ':'')+'#'+me+(online?(DB?' · ONLINE':' · LOKAL'):' · OFFLINE');
  renderList();if(peer)renderChat();
}
function renderList(){
  const l=$('#list');l.replaceChildren();
  const cs=convos();
  if(!cs.length){const e=document.createElement('div');e.className='empty';e.textContent='Belum ada percakapan. Masukkan ID temanmu di atas.';l.append(e)}
  let total=0;
  for(const c of cs){
    total+=c.unread;
    const r=document.createElement('button');r.className='row';
    const a=document.createElement('div');a.className='av';avatar(a,c.p);
    const m=document.createElement('div');m.className='meta';
    const b=document.createElement('b');b.textContent=label(c.p);
    const s=document.createElement('span');s.textContent=(c.last.from===me?'Kamu: ':'')+c.last.text;
    m.append(b,s);
    const sd=document.createElement('div');sd.className='side-r';sd.textContent=hm(c.last.ts);
    if(c.unread){const g=document.createElement('div');g.className='badge';g.textContent=c.unread;sd.append(g)}
    r.append(a,m,sd);r.onclick=()=>open(c.p);l.append(r);
  }
  document.title=(total?'('+total+') ':'')+'Sakura Pesan';
}
function open(p){peer=p;peerRead=0;peerTyping=false;renderChat.fresh=null;show('#chat');renderChat();sync()}
function back(){peer=null;show('#home');render()}
window.mpBack=()=>{if(peer){back();return true}return false};
function renderChat(){
  const box=$('#msgs');
  avatar($('#cav'),peer);
  $('#cname').textContent=label(peer);$('#csub').textContent=names[peer]?'ID #'+peer:'Chat lewat ID, tanpa nomor telepon';
  $('#typing').classList.toggle('hide',!peerTyping);
  const near=box.scrollHeight-box.scrollTop-box.clientHeight<140;
  box.replaceChildren();
  let day='';
  const mm=[...msgs.values()].filter(m=>m.from===peer||m.to===peer).sort((a,b)=>a.ts-b.ts);
  if(!mm.length){const e=document.createElement('div');e.className='empty';e.style.margin='auto';e.textContent='Belum ada pesan. Sapa '+label(peer)+' sekarang ✧';box.append(e)}
  for(const m of mm){
    const d=dl(m.ts);
    if(d!==day){day=d;const x=document.createElement('div');x.className='day';x.textContent=d;box.append(x)}
    const b=document.createElement('div');b.className='b '+(m.from===me?'mine':'their');
    b.append(document.createTextNode(m.text));
    const s=document.createElement('small');
    s.textContent=hm(m.ts)+(m.from===me?(peerRead>=m.ts?'  ✓✓ DIBACA':'  ✓ TERKIRIM'):'');
    b.append(s);box.append(b);
  }
  if(near||renderChat.fresh!==peer){box.scrollTop=box.scrollHeight;renderChat.fresh=peer}
}

/* ---------- Interaksi ---------- */
$('#newf').onsubmit=e=>{
  e.preventDefault();const v=$('#newid').value.trim();
  if(!/^\d{4}$/.test(v)){toast('ID tujuan harus 4 digit angka.');return}
  if(v===me){toast('Itu ID-mu sendiri. Masukkan ID teman.');return}
  $('#newid').value='';open(v);
};
$('#newid').addEventListener('input',e=>e.target.value=e.target.value.replace(/\D/g,''));
$('#back').onclick=back;
$('#copy').onclick=async()=>{try{await navigator.clipboard.writeText(me);toast('ID '+me+' disalin.')}catch(e){toast('ID-mu: '+me)}};
$('#rename').onclick=async()=>{
  const n=prompt('Nama panggilan (maks 16 huruf):',names[me]||'');
  if(n===null)return;
  const v=n.trim().slice(0,16);names[me]=v;ls.set('mp_nick',v);
  try{await api.put('names/'+me,v||null);toast('Nama diperbarui.')}catch(e){toast('Nama gagal disimpan ke server.')}
  render();
};
$('#sms').onclick=()=>{location.href='sms:?&body='+encodeURIComponent('Chat denganku di Sakura Pesan. ID-ku: '+me)};
const ta=$('#ta');
function grow(){ta.style.height='auto';ta.style.height=Math.min(ta.scrollHeight,120)+'px';$('#cnt').textContent=ta.value.length>400?ta.value.length+'/500':''}
ta.addEventListener('input',()=>{
  grow();
  if(peer&&ta.value&&Date.now()-typingSent>2000){typingSent=Date.now();api.put(`typing/${me}_${peer}`,Date.now()).catch(()=>{})}
});
ta.addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();send()}});
$('#sendb').onclick=send;
async function send(){
  const t=ta.value.trim();
  if(!peer||!t)return;
  ta.value='';grow();
  try{await api.put('msgs/'+newKey(),{from:me,to:peer,text:t.slice(0,500),ts:Date.now()});typingSent=0;await sync()}
  catch(e){ta.value=t;grow();toast('Pesan gagal terkirim. Periksa koneksi lalu coba lagi.')}
}

/* ---------- Boot ---------- */
(function(){const s=ls.get('mp_id');if(/^\d{4}$/.test(s||''))start(s);else pins[0].focus()})();
